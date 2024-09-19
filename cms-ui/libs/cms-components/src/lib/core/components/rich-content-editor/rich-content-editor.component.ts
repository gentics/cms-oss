import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, NgZone, ViewChild } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ModalCloseError, ModalClosingReason } from '@gentics/cms-integration-api-models';
import { BaseFormElementComponent, cancelEvent, generateFormProvider, ModalService } from '@gentics/ui-core';
import { CLASS_RICH_ELEMENT, RichContent, RichContentType } from '../../../common/models';
import {
    elementToRichContentString,
    extractRichContent,
    extractRichContentFromElement,
    findParentRichElement,
    richContentToHtml,
    updateElementWithContent,
} from '../../../common/utils/rich-content';
import { RichContentModal } from '../rich-content-modal/rich-content-modal.component';

enum UpdateQueue {
    /** If no update on blur has to be performed */
    NONE,
    /** If the value from the DOM should be used to trigger a change */
    FROM_DOM,
    /** If the value from the component should be used to update the DOM */
    FROM_VALUE,
}

/**
 * Component which allows "rich content" to be edited.
 * In our case, rich content currently is only a link to a page/file.
 * Format for placeholders are generally `{{CONTENT_TYPE|...DATA}}`.
 *
 * Rich content is automatically getting converted back and forth here and displayed to the user
 * with an appropiate editor/input.
 */
@Component({
    selector: 'gtx-rich-content-editor',
    templateUrl: './rich-content-editor.component.html',
    styleUrls: ['./rich-content-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(RichContentEditorComponent)],
})
export class RichContentEditorComponent extends BaseFormElementComponent<string> implements AfterViewInit {

    public readonly RichContentType = RichContentType;

    @ViewChild('container')
    public containerRef: ElementRef<HTMLDivElement>;

    public rawHtml: SafeHtml;

    public activeElement: HTMLElement | null = null;
    public activeContent: RichContent | null = null;

    public focused = false;
    /**
     * Value updates only occur when the element is not focused/on blur.
     * This is to prevent jittering when the user is editing the content,
     * and it is getting updated/replaced.
     */
    protected queuedUpdate: UpdateQueue = UpdateQueue.NONE;

    constructor(
        changeDetector: ChangeDetectorRef,
        private sanitizer: DomSanitizer,
        private zone: NgZone,
        private modals: ModalService,
    ) {
        super(changeDetector);
    }

    public ngAfterViewInit(): void {
        this.zone.runOutsideAngular(() => {
            this.containerRef.nativeElement.ownerDocument.addEventListener('selectionchange', () => {
                if (this.updateActiveElementFromSelection()) {
                    this.changeDetector.markForCheck();
                }
            });
        });
    }

    protected onValueChange(): void {
        if (this.focused) {
            this.queuedUpdate = UpdateQueue.FROM_VALUE;
            return;
        }

        this.updateHtmlFromValue();
    }

    protected updateHtmlFromValue(): void {
        const extracted = extractRichContent(this.value || '');
        const html = extracted.map(elem => {
            return typeof elem === 'string' ? elem : richContentToHtml(elem);
        });

        this.rawHtml = this.sanitizer.bypassSecurityTrustHtml(html.join(''));
    }

    protected updateValueFromDom(): void {
        let str = '';

        const children = Array.from(this.containerRef.nativeElement.childNodes);
        for (const node of children) {
            if (
                node.nodeType !== Node.ELEMENT_NODE
                || !(node as HTMLElement).classList.contains(CLASS_RICH_ELEMENT)
            ) {
                str += node.textContent;
                continue;
            }

            str += elementToRichContentString(node as HTMLElement);
        }

        this.triggerChange(str);
    }

    /**
     * Updates the activeElement and activeType based on the current selection/cursor position.
     * @returns If anything has actually changed
     */
    protected updateActiveElementFromSelection(): boolean {
        const doc = this.containerRef.nativeElement.ownerDocument;
        const selection = doc.getSelection();

        // If none or multiple ranges are available, we can't determine which type
        // is actually currently active.
        if (selection.rangeCount !== 1 || !this.focused) {
            if (this.activeElement != null || this.activeContent != null) {
                this.activeElement = null;
                this.activeContent = null;
                return true;
            }

            return false;
        }

        const range = selection.getRangeAt(0);
        let elem = findParentRichElement(range.startContainer, this.containerRef.nativeElement);
        if (!elem && !range.collapsed) {
            elem = findParentRichElement(range.endContainer, this.containerRef.nativeElement);
        }

        if (!elem) {
            if (this.activeElement != null || this.activeContent != null) {
                this.activeElement = null;
                this.activeContent = null;
                return true;
            }
            return false;
        }

        if (this.activeElement === elem) {
            return false;
        }

        this.activeContent = extractRichContentFromElement(elem);
        this.activeElement = elem;
        return true;
    }

    public handleFocus(): void {
        this.focused = true;
        this.updateActiveElementFromSelection();
    }

    public handleBlur(): void {
        this.focused = false;
        this.activeElement = null;
        this.activeContent = null;

        switch (this.queuedUpdate) {
            case UpdateQueue.FROM_VALUE:
                this.updateHtmlFromValue();
                break;

            case UpdateQueue.FROM_DOM:
                this.updateValueFromDom();
                break;

            default:
                break;
        }
    }

    public handleContentChange(): void {
        this.queuedUpdate = UpdateQueue.FROM_DOM;
    }

    protected async manageContentElement(type: RichContentType, content?: RichContent): Promise<RichContent> {
        const modal = await this.modals.fromComponent(RichContentModal, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
        }, {
            type,
            content,
        });

        try {
            const content = await modal.open();
            return content;
        } catch (err) {
            // Simple modal close, ignore
            if (err instanceof ModalCloseError && err.reason !== ModalClosingReason.ERROR) {
                return null;
            }

            // TODO: Pass to error handler?
            console.error(err);
            return null;
        }
    }

    public async addNewContent(type: RichContentType, event?: Event): Promise<void> {
        cancelEvent(event);

        const container = this.containerRef.nativeElement;
        const doc = container.ownerDocument;
        const range = doc.getSelection().getRangeAt(0).cloneRange();

        const content = await this.manageContentElement(type);
        if (content == null) {
            return;
        }

        const template = doc.createElement('template');
        template.innerHTML = richContentToHtml(content);
        range.surroundContents(template.content.firstElementChild);
        container.focus();
    }

    public async editCurrentContent(event?: Event): Promise<void> {
        cancelEvent(event);

        const content = await this.manageContentElement(this.activeContent.type, this.activeContent);
        if (content == null) {
            return;
        }

        updateElementWithContent(this.activeElement, content);
    }

    public deleteCurrentContent(event?: Event): void {
        cancelEvent(event);

        const content = this.activeElement.textContent;
        this.activeElement.replaceWith(content);
    }
}

import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Input, NgZone, ViewChild } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ModalCloseError, ModalClosingReason } from '@gentics/cms-integration-api-models';
import { BaseFormElementComponent, cancelEvent, generateFormProvider, ModalService } from '@gentics/ui-core';
import { ATTR_CONTENT_TYPE, LINK_DEFAULT_DISPLAY_VALUE, RichContent, RichContentType } from '../../../common/models';
import {
    elementToRichContentString,
    extractRichContent,
    extractRichContentFromElement,
    findParentRichElement,
    normalizeWhitespaces,
    removeNewLines,
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

    @Input()
    public allowMultiLine = false;

    @ViewChild('container', { static: true })
    public containerRef: ElementRef<HTMLDivElement>;

    public rawHtml: SafeHtml;

    public activeElement: HTMLElement | null = null;
    public activeContent: RichContent | null = null;

    public focused = false;
    /** Internal flag to not check for seleciton changes for a while */
    protected ignoreSelectionChanges = false;
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
        this.booleanInputs.push('allowMultiLine');
    }

    public ngAfterViewInit(): void {
        this.zone.runOutsideAngular(() => {
            this.containerRef.nativeElement.ownerDocument.addEventListener('selectionchange', () => {
                if (this.ignoreSelectionChanges) {
                    return;
                }

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
        let html = extracted.map(elem => {
            return typeof elem === 'string' ? elem : richContentToHtml(elem);
        }).join('');

        if (!this.allowMultiLine) {
            html = removeNewLines(html);
        }

        this.rawHtml = this.sanitizer.bypassSecurityTrustHtml(html);
        this.queuedUpdate = UpdateQueue.NONE;
    }

    protected updateValueFromDom(): void {
        let str = '';

        const children = Array.from(this.containerRef.nativeElement.childNodes);
        for (const node of children) {
            if (
                node.nodeType !== Node.ELEMENT_NODE
                || !(node as HTMLElement).hasAttribute(ATTR_CONTENT_TYPE)
            ) {
                str += node.textContent;
                continue;
            }

            str += elementToRichContentString(node as HTMLElement);
        }

        // Edge-Case: Sometimes the editor/browser will use non-breaking space instead of a regular
        // space, to make relations between nodes more apparent.
        // This breaks tests and is quite unhelpful in 99% of cases, which is why we replace it with
        // regular spaces here.
        str = normalizeWhitespaces(str);

        // And if there's no multi-lines allowed, we replace line-breaks and <br> elements with a whitespace instead.
        if (!this.allowMultiLine) {
            str = removeNewLines(str);
        }

        this.triggerChange(str);
        this.queuedUpdate = UpdateQueue.NONE;
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
        if (!this.ignoreSelectionChanges) {
            this.updateActiveElementFromSelection();
        }
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

    public handleKeyDown(event: KeyboardEvent): void {
        if (this.allowMultiLine) {
            return;
        }
        // Prevent creating a new line with enter.
        // When using the ctrl key however, it's a request to submit the form (if available),
        // which should still be allowed.
        if (event.key === 'Enter' && !event.ctrlKey) {
            cancelEvent(event);
        }
    }

    public handleContentChange(): void {
        this.queuedUpdate = UpdateQueue.FROM_DOM;
    }

    protected async manageContentElement(
        type: RichContentType,
        content?: RichContent,
        enterDisplayText: boolean = false,
    ): Promise<RichContent> {
        this.ignoreSelectionChanges = true;
        const doc = this.containerRef.nativeElement.ownerDocument;
        const range = doc.getSelection().getRangeAt(0).cloneRange();

        const modal = await this.modals.fromComponent(RichContentModal, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
        }, {
            type,
            content,
            enterLinkDisplayText: enterDisplayText,
        });

        try {
            const content = await modal.open();

            // Restore selection
            this.containerRef.nativeElement.focus();
            doc.getSelection().removeAllRanges();
            doc.getSelection().addRange(range);
            this.ignoreSelectionChanges = false;

            return content;
        } catch (err) {
            // Restore selection
            this.containerRef.nativeElement.focus();
            doc.getSelection().removeAllRanges();
            doc.getSelection().addRange(range);
            this.ignoreSelectionChanges = false;

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

        const content = await this.manageContentElement(type, null, true);
        if (content == null) {
            return;
        }

        // Default the link content to something, otherwise an empty link element can't be properly edited.
        if (!content.displayText) {
            content.displayText = LINK_DEFAULT_DISPLAY_VALUE;
        }

        const template = doc.createElement('template');
        template.innerHTML = richContentToHtml(content);
        if (range.collapsed) {
            range.insertNode(template.content.firstElementChild);
        } else {
            range.surroundContents(template.content.firstElementChild);
        }
        container.focus();
        this.updateValueFromDom();
    }

    public async editCurrentContent(event?: Event): Promise<void> {
        cancelEvent(event);

        if (!this.activeContent || !this.activeElement) {
            return;
        }

        const element = this.activeElement;
        const content = await this.manageContentElement(this.activeContent.type, this.activeContent);
        if (content == null) {
            return;
        }

        updateElementWithContent(element, content);
        this.updateValueFromDom();
    }

    public deleteCurrentContent(event?: Event): void {
        cancelEvent(event);

        if (!this.activeContent || !this.activeElement) {
            return;
        }

        const content = this.activeElement.textContent;
        this.activeElement.replaceWith(content);
        this.updateValueFromDom();
    }
}

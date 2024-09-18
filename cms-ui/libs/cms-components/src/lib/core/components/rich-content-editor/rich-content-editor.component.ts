import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, NgZone, ViewChild } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { RichContentLink, RichContentType } from '../../../common/models';
import { extractRichContentLinks, toRichContentLinkTemplate } from '../../../common/utils/rich-content';

const CLASS_RICH_ELEMENT = 'rich-element';
const CLASS_ITEM_LINK = 'item-link';
const ATTR_CONTENT_TYPE = 'data-rich-content-type';
const ATTR_LINK_TYPE = 'data-link-type';
const ATTR_NODE_ID = 'data-node-id';
const ATTR_ITEM_ID = 'data-item-id';
const ATTR_LANG_CODE = 'data-lang-code';
const ATTR_TARGET = 'data-link-target';

enum UpdateQueue {
    /** If no update on blur has to be performed */
    NONE,
    /** If the value from the DOM should be used to trigger a change */
    FROM_DOM,
    /** If the value from the component should be used to update the DOM */
    FROM_VALUE,
}

function linkToAnchorString(link: RichContentLink): string {
    return `<span
        class="${CLASS_RICH_ELEMENT} ${CLASS_ITEM_LINK}"
        ${ATTR_CONTENT_TYPE}="${RichContentType.LINK}"
        ${ATTR_LINK_TYPE}="${link.linkType}"
        ${ATTR_NODE_ID}="${link.nodeId}"
        ${ATTR_ITEM_ID}="${link.itemId}"
        ${ATTR_LANG_CODE}="${link.langCode || ''}"
        ${ATTR_TARGET}="${link.target || ''}"
    >${link.displayText}</span>`
}

function getLinkFromElement(element: HTMLElement): RichContentLink {
    return {
        type: RichContentType.LINK,
        linkType: element.getAttribute(ATTR_LINK_TYPE) as any,
        nodeId: element.getAttribute(ATTR_NODE_ID),
        itemId: element.getAttribute(ATTR_ITEM_ID),
        langCode: element.getAttribute(ATTR_LANG_CODE),
        target: element.getAttribute(ATTR_TARGET),
        displayText: element.textContent,
    };
}

function anchorToTemplateString(element: HTMLElement): string {
    const link: RichContentLink = getLinkFromElement(element);
    return toRichContentLinkTemplate(link);
}

function findParentRichElement(start: Node, boundary: HTMLElement): HTMLElement | null {
    let elem = start;

    while (elem != null) {
        if (elem.nodeType !== Node.ELEMENT_NODE) {
            elem = elem.parentElement;
            continue;
        }

        // Max reached
        if (elem === boundary) {
            elem = null;
            break;
        }

        if ((elem as HTMLElement).classList.contains(CLASS_RICH_ELEMENT)) {
            break;
        }

        elem = elem.parentElement;
    }

    return elem as HTMLElement;
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
    public activeType: RichContentType | null = null;
    public activeLink: RichContentLink | null = null;

    protected focused = false;
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
        const extracted = extractRichContentLinks(this.value || '');
        const html = extracted.map(elem => {
            return typeof elem === 'string' ? elem : linkToAnchorString(elem);
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

            const elem = node as HTMLElement;
            const type = elem.getAttribute(ATTR_CONTENT_TYPE) as RichContentType;

            switch (type) {
                case RichContentType.LINK:
                    str += anchorToTemplateString(elem);
                    break;

                default:
                    str += elem.textContent;
                    break;
            }
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
            if (this.activeElement != null || this.activeType != null) {
                this.activeElement = null;
                this.activeType = null;
                this.activeLink = null;
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
            if (this.activeElement != null || this.activeType != null) {
                this.activeElement = null;
                this.activeType = null;
                this.activeLink = null;
                return true;
            }
            return false;
        }

        if (this.activeElement === elem) {
            return false;
        }

        const type = elem.getAttribute(ATTR_CONTENT_TYPE) as RichContentType;

        switch (type) {
            case RichContentType.LINK:
                this.activeElement = elem;
                this.activeType = RichContentType.LINK;
                this.activeLink = getLinkFromElement(elem);
                console.log('found link', this.activeLink, this.activeElement);
                return true;

            default:
                if (this.activeElement != null || this.activeType != null) {
                    this.activeElement = null;
                    this.activeType = null;
                    this.activeLink = null;
                    return true;
                }
                return false;
        }
    }

    public handleFocus(): void {
        this.focused = true;
        this.updateActiveElementFromSelection();
    }

    public handleBlur(): void {
        this.focused = false;
        this.activeElement = null;
        this.activeType = null;
        this.activeLink = null;

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
}

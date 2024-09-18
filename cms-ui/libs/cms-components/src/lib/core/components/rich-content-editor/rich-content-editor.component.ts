import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { extractLinks } from '../../../common/utils/link-extraction';

const CLASS_ITEM_LINK = 'item-link';
const ATTR_NODE_ID = 'data-node-id';
const ATTR_ITEM_ID = 'data-item-id';
const ATTR_LANG_CODE = 'data-lang-code';
const ATTR_TARGET = 'target';

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
export class RichContentEditorComponent extends BaseFormElementComponent<string> {

    public rawHtml: SafeHtml;

    constructor(
        changeDetector: ChangeDetectorRef,
        private sanitizer: DomSanitizer,
    ) {
        super(changeDetector);
    }

    protected onValueChange(): void {
        const extracted = extractLinks(this.value || '');
        const html = extracted.map(elem => {
            if (typeof elem === 'string') {
                return elem;
            }

            return `<a
                class="${CLASS_ITEM_LINK}"
                ${ATTR_NODE_ID}="${elem.nodeId}"
                ${ATTR_ITEM_ID}="${elem.pageId}"
                ${ATTR_LANG_CODE}="${elem.langCode || ''}"
                ${ATTR_TARGET}="${elem.target || ''}"
            >${elem.displayText}</a>`;
        });

        this.rawHtml = this.sanitizer.bypassSecurityTrustHtml(html.join(''));
    }

    public handleContentChange(event: Event): void {
        debugger;
    }
}

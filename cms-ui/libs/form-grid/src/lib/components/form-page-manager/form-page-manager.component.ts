import { ChangeDetectionStrategy, Component, input, model, output } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { FormElement, FormSchema, FormUISchema } from '@gentics/cms-models';
import { ISortableEvent, ModalService, SortableGroup } from '@gentics/ui-core';
import { ATTR_ELEMENT_ID, ElementInterPageMoveEvent, ElementMoveData, FormGridEditMode } from '../../models';

function collectElementIds(elements: FormElement[]): string[] {
    const ids: string[] = [];
    for (const el of elements) {
        ids.push(el.id);
        if (Array.isArray(el.elements)) {
            ids.push(...collectElementIds(el.elements));
        }
    }
    return ids;
}

@Component({
    selector: 'gtx-form-page-manager',
    templateUrl: './form-page-manager.component.html',
    styleUrls: ['./form-page-manager.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormPageManagerComponent {

    public readonly FormGridEditMode = FormGridEditMode;

    public readonly uiSchema = model.required<FormUISchema>();
    public readonly pageIndex = model.required<number>();
    /** Optional: required for schema cleanup when deleting pages. */
    public readonly schema = model<FormSchema | null>(null);
    public readonly mode = input<FormGridEditMode>(FormGridEditMode.NONE);
    public readonly elementMoving = input<ElementMoveData | null>(null);

    public readonly elementInterPageMove = output<ElementInterPageMoveEvent>();
    public readonly editPage = output<number>();

    /** Shared configuration for all page entries */
    public readonly sortableGroup: SortableGroup = {
        name: 'form-pages',
        pull: false,
        put: (to, from) => {
            if (this.mode() !== FormGridEditMode.FULL) {
                return false;
            }
            if (from.option('group') === 'form-palette') {
                return false;
            }
            if (this.elementMoving()?.inserting) {
                return false;
            }

            // Moving to the current page should not be allowed
            try {
                const toPageIdx = parseInt(to.el.getAttribute('data-page-index') ?? '', 10);
                return Number.isInteger(toPageIdx) && toPageIdx !== this.pageIndex();
            } catch (err) {
                return false;
            }
        },
    };

    constructor(
        private modals: ModalService,
        private i18n: I18nService,
    ) {}

    public addPage(): void {
        const copy = structuredClone(this.uiSchema());
        copy.pages.push({ pagename: {}, elements: [] });
        this.uiSchema.set(copy);
        this.pageIndex.set(copy.pages.length - 1);
    }

    public deletePage(index: number): void {
        const page = this.uiSchema().pages[index];
        if (page.elements.length > 0) {
            this.modals.dialog({
                title: this.i18n.instant('form_grid.page_delete_title'),
                body: this.i18n.instant('form_grid.page_delete_confirm'),
                buttons: [
                    {
                        id: 'confirm',
                        type: 'alert',
                        label: this.i18n.instant('common.delete_button'),
                        returnValue: true,
                    },
                    {
                        id: 'cancel',
                        type: 'secondary',
                        label: this.i18n.instant('common.cancel_button'),
                        returnValue: false,
                    },
                ],
            })
                .then((dialog) => dialog.open())
                .then((confirmed) => {
                    if (confirmed) {
                        this.removePageAndCleanup(index);
                    }
                });
        } else {
            this.removePageAndCleanup(index);
        }
    }

    public moveElementToPage(event: ISortableEvent, targetPageIndex: number): void {
        if (this.mode() !== FormGridEditMode.FULL) {
            return;
        }
        const elementId = event.item.getAttribute(ATTR_ELEMENT_ID);
        if (!elementId) {
            return;
        }
        const fromPage = this.pageIndex();
        if (fromPage === targetPageIndex) {
            return;
        }
        this.elementInterPageMove.emit({ elementId, fromPage, toPage: targetPageIndex });
        // Remove the item from the DOM, as we don't need it in the page entry and it'll be rendered by the elements-container
        event.item.remove();
    }

    private removePageAndCleanup(index: number): void {
        const uiCopy = structuredClone(this.uiSchema());

        // Collect and remove all element IDs from the deleted page
        const currentSchema = this.schema();
        if (currentSchema != null) {
            const schemaCopy = structuredClone(currentSchema);
            const idsToRemove = collectElementIds(uiCopy.pages[index].elements);
            for (const id of idsToRemove) {
                delete schemaCopy.properties?.[id];
            }
            this.schema.set(schemaCopy);
        }

        // Remove the page
        uiCopy.pages.splice(index, 1);

        // Fix uiSchemaPage references for remaining pages
        for (let p = index; p < uiCopy.pages.length; p++) {
            for (const el of uiCopy.pages[p].elements) {
                el.uiSchemaPage = p;
            }
        }

        // Navigate to nearest valid page
        const newIndex = Math.min(index, uiCopy.pages.length - 1);

        this.uiSchema.set(uiCopy);
        this.pageIndex.set(Math.max(0, newIndex));
    }
}

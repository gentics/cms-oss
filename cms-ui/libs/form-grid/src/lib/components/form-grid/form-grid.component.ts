import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    HostListener,
    input,
    model,
    OnDestroy,
    OnInit,
    signal,
} from '@angular/core';
import { I18nNotificationService, I18nService } from '@gentics/cms-components';
import {
    FormElement,
    FormPage,
    FormSchema,
    FormSchemaProperty,
    FormTypeConfiguration,
    FormUISchema,
    I18nString,
} from '@gentics/cms-models';
import { BaseComponent, cancelEvent, ISortableEvent, SortableGroup } from '@gentics/ui-core';
import { TranslateStore } from '@ngx-translate/core';
import { v4 as uuidV4 } from 'uuid';
import {
    CLIPBOARD_MIME,
    CLIPBOARD_STORAGE_KEY,
    ElementContainerMoveEvent,
    ElementInterPageMoveEvent,
    ElementMoveData,
    ElementSelectionEvent,
    FormGridClipboardData,
    FormGridEditMode,
    FormGridViewMode,
} from '../../models';

function addElementsToMap(data: Record<string, FormElement>, elements: FormElement[]): void {
    for (const el of elements) {
        if (data[el.id]) {
            console.error(`An element with ID ${el.id} already exists in the map, which indicates that elements with the same ID are duplicated!`);
        }
        data[el.id] = el;

        if (Array.isArray(el.elements)) {
            addElementsToMap(data, el.elements);
        }
    }
}

function moveNestedElement(
    currentContainerId: null | string,
    entries: FormElement[],
    element: FormElement,
    fromId: string,
    toId: string,
    targetIndex: number,
): { removed: boolean; added: boolean } {
    let removed = false;
    let added = false;

    if (currentContainerId === fromId) {
        const idx = entries.findIndex((inner) => inner.id === element.id);
        if (idx !== -1) {
            entries.splice(idx, 1);
            removed = true;
        }
    }
    if (currentContainerId === toId) {
        entries.splice(targetIndex, 0, element);
        added = true;
    }

    // eslint-disable-next-line @typescript-eslint/prefer-for-of
    for (let i = 0; i < entries.length; i++) {
        if (removed && added) {
            break;
        }
        const cur = entries[i];
        if (!cur.elements) {
            continue;
        }
        const res = moveNestedElement(cur.id, cur.elements, element, fromId, toId, targetIndex);
        removed = removed || res.removed;
        added = added || res.added;
    }

    return { removed, added };
}

enum EditTabs {
    DEFINITION = 'definition',
    SETTINGS = 'settings',
    TRANSLATIONS = 'translations',
}

@Component({
    selector: 'gtx-form-grid',
    templateUrl: './form-grid.component.html',
    styleUrls: ['./form-grid.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormGridComponent extends BaseComponent implements OnInit, OnDestroy {

    public readonly ELEMENT_ROOT_CONTAINER_ID = uuidV4();
    public readonly EditTabs = EditTabs;
    public readonly FormGridEditMode = FormGridEditMode;
    public readonly FormGridViewMode = FormGridViewMode;

    public readonly paletteGroup: SortableGroup = {
        name: 'form-palette',
        pull: 'clone',
        put: false,
    };

    /* INPUTS / OUTPUTS
     * ===================================================================== */

    /** The config for this form-grid */
    public readonly config = input.required<FormTypeConfiguration>();
    /** Which mode this form-grid operates in. */
    public readonly mode = input.required<FormGridEditMode>();
    /** All languages of the current form */
    public readonly languages = input.required<string[]>();

    /** Schema of the current form */
    public readonly schema = model.required<FormSchema>();
    /** UI-Schema of the current form */
    public readonly uiSchema = model.required<FormUISchema>();
    /** Index of the currently viewed form page */
    public readonly pageIndex = model.required<number>();
    /** Which content to display */
    public readonly viewMode = input.required<FormGridViewMode>();

    /** The ID of the form currently being edited */
    public readonly formId = input<number | null>(null);
    /** The type of the form currently being edited */
    public readonly formType = input<string>('');
    /** The name of the form currently being edited */
    public readonly formName = input<string>('');

    /* PAGE & VIEW
     * ===================================================================== */

    /** All currently visible items, i.E. all elements from the current form page */
    public readonly elements = computed<FormElement[]>(() => {
        return this.uiSchema()?.pages?.[this.pageIndex()]?.elements || [];
    });

    /** Mapping of ID to each element in a flat map for the current page */
    public readonly elementMap = computed<Record<string, FormElement>>(() => {
        const data: Record<string, FormElement> = {};

        for (const page of (this.uiSchema()?.pages || [])) {
            if (page?.elements?.length > 0) {
                addElementsToMap(data, page.elements);
            }
        }

        return data;
    });

    /** Whether the selected element has any missing translations across all form languages */
    public hasMissingTranslations = signal(false);
    /** The type of the current element which is being dragged. If null, then nothing is being dragged right now. */
    public elementMoving = signal<ElementMoveData | null>(null);

    /* PAGE EDITING
     * ===================================================================== */

    /** Index of the page currently being edited in the sidebar (null = none) */
    public readonly editingPageIndex = signal<number | null>(null);
    /** Selected language for page name editing */
    public readonly selectedPageLanguage = signal<string>('');
    /** The page currently being edited */
    public readonly editingPage = computed<FormPage | null>(() => {
        const idx = this.editingPageIndex();
        return idx == null ? null : (this.uiSchema()?.pages?.[idx] ?? null);
    });

    /** If the left sidebar is expanded */
    public readonly leftSidebarExpanded = signal(true);
    /** If the right sidebar is expanded */
    public readonly rightSidebarExpanded = signal(false);

    /* SELECTION & EDITING
     * ===================================================================== */

    /** The ID of the current selected element. */
    public readonly selectedElementId = signal<string | null>(null);
    /** The ID of the container where the selectedElement is contained in. */
    public selectedElementContainerId: string | null = null;

    /** The currently selected element, which may be getting edited. */
    public readonly selectedElement = computed(() => {
        const id = this.selectedElementId();
        return id == null ? undefined : this.elementMap()[id];
    });

    /** The schema definition of the selected element (if it has one) */
    public readonly selectedElementSchema = computed(() => {
        const id = this.selectedElementId();
        return id == null ? undefined : (this.schema()?.properties || {})?.[id];
    });

    /** Which type the element is */
    public selectedElementType = computed(() => {
        const element = this.selectedElement();
        if (element == null) {
            return null;
        }

        const schema = this.selectedElementSchema();
        return schema == null ? element.formGridOptions!.type : schema.type;
    });

    /** The configuration of the currently selected element */
    public readonly selectedElementConfiguration = computed(() => {
        const element = this.selectedElement();
        if (element == null) {
            return undefined;
        }

        const schema = this.selectedElementSchema();

        return schema == null
            ? (this.config().blocks || {})[element.formGridOptions!.type]
            : this.config().controls[schema.type];
    });

    /* PREVIEW
     * ===================================================================== */

    public previewFormJson = '';
    public previewRemountToken = 0;
    public previewLanguage: string | null = null;

    /* RESIZE
     * ===================================================================== */

    /** If the editor is currently resizing an element */
    public resizeActive = false;

    /* CONSTRUCTOR
     * ===================================================================== */

    constructor(
        changeDetector: ChangeDetectorRef,
        private i18n: I18nService,
        private translateStore: TranslateStore,
        private notification: I18nNotificationService,
    ) {
        super(changeDetector);
    }

    /* HOOKS
     * ===================================================================== */

    public ngOnInit(): void {
        this.previewLanguage = this.i18n.getCurrentLanguage();
    }

    public override ngOnDestroy(): void {
        super.ngOnDestroy();
    }

    /* CLIPBOARD
     * ===================================================================== */

    @HostListener('document:copy', ['$event'])
    handleCopyEvent(event: ClipboardEvent): void {
        const element = this.selectedElement();
        if (element == null) {
            return;
        }

        // Don't intercept copy when the user is working in an input field
        const activeEl = document.activeElement;
        const isEditable = activeEl instanceof HTMLInputElement
          || activeEl instanceof HTMLTextAreaElement
          || activeEl?.getAttribute('contenteditable') === 'true';

        if (isEditable) {
            return;
        }

        // nested child elements
        const childSchemas: Record<string, FormSchemaProperty> = {};
        if (Array.isArray(element.elements)) {
            this.collectChildSchemas(element.elements, childSchemas);
        }

        const data: FormGridClipboardData = {
            element: structuredClone(element),
            elementSchema: this.selectedElementSchema() ? structuredClone(this.selectedElementSchema()!) : undefined,
            childSchemas: Object.keys(childSchemas).length > 0 ? structuredClone(childSchemas) : undefined,
            formId: this.formId(),
            formType: this.formType(),
            formName: this.formName(),
        };

        const json = JSON.stringify(data);

        try {
            event.clipboardData?.setData(CLIPBOARD_MIME, json);
            event.preventDefault();
        } catch {
            localStorage.setItem(CLIPBOARD_STORAGE_KEY, json);
        }

        this.notification.show({
            message: 'form_grid.copy_element_success',
            translationParams: {
                name: this.i18n.fromObject(element.label),
            },
            type: 'success',
        });
    }

    @HostListener('document:paste', ['$event'])
    handlePasteEvent(event: ClipboardEvent): void {
        if (this.mode() !== FormGridEditMode.FULL) {
            return;
        }

        const activeEl = document.activeElement;
        const isEditable = activeEl instanceof HTMLInputElement
          || activeEl instanceof HTMLTextAreaElement
          || activeEl?.getAttribute('contenteditable') === 'true';

        if (isEditable) {
            return;
        }

        let json = event.clipboardData?.getData(CLIPBOARD_MIME);
        if (!json) {
            json = localStorage.getItem(CLIPBOARD_STORAGE_KEY) ?? '';
        }

        if (!json) {
            return;
        }

        let clipboardData: FormGridClipboardData;
        try {
            clipboardData = JSON.parse(json);
        } catch {
            return;
        }

        if (!clipboardData?.element) {
            return;
        }

        event.preventDefault();

        // Validate matching form type
        if (clipboardData.formType && clipboardData.formType !== this.formType()) {
            this.notification.show({
                message: 'form_grid.paste_form_type_mismatch',
                translationParams: {
                    sourceType: clipboardData.formType,
                },
                type: 'warning',
            });
            return;
        }

        const pastedElement = structuredClone(clipboardData.element);
        const oldToNewIdMap = this.reassignElementIds(pastedElement);
        pastedElement.uiSchemaPage = this.pageIndex();

        // Insert schema properties for the pasted element and its children
        const schemaCopy = structuredClone(this.schema());
        let schemaChanged = false;

        if (clipboardData.elementSchema) {
            const newSchema = structuredClone(clipboardData.elementSchema);
            newSchema.formPage = this.pageIndex();
            schemaCopy.properties[pastedElement.id] = newSchema;

            // Remap nested schema properties (inside the parent's .properties, for aggregates)
            for (const [oldId, newId] of Object.entries(oldToNewIdMap)) {
                if (oldId === pastedElement.id) {
                    continue;
                }
                const nestedSchema = clipboardData.elementSchema.properties?.[oldId];
                if (nestedSchema) {
                    if (!schemaCopy.properties[pastedElement.id].properties) {
                        schemaCopy.properties[pastedElement.id].properties = {};
                    }
                    schemaCopy.properties[pastedElement.id].properties![newId] = structuredClone(nestedSchema);
                }
            }

            schemaChanged = true;
        }

        if (clipboardData.childSchemas) {
            for (const [oldId, childSchema] of Object.entries(clipboardData.childSchemas)) {
                const newId = oldToNewIdMap[oldId];
                if (newId) {
                    const newChildSchema = structuredClone(childSchema);
                    newChildSchema.formPage = this.pageIndex();
                    schemaCopy.properties[newId] = newChildSchema;
                    schemaChanged = true;
                }
            }
        }

        if (schemaChanged) {
            this.updateSchema(schemaCopy);
        }

        const pageElements = structuredClone(this.elements());
        const selected = this.selectedElement();

        if (selected != null) {
            // If container, paste at the end of the container
            if (Array.isArray(selected.elements)) {
                const updatedSelected = structuredClone(selected);
                updatedSelected.elements!.push(pastedElement);
                if (this.replaceElementInTree(pageElements, updatedSelected)) {
                    this.updatePageElements(pageElements);
                }
            } else {
                // Insert after selected element of current page
                const selectedIndex = this.findElementIndex(pageElements, selected.id);
                if (selectedIndex !== -1) {
                    pageElements.splice(selectedIndex + 1, 0, pastedElement);
                } else {
                    pageElements.push(pastedElement);
                }
                this.updatePageElements(pageElements);
            }
        } else {
            // to end of current page
            pageElements.push(pastedElement);
            this.updatePageElements(pageElements);
        }

        this.setSelectedElement({
            element: pastedElement,
            containerId: this.selectedElementContainerId ?? this.ELEMENT_ROOT_CONTAINER_ID,
        });

        this.notification.show({
            message: 'form_grid.paste_element_success',
            translationParams: {
                name: this.i18n.fromObject(pastedElement.label),
            },
            type: 'success',
        });
    }

    public onPaletteStart(event: ISortableEvent): void {
        const type = event.item.getAttribute('data-element-type');
        if (type == null) {
            return;
        }
        this.elementMoving.set({
            elementType: type,
            inserting: true,
        });
    }

    public onPaletteEnd(_event: ISortableEvent): void {
        this.elementMoving.set(null);
    }

    /**
     * Assigns new UUIDs to the element and all nested child elements.
     * Returns a map of old ID -> new ID for schema remapping.
     */
    private reassignElementIds(element: FormElement): Record<string, string> {
        const idMap: Record<string, string> = {};
        this.reassignElementIdsRecursive(element, idMap);
        return idMap;
    }

    private reassignElementIdsRecursive(element: FormElement, idMap: Record<string, string>): void {
        const oldId = element.id;
        element.id = uuidV4();
        idMap[oldId] = element.id;

        if (Array.isArray(element.elements)) {
            for (const child of element.elements) {
                this.reassignElementIdsRecursive(child, idMap);
            }
        }
    }

    private collectChildSchemas(elements: FormElement[], out: Record<string, FormSchemaProperty>): void {
        const schemaProps = this.schema()?.properties || {};

        for (const child of elements) {
            const childSchema = schemaProps[child.id];
            if (childSchema) {
                out[child.id] = childSchema;
            }

            if (Array.isArray(child.elements)) {
                this.collectChildSchemas(child.elements, out);
            }
        }
    }

    /**
     * Finds the index of an element by ID in a flat list (top-level only).
     */
    private findElementIndex(elements: FormElement[], id: string): number {
        return elements.findIndex((el) => el.id === id);
    }

    /* IMPLEMENTATION
     * ===================================================================== */

    public setResizing(flag: boolean): void {
        this.resizeActive = flag;
    }

    public toggleLeftSidebar(): void {
        this.leftSidebarExpanded.update((val) => !val);
    }

    public toggleRightSidebar(): void {
        this.rightSidebarExpanded.update((val) => !val);
    }

    public updatePageElements(elements: FormElement[]): void {
        // Keep backing data structures in sync for both root and nested drops.
        const copy = structuredClone(this.uiSchema());
        copy.pages[this.pageIndex()].elements = elements;
        this.uiSchema.set(copy);
    }

    public updateSchema(schema: FormSchema): void {
        this.schema.set(schema);
    }

    public moveElementToContainer(event: ElementContainerMoveEvent): void {
        const element = this.elementMap()[event.elementId];

        // If the elements couldn't be properly determined
        if (!element) {
            return;
        }

        const copy = structuredClone(this.uiSchema());

        // Moving between containers can only be done within the same page, therefore safe
        const page = copy.pages[event.pageIndex];

        // Go through all elements (and nested ones), to find the from/to container elements in the page,
        // then update the elements within that.
        const { removed, added } = moveNestedElement(
            this.ELEMENT_ROOT_CONTAINER_ID,
            page.elements,
            element,
            event.fromContainerId,
            event.toContainerId,
            event.targetIndex,
        );

        if (!removed) {
            console.warn(`While moving element ${element.id} from ${event.fromContainerId} to ${event.toContainerId}, it could not be removed from the source container`);
        }
        if (!added) {
            console.warn(`While moving element ${element.id} from ${event.fromContainerId} to ${event.toContainerId}, it could not be added from the target container`);
        }

        // After all has been updated, push it as one change
        this.uiSchema.set(copy);
    }

    public moveElementBetweenPages(event: ElementInterPageMoveEvent): void {
        const copy = structuredClone(this.uiSchema());
        const fromElements = copy.pages[event.fromPage]?.elements;
        if (!fromElements) {
            return;
        }
        const idx = fromElements.findIndex((el) => el.id === event.elementId);
        if (idx === -1) {
            return;
        }
        const [element] = fromElements.splice(idx, 1);
        element.uiSchemaPage = event.toPage;
        copy.pages[event.toPage].elements.push(element);
        this.uiSchema.set(copy);
        this.elementMoving.set(null);
        this.clearSelectedElement();

        // Hacky workaround, as updating the ui-schema and the page at the same time,
        // doesn't properly refresh the computed value correctly, and makes the element not
        // appear in the new page.
        setTimeout(() => {
            this.pageIndex.set(event.toPage);
        });
    }

    public upsetElementChanges(data: FormElement): void {
        const pageElements = structuredClone(this.elements());
        if (this.replaceElementInTree(pageElements, data)) {
            this.updatePageElements(pageElements);
        }
    }

    /**
     * Recursively walks the element tree to find the element with the matching ID and replaces it.
     * The existing children (`.elements`) of the matched node are preserved so an edit on a
     * container's settings cannot accidentally wipe its children.
     */
    private replaceElementInTree(elements: FormElement[], replacement: FormElement): boolean {
        for (let i = 0; i < elements.length; i++) {
            if (elements[i].id === replacement.id) {
                elements[i] = {
                    ...replacement,
                    elements: elements[i].elements ?? replacement.elements,
                };
                return true;
            }
            if (Array.isArray(elements[i].elements)
              && this.replaceElementInTree(elements[i].elements!, replacement)
            ) {
                return true;
            }
        }
        return false;
    }

    public updateElementSchema(elementSchema?: FormSchemaProperty): void {
        if (elementSchema == null) {
            return;
        }

        const elementId = this.selectedElementId();

        if (!elementId) {
            return;
        }

        const currentSchema = this.schema();

        if (!currentSchema?.properties) {
            return;
        }

        const copy = structuredClone(currentSchema);

        if (copy.properties[elementId] != null) {
            Object.assign(copy.properties[elementId], elementSchema);
            this.updateSchema(copy);
            return;
        }

        for (const innerProp of Object.values(copy.properties)) {
            if (innerProp.properties?.[elementId] != null) {
                Object.assign(innerProp.properties[elementId], elementSchema);
                this.updateSchema(copy);
                return;
            }
        }
    }

    private ensurePreviewBootstrapData(): void {
        try {
            this.previewFormJson = JSON.stringify({
                schema: this.schema(),
                uiSchema: this.uiSchema(),
            });
        } catch {
            this.previewFormJson = '';
        }
    }

    setSelectedElement(data?: ElementSelectionEvent | null, event?: Event): void {
        cancelEvent(event);

        if (data == null || data.element == null || data.containerId == null) {
            this.clearSelectedElement();
            return;
        }

        this.editingPageIndex.set(null);

        const blockType = data.element.formGridOptions?.type || null;
        const elementSchema = this.schema()?.properties?.[data.element.id] || null;
        const controlConfig = elementSchema
            ? this.config().controls[elementSchema?.type]
            : null;
        const blockConfig = blockType
            ? this.config().blocks?.[blockType] || null
            : null;

        // If we have no configuration, we can't do anything with the element
        if (!controlConfig && !blockConfig) {
            this.clearSelectedElement();
            return;
        }

        // If we had no element before, then the sidebar should be open fully
        if (this.selectedElementId() == null) {
            this.rightSidebarExpanded.set(true);
        }

        this.selectedElementId.set(data.element.id);
        this.selectedElementContainerId = data.containerId;
    }

    public startEditPage(index: number): void {
        this.selectedPageLanguage.set(this.languages()[0] ?? '');
        this.clearSelectedElement();

        // Pre-fill pagename with the default "Page N" label if the page has no name yet
        const page = this.uiSchema().pages[index];
        const hasName = page?.pagename && Object.values(page.pagename).some((v) => !!v);
        if (!hasName) {
            const pageNum = index + 1;
            const fallback = this.i18n.instant('form_grid.page_untitled', { ['number']: pageNum });
            const copy = structuredClone(this.uiSchema());
            const filled: I18nString = {};
            for (const lang of this.languages()) {
                const langTrans = this.translateStore.getTranslations(lang) as any;
                const template: string | undefined = langTrans?.['form_grid']?.['page_untitled'];
                filled[lang] = template
                    ? template.replace(/\{\{number\}\}/g, String(pageNum))
                    : fallback;
            }
            copy.pages[index].pagename = filled;
            this.uiSchema.set(copy);
        }

        this.editingPageIndex.set(index);
        this.rightSidebarExpanded.set(true);
    }

    public stopEditPage(): void {
        this.editingPageIndex.set(null);
    }

    public updateEditingPageName(value: I18nString | null): void {
        const idx = this.editingPageIndex();
        if (idx == null) {
            return;
        }
        const copy = structuredClone(this.uiSchema());
        copy.pages[idx].pagename = value ?? {};
        this.uiSchema.set(copy);
    }

    public clearSelectedElement(): void {
        this.selectedElementId.set(null);
        this.selectedElementContainerId = null;
        this.editingPageIndex.set(null);
    }
}

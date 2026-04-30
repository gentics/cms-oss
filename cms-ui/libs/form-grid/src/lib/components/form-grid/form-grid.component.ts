import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    effect,
    input,
    model,
    OnDestroy,
    OnInit,
    signal,
} from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { TranslateStore } from '@ngx-translate/core';
import {
    FormElement,
    FormElementConfiguration,
    FormPage,
    FormSchema,
    FormSchemaProperty,
    FormTypeConfiguration,
    FormUISchema,
    I18nString,
} from '@gentics/cms-models';
import { BaseComponent, cancelEvent } from '@gentics/ui-core';
import { v4 as uuidV4 } from 'uuid';
import { ElementInterPageMoveEvent, ElementSelectionEvent, FormGridEditMode, FormGridViewMode, PALETTE_MIME, PaletteDropTarget } from '../../models';

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
    public isElementDragging = signal(false);

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

    /* PALETTE
     * ===================================================================== */

    /** If an element from the palette is currently being dragged (i.E. new element is getting added) */
    public isPaletteDragging = false;
    /** The control/block type which is being dragged */
    public paletteDragType: string | null = null;
    /** The configuration of the element that is being dragged */
    public paletteDragConfig: FormElementConfiguration | null = null;
    /** The currently active drop location/target */
    public paletteDropTarget: PaletteDropTarget | null = null;
    /** Ghost element which is shown during dragging (from palette to editor) */
    private paletteDragGhost: HTMLElement | null = null;

    /* PREVIEW
     * ===================================================================== */

    /** JSON serialization of the current form for the FormGen preview bundle. */
    public readonly previewFormJson = computed(() => {
        try {
            return JSON.stringify({
                schema: this.schema(),
                uiSchema: this.uiSchema(),
            });
        } catch {
            return '';
        }
    });

    public previewRemountToken = 0;
    public previewLanguage: string | null = null;

    /* RESIZE
     * ===================================================================== */

    /** If the editor is currently resizing an element */
    public resizeActive = false;
    /** If the resize-overlay should be displayed */
    public resizeOverlayActive = false;
    /**
     * How many column-spans the current element has.
     * Indicates how many bars are filled in the resize overlay.
     */
    public resizeOverlaySpan = 12;

    /* CONSTRUCTOR
     * ===================================================================== */

    constructor(
        changeDetector: ChangeDetectorRef,
        private i18n: I18nService,
        private translateStore: TranslateStore,
    ) {
        super(changeDetector);

        // Force a fresh remount of the FormGen preview whenever the user switches
        // to preview mode so it picks up the latest schema/ui-schema state.
        effect(() => {
            if (this.viewMode() === FormGridViewMode.PREVIEW) {
                this.previewRemountToken++;
            }
        });
    }

    /* HOOKS
     * ===================================================================== */

    public ngOnInit(): void {
        this.previewLanguage = this.i18n.getCurrentLanguage();
    }

    public override ngOnDestroy(): void {
        super.ngOnDestroy();

        if (this.paletteDragGhost != null) {
            this.paletteDragGhost.remove();
        }
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

    public paletteDragStart(event: DragEvent, id: string, element: FormElementConfiguration): void {
        this.isPaletteDragging = true;
        this.paletteDragType = id;
        this.paletteDragConfig = element;
        this.paletteDropTarget = null;

        const transfer = event?.dataTransfer;

        if (!transfer) {
            return;
        }

        transfer.effectAllowed = 'copy';
        transfer.dropEffect = 'copy';
        transfer.setData(PALETTE_MIME, id);
        transfer.setData('text/plain', id);

        const ghost = this.buildPaletteDragGhost(this.i18n.fromObject(element.labelI18n));

        if (ghost) {
            this.paletteDragGhost = ghost;
            document.body.appendChild(ghost);
            transfer.setDragImage(ghost, 24, 24);
        }
    }

    public paletteDragEnd(): void {
        this.isPaletteDragging = false;
        this.paletteDragType = null;
        this.paletteDragConfig = null;
        this.paletteDropTarget = null;

        if (this.paletteDragGhost) {
            this.paletteDragGhost.remove();
            this.paletteDragGhost = null;
        }
    }

    public editorDragOver(event: DragEvent): void {
        if (!this.isPaletteDragging) {
            return;
        }

        event.preventDefault();

        if (event.dataTransfer) {
            event.dataTransfer.dropEffect = 'copy';
        }
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
        this.pageIndex.set(event.toPage);
        this.isElementDragging.set(false);
        this.clearSelectedElement();
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

    /**
     * Called when the user selects an element in the FormGen preview.
     * Synchronizes the selection back to the editor so the right sidebar opens.
     */
    public onPreviewElementSelected(elementId: string | null): void {
        if (elementId == null) {
            this.clearSelectedElement();
            return;
        }

        const element = this.elementMap()[elementId];
        if (!element) {
            this.clearSelectedElement();
            return;
        }

        // Find which container holds this element
        const containerId = this.findContainerForElement(elementId) ?? this.ELEMENT_ROOT_CONTAINER_ID;

        this.setSelectedElement({
            element,
            containerId,
        });
    }

    public clearSelectedElement(): void {
        this.selectedElementId.set(null);
        this.selectedElementContainerId = null;
        this.editingPageIndex.set(null);
    }

    /**
     * Finds the parent container ID for a given element ID.
     * Returns null if the element is at the root level.
     */
    private findContainerForElement(elementId: string, elements?: FormElement[], containerId?: string): string | null {
        const els = elements ?? this.elements();
        const cId = containerId ?? this.ELEMENT_ROOT_CONTAINER_ID;

        for (const el of els) {
            if (el.id === elementId) {
                return cId;
            }
            if (Array.isArray(el.elements)) {
                const found = this.findContainerForElement(elementId, el.elements, el.id);
                if (found) {
                    return found;
                }
            }
        }

        return null;
    }

    // TODO: Do it more angular like, as creating a new untracked HTMLElement is not a great idea
    private buildPaletteDragGhost(label: string): HTMLElement {
        const ghost = document.createElement('div');
        ghost.className = 'andp-palette-drag-ghost';
        ghost.innerHTML = `
            <span class="material-icons" aria-hidden="true"></span>
            <span>${label}</span>
        `;

        Object.assign(ghost.style, {
            position: 'fixed',
            top: '-9999px',
            left: '-9999px',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '10px',
            padding: '10px 14px',
            borderRadius: '14px',
            border: '1px solid rgba(0, 150, 220, 0.22)',
            background: 'rgba(255, 255, 255, 0.98)',
            boxShadow: '0 18px 40px rgba(0, 0, 0, 0.16)',
            color: '#0f172a',
            fontSize: '13px',
            fontWeight: '800',
            pointerEvents: 'none',
            zIndex: '99999',
        } as Partial<CSSStyleDeclaration>);

        return ghost;
    }
}

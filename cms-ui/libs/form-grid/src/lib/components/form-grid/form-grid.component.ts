import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    input,
    model,
    OnDestroy,
    OnInit,
} from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import {
    FormElement,
    FormElementConfiguration,
    FormSchema,
    FormSchemaProperty,
    FormTypeConfiguration,
    FormUISchema,
} from '@gentics/cms-models';
import { BaseComponent, cancelEvent } from '@gentics/ui-core';
import { v4 as uuidV4 } from 'uuid';
import { LINE_OPTIONS } from '../../constants/line-options';
import { ElementSelectionEvent, FormGridViewMode, PALETTE_MIME, PaletteDropTarget } from '../../models';

enum EditTabs {
    DEFINITION = 'definition',
    SETTINGS = 'settings',
    TRANSLATIONS = 'translations',
}

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

@Component({
    selector: 'gtx-form-grid',
    templateUrl: './form-grid.component.html',
    styleUrls: ['./form-grid.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormGridComponent extends BaseComponent implements OnInit, OnDestroy {

    public readonly ELEMENT_ROOT_CONTAINER_ID = uuidV4();
    public readonly LINE_OPTIONS = LINE_OPTIONS;
    public readonly EditTabs = EditTabs;
    public readonly FormGridViewMode = FormGridViewMode;

    /* INPUTS / OUTPUTS
     * ===================================================================== */

    public readonly config = input.required<FormTypeConfiguration>();
    /** If this form-grid is in the restricted mode */
    public readonly restricted = input.required<boolean>();
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

    /** Mapping of ID to each element in a flat map */
    public readonly elementMap = computed<Record<string, FormElement>>(() => {
        const data: Record<string, FormElement> = {};

        for (const page of (this.uiSchema()?.pages || [])) {
            if (page?.elements?.length > 0) {
                addElementsToMap(data, page.elements);
            }
        }

        return data;
    });

    /** If the left sidebar is expanded */
    public leftSidebarExpanded = true;
    /** If the right sidebar is expanded */
    public rightSidebarExpanded = false;

    /* SELECTION & EDITING
     * ===================================================================== */

    /** The currently selected element, which may be getting edited. */
    public selectedElement: FormElement | null = null;
    /** Which type the element is */
    public selectedElementType: string | null = null;
    /** The ID of the container where the selectedElement is contained in. */
    public selectedElementContainerId: string | null = null;
    /** The configuration of the currently selected element */
    public selectedElementConfiguration: FormElementConfiguration | null = null;
    /** The schema definition of the selected element (if it has one) */
    public selectedElementSchema: FormSchemaProperty | null = null;
    /** Editable copy of the selected element's schema property Null for formgrid blocks (Text, Image, Spacer) which have no schema entry. */
    public selectedElementSchemaDraft: Partial<FormSchemaProperty> | null = null;

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

    public previewFormJson = '';
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
        this.leftSidebarExpanded = !this.leftSidebarExpanded;
    }

    public toggleRightSidebar(): void {
        this.rightSidebarExpanded = !this.rightSidebarExpanded;
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

    public upsetElementChanges(data: FormElement): void {
        if (this.selectedElementContainerId !== this.ELEMENT_ROOT_CONTAINER_ID) {
            // TODO: handle nested elements
            return;
        }

        const newElements = this.elements().slice();
        const idx = newElements.findIndex((el) => el.id === data.id);

        // If we can't find the element in the container data, then we have a problem
        if (idx === -1) {
            return;
        }

        newElements.splice(idx, 1, data);
        this.updatePageElements(newElements);
    }

    public onDefinitionChange(draft: Partial<FormSchemaProperty>): void {
        this.selectedElementSchemaDraft = draft;

        if (!this.selectedElement?.id) {
            return;
        }

        const elementId = this.selectedElement.id;
        const currentSchema = this.schema();

        if (!currentSchema?.properties) {
            return;
        }

        const copy = structuredClone(currentSchema);

        if (copy.properties[elementId] != null) {
            Object.assign(copy.properties[elementId], this.selectedElementSchemaDraft);
            this.updateSchema(copy);
            return;
        }

        for (const innerProp of Object.values(copy.properties)) {
            if (innerProp.properties?.[elementId] != null) {
                Object.assign(innerProp.properties[elementId], this.selectedElementSchemaDraft);
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
        if (this.selectedElement == null) {
            this.rightSidebarExpanded = true;
        }

        this.selectedElement = data.element;
        this.selectedElementType = elementSchema != null
            ? elementSchema.type
            : blockType;
        this.selectedElementContainerId = data.containerId;
        this.selectedElementConfiguration = controlConfig || blockConfig;
        this.selectedElementSchema = elementSchema;
        this.selectedElementSchemaDraft = elementSchema
            ? structuredClone(elementSchema)
            : null;
    }

    public clearSelectedElement(): void {
        this.selectedElement = null;
        this.selectedElementType = null;
        this.selectedElementContainerId = null;
        this.selectedElementConfiguration = null;
        this.selectedElementSchema = null;
        this.selectedElementSchemaDraft = null;
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

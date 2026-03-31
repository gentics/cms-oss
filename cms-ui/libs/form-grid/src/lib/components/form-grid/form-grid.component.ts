import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    input,
    model,
    OnDestroy,
    OnInit,
    signal,
} from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import {
    FormElement,
    FormElementConfiguration,
    FormSchema,
    FormSchemaProperties,
    FormSchemaProperty,
    FormTypeConfiguration,
    FormUISchema,
} from '@gentics/cms-models';
import { BaseComponent, cancelEvent } from '@gentics/ui-core';
import { v4 as uuidV4 } from 'uuid';
import { LINE_OPTIONS } from '../../constants/line-options';
import { ElementLocation, PALETTE_MIME, PaletteDropTarget } from '../../models';

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
    public readonly LINE_OPTIONS = LINE_OPTIONS;
    public readonly EditTabs = EditTabs;

    /* INPUTS / OUTPUTS
     * ===================================================================== */

    public readonly config = input.required<FormTypeConfiguration>();
    public readonly restricted = input.required<boolean>();
    public readonly languages = input.required<string[]>();

    public readonly schema = model.required<FormSchema>();
    public readonly uiSchema = model.required<FormUISchema>();

    /* PAGE & VIEW
     * ===================================================================== */

    /** All currently visible items, i.E. all elements from the current form page */
    public readonly items = computed<FormElement[]>(() => {
        return this.uiSchema()?.pages?.[this.pageIndex()]?.elements || [];
    });

    /** Index of the currently viewed form page */

    public readonly pageIndex = signal<number>(0);
    /** The ID of the currently active element */
    public selectedElementId: string | null = null;
    /** Location of the currently selected element */
    public selectedElementLoc: ElementLocation | null = null;
    /** The schema definition of the selected element (if it has one) */
    public selectedElementSchema: FormSchemaProperty | null = null;
    /** Data that is being edited in the right panel */
    public selectedElementDraft: Partial<FormElement> | null = null;
    public leftSidebarExpanded = true;
    public rightSidebarExpanded = false;

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

    public resizeActive = false;
    public resizeOverlayActive = false;
    public resizeOverlaySpan = 12;

    /* MISC
     * ===================================================================== */

    /** Which tab/mode is currently used */
    public viewMode: 'preview' | 'editor' = 'editor';

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

    // TODO: Make static
    public get isSelectedFormgridElement(): boolean {
        return !!this.selectedElementDraft?.formGridOptions?.type;
    }

    // TODO: Make static
    public get isSelectedNormalElement(): boolean {
        return (
            !!this.selectedElementDraft
            && !this.selectedElementDraft?.formGridOptions?.type
        );
    }

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

    selectElementById(id?: string | null, event?: Event): void {
        cancelEvent(event);

        this.selectedElementId = id || null;
        if (id) {
            this.rightSidebarExpanded = true;
            this.selectedElementLoc = this.locateElementById(id, this.items());
            this.selectedElementDraft = this.selectedElementLoc
                ? structuredClone(this.selectedElementLoc.element)
                : null;
            this.selectedElementSchema = this.selectedElementDraft
                ? this.findSchemaElement(
                    (this.schema() || {}).properties,
                    this.selectedElementDraft.id as string,
                ) || null
                : null;
        } else {
            this.selectedElementLoc = null;
            this.selectedElementDraft = null;
            this.selectedElementSchema = null;
        }
        if (this.viewMode === 'preview') {
            this.ensurePreviewBootstrapData();
        }
    }

    private locateElementById(
        id: string,
        items: FormElement[] = [],
    ): ElementLocation | null {
        if (!Array.isArray(items)) {
            return null;
        }

        for (let i = 0; i < items.length; i++) {
            const el = items[i];
            if (el.id === id) {
                return { location: items, index: i, element: el };
            }
            if (el.type === 'aggregate' && Array.isArray(el.elements)) {
                const nested = this.locateElementById(id, el.elements);
                if (nested) {
                    return nested;
                }
            }
        }

        return null;
    }

    private findSchemaElement(properties: FormSchemaProperties, key: string): FormSchemaProperty | null {
        if (properties == null) {
            return null;
        }

        if (properties[key]) {
            return properties[key];
        }

        const entries = Object.values(properties);
        for (const innerProps of entries) {
            if (innerProps.properties?.[key]) {
                return innerProps.properties[key];
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

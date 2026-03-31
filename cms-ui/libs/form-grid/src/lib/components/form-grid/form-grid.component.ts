import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    input,
    model,
    NgZone,
    OnChanges,
    OnDestroy,
    OnInit,
    signal,
    SimpleChanges,
} from '@angular/core';
import { FALLBACK_LANGUAGE, I18nService } from '@gentics/cms-components';
import {
    FormControlConfiguration,
    FormElement,
    FormElementConfiguration,
    FormSchema,
    FormSchemaProperties,
    FormSchemaProperty,
    FormTypeConfiguration,
    FormUISchema,
    I18nString,
} from '@gentics/cms-models';
import { BaseComponent, cancelEvent, ISortableEvent, ModalService } from '@gentics/ui-core';
import { v4 as uuidV4 } from 'uuid';
import { LINE_OPTIONS } from '../../constants/line-options';

interface PaletteDropTarget {
    containerId: string;
    list: any[];
    index: number;
    span: number;
}

interface DropItemRect {
    index: number;
    rect: DOMRect;
}

interface DropRow {
    top: number;
    bottom: number;
    items: DropItemRect[];
}

interface ElementLocation {
    location: FormElement[];
    index: number;
    element: FormElement;
}

enum EditTabs {
    DEFINITION = 'definition',
    SETTINGS = 'settings',
    TRANSLATIONS = 'translations',
}

interface DisplayItem {
    id: string;
    label: I18nString;
    element: FormElement;
    isBlock: boolean;
    isControl: boolean;
    config: FormElementConfiguration;
    schema?: FormSchemaProperty;
}

const PALETTE_MIME = 'application/x-andp-formgrid-palette';
const DROP_ROW_TOLERANCE = 10;

@Component({
    selector: 'gtx-form-grid',
    templateUrl: './form-grid.component.html',
    styleUrls: ['./form-grid.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormGridComponent extends BaseComponent implements OnInit, OnChanges, OnDestroy {

    // Temporary to make it build
    public ctx: any = {};

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

    public readonly displayItems = computed<DisplayItem[]>(() => {
        // Can't use computed items here, as it wouldn't update/call correctly.
        return (this.uiSchema()?.pages?.[this.pageIndex()]?.elements || []).map((el) => {
            const itemSchema = this.schema().properties[el.id];

            if (itemSchema) {
                const itemConfig = this.config().controls[itemSchema?.type];

                if (!itemConfig) {
                    return null;
                }

                return {
                    id: el.id,
                    label: itemConfig.labelI18n,
                    element: el,
                    isBlock: false,
                    isControl: true,
                    config: itemConfig,
                    schema: itemSchema,
                };
            } else {
                const type = el.formGridOptions?.type;

                if (!type) {
                    return null;
                }

                const itemConfig = (this.config().blocks || {})[type];

                if (!itemConfig) {
                    return null;
                }

                return {
                    id: el.id,
                    label: itemConfig.labelI18n,
                    element: el,
                    isControl: false,
                    isBlock: true,
                    config: itemConfig,
                };
            }
        }).filter((item) => item != null);
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
    /** How many spans/columns the currently dragged element would use if dropped */
    public pendingPaletteDropSpan: number | null;
    /** Ghost element which is shown during dragging (from palette to editor) */
    private paletteDragGhost: HTMLElement | null = null;
    /** Request-Frame timer */
    private paletteDragOverFrame = 0;

    /* PREVIEW
     * ===================================================================== */

    public previewFormJson = '';
    public previewRemountToken = 0;
    public previewLanguage: string | null = null;

    /* RESIZE
     * ===================================================================== */

    public resizeOverlayActive = false;
    public resizeOverlaySpan = 12;
    public resizeActive = false;
    public resizePointerId: number | null = null;
    public resizeStartX: number;
    public resizeTarget: FormElement | null = null;
    public resizeSurfaceEl: HTMLElement | null = null;
    public resizeRowBaseCols: number;
    public resizeRowMaxSpan: number;
    public resizeStartSpan: number;

    /* MISC
     * ===================================================================== */

    /** Current UI language */
    public uiLanguage = FALLBACK_LANGUAGE;
    /** Which tab/mode is currently used */
    public viewMode: 'preview' | 'editor' = 'editor';

    /* CONSTRUCTOR
     * ===================================================================== */

    constructor(
        changeDetector: ChangeDetectorRef,
        public zone: NgZone,
        public i18n: I18nService,
        public modals: ModalService,
    ) {
        super(changeDetector);
    }

    /* HOOKS
     * ===================================================================== */

    public ngOnInit(): void {
        this.subscriptions.push(this.i18n.onLanguageChange().subscribe((lang) => {
            this.uiLanguage = lang;
            if (!this.previewLanguage) {
                if (this.languages().includes(lang)) {
                    this.previewLanguage = this.uiLanguage;
                } else {
                    this.previewLanguage = this.languages()[0];
                }
            }
            this.changeDetector.markForCheck();
        }));
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);
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

    public hideLeftSidebar(): void {

    }

    public hideRightSidebar(): void {

    }

    public paletteDragStart(event: DragEvent, id: string, element: FormElementConfiguration): void {
        this.isPaletteDragging = true;
        this.paletteDragType = id;
        this.paletteDragConfig = element;
        this.paletteDropTarget = null;
        this.pendingPaletteDropSpan = null;

        const transfer = event?.dataTransfer;

        if (!transfer) {
            return;
        }

        transfer.effectAllowed = 'copy';
        transfer.dropEffect = 'copy';
        transfer.setData(PALETTE_MIME, id);
        transfer.setData('text/plain', id);

        const ghost = this.buildPaletteDragGhost(element.labelI18n[this.uiLanguage]);

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

        if (this.paletteDragOverFrame) {
            cancelAnimationFrame(this.paletteDragOverFrame);
            this.paletteDragOverFrame = 0;
        }

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

    onPaletteContainerDragOver(
        event: DragEvent,
        elements: FormElement[],
        containerId: string,
    ): void {
        if (
            !this.isPaletteDragging
            || !this.paletteDragType
            || !this.paletteDragConfig
            || !Array.isArray(elements)
        ) {
            return;
        }

        cancelEvent(event);

        if (event.dataTransfer) {
            event.dataTransfer.dropEffect = 'copy';
        }

        const container = event.currentTarget as HTMLElement | null;

        if (!container) {
            return;
        }

        const clientX = event.clientX;
        const clientY = event.clientY;

        if (this.paletteDragOverFrame) {
            cancelAnimationFrame(this.paletteDragOverFrame);
        }

        this.paletteDragOverFrame = requestAnimationFrame(() => {
            const nextTarget = this.calculatePaletteInsertTarget(
                container,
                clientX,
                clientY,
                elements,
            );

            const rootElements = this.uiSchema()?.pages?.[this.pageIndex()]?.elements;

            if (
                !this.paletteDropTarget
                // TODO: Find a better way to determine root levels
                || this.paletteDropTarget.list !== rootElements
                || this.paletteDropTarget.containerId !== containerId
                || this.paletteDropTarget.index !== nextTarget.index
                || this.paletteDropTarget.span !== nextTarget.span
            ) {
                this.paletteDropTarget = {
                    containerId,
                    list: rootElements,
                    index: nextTarget.index,
                    span: nextTarget.span,
                };
            }

            this.paletteDragOverFrame = 0;
        });
    }

    onPaletteContainerDrop(
        event: DragEvent,
        elements: FormElement[],
        containerId: string,
    ): void {
        if (
            !this.isPaletteDragging
            || !this.paletteDragType
            || !this.paletteDragConfig
            || !Array.isArray(elements)
        ) {
            return;
        }

        cancelEvent(event);

        const container = event.currentTarget as HTMLElement | null;
        const fallbackTarget = container
            ? this.calculatePaletteInsertTarget(
                container,
                event.clientX,
                event.clientY,
                elements,
            )
            : {
                index: elements.length,
                span: this.getPaletteDefaultSpanForList(elements, this.paletteDragType),
            };

        const target = (
            this.paletteDropTarget
            && this.paletteDropTarget.list === elements
            && this.paletteDropTarget.containerId === containerId
        )
            ? this.paletteDropTarget
            : {
                containerId,
                list: elements,
                index: fallbackTarget.index,
                span: fallbackTarget.span,
            };

        this.onEditorDrop(event, target.index, elements, target.span);
        this.onPaletteDragEnd();
    }

    onEditorDrop(
        event: DragEvent,
        insertIndex: number,
        elements: FormElement[],
        resolvedSpan?: number,
    ): void {
        cancelEvent(event);

        const dt = event?.dataTransfer;
        if (!dt || !Array.isArray(elements)) {
            this.onPaletteDragEnd();
            return;
        }

        const type = dt.getData(PALETTE_MIME) || dt.getData('text/plain');
        // TODO: Add type check?
        const safeIndex = Math.max(0, Math.min(insertIndex, elements.length));
        const isControl = this.config().controls[type] != null;

        this.pendingPaletteDropSpan = (resolvedSpan ?? (this.paletteDropTarget?.list === elements)
            ? this.paletteDropTarget!.span
            : this.getPaletteDefaultSpanForList(elements, type));

        const created = this.createPaletteElement(type);
        elements.splice(safeIndex, 0, created);

        if (isControl) {
            const typeConfig = this.config().controls[type];
            const copy = structuredClone(this.schema());
            copy.properties[created.id] = {
                type: type,
                name: typeConfig.labelI18n[this.uiLanguage] || type,
                formPage: this.pageIndex(),
                isList: typeConfig.aggregate,
                validation: {},
            };
            this.schema.set(copy);
        }

        this.updatePageElements(elements);

        this.selectElementById(created.id);

        if (this.viewMode === 'preview') {
            this.ensurePreviewBootstrapData();
            this.previewRemountToken++;
        }

        this.isPaletteDragging = false;
    }

    private updatePageElements(elements: FormElement[]): void {
        // Keep backing data structures in sync for both root and nested drops.
        const copy = structuredClone(this.uiSchema()) || {
            formGrid: {},
            pages: [],
        } as any;
        if (!Array.isArray(copy.pages)) {
            copy.pages = [];
        }
        if (copy.pages.length === 0) {
            copy.pages.push({
                pagename: {
                    en: 'Default',
                    de: 'Standard',
                },
                elements: [],
            });
        }
        if (copy.pages[this.pageIndex()] == null) {
            this.pageIndex.set(0);
        }
        copy.pages[this.pageIndex()].elements = elements;
        this.uiSchema.set(copy);
    }

    onPaletteDragEnd(): void {
        this.isPaletteDragging = false;
        this.paletteDragType = null;
        this.paletteDragConfig = null;
        this.paletteDropTarget = null;

        if (this.paletteDragOverFrame) {
            cancelAnimationFrame(this.paletteDragOverFrame);
            this.paletteDragOverFrame = 0;
        }

        if (this.paletteDragGhost) {
            this.paletteDragGhost.remove();
            this.paletteDragGhost = null;
        }
    }

    sortList(event: ISortableEvent): void {
        // Sort the visible list
        const newElements = [...this.items()];
        const sorted = event.sort(newElements);

        // Persist back into the current page (this.pages is usually a reference to uiSchema.pages,
        // but we also explicitly write to uiSchema to be safe)
        this.updatePageElements(sorted);

        // Keep preview in sync
        if (this.viewMode === 'preview') {
            this.ensurePreviewBootstrapData();
            this.previewRemountToken++;
        }
    }

    onResizeStart(
        event: PointerEvent,
        index: number,
        items: any[],
        surfaceEl: HTMLElement,
    ): void {
        cancelEvent(event);

        if (this.resizeActive) {
            return;
        }

        const handleEl = event.currentTarget as HTMLElement | null;
        const hostEl = handleEl?.closest('gtx-sortable-item') as HTMLElement | null;
        const parentEl = hostEl?.parentElement as HTMLElement | null;

        let rowBase = 0;
        let rowMax = 12;

        if (hostEl && parentEl) {
            const hostTop = hostEl.offsetTop;
            const itemEls = Array.from(parentEl.children).filter(
                (el) =>
                    (el as HTMLElement).tagName?.toLowerCase() === 'gtx-sortable-item',
            ) as HTMLElement[];

            for (let i = 0; i < index; i++) {
                const el = itemEls[i];
                if (el && el.offsetTop === hostTop) {
                    rowBase += this.getSpan(items[i]);
                }
            }

            rowBase = Math.max(0, Math.min(11, rowBase));
            rowMax = Math.max(1, 12 - rowBase);
        }

        this.resizeActive = true;
        this.resizePointerId = event.pointerId;
        this.resizeStartX = event.clientX;
        this.resizeTarget = items[index];
        this.resizeSurfaceEl = surfaceEl;
        this.resizeRowBaseCols = rowBase;
        this.resizeRowMaxSpan = rowMax;

        const initialSpan = Math.min(
            this.getSpan(this.resizeTarget),
            this.resizeRowMaxSpan,
        );
        this.setSpan(this.resizeTarget, initialSpan);

        this.resizeStartSpan = initialSpan;
        this.resizeOverlayActive = true;
        this.resizeOverlaySpan = Math.max(
            1,
            Math.min(12, this.resizeRowBaseCols + this.resizeStartSpan),
        );

        try {
            (event.target as HTMLElement)?.setPointerCapture?.(event.pointerId);
        } catch {}

        this.zone.runOutsideAngular(() => {
            const onMove = (e: PointerEvent) => {
                if (!this.resizeActive || this.resizePointerId !== e.pointerId) {
                    return;
                }

                const surface = this.resizeSurfaceEl;
                if (!surface) {
                    return;
                }

                const rect = surface.getBoundingClientRect();
                const colW = rect.width / 12;
                const dx = e.clientX - this.resizeStartX;
                const deltaCols = Math.round(dx / colW);

                const nextRaw = this.resizeStartSpan + deltaCols;
                const nextSpan = Math.max(1, Math.min(this.resizeRowMaxSpan, nextRaw));

                this.zone.run(() => {
                    this.setSpan(this.resizeTarget, nextSpan);
                    this.resizeOverlaySpan = Math.max(
                        1,
                        Math.min(12, this.resizeRowBaseCols + nextSpan),
                    );
                    this.changeDetector.markForCheck();
                });
            };

            const onUp = (e: PointerEvent) => {
                if (
                    !this.resizeActive
                    || (this.resizePointerId !== null
                      && this.resizePointerId !== e.pointerId)
                ) {
                    return;
                }

                this.zone.run(() => {
                    this.resizeActive = false;
                    this.resizePointerId = null;
                    this.resizeTarget = null;
                    this.resizeSurfaceEl = null;
                    this.resizeRowBaseCols = 0;
                    this.resizeRowMaxSpan = 12;
                    this.resizeOverlayActive = false;
                });

                window.removeEventListener('pointermove', onMove);
                window.removeEventListener('pointerup', onUp);
                window.removeEventListener('pointercancel', onUp);
            };

            window.addEventListener('pointermove', onMove);
            window.addEventListener('pointerup', onUp);
            window.addEventListener('pointercancel', onUp);
        });
    }

    private setSpan(el: any, span: number): void {
        if (!el.formGridOptions) {
            el.formGridOptions = {};
        }
        // TODO: Update uiSchema?
        el.formGridOptions.numberOfColumns = Math.max(1, Math.min(12, span));
    }

    deleteElement(event: MouseEvent, element: FormElement, index: number, items: FormElement[]): void {
        cancelEvent(event);

        this.modals.dialog({
            title: this.i18n.instant('form_grid.title_delete_element'),
            body: this.i18n.instant('form_grid_confirm_element_delete', {
                name: element.label[this.uiLanguage],
            }),
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
            .then((shouldDelete) => {
                if (!shouldDelete) {
                    return;
                }
                const newElements = items.slice();
                newElements.splice(index, 1);
                this.updatePageElements(newElements);
            });
    }

    private createPaletteElement(type: string): FormElement {
        const defaultSpan = this.pendingPaletteDropSpan ?? 12;
        this.pendingPaletteDropSpan = null;
        const isAggregate = (this.paletteDragConfig as FormControlConfiguration).aggregate;

        const el: FormElement = {
            id: uuidV4(),
            type: isAggregate ? 'aggregate' : 'property',
            label: {},
            formGridOptions: {
                type,
                numberOfColumns: defaultSpan,
                value: {},
                valueSummary: {},
                inForm: true,
                inSummary: false,
                overlayOptions: {
                    isShowOverlay: false,
                    texts: { textPre: {}, textPost: {} },
                },
            },
            uiSchemaPage: this.pageIndex(),
        };

        // If it's an aggregate, then default an empty items array
        if (isAggregate) {
            el.elements = [];
        }

        for (const langCode of this.languages()) {
            const label = this.paletteDragConfig?.labelI18n[langCode];
            if (label) {
                el.label[langCode] = label;
            }
        }

        return el;
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

    private calculatePaletteInsertTarget(
        container: HTMLElement,
        clientX: number,
        clientY: number,
        elements: FormElement[],
    ): { index: number; span: number } {
        const defaultSpan = this.getPaletteDefaultSpanForList(
            elements,
            this.paletteDragType,
        );
        const rows = this.buildPaletteDropRows(container);

        if (!rows.length) {
            return { index: 0, span: defaultSpan };
        }

        const activeRow = this.findPaletteDropRow(rows, clientY);

        if (!activeRow) {
            if (clientY < rows[0].top - DROP_ROW_TOLERANCE) {
                return { index: 0, span: defaultSpan };
            }

            for (let i = 0; i < rows.length - 1; i++) {
                const currentRow = rows[i];
                const nextRow = rows[i + 1];

                if (
                    clientY > currentRow.bottom + DROP_ROW_TOLERANCE
                    && clientY < nextRow.top - DROP_ROW_TOLERANCE
                ) {
                    return { index: nextRow.items[0].index, span: defaultSpan };
                }
            }

            return { index: elements.length, span: defaultSpan };
        }

        let index = activeRow.items[activeRow.items.length - 1].index + 1;

        for (const item of activeRow.items) {
            const midpointX = item.rect.left + item.rect.width / 2;
            if (clientX < midpointX) {
                index = item.index;
                break;
            }
        }

        const usedCols = activeRow.items.reduce(
            (sum, item) => sum + this.getItemSpanByIndex(elements, item.index),
            0,
        );
        const freeCols = Math.max(0, 12 - usedCols);

        const preferredSpan = this.getPaletteInRowSpan(
            elements,
            activeRow,
            index,
            defaultSpan,
        );

        const span = freeCols > 0
            ? Math.max(1, Math.min(preferredSpan, freeCols))
            : Math.max(1, Math.min(12, preferredSpan));

        return { index, span };
    }

    selectElementById(id: string | null, event?: Event): void {
        cancelEvent(event);

        this.selectedElementId = id;
        if (id) {
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

    findSchemaElement(properties: FormSchemaProperties, key: string): FormSchemaProperty | null {
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

    private findPaletteDropRow(rows: DropRow[], clientY: number): DropRow | null {
        return rows.find((row) =>
            clientY >= row.top - DROP_ROW_TOLERANCE
            && clientY <= row.bottom + DROP_ROW_TOLERANCE,
        ) ?? null;
    }

    private getPaletteDefaultSpanForList(
        elements: FormElement[],
        type: string | null,
    ): number {
        // TODO: Find a better way to check this, this isn't good
        const isRootContainer = elements === this.items();

        // TODO: check how to size
        switch (type) {
            default:
                return isRootContainer ? 12 : 6;
        }
    }

    private buildPaletteDropRows(container: HTMLElement): DropRow[] {
        const elements = Array.from(container.children).filter(
            (child): child is HTMLElement =>
                child instanceof HTMLElement && child.dataset['dropItem'] === 'true',
        );

        const rows: DropRow[] = [];

        for (const el of elements) {
            const indexAttr = el.getAttribute('data-drop-index');
            const index = Number(indexAttr);

            if (!Number.isFinite(index)) {
                continue;
            }

            const rect = el.getBoundingClientRect();

            const row = rows.find(
                (candidate) => Math.abs(candidate.top - rect.top) <= DROP_ROW_TOLERANCE,
            );

            if (row) {
                row.top = Math.min(row.top, rect.top);
                row.bottom = Math.max(row.bottom, rect.bottom);
                row.items.push({ index, rect });
            } else {
                rows.push({
                    top: rect.top,
                    bottom: rect.bottom,
                    items: [{ index, rect }],
                });
            }
        }

        rows.sort((a, b) => a.top - b.top);
        rows.forEach((row) => row.items.sort((a, b) => a.rect.left - b.rect.left));

        return rows;
    }

    private getItemSpanByIndex(elements: FormElement[], index: number): number {
        return this.getSpan(Array.isArray(elements) ? elements[index] : null);
    }

    private getSpan(el: FormElement | null): number {
        const raw = el?.formGridOptions?.numberOfColumns;
        const n = typeof raw === 'string' ? parseInt(raw, 10) : raw;
        if (n == null || !Number.isInteger(n)) {
            return 12;
        }
        return Math.max(1, Math.min(12, n));
    }

    private getPaletteInRowSpan(
        elements: FormElement[],
        row: DropRow,
        insertIndex: number,
        fallback: number,
    ): number {
        const rowIndices = new Set(row.items.map((item) => item.index));
        const leftIndex = insertIndex - 1;
        const rightIndex = insertIndex;

        const leftSpan = rowIndices.has(leftIndex)
            ? this.getItemSpanByIndex(elements, leftIndex)
            : null;

        const rightSpan = rowIndices.has(rightIndex)
            ? this.getItemSpanByIndex(elements, rightIndex)
            : null;

        if (leftSpan != null && rightSpan != null) {
            return Math.min(leftSpan, rightSpan);
        }

        if (leftSpan != null) {
            return leftSpan;
        }

        if (rightSpan != null) {
            return rightSpan;
        }

        return fallback;
    }

    getPalettePlaceholderSpan(elements: FormElement[] | null | undefined): number {
        if (
            !!this.isPaletteDragging
            && Array.isArray(elements)
            && this.paletteDropTarget?.list === elements
            && this.paletteDropTarget?.span
        ) {
            return this.paletteDropTarget.span;
        }

        return this.getPaletteDefaultSpanForList(elements!, this.paletteDragType);
    }

    // TODO: Change to static variable
    public isPaletteDropContainer(elements: FormElement[]): boolean {
        return (
            !!this.isPaletteDragging
            && Array.isArray(elements)
            && this.paletteDropTarget?.list === elements
        );
    }

    // TODO: Change to static variable
    isPalettePlaceholderVisible(
        elements: FormElement[] | null | undefined,
        index: number,
    ): boolean {
        return (
            !!this.isPaletteDragging
            && Array.isArray(elements)
            && this.paletteDropTarget?.list === elements
            && this.paletteDropTarget.index === index
        );
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

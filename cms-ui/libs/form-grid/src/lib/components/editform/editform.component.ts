import { Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {
    Element,
    FormFlow,
    ISchema,
    ISchemaFieldProperties,
    Page,
    UiSchema,
} from '@gentics/cms-models';
import {
    IModalOptions,
    ISortableEvent,
    ModalService,
    NotificationService,
} from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';
import { columnOptions } from '../../constants/column-options';
import { lineOptions } from '../../constants/line-options';
import { FormElementModalComponent } from '../form-element-modal/form-element-modal.component';
import { FormGridElementModalComponent } from '../formgrid-element-modal/formgrid-element-modal.component';

type PaletteDragType = 'formgridText' | 'formgridSpacer' | 'formgridImage';

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

@Component({
    selector: 'andp-editform',
    templateUrl: './editform.component.html',
    styleUrls: ['./editform.component.scss'],
})
export class EditformComponent implements OnInit, OnDestroy {
    options = columnOptions;
    lines = lineOptions;
    items: Partial<Element>[] = [];

    public isLoading = true;
    public user = null;
    public meshNodes = null;
    public validKeys;
    public validSubKeys;
    public textSpacer;
    public getKey;
    public uiSchema: UiSchema;
    public isReport: boolean;
    public availableFormFlowTemplates: FormFlow[] = [];
    public formFlowTemplateKey = 'default';
    public schema: ISchema;
    public actanovaFormId;

    pageIndex = 0;
    pages: any[];

    public viewMode: 'editor' | 'preview' = 'editor';

    // Selection state for editor items (selected by element id)
    public selectedElementId: string | null = null;
    // Right-panel embedded editor draft + location
    public selectedElementDraft: Partial<Element> | null = null;
    private selectedElementLoc: { parent: any[]; index: number } | null = null;
    public selectedElementSchema: ISchemaFieldProperties | null = null;

    /** Select an element by its id (used by editor-form clicks) */
    selectElementById(id: string | null, ev?: Event): void {
        if (ev) {
            (ev as any).preventDefault?.();
            (ev as any).stopPropagation?.();
        }
        this.selectedElementId = id;
        if (id) {
            const loc = this.locateElementById(id, this.items as any);
            this.selectedElementLoc = loc
                ? { parent: loc.parent, index: loc.index }
                : null;
            this.selectedElementDraft = loc
                ? JSON.parse(JSON.stringify(loc.element))
                : null;
            this.selectedElementSchema = this.selectedElementDraft
                ? this.findSchemaElement(
                    this.schema?.properties as any,
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

    /** Helper for template class binding */
    isSelected(el: any): boolean {
        return !!el?.id && this.selectedElementId === el.id;
    }

    /** Returns true if the currently selected element is a formgrid element (formgridText/formgridSpacer/formgridImage/...) */
    public get isSelectedFormgridElement(): boolean {
        return !!this.selectedElementDraft?.formGridOptions?.type;
    }

    public get isSelectedNormalElement(): boolean {
        return (
            !!this.selectedElementDraft
            && !this.selectedElementDraft?.formGridOptions?.type
        );
    }

    private locateElementById(
        id: string,
        items: any[] = [],
    ): { parent: any[]; index: number; element: any } | null {
        for (let i = 0; i < (items || []).length; i++) {
            const el = items[i];
            if (el?.id === id) {
                return { parent: items, index: i, element: el };
            }
            if (el?.type === 'aggregate' && Array.isArray(el?.elements)) {
                const nested = this.locateElementById(id, el.elements);
                if (nested) {
                    return nested;
                }
            }
        }
        return null;
    }

    public applySelectedElement(updated: Partial<Element>): void {
        if (!this.selectedElementId || !this.selectedElementLoc || !updated) {
            return;
        }

        const { parent, index } = this.selectedElementLoc;
        if (!Array.isArray(parent) || index < 0 || index >= parent.length) {
            return;
        }

        parent.splice(index, 1, updated as any);

        // Ensure change detection + persistence structures stay in sync
        this.items = [...this.items];
        if (this.pages?.[this.pageIndex]) {
            this.pages[this.pageIndex].elements = this.items;
        }
        if (this.uiSchema?.pages?.[this.pageIndex]) {
            this.uiSchema.pages[this.pageIndex].elements = this.items as any;
        }

        // refresh draft so further edits continue from applied state
        this.selectedElementDraft = JSON.parse(JSON.stringify(updated));
        this.selectedElementSchema = this.selectedElementDraft
            ? this.findSchemaElement(
                this.schema?.properties as any,
                this.selectedElementDraft.id as string,
            ) || null
            : null;

        if (this.viewMode === 'preview') {
            this.ensurePreviewBootstrapData();
            this.previewRemountToken++;
        }
    }

    public applySelectedElementAndClose(updated: Partial<Element>): void {
        this.applySelectedElement(updated);
        this.selectElementById(null);
    }

    public previewFormId = '';
    public previewFormJson = '';
    public previewFeaturesJson = '';
    public previewLanguage = 'de-CH';
    public previewCurrentPage = 0;
    public isPaletteDragging = false;
    public previewRemountToken = 0;

    private readonly PALETTE_MIME = 'application/x-andp-formgrid-palette';

    private resizeActive = false;
    private resizePointerId: number | null = null;
    private resizeStartX = 0;
    private resizeStartSpan = 12;
    private resizeTarget: any = null;
    private resizeSurfaceEl: HTMLElement | null = null;
    private resizeRowBaseCols = 0;
    private resizeRowMaxSpan = 12;

    public resizeOverlayActive = false;
    public resizeOverlaySpan = 12;

    private subscription: Subscription[] = [];

    private paletteDragType: PaletteDragType | null = null;
    private paletteDropTarget: PaletteDropTarget | null = null;
    private paletteDragGhost?: HTMLElement;
    private paletteDragOverFrame = 0;
    private pendingPaletteDropSpan: number | null = null;
    private readonly dropRowTolerance = 10;
    private readonly anonymousTrackIds = new WeakMap<object, string>();

    public trackEditorItem = (index: number, item: any): string => {
        if (item?.id) {
            return item.id;
        }

        if (item && typeof item === 'object') {
            let stableId = this.anonymousTrackIds.get(item);

            if (!stableId) {
                stableId = `tmp-${Math.random().toString(36).slice(2, 9)}`;
                this.anonymousTrackIds.set(item, stableId);
            }

            return stableId;
        }

        return `idx-${index}`;
    };

    constructor(
        private modalService: ModalService,
        public mesh: MeshService,
        private route: ActivatedRoute,
        private zone: NgZone,
        private notification: NotificationService,
    ) {}

    setViewMode(mode: 'editor' | 'preview'): void {
        this.viewMode = mode;

        if (mode === 'preview') {
            this.ensurePreviewBootstrapData();
            this.previewRemountToken++;
        }
    }

    private ensurePreviewBootstrapData(): void {
        this.previewFormId = this.actanovaFormId ?? '';
        this.previewCurrentPage = this.pageIndex ?? 0;

        try {
            this.previewFormJson = JSON.stringify({
                schema: this.schema,
                uiSchema: this.uiSchema,
            });
        } catch {
            this.previewFormJson = '';
        }

        try {
            this.previewFeaturesJson = JSON.stringify(this.mesh?.features ?? {});
        } catch {
            this.previewFeaturesJson = '';
        }

        // Prefer full locale (e.g. de-CH) if available.
        // mesh.language is sometimes only a base language like "de".
        const rawLang = (this.mesh as any)?.language as string | undefined;
        const locales
            = typeof (this.mesh as any)?.getLocales === 'function'
                ? ((this.mesh as any).getLocales() as string[])
                : [];

        if (rawLang) {
            // If rawLang already contains a region (e.g. de-CH), use it.
            if (rawLang.includes('-')) {
                this.previewLanguage = rawLang;
            } else {
                // Try to find a matching full locale from configured locales.
                const match = (locales || []).find((l) =>
                    (l || '').toLowerCase().startsWith(rawLang.toLowerCase() + '-'),
                );
                this.previewLanguage = match || rawLang;
            }
        } else {
            // Fallback: use the first configured locale if present.
            this.previewLanguage
                = locales && locales.length ? locales[0] : this.previewLanguage;
        }
    }

    ngOnInit() {
        this.route.params.subscribe((params) => {
            if (params.id) {
                this.actanovaFormId = params.id;
                this.mesh.loadFeatures().subscribe((features) => {
                    this.mesh.features = features as IFeatures;
                    try {
                        this.previewFeaturesJson = JSON.stringify(features ?? {});
                    } catch (e) {
                        this.previewFeaturesJson = '';
                    }
                    const raw = features['feature_form_flow']?.formFlows;
                    try {
                        this.availableFormFlowTemplates = raw
                            ? (JSON.parse(raw) as FormFlow[])
                            : [];
                    } catch (e) {
                        this.availableFormFlowTemplates = [];
                        console.error('FormFlow-JSON ungültig:', e);
                    }
                    this.subscription.push(
                        this.mesh.loadFormGrid(this.actanovaFormId).subscribe(
                            (form) => {
                                this.schema = form.schema;
                                this.uiSchema = form.uiSchema;
                                this.previewFormId = this.actanovaFormId ?? '';
                                this.ensurePreviewBootstrapData();
                                if (
                                    this.uiSchema.formFlowTemplateKey == undefined
                                    || this.uiSchema.formFlowTemplateKey == null
                                ) {
                                    this.uiSchema.formFlowTemplateKey
                                        = this.availableFormFlowTemplates[0].key;
                                    this.formFlowTemplateKey = this.uiSchema.formFlowTemplateKey;
                                }
                                this.formFlowTemplateKey = this.uiSchema.formFlowTemplateKey;

                                this.validKeys = Object.keys(this.schema.properties).filter(
                                    (key) =>
                                        this.schema.properties[key].type.toLowerCase()
                                        === 'string' && this.schema.properties[key].numberOfLines,
                                );
                                this.validSubKeys = (() => {
                                    const validAggSubKeys = [];
                                    Object.keys(this.schema.properties).map((key) => {
                                        if (
                                            this.schema.properties[key].type.toLowerCase()
                                            === 'aggregate'
                                        ) {
                                            Object.keys(this.schema.properties[key].properties).map(
                                                (aggkey) => {
                                                    if (
                                                        this.schema.properties[key].properties[
                                                            aggkey
                                                        ].type.toLowerCase() === 'string'
                                                        && this.schema.properties[key].properties[aggkey]
                                                            .numberOfLines
                                                    ) {
                                                        validAggSubKeys.push(aggkey);
                                                    }
                                                },
                                            );
                                        }
                                    });
                                    return validAggSubKeys;
                                })();

                                if (this.uiSchema.formwidth) {
                                    this.uiSchema.formwidth = this.uiSchema.formwidth;
                                } else {
                                    this.uiSchema.formwidth = 6;
                                }
                                if (this.uiSchema.formwidthOptimized) {
                                    this.uiSchema.formwidthOptimized
                                        = this.uiSchema.formwidthOptimized;
                                } else {
                                    this.uiSchema.formwidthOptimized = false;
                                }

                                const pages: Page[] = this.uiSchema.pages;
                                function addFormGridOptions(element) {
                                    if (!element.formGridOptions) {
                                        element.formGridOptions = {};
                                    }
                                    if (!element.formGridOptions.numberOfColumns) {
                                        element.formGridOptions.numberOfColumns = 12;
                                    }
                                    if (!element.formGridOptions.overlayOptions) {
                                        element.formGridOptions.overlayOptions = {
                                            isShowOverlay: false,
                                            texts: {
                                                textPre: {},
                                                textPost: {},
                                            },
                                        };
                                    }
                                    return element;
                                }
                                pages.forEach((p) => {
                                    p.elements.forEach((el) => {
                                        addFormGridOptions(el);
                                        if (el && el.elements) {
                                            el.elements.forEach((aggregateElement) => {
                                                addFormGridOptions(aggregateElement);
                                            });
                                        }
                                    });
                                });
                                if (!this.schema || !this.schema['formType']) {
                                    this.isReport = false;
                                }
                                if (this.schema['formType'] === 'report') {
                                    this.isReport = true;
                                } else {
                                    this.isReport = false;
                                }
                                this.pages = pages;

                                this.changePage();
                            },
                            (error) => {
                                console.error(error);
                            },
                            () => {
                                console.log('done');
                            },
                        ),
                    );
                });
            }
        });
    }

    ngOnDestroy() {
        this.onPaletteDragEnd();
        this.subscription.forEach((sub) => sub.unsubscribe());
    }

    changePage(increment: number = 0) {
        this.pageIndex += increment;

        this.items = this.pages[this.pageIndex].elements;

        // Defensive: keep the saved model in sync with the displayed list
        // (prevents stale references when switching pages and then saving)
        if (this.uiSchema?.pages?.[this.pageIndex]) {
            this.uiSchema.pages[this.pageIndex].elements = this.items as any;
        }

        if (this.viewMode === 'preview') {
            this.ensurePreviewBootstrapData();
        }
    }

    sortList(e: ISortableEvent): void {
    // Sort the visible list
        const sorted = e.sort(this.items);

        // Assign a new array reference so change detection always sees the update
        this.items = [...sorted];

        // Persist back into the current page (this.pages is usually a reference to uiSchema.pages,
        // but we also explicitly write to uiSchema to be safe)
        if (this.pages?.[this.pageIndex]) {
            this.pages[this.pageIndex].elements = this.items;
        }
        if (this.uiSchema?.pages?.[this.pageIndex]) {
            this.uiSchema.pages[this.pageIndex].elements = this.items as any;
        }

        // Keep preview in sync
        if (this.viewMode === 'preview') {
            this.ensurePreviewBootstrapData();
            this.previewRemountToken++;
        }
    }

    sortSubList(e: ISortableEvent, index: number): void {
        if (!this.items?.[index]) {
            return;
        }

        const current = this.items[index].elements || [];
        const sorted = e.sort(current);

        // New reference for nested array
        this.items[index].elements = [...sorted];

        // Persist to the backing page data as well
        if (this.pages?.[this.pageIndex]?.elements?.[index]) {
            this.pages[this.pageIndex].elements[index].elements
                = this.items[index].elements;
        }
        if (this.uiSchema?.pages?.[this.pageIndex]?.elements?.[index]) {
            (this.uiSchema.pages[this.pageIndex].elements[index] as any).elements
                = this.items[index].elements;
        }

        if (this.viewMode === 'preview') {
            this.ensurePreviewBootstrapData();
            this.previewRemountToken++;
        }
    }

    onPaletteDragStart(ev: DragEvent, type: PaletteDragType): void {
        this.isPaletteDragging = true;
        this.paletteDragType = type;
        this.paletteDropTarget = null;
        this.pendingPaletteDropSpan = null;

        const transfer = ev?.dataTransfer;

        if (!transfer) {
            return;
        }

        transfer.effectAllowed = 'copy';
        transfer.dropEffect = 'copy';
        transfer.setData(this.PALETTE_MIME, type);
        transfer.setData('text/plain', type);

        const ghost = this.buildPaletteDragGhost(type);

        if (ghost) {
            this.paletteDragGhost = ghost;
            document.body.appendChild(ghost);
            transfer.setDragImage(ghost, 24, 24);
        }
    }

    onPaletteDragEnd(): void {
        this.isPaletteDragging = false;
        this.paletteDragType = null;
        this.paletteDropTarget = null;

        if (this.paletteDragOverFrame) {
            cancelAnimationFrame(this.paletteDragOverFrame);
            this.paletteDragOverFrame = 0;
        }

        if (this.paletteDragGhost) {
            this.paletteDragGhost.remove();
            this.paletteDragGhost = undefined;
        }
    }

    onEditorDragOver(ev: DragEvent): void {
        if (!this.isPaletteDragging) {
            return;
        }

        ev.preventDefault();

        if (ev.dataTransfer) {
            ev.dataTransfer.dropEffect = 'copy';
        }
    }

    onPaletteContainerDragOver(
        event: DragEvent,
        list: any[],
        containerId: string,
    ): void {
        if (
            !this.isPaletteDragging
            || !this.paletteDragType
            || !Array.isArray(list)
        ) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

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
                list,
            );

            if (
                !this.paletteDropTarget
                || this.paletteDropTarget.list !== list
                || this.paletteDropTarget.containerId !== containerId
                || this.paletteDropTarget.index !== nextTarget.index
                || this.paletteDropTarget.span !== nextTarget.span
            ) {
                this.paletteDropTarget = {
                    containerId,
                    list,
                    index: nextTarget.index,
                    span: nextTarget.span,
                };
            }

            this.paletteDragOverFrame = 0;
        });
    }

    onPaletteContainerDrop(
        event: DragEvent,
        list: any[],
        containerId: string,
    ): void {
        if (
            !this.isPaletteDragging
            || !this.paletteDragType
            || !Array.isArray(list)
        ) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        const container = event.currentTarget as HTMLElement | null;
        const fallbackTarget = container
            ? this.calculatePaletteInsertTarget(
                container,
                event.clientX,
                event.clientY,
                list,
            )
            : {
                index: list.length,
                span: this.getPaletteDefaultSpanForList(list, this.paletteDragType),
            };

        const target
            = this.paletteDropTarget
              && this.paletteDropTarget.list === list
              && this.paletteDropTarget.containerId === containerId
                ? this.paletteDropTarget
                : {
                    containerId,
                    list,
                    index: fallbackTarget.index,
                    span: fallbackTarget.span,
                };

        this.onEditorDrop(event, target.index, list, target.span);
        this.onPaletteDragEnd();
    }

    isPaletteDropContainer(list: any[] | null | undefined): boolean {
        return (
            !!this.isPaletteDragging
            && Array.isArray(list)
            && this.paletteDropTarget?.list === list
        );
    }

    isPalettePlaceholderVisible(
        list: any[] | null | undefined,
        index: number,
    ): boolean {
        return (
            !!this.isPaletteDragging
            && Array.isArray(list)
            && this.paletteDropTarget?.list === list
            && this.paletteDropTarget.index === index
        );
    }

    getPalettePlaceholderLabel(): string {
        switch (this.paletteDragType) {
            case 'formgridText':
                return 'Text hier einfügen';
            case 'formgridSpacer':
                return 'Spacer hier einfügen';
            case 'formgridImage':
                return 'Bild hier einfügen';
            default:
                return 'Element hier einfügen';
        }
    }

    getPalettePlaceholderSpan(list: any[] | null | undefined): number {
        if (
            !!this.isPaletteDragging
            && Array.isArray(list)
            && this.paletteDropTarget?.list === list
            && this.paletteDropTarget?.span
        ) {
            return this.paletteDropTarget.span;
        }

        return this.getPaletteDefaultSpanForList(list, this.paletteDragType);
    }

    onEditorDrop(
        ev: DragEvent,
        insertIndex: number,
        items: any[],
        resolvedSpan?: number,
    ): void {
        ev.preventDefault();
        ev.stopPropagation();

        const dt = ev?.dataTransfer;
        if (!dt || !Array.isArray(items)) {
            this.onPaletteDragEnd();
            return;
        }

        const type = (dt.getData(this.PALETTE_MIME)
          || dt.getData('text/plain')) as PaletteDragType;

        if (
            !type
            || !['formgridText', 'formgridSpacer', 'formgridImage'].includes(type)
        ) {
            this.onPaletteDragEnd();
            return;
        }

        const safeIndex = Math.max(0, Math.min(insertIndex, items.length));

        this.pendingPaletteDropSpan
            = resolvedSpan
              ?? (this.paletteDropTarget?.list === items
                  ? this.paletteDropTarget.span
                  : this.getPaletteDefaultSpanForList(items, type));

        const created = this.createPaletteElement(type) as any;
        items.splice(safeIndex, 0, created);

        // Keep backing data structures in sync for both root and nested drops.
        this.items = [...this.items];
        if (this.pages?.[this.pageIndex]) {
            this.pages[this.pageIndex].elements = this.items;
        }
        if (this.uiSchema?.pages?.[this.pageIndex]) {
            this.uiSchema.pages[this.pageIndex].elements = this.items as any;
        }

        this.selectElementById(created.id);

        if (this.viewMode === 'preview') {
            this.ensurePreviewBootstrapData();
            this.previewRemountToken++;
        }

        this.isPaletteDragging = false;
    }

    onEditorDropBeside(
        ev: DragEvent,
        targetIndex: number,
        side: 'left' | 'right',
        items: any[],
    ): void {
        ev.preventDefault();
        ev.stopPropagation();

        const dt = ev?.dataTransfer;
        if (!dt) {
            this.isPaletteDragging = false;
            return;
        }

        const type = (dt.getData(this.PALETTE_MIME)
          || dt.getData('text/plain')) as any;

        if (
            !type
            || !['formgridText', 'formgridSpacer', 'formgridImage'].includes(type)
        ) {
            this.isPaletteDragging = false;
            return;
        }

        const target = items[targetIndex];
        const targetSpan = this.getSpan(target);

        const insertAt = side === 'left' ? targetIndex : targetIndex + 1;
        const safeIndex = Math.max(0, Math.min(insertAt, items.length));

        const remaining = Math.max(1, 12 - targetSpan);
        const newDefaultSpan = remaining;

        const nextTargetSpan = Math.max(1, 12 - newDefaultSpan);
        this.setSpan(target, nextTargetSpan);

        this.pendingPaletteDropSpan = newDefaultSpan;
        const created = this.createPaletteElement(type) as any;
        items.splice(safeIndex, 0, created);

        this.items = [...this.items];
        if (this.pages?.[this.pageIndex]) {
            this.pages[this.pageIndex].elements = this.items;
        }
        if (this.uiSchema?.pages?.[this.pageIndex]) {
            this.uiSchema.pages[this.pageIndex].elements = this.items as any;
        }

        this.selectElementById(created.id);

        if (this.viewMode === 'preview') {
            this.ensurePreviewBootstrapData();
            this.previewRemountToken++;
        }

        this.isPaletteDragging = false;
    }

    onResizeStart(
        ev: PointerEvent,
        index: number,
        items: any[],
        surfaceEl: HTMLElement,
    ): void {
        ev.preventDefault();
        ev.stopPropagation();

        if (this.resizeActive) {
            return;
        }

        const handleEl = ev.currentTarget as HTMLElement | null;
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
        this.resizePointerId = ev.pointerId;
        this.resizeStartX = ev.clientX;
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
            (ev.target as HTMLElement)?.setPointerCapture?.(ev.pointerId);
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

    private createPaletteElement(type: PaletteDragType): Partial<Element> {
        const defaultSpan = this.pendingPaletteDropSpan ?? 12;
        this.pendingPaletteDropSpan = null;

        const el: any = {
            id: uuidv4(),
            type: 'property',
            label: { de: this.getPaletteDefaultLabel(type) },
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
        };

        if (!el.formGridOptions.numberOfColumns) {
            el.formGridOptions.numberOfColumns = defaultSpan;
        }
        if (!el.formGridOptions.overlayOptions) {
            el.formGridOptions.overlayOptions = {
                isShowOverlay: false,
                texts: { textPre: {}, textPost: {} },
            };
        }

        return el as Partial<Element>;
    }

    private getPaletteDefaultLabel(
        type: 'formgridText' | 'formgridSpacer' | 'formgridImage',
    ): string {
        switch (type) {
            case 'formgridText':
                return 'Text';
            case 'formgridSpacer':
                return 'Spacer';
            case 'formgridImage':
                return 'Bild';
            default:
                return type;
        }
    }

    private getSpan(el: any): number {
        const raw = el?.formGridOptions?.numberOfColumns;
        const n = typeof raw === 'string' ? parseInt(raw, 10) : raw;
        if (!n || isNaN(n)) {
            return 12;
        }
        return Math.max(1, Math.min(12, n));
    }

    private setSpan(el: any, span: number): void {
        if (!el.formGridOptions) {
            el.formGridOptions = {};
        }
        el.formGridOptions.numberOfColumns = Math.max(1, Math.min(12, span));
    }

    public buildDependsOnOptions(items: any[] = [], excludeId?: string) {
        return (items || [])
            .filter((opt) => !!opt?.id && opt.id !== excludeId)
            .map((opt) => ({ id: opt.id, label: opt.label }));
    }

    public getElementTypeTag(element: any): string | null {
        if (!element) {
            return null;
        }

        if (element?.formGridOptions?.type) {
            return this.mesh.getFormgridTypeName(element.formGridOptions.type);
        }

        const schemaElement = this.findSchemaElement(
            this.schema?.properties as any,
            element.id,
        );
        const type = (schemaElement?.type || '').toLowerCase();

        switch (type) {
            case 'string':
                return 'Input';
            case 'number':
            case 'integer':
                return 'Input';
            case 'catalog':
            case 'reference':
                return 'Dropdown';
            case 'autocomplete':
                return 'Autocomplete';
            case 'boolean':
                return 'Checkbox';
            case 'date':
                return 'Date';
            case 'time':
                return 'Time';
            case 'datetime':
                return 'Datetime';
            case 'aggregate':
                return 'Aggregate';
            default:
                return schemaElement?.type || null;
        }
    }

    deleteElement(element, index, items: any[]) {
        items.splice(index, 1);
    }

    editFormGridElement(element, index, items: any[]) {
        const options: IModalOptions = {
            padding: true,
            closeOnOverlayClick: false,
            closeOnEscape: true,
        };

        const clone = JSON.parse(JSON.stringify(element));
        this.modalService
            .fromComponent(FormGridElementModalComponent, options, {
                element: clone,
                edit: true,
                dependsOnOptions: this.buildDependsOnOptions(items, element?.id),
            })
            .then((modal) => modal.open())
            .then((result: Element) => {
                if (result && result.formGridOptions.numberOfColumns) {
                    items.splice(index, 1, result);
                }
            });
    }

    editFormElement(element, index, items: any[]) {
        const options: IModalOptions = {
            padding: true,
            closeOnOverlayClick: false,
            closeOnEscape: true,
        };

        const clone = JSON.parse(JSON.stringify(element));
        this.modalService
            .fromComponent(FormElementModalComponent, options, {
                element: clone,
                schema: this.findSchemaElement(this.schema.properties, element.id),
            })
            .then((modal) => modal.open())
            .then((result: Element) => {
                if (result && result.formGridOptions.numberOfColumns) {
                    items.splice(index, 1, result);
                }
            });
    }

    checkItem(element) {
        return this.validKeys && this.validKeys.includes(element.id);
    }

    checkAggItem(element) {
        return this.validSubKeys && this.validSubKeys.includes(element.id);
    }

    addElement(
        element: any,
        index: number,
        items: any[],
        paletteType?: 'formgridText' | 'formgridSpacer' | 'formgridImage',
    ) {
        const options: IModalOptions = {
            padding: true,
            closeOnOverlayClick: false,
            closeOnEscape: true,
        };

        // If we are adding via Drag & Drop from the palette, prefill the modal with the dropped type
        // so the user gets the same flow as the add button (modal decides final element).
        const prefillElement = paletteType
            ? this.createPaletteElement(paletteType)
            : null;

        if (prefillElement && paletteType) {
            items.splice(index, 0, prefillElement as any);

            this.items = [...this.items];
            if (this.pages?.[this.pageIndex]) {
                this.pages[this.pageIndex].elements = this.items;
            }
            if (this.uiSchema?.pages?.[this.pageIndex]) {
                this.uiSchema.pages[this.pageIndex].elements = this.items as any;
            }

            this.selectElementById((prefillElement as any).id);

            if (this.viewMode === 'preview') {
                this.ensurePreviewBootstrapData();
                this.previewRemountToken++;
            }

            return;
        }

        const modalData: any = {
            edit: false,
            dependsOnOptions: this.buildDependsOnOptions(items),
        };

        this.modalService
            .fromComponent(FormGridElementModalComponent, options, modalData)
            .then((modal) => modal.open())
            .then((result: Element) => {
                if (result && result.formGridOptions.numberOfColumns) {
                    items.splice(index, 0, result);
                }
            });
    }

    save() {
        this.uiSchema.formFlowTemplateKey = this.formFlowTemplateKey;

        // Defensive: ensure uiSchema references the latest pages array (including drag&drop ordering)
        if (this.pages) {
            this.uiSchema.pages = this.pages as any;
        }

        this.mesh
            .saveFormGrid(this.actanovaFormId, this.uiSchema)
            .toPromise()
            .then(() => {
                this.notification.show({
                    message: 'Formular gespeichert!',
                    type: 'success',
                });
            })
            .catch(() => {
                this.notification.show({
                    message: 'Fehler beim speichern!',
                    type: 'alert',
                });
            });
    }

    findSchemaElement(properties: string, key: string): ISchemaFieldProperties {
        let value;
        if (typeof properties !== 'undefined') {
            if (properties[key]) {
                return properties[key];
            }
            const that = this;
            Object.keys(properties).some(function (k) {
                if (k === key) {
                    value = properties[k];
                    return true;
                }
                if (properties[k].properties) {
                    value = that.findSchemaElement(properties[k].properties, key);
                    return value !== undefined;
                }
                return false;
            });
        }
        return value;
    }

    private getPaletteDefaultSpanForList(
        list: any[] | null | undefined,
        type: PaletteDragType | null,
    ): number {
        const isRootContainer = list === this.items;

        switch (type) {
            case 'formgridSpacer':
            case 'formgridImage':
            case 'formgridText':
            default:
                return isRootContainer ? 12 : 6;
        }
    }

    private calculatePaletteInsertIndex(
        container: HTMLElement,
        clientX: number,
        clientY: number,
        listLength: number,
    ): number {
        const items = this.readDropItems(container);

        if (!items.length) {
            return 0;
        }

        const rows = this.groupDropItemsIntoRows(items);
        const targetRow = this.pickDropRow(rows, clientY);

        if (!targetRow) {
            return listLength;
        }

        return this.findInsertIndexInRow(targetRow.items, clientX, listLength);
    }

    private readDropItems(container: HTMLElement): DropItemRect[] {
        const result: DropItemRect[] = [];

        for (const child of Array.from(container.children)) {
            if (
                !(child instanceof HTMLElement)
                || child.dataset.dropItem !== 'true'
            ) {
                continue;
            }

            const index = Number(child.dataset.dropIndex);

            if (!Number.isFinite(index)) {
                continue;
            }

            result.push({
                index,
                rect: child.getBoundingClientRect(),
            });
        }

        result.sort((left, right) => {
            if (Math.abs(left.rect.top - right.rect.top) > this.dropRowTolerance) {
                return left.rect.top - right.rect.top;
            }

            return left.rect.left - right.rect.left;
        });

        return result;
    }

    private groupDropItemsIntoRows(items: DropItemRect[]): DropRow[] {
        const rows: DropRow[] = [];

        for (const item of items) {
            const currentRow = rows[rows.length - 1];

            if (
                !currentRow
                || Math.abs(item.rect.top - currentRow.top) > this.dropRowTolerance
            ) {
                rows.push({
                    top: item.rect.top,
                    bottom: item.rect.bottom,
                    items: [item],
                });
                continue;
            }

            currentRow.bottom = Math.max(currentRow.bottom, item.rect.bottom);
            currentRow.items.push(item);
        }

        for (const row of rows) {
            row.items.sort((left, right) => left.rect.left - right.rect.left);
        }

        return rows;
    }

    private pickDropRow(rows: DropRow[], clientY: number): DropRow | null {
        if (!rows.length) {
            return null;
        }

        if (clientY <= rows[0].top) {
            return rows[0];
        }

        const lastRow = rows[rows.length - 1];

        if (clientY >= lastRow.bottom) {
            return lastRow;
        }

        const directHit = rows.find(
            (row) => clientY >= row.top && clientY <= row.bottom,
        );

        if (directHit) {
            return directHit;
        }

        let closestRow = rows[0];
        let closestDistance = Number.POSITIVE_INFINITY;

        for (const row of rows) {
            const middle = row.top + (row.bottom - row.top) / 2;
            const distance = Math.abs(clientY - middle);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestRow = row;
            }
        }

        return closestRow;
    }

    private findInsertIndexInRow(
        rowItems: DropItemRect[],
        clientX: number,
        listLength: number,
    ): number {
        if (!rowItems.length) {
            return listLength;
        }

        for (const item of rowItems) {
            const midpoint = item.rect.left + item.rect.width / 2;

            if (clientX < midpoint) {
                return item.index;
            }
        }

        return Math.min(rowItems[rowItems.length - 1].index + 1, listLength);
    }

    private buildPaletteDragGhost(type: PaletteDragType): HTMLElement {
        const ghost = document.createElement('div');
        ghost.className = 'andp-palette-drag-ghost';
        ghost.innerHTML = `
      <span class="material-icons" aria-hidden="true">${this.getPaletteGhostIcon(
            type,
        )}</span>
      <span>${this.getPaletteGhostText(type)}</span>
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

    private getPaletteGhostIcon(type: PaletteDragType): string {
        switch (type) {
            case 'formgridText':
                return 'text_fields';
            case 'formgridSpacer':
                return 'space_bar';
            case 'formgridImage':
                return 'image';
            default:
                return 'add';
        }
    }

    private getPaletteGhostText(type: PaletteDragType): string {
        switch (type) {
            case 'formgridText':
                return 'Text';
            case 'formgridSpacer':
                return 'Spacer';
            case 'formgridImage':
                return 'Bild';
            default:
                return 'Element';
        }
    }

    private calculatePaletteInsertTarget(
        container: HTMLElement,
        clientX: number,
        clientY: number,
        list: any[],
    ): { index: number; span: number } {
        const defaultSpan = this.getPaletteDefaultSpanForList(
            list,
            this.paletteDragType,
        );
        const rows = this.buildPaletteDropRows(container);

        if (!rows.length) {
            return { index: 0, span: defaultSpan };
        }

        const activeRow = this.findPaletteDropRow(rows, clientY);

        if (!activeRow) {
            if (clientY < rows[0].top - this.dropRowTolerance) {
                return { index: 0, span: defaultSpan };
            }

            for (let i = 0; i < rows.length - 1; i++) {
                const currentRow = rows[i];
                const nextRow = rows[i + 1];

                if (
                    clientY > currentRow.bottom + this.dropRowTolerance
                    && clientY < nextRow.top - this.dropRowTolerance
                ) {
                    return { index: nextRow.items[0].index, span: defaultSpan };
                }
            }

            return { index: list.length, span: defaultSpan };
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
            (sum, item) => sum + this.getItemSpanByIndex(list, item.index),
            0,
        );
        const freeCols = Math.max(0, 12 - usedCols);

        const preferredSpan = this.getPaletteInRowSpan(
            list,
            activeRow,
            index,
            defaultSpan,
        );

        const span
            = freeCols > 0
                ? Math.max(1, Math.min(preferredSpan, freeCols))
                : Math.max(1, Math.min(12, preferredSpan));

        return { index, span };
    }

    private buildPaletteDropRows(container: HTMLElement): DropRow[] {
        const elements = Array.from(container.children).filter(
            (child): child is HTMLElement =>
                child instanceof HTMLElement && child.dataset.dropItem === 'true',
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
                (candidate) =>
                    Math.abs(candidate.top - rect.top) <= this.dropRowTolerance,
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

    private findPaletteDropRow(rows: DropRow[], clientY: number): DropRow | null {
        return (
            rows.find(
                (row) =>
                    clientY >= row.top - this.dropRowTolerance
                    && clientY <= row.bottom + this.dropRowTolerance,
            ) ?? null
        );
    }

    private getPaletteInRowSpan(
        list: any[],
        row: DropRow,
        insertIndex: number,
        fallback: number,
    ): number {
        const rowIndices = new Set(row.items.map((item) => item.index));
        const leftIndex = insertIndex - 1;
        const rightIndex = insertIndex;

        const leftSpan = rowIndices.has(leftIndex)
            ? this.getItemSpanByIndex(list, leftIndex)
            : null;

        const rightSpan = rowIndices.has(rightIndex)
            ? this.getItemSpanByIndex(list, rightIndex)
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

    private getItemSpanByIndex(list: any[], index: number): number {
        return this.getSpan(Array.isArray(list) ? list[index] : null);
    }
}

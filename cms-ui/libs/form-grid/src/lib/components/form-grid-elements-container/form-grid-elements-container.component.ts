import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    input,
    model,
    NgZone,
    OnChanges,
    output,
    signal,
    SimpleChanges,
} from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import {
    FormControlConfiguration,
    FormElement,
    FormElementConfiguration,
    FormSchema,
    FormSchemaProperty,
    FormTypeConfiguration,
    I18nString,
} from '@gentics/cms-models';
import { cancelEvent, ISortableEvent, ModalService } from '@gentics/ui-core';
import { v4 as uuidV4 } from 'uuid';
import { DropRow, ElementSelectionEvent, FormGridEditMode, PALETTE_MIME, PaletteDropTarget } from '../../models';

interface DisplayItem {
    id: string;
    label: I18nString;
    element: FormElement;
    isBlock: boolean;
    isControl: boolean;
    config: FormElementConfiguration;
    schema?: FormSchemaProperty;
}

const DROP_ROW_TOLERANCE = 10;

@Component({
    selector: 'gtx-form-grid-elements-container',
    templateUrl: './form-grid-elements-container.component.html',
    styleUrls: ['./form-grid-elements-container.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormGridElementsContainerComponent implements OnChanges {

    public readonly FormGridEditMode = FormGridEditMode;

    /* BASIC INPUT/OUTPUT
     * ===================================================================== */

    public readonly rootId = input.required<string>();
    public readonly id = input.required<string>();
    public readonly config = input.required<FormTypeConfiguration>();
    public readonly level = input.required<number>();
    public readonly mode = input.required<FormGridEditMode>();
    public readonly pageIndex = input.required<number>();
    public readonly languages = input.required<string[]>();
    public readonly gridSurface = input.required<HTMLElement>();

    public readonly schema = model.required<FormSchema>();
    public readonly elements = model.required<FormElement[]>();
    public readonly selectedElement = input<FormElement | null>();
    public readonly selectedElementContainerId = input<string | null>();
    public readonly elementSelect = output<ElementSelectionEvent | null>();

    /* PALETTE
     * ===================================================================== */

    public readonly paletteDragging = model<boolean>();
    public readonly paletteDropTarget = model<PaletteDropTarget | null>();
    public readonly paletteDragType = input<string | null>();
    public readonly paletteDragConfig = input<FormElementConfiguration | null>();
    public readonly platteDragStop = output<void>();

    /** How many spans/columns the currently dragged element would use if dropped */
    public pendingPaletteDropSpan: number | null;

    private paletteDragOverFrame: number | null = null;

    /* RESIZE
     * ===================================================================== */

    public readonly resizing = model<boolean>();
    public readonly resizeOverlayActive = model<boolean>();
    public readonly resizeOverlaySpan = model<number>();

    private resizePointerId: number | null = null;
    private resizeStartX: number;
    private resizeTarget: FormElement | null = null;
    private resizeSurfaceEl: HTMLElement | null = null;
    private resizeRowBaseCols: number;
    private resizeRowMaxSpan: number;
    private resizeStartSpan: number;

    /* MISC
     * ===================================================================== */

    public readonly displayItems = signal<DisplayItem[]>([]);

    /* CONSTRUCTOR
     * ===================================================================== */

    constructor(
        private changeDetector: ChangeDetectorRef,
        private zone: NgZone,
        private i18n: I18nService,
        private modals: ModalService,
    ) {}

    /* HOOKS
     * ===================================================================== */

    public ngOnChanges(changes: SimpleChanges): void {
        if (changes['elements'] || changes['schema'] || changes['config']) {
            this.updateDisplayItems();
        }
    }

    /* TEMPLATE HANDLERS
     * ===================================================================== */

    public selectElement(element: FormElement, event?: MouseEvent): void {
        cancelEvent(event);

        if (this.resizing()) {
            return;
        }

        this.elementSelect.emit({
            element,
            containerId: this.id(),
        });
    }

    public getDisplaySpan(element: FormElement, index: number): number {
        const isTarget = this.paletteDragging() 
                      && this.paletteDropTarget()?.elementContainerId === this.id();
        const neighbor = this.paletteDropTarget()?.resizeNeighbor;

        if (isTarget && neighbor && neighbor.index === index) {
            return neighbor.span;
        }

        return this.getSpan(element);
    }

    public onPaletteContainerDragOver(event: DragEvent): void {
        if (
            !this.paletteDragging()
            || !this.paletteDragType()
            || !this.paletteDragConfig()
            || !Array.isArray(this.elements())
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

        // Use elementFromPoint for 100% reliable read of cursor target, immune to fast hit-testing loops
        const realTarget = document.elementFromPoint(clientX, clientY) as HTMLElement | null;
        const isHoveringPlaceholder = realTarget && realTarget.closest('.drop-placeholder') !== null;
        const hoveredElementId = realTarget?.closest('gtx-contents-list-item')?.getAttribute('data-element-id');

        if (this.paletteDragOverFrame) {
            cancelAnimationFrame(this.paletteDragOverFrame);
        }

        this.paletteDragOverFrame = requestAnimationFrame(() => {
            const currentTarget = this.paletteDropTarget();

            // Flickering defense: lock the hover state if the DOM shifted the placeholder under the cursor
            // or if we're hitting the specific element we dynamically shrunk to prevent infinite hit-test loops.
            if (currentTarget?.resizeNeighbor) {
                const neighborId = this.elements()[currentTarget.resizeNeighbor.index]?.id;
                if (isHoveringPlaceholder || (hoveredElementId && hoveredElementId === neighborId)) {
                    this.paletteDragOverFrame = 0;
                    return;
                }
            } else if (isHoveringPlaceholder) {
                this.paletteDragOverFrame = 0;
                return;
            }

            const nextTarget = this.calculatePaletteInsertTarget(
                container,
                clientX,
                clientY,
            );

            if (
                !this.paletteDropTarget()
                || this.paletteDropTarget()?.elementContainerId !== this.id()
                || this.paletteDropTarget()?.index !== nextTarget.index
                || this.paletteDropTarget()?.span !== nextTarget.span
                || this.paletteDropTarget()?.resizeNeighbor?.index !== nextTarget.resizeNeighbor?.index
                || this.paletteDropTarget()?.resizeNeighbor?.span !== nextTarget.resizeNeighbor?.span
            ) {
                this.paletteDropTarget.set({
                    elementContainerId: this.id(),
                    index: nextTarget.index,
                    span: nextTarget.span,
                    resizeNeighbor: nextTarget.resizeNeighbor,
                });
            }

            this.paletteDragOverFrame = 0;
        });
    }

    public onPaletteContainerDrop(event: DragEvent): void {
        if (
            !this.paletteDragging()
            || !this.paletteDragType()
            || !this.paletteDragConfig()
            || !Array.isArray(this.elements())
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
            )
            : {
                index: this.elements().length,
                span: this.getPaletteDefaultSpanForList(),
            };

        const target: PaletteDropTarget = (
            this.paletteDropTarget()
            && this.paletteDropTarget()?.elementContainerId === this.id()
        )
            ? this.paletteDropTarget()!
            : {
                elementContainerId: this.id(),
                index: fallbackTarget.index,
                span: fallbackTarget.span,
                resizeNeighbor: fallbackTarget.resizeNeighbor,
            };

        this.insertDropElement(event, target);
        this.cleanupPaletteDrag();
    }

    public onResizeStart(
        event: PointerEvent,
        index: number,
    ): void {
        cancelEvent(event);

        if (this.resizing() || !this.gridSurface()) {
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
                    rowBase += this.getSpan(this.elements()[i]);
                }
            }

            rowBase = Math.max(0, Math.min(11, rowBase));
            rowMax = Math.max(1, 12 - rowBase);
        }

        this.resizing.set(true);
        this.resizePointerId = event.pointerId;
        this.resizeStartX = event.clientX;
        this.resizeTarget = this.elements()[index];
        this.resizeSurfaceEl = this.gridSurface();
        this.resizeRowBaseCols = rowBase;
        this.resizeRowMaxSpan = rowMax;

        const initialSpan = Math.min(
            this.getSpan(this.resizeTarget),
            this.resizeRowMaxSpan,
        );
        this.setSpan(this.resizeTarget, initialSpan);

        this.resizeStartSpan = initialSpan;
        this.resizeOverlayActive.set(true);
        this.resizeOverlaySpan.set(Math.max(
            1,
            Math.min(12, this.resizeRowBaseCols + this.resizeStartSpan),
        ));

        try {
            (event.target as HTMLElement)?.setPointerCapture?.(event.pointerId);
        } catch {}

        this.zone.runOutsideAngular(() => {
            const onMove = (e: PointerEvent) => {
                if (!this.resizing() || this.resizePointerId !== e.pointerId) {
                    return;
                }

                cancelEvent(e);
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
                    this.resizeOverlaySpan.set(Math.max(
                        1,
                        Math.min(12, this.resizeRowBaseCols + nextSpan),
                    ));
                    this.changeDetector.markForCheck();
                });
            };

            const onUp = (e: PointerEvent) => {
                if (
                    !this.resizing()
                    || (this.resizePointerId !== null
                      && this.resizePointerId !== e.pointerId)
                ) {
                    return;
                }

                cancelEvent(e);

                this.resizePointerId = null;
                this.resizeTarget = null;
                this.resizeSurfaceEl = null;
                this.resizeRowBaseCols = 0;
                this.resizeRowMaxSpan = 12;
                this.resizeOverlayActive.set(false);

                // Hacky way to prevent an accidental "click"/selection to occur during resizing of an element
                setTimeout(() => {
                    this.resizing.set(false);
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

    public deleteElement(event: MouseEvent, element: FormElement, index: number): void {
        cancelEvent(event);

        this.modals.dialog({
            title: this.i18n.instant('form_grid.title_delete_element'),
            body: this.i18n.instant('form_grid.confirm_element_delete', {
                name: this.i18n.fromObject(element.label),
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

                if (
                    this.selectedElement()?.id === element.id
                    && this.selectedElementContainerId() === this.id()
                ) {
                    this.elementSelect.emit(null);
                }

                const newElements = this.elements().slice();
                newElements.splice(index, 1);
                this.elements.set(newElements);
                this.updateDisplayItems();
            })
            .catch(() => {
                // Ignore errors
            });
    }

    public sortElements(event: ISortableEvent): void {
        // Sort the visible list
        const newElements = [...this.elements()];
        const sorted = event.sort(newElements);

        // Persist back into the current page (this.pages is usually a reference to uiSchema.pages,
        // but we also explicitly write to uiSchema to be safe)
        this.elements.set(sorted);
        this.updateDisplayItems();
    }

    /* INTERNALS
     * ===================================================================== */

    private updateDisplayItems(): void {
        // Can't use computed, as it wouldn't update/call correctly when updating the elements manually.
        this.displayItems.set((this.elements() || []).map((el) => {
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
        }).filter((item) => item != null));
    }

    private calculatePaletteInsertTarget(
        container: HTMLElement,
        clientX: number,
        clientY: number,
    ): { index: number; span: number; resizeNeighbor?: { index: number; span: number } } {
        const defaultSpan = this.getPaletteDefaultSpanForList();
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

            return { index: this.elements().length, span: defaultSpan };
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
            (sum, item) => sum + this.getItemSpanByIndex(item.index),
            0,
        );
        const freeCols = Math.max(0, 12 - usedCols);

        const preferredSpan = this.getPaletteInRowSpan(
            activeRow,
            index,
            defaultSpan,
        );

        let span: number;
        let resizeNeighbor: { index: number; span: number } | undefined;

        if (freeCols > 0) {
            span = Math.max(1, Math.min(preferredSpan, freeCols));
        } else {
            // No space left: shrink the neighbor element to accommodate the drop safely
            const targetIndex = index === activeRow.items[activeRow.items.length - 1].index + 1
                                ? activeRow.items[activeRow.items.length - 1].index
                                : index;

            const neighborSpan = this.getItemSpanByIndex(targetIndex);
            span = Math.max(1, Math.floor(neighborSpan / 2));
            resizeNeighbor = { index: targetIndex, span: neighborSpan - span };
        }

        return { index, span, resizeNeighbor };
    }

    private setSpan(el: any, span: number): void {
        if (!el.formGridOptions) {
            el.formGridOptions = {};
        }
        // TODO: Update uiSchema?
        el.formGridOptions.numberOfColumns = Math.max(1, Math.min(12, span));
    }

    private insertDropElement(
        event: DragEvent,
        target: PaletteDropTarget,
    ): void {
        cancelEvent(event);

        const dt = event?.dataTransfer;
        if (!dt || !Array.isArray(this.elements())) {
            this.cleanupPaletteDrag();
            return;
        }

        const newElements = [...this.elements()];
        const insertIndex = target.index;
        const resolvedSpan = target.span;
        const resizeNeighbor = target.resizeNeighbor;

        const type = dt.getData(PALETTE_MIME) || dt.getData('text/plain');
        // TODO: Add type check?
        const safeIndex = Math.max(0, Math.min(insertIndex, newElements.length));
        const isControl = this.config().controls[type] != null;

        if (resizeNeighbor) {
            const neighbor = newElements[resizeNeighbor.index];
            if (neighbor) {
                this.setSpan(neighbor, resizeNeighbor.span);
            }
        }

        this.pendingPaletteDropSpan = (resolvedSpan ?? (this.paletteDropTarget()?.elementContainerId === this.id())
            ? this.paletteDropTarget()!.span
            : this.getPaletteDefaultSpanForList()
        );

        const created = this.createPaletteElement(type);
        newElements.splice(safeIndex, 0, created);

        if (isControl) {
            const typeConfig = this.config().controls[type];
            const copy = structuredClone(this.schema());
            copy.properties[created.id] = {
                type: type,
                name: this.i18n.fromObject(typeConfig.labelI18n) || type,
                formPage: this.pageIndex(),
                isList: typeConfig.aggregate,
                validation: {},
            };
            this.schema.set(copy);
        }

        this.elements.set(newElements);
        this.updateDisplayItems();
        this.selectElement(created);
        this.paletteDragging.set(false);
    }

    private cleanupPaletteDrag(): void {
        this.pendingPaletteDropSpan = null;

        if (this.paletteDragOverFrame) {
            cancelAnimationFrame(this.paletteDragOverFrame);
            this.paletteDragOverFrame = 0;
        }

        this.platteDragStop.emit();
    }

    private createPaletteElement(type: string): FormElement {
        const defaultSpan = this.pendingPaletteDropSpan ?? 12;
        this.pendingPaletteDropSpan = null;
        const isAggregate = (this.paletteDragConfig() as FormControlConfiguration).aggregate;

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
            const label = this.paletteDragConfig()?.labelI18n?.[langCode];
            if (label) {
                el.label[langCode] = label;
            }
        }

        return el;
    }

    private getPaletteInRowSpan(
        row: DropRow,
        insertIndex: number,
        fallback: number,
    ): number {
        const rowIndices = new Set(row.items.map((item) => item.index));
        const leftIndex = insertIndex - 1;
        const rightIndex = insertIndex;

        const leftSpan = rowIndices.has(leftIndex)
            ? this.getItemSpanByIndex(leftIndex)
            : null;

        const rightSpan = rowIndices.has(rightIndex)
            ? this.getItemSpanByIndex(rightIndex)
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

    private getItemSpanByIndex(index: number): number {
        return this.getSpan(Array.isArray(this.elements()) ? this.elements()[index] : null);
    }

    private getSpan(el: FormElement | null): number {
        const raw = el?.formGridOptions?.numberOfColumns;
        const n = typeof raw === 'string' ? parseInt(raw, 10) : raw;
        if (n == null || !Number.isInteger(n)) {
            return 12;
        }
        return Math.max(1, Math.min(12, n));
    }

    private findPaletteDropRow(rows: DropRow[], clientY: number): DropRow | null {
        return rows.find((row) =>
            clientY >= row.top - DROP_ROW_TOLERANCE
            && clientY <= row.bottom + DROP_ROW_TOLERANCE,
        ) ?? null;
    }

    private getPaletteDefaultSpanForList(): number {
        return this.id() === this.rootId() ? 12 : 6;
    }
}

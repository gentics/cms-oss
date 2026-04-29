import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
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
    FormBlockConfiguration,
    FormControlConfiguration,
    FormElement,
    FormElementConfiguration,
    FormSchema,
    FormSchemaProperty,
    FormTypeConfiguration,
    I18nString,
} from '@gentics/cms-models';
import { cancelEvent, ISortableEvent, ModalService, SortableGroup } from '@gentics/ui-core';
import { v4 as uuidV4 } from 'uuid';
import {
    ATTR_CONTAINER_ID,
    ATTR_ELEMENT_ID,
    DropRow,
    ElementContainerMoveEvent,
    ElementSelectionEvent,
    FormGridEditMode,
    PALETTE_MIME,
    PaletteDropTarget,
} from '../../models';

interface DisplayItem {
    id: string;
    label: I18nString;
    element: FormElement;
    isBlock: boolean;
    isControl: boolean;
    isAggregate: boolean;
    isContainer: boolean;
    whitelist: string[] | null;
    config: FormElementConfiguration;
    schema?: FormSchemaProperty;
}

const DEFAULT_ELEMENT_SPAN = 12;
const MIN_SPAN_CONTAINER = 6;
const MIN_SPAN_ELEMENT = 3;
const DROP_ROW_TOLERANCE = 0;
/**
 * How close (in px) the cursor must be to a container/aggregate item's left or right edge
 * for the parent grid to claim a "before/after" insert slot. Anywhere else over the body
 * defers to the inner container's own drop handler.
 */
const CONTAINER_EDGE_THRESHOLD_PX = 24;

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

    /** Root element ID */
    public readonly rootId = input.required<string>();
    /** ID of this elements-container. */
    public readonly id = input.required<string>();

    /** The configuration for this form. */
    public readonly config = input.required<FormTypeConfiguration>();
    /** How many levels deep this container is. */
    public readonly level = input.required<number>();
    /** The mode in which the form-grid operates in. */
    public readonly mode = input.required<FormGridEditMode>();
    /** The current page index that is being displayed. */
    public readonly pageIndex = input.required<number>();
    /** All languages the form is available in. */
    public readonly languages = input.required<string[]>();

    /** The schema of the form */
    public readonly schema = model.required<FormSchema>();
    /** The elements of this container */
    public readonly elements = model.required<FormElement[]>();

    /** HTML element which indicates the resizing. */
    public readonly gridSurface = input.required<HTMLElement>();
    /** The whitelist of this container. */
    public readonly whitelist = input<string[] | null>(null);

    /** The currently selected element to be edited */
    public readonly selectedElement = input<FormElement | null>();
    /** The container in which the selected element is in */
    public readonly selectedElementContainerId = input<string | null>();
    /** The type of the element that is being moved/dragged/re-ordererd around. */
    public readonly elementMoving = model<string | null>();

    /** Event to select a element. */
    public readonly elementSelect = output<ElementSelectionEvent | null>();
    /** Event to move an element to another elements-container component */
    public readonly moveToContainer = output<ElementContainerMoveEvent>();

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
    /**
     * Original Y-bounds of the row at the moment resizeNeighbor was activated.
     * Used to prevent the oscillation caused by the shrunk neighbor reflowing
     * into a previous row.
     */
    private lockedResizeRowBounds: { top: number; bottom: number } | null = null;

    /* RESIZE
     * ===================================================================== */

    public readonly resizing = model<boolean>();
    public readonly resizeOverlayActive = model<boolean>();
    public readonly resizeOverlaySpan = model<number>();

    /** Local overlay state — used when this is a nested container (level > 0). */
    public resizeOverlayActiveLocal = signal(false);
    public resizeOverlaySpanLocal = signal(12);

    private resizePointerId: number | null = null;
    private resizeStartX: number;
    private resizeTarget: FormElement | null = null;
    private resizeSurfaceEl: HTMLElement | null = null;
    private resizeRowBaseCols: number;
    private resizeRowMaxSpan: number;
    private resizeStartSpan: number;

    /* SORTABLE GROUP
     * ===================================================================== */

    /** Shared group configuration so elements can be dragged between containers */
    public readonly sortableGroup: SortableGroup = {
        name: 'formgrid-elements',
        pull: () => this.mode() === FormGridEditMode.FULL,
        // Check for the whitelist in the new group we want to drop into, if available
        put: (to) => {
            const targetId = to.el.getAttribute(ATTR_CONTAINER_ID);

            // Always allow elements to be pulled to the root
            if (targetId === this.rootId()) {
                return true;
            }

            return !to.el.classList.contains('drag-blocked');
        },
    };

    /* MISC
     * ===================================================================== */

    public readonly displayItems = signal<DisplayItem[]>([]);

    /** True when palette-dragging is active and this container's whitelist rejects the dragged type. */
    public isDragBlocked = computed(() => {
        const wl = this.whitelist();
        if (!Array.isArray(wl)) {
            return false;
        }

        const elType = this.elementMoving();

        if (elType) {
            return !wl.includes(elType);
        }

        if (!this.paletteDragging()) {
            return false;
        }
        const type = this.paletteDragType();
        return type == null || !wl.includes(type);
    });

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

    public isPaletteContainerType(): boolean {
        const cfg = this.paletteDragConfig() as any;
        return !!(cfg?.container || cfg?.aggregate);
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

        if (!this.isTypeAllowed(this.paletteDragType())) {
            if (event.dataTransfer) {
                event.dataTransfer.dropEffect = 'none';
            }
            event.preventDefault();
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
        const hoveredElementId = realTarget?.closest('gtx-contents-list-item')?.getAttribute(ATTR_ELEMENT_ID);

        if (this.paletteDragOverFrame) {
            cancelAnimationFrame(this.paletteDragOverFrame);
        }

        this.paletteDragOverFrame = requestAnimationFrame(() => {
            const currentTarget = this.paletteDropTarget();

            // for flickering defense purposes
            if (currentTarget?.resizeNeighbor) {
                const neighborId = this.elements()[currentTarget.resizeNeighbor.index]?.id;

                if (isHoveringPlaceholder) {
                    this.paletteDragOverFrame = 0;
                    return;
                }

                if (hoveredElementId && hoveredElementId === neighborId) {
                    // Check if the placeholder is logically before (left) or after (right) the neighbor
                    const isPlaceholderBefore = currentTarget.index <= currentTarget.resizeNeighbor.index;

                    const neighborEl = realTarget?.closest('[data-drop-item="true"]') as HTMLElement | null;
                    if (neighborEl) {
                        const neighborRect = neighborEl.getBoundingClientRect();
                        const midpointX = neighborRect.left + neighborRect.width / 2;
                        const isOnLeftHalf = clientX < midpointX;

                        // Stay locked if placeholder is left AND mouse is on the left half
                        // OR if placeholder is right AND mouse is on the right half
                        if (isPlaceholderBefore && isOnLeftHalf) {
                            this.paletteDragOverFrame = 0;
                            return;
                        } else if (!isPlaceholderBefore && !isOnLeftHalf) {
                            this.paletteDragOverFrame = 0;
                            return;
                        }
                        // If we reached here, the user moved the mouse to the OTHER half of the shrunk element!
                        // We do NOT return/lock, but intentionally fall through so calculatePaletteInsertTarget
                        // is re-run to cleanly swap sides.
                    } else {
                        // Fallback lock if we cannot find the exact bounding box
                        this.paletteDragOverFrame = 0;
                        return;
                    }
                } else {
                    // Cursor is not over the neighbor element (and not over the placeholder).
                    // If we are still within the row's original Y-bounds, lock to prevent the
                    // oscillation where the shrunk neighbor reflows into a previous row, making
                    // the cursor appear to be "below all rows" and resetting the state.
                    if (this.lockedResizeRowBounds
                      && clientY >= this.lockedResizeRowBounds.top
                      && clientY <= this.lockedResizeRowBounds.bottom) {
                        this.paletteDragOverFrame = 0;
                        return;
                    }
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

            // null means: cursor is over the body of a nested container element.
            if (nextTarget == null) {
                this.paletteDragOverFrame = 0;
                return;
            }

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
                this.lockedResizeRowBounds = nextTarget.resizeNeighbor ? (nextTarget.rowBounds ?? null) : null;
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

        // Whitelist enforcement
        if (!this.isTypeAllowed(this.paletteDragType())) {
            event.preventDefault();
            return;
        }

        const container = event.currentTarget as HTMLElement | null;
        const computedTarget = container
            ? this.calculatePaletteInsertTarget(
                container,
                event.clientX,
                event.clientY,
            )
            : null;

        const claimedByThis = this.paletteDropTarget()
          && this.paletteDropTarget()?.elementContainerId === this.id();

        // Cursor is over a nested container's body (calculate returned null).
        // Don't claim the drop — bubble up so the inner container can handle it.
        if (!claimedByThis && computedTarget == null) {
            event.preventDefault();
            return;
        }

        cancelEvent(event);

        const fallbackTarget = computedTarget ?? {
            index: this.elements().length,
            span: DEFAULT_ELEMENT_SPAN,
        };

        const target: PaletteDropTarget = claimedByThis
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
        this.resizeSurfaceEl = parentEl ?? this.gridSurface();
        this.resizeRowBaseCols = rowBase;
        this.resizeRowMaxSpan = rowMax;

        const initialSpan = Math.min(
            this.getSpan(this.resizeTarget),
            this.resizeRowMaxSpan,
        );
        this.setSpan(this.resizeTarget, initialSpan);

        this.resizeStartSpan = initialSpan;
        const initialOverlaySpan = Math.max(1, Math.min(12, this.resizeRowBaseCols + this.resizeStartSpan));
        if (this.level() > 0) {
            this.resizeOverlayActiveLocal.set(true);
            this.resizeOverlaySpanLocal.set(initialOverlaySpan);
        } else {
            this.resizeOverlayActive.set(true);
            this.resizeOverlaySpan.set(initialOverlaySpan);
        }

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
                const minSpan = this.getMinSpan(this.resizeTarget);
                const nextSpan = Math.max(minSpan, Math.min(this.resizeRowMaxSpan, nextRaw));

                this.zone.run(() => {
                    this.setSpan(this.resizeTarget, nextSpan);
                    const nextOverlaySpan = Math.max(1, Math.min(12, this.resizeRowBaseCols + nextSpan));
                    if (this.level() > 0) {
                        this.resizeOverlaySpanLocal.set(nextOverlaySpan);
                    } else {
                        this.resizeOverlaySpan.set(nextOverlaySpan);
                    }
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
                this.resizeOverlayActiveLocal.set(false);

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

        const childCount = Array.isArray(element.elements) ? element.elements.length : 0;
        const hasChildren = childCount > 0;

        this.modals.dialog({
            title: this.i18n.instant(hasChildren
                ? 'form_grid.title_delete_container'
                : 'form_grid.title_delete_element'),
            body: this.i18n.instant(hasChildren
                ? 'form_grid.confirm_container_delete_with_children'
                : 'form_grid.confirm_element_delete', {
                name: this.i18n.fromObject(element.label),
                count: childCount,
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
                this.schema.update((data) => {
                    delete data.properties[element.id];
                    return data;
                });
                this.updateDisplayItems();
            })
            .catch(() => {
                // Ignore errors
            });
    }

    /**
     * Called by a recursively-rendered child container when its element list changes.
     */
    public updateChildElements(parent: FormElement, children: FormElement[]): void {
        const updated = this.elements().map((el) => el.id === parent.id ? { ...el, elements: children } : el);
        this.elements.set(updated);
        this.updateDisplayItems();
    }

    /**
     * Called when SortableJS begins dragging an item from this list.
     */
    public onSortableDragStart(event: ISortableEvent): void {
        const elId = event.item.getAttribute(ATTR_ELEMENT_ID);
        const el = this.elements().find((tmp) => tmp.id === elId);
        if (!el || !el.formGridOptions?.type) {
            return;
        }
        this.elementMoving.set(el?.formGridOptions.type);
    }

    public sortElements(event: ISortableEvent): void {
        // Cross-list moves are handled by onSortableAdd/onSortableRemove
        if (event.from !== event.to) {
            return;
        }

        // Sort the visible list
        const newElements = this.elements().slice(0);
        const sorted = event.sort(newElements);

        // Persist back into the current page (this.pages is usually a reference to uiSchema.pages,
        // but we also explicitly write to uiSchema to be safe)
        this.elements.set(sorted);
        this.updateDisplayItems();
        this.elementMoving.set(null);
    }

    /**
     * Called when SortableJS drops an element INTO this container from another container.
     */
    public onSortableAdd(event: ISortableEvent): void {
        // Revert the DOM insertion SortableJS made — Angular will re-render from the data model.
        if (event.item?.parentNode) {
            event.item.parentNode.removeChild(event.item);
        }

        const fromId = event.from.getAttribute(ATTR_CONTAINER_ID);
        const toId = event.to.getAttribute(ATTR_CONTAINER_ID);
        const elId = (event.item).getAttribute(ATTR_ELEMENT_ID);

        if (!fromId || !toId || !elId) {
            return;
        }

        this.moveToContainer.emit({
            pageIndex: this.pageIndex(),
            elementId: elId,
            fromContainerId: fromId,
            toContainerId: toId,
            targetIndex: Math.max(0, event.newIndex! - 1),
        });

        this.elementMoving.set(null);
    }

    /* INTERNALS
     * ===================================================================== */

    private isTypeAllowed(type: string | null | undefined): boolean {
        const wl = this.whitelist();
        if (!wl || wl.length === 0) {
            return true;
        }
        return type != null && wl.includes(type);
    }

    private updateDisplayItems(): void {
        // Can't use computed, as it wouldn't update/call correctly when updating the elements manually.
        this.displayItems.set((this.elements() || []).map((el) => {
            const itemSchema = this.schema().properties[el.id];

            if (itemSchema) {
                const itemConfig = this.config().controls[itemSchema?.type];

                if (!itemConfig) {
                    return null;
                }

                const isAggregate = !!(itemConfig).aggregate;

                return {
                    id: el.id,
                    label: itemConfig.labelI18n,
                    element: el,
                    isBlock: false,
                    isControl: true,
                    isAggregate,
                    isContainer: false,
                    whitelist: isAggregate ? ((itemConfig as any).whitelist || null) : null,
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

                const isContainer = !!(itemConfig).container;

                return {
                    id: el.id,
                    label: itemConfig.labelI18n,
                    element: el,
                    isControl: false,
                    isBlock: true,
                    isAggregate: false,
                    isContainer,
                    whitelist: isContainer ? ((itemConfig as any).whitelist || null) : null,
                    config: itemConfig,
                };
            }
        }).filter((item) => item != null));
    }

    private calculatePaletteInsertTarget(
        container: HTMLElement,
        clientX: number,
        clientY: number,
    ): { index: number; span: number; resizeNeighbor?: { index: number; span: number }; rowBounds?: { top: number; bottom: number } } | null {
        const defaultSpan = DEFAULT_ELEMENT_SPAN;
        const rows = this.buildPaletteDropRows(container);

        if (!rows.length) {
            return { index: 0, span: defaultSpan };
        }

        const activeRow = this.findPaletteDropRow(rows, clientY);

        if (!activeRow) {
            if (clientY <= rows[0].top - DROP_ROW_TOLERANCE) {
                return { index: 0, span: defaultSpan };
            }

            for (let i = 0; i < rows.length - 1; i++) {
                const currentRow = rows[i];
                const nextRow = rows[i + 1];

                if (
                    clientY >= currentRow.bottom + DROP_ROW_TOLERANCE
                    && clientY <= nextRow.top - DROP_ROW_TOLERANCE
                ) {
                    // The cursor is in the visual gap between two rows → insert a new row before the next
                    return { index: nextRow.items[0].index, span: defaultSpan };
                }
            }

            // Cursor is below the last row (in the bottom padding area) → append new row at the end
            const lastRow = rows[rows.length - 1];
            if (clientY >= lastRow.bottom + DROP_ROW_TOLERANCE) {
                return { index: this.elements().length, span: defaultSpan };
            }

            return { index: this.elements().length, span: defaultSpan };
        }

        let index = activeRow.items[activeRow.items.length - 1].index + 1;

        for (const item of activeRow.items) {
            const el = this.elements()[item.index];
            const isContainerLike = el?.type === 'aggregate' || el?.type === 'container';

            if (isContainerLike) {
                // For containers, only claim a before/after slot when the cursor is
                // near the left/right edge. When the cursor is over the container's body, defer
                // to the inner container's own drop handler.
                const edgeThreshold = Math.min(CONTAINER_EDGE_THRESHOLD_PX, item.rect.width / 4);

                if (clientX < item.rect.left + edgeThreshold) {
                    index = item.index;
                    break;
                }
                if (clientX < item.rect.right - edgeThreshold) {
                    return null;
                }
            } else {
                const midpointX = item.rect.left + item.rect.width / 2;
                if (clientX < midpointX) {
                    index = item.index;
                    break;
                }
            }
        }

        const usedCols = activeRow.items.reduce(
            (sum, item) => sum + this.getItemSpanByIndex(item.index),
            0,
        );
        const freeCols = Math.max(0, 12 - usedCols);

        let span: number;
        let resizeNeighbor: { index: number; span: number } | undefined;

        if (freeCols > 0) {
            // The row already has elements but is not full — fill the remaining space entirely.
            span = freeCols;
        } else {
            // No space left: shrink the neighbor element. Find the element the mouse is physically
            // hovering to use as the shrink target. This ensures that when hovering an element's
            // right half, that specific element shrinks instead of the next one.
            let resizeTargetIndex = index === activeRow.items[activeRow.items.length - 1].index + 1
                ? activeRow.items[activeRow.items.length - 1].index
                : index;

            for (const item of activeRow.items) {
                if (clientX >= item.rect.left && clientX <= item.rect.right) {
                    resizeTargetIndex = item.index;
                    break;
                }
            }

            const neighborSpan = this.getItemSpanByIndex(resizeTargetIndex);
            span = Math.max(1, Math.floor(neighborSpan / 2));
            resizeNeighbor = { index: resizeTargetIndex, span: neighborSpan - span };
        }

        return { index, span, resizeNeighbor, rowBounds: { top: activeRow.top, bottom: activeRow.bottom } };
    }

    private setSpan(el: any, span: number): void {
        if (!el.formGridOptions) {
            el.formGridOptions = {};
        }
        const min = this.getMinSpan(el);
        el.formGridOptions.numberOfColumns = Math.max(min, Math.min(12, span));
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
            : DEFAULT_ELEMENT_SPAN
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
        this.lockedResizeRowBounds = null;
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
        const isAggregate = !!(this.paletteDragConfig() as FormControlConfiguration).aggregate;
        const isContainer = !!(this.paletteDragConfig() as FormBlockConfiguration).container;

        const el: FormElement = {
            id: uuidV4(),
            type: isContainer ? 'container' : (isAggregate ? 'aggregate' : 'property'),
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

        // If it's an aggregate or container, then default an empty items array
        if (isAggregate || isContainer) {
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

    private getMinSpan(el: FormElement | null): number {
        if (el == null) {
            return MIN_SPAN_ELEMENT;
        }
        if (el.type === 'container' || el.type === 'aggregate') {
            return MIN_SPAN_CONTAINER;
        }
        return MIN_SPAN_ELEMENT;
    }

    private getSpan(el: FormElement | null): number {
        const raw = el?.formGridOptions?.numberOfColumns;
        const n = typeof raw === 'string' ? parseInt(raw, 10) : raw;
        if (n == null || !Number.isInteger(n)) {
            return 12;
        }
        const min = this.getMinSpan(el);
        return Math.max(min, Math.min(12, n));
    }

    private findPaletteDropRow(rows: DropRow[], clientY: number): DropRow | null {
        return rows.find((row) =>
            clientY >= row.top - DROP_ROW_TOLERANCE
            && clientY <= row.bottom + DROP_ROW_TOLERANCE,
        ) ?? null;
    }
}

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
    ElementContainerMoveEvent,
    ElementMoveData,
    ElementSelectionEvent,
    FormGridEditMode,
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

const MIN_SPAN_CONTAINER = 6;
const MIN_SPAN_ELEMENT = 3;

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
    public readonly elementMoving = model<ElementMoveData | null>();

    /** Event to select a element. */
    public readonly elementSelect = output<ElementSelectionEvent | null>();
    /** Event to move an element to another elements-container component */
    public readonly moveToContainer = output<ElementContainerMoveEvent>();

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
        pull: (to) => {
            if (this.mode() !== FormGridEditMode.FULL) {
                return false;
            }

            return to.option('group') !== 'form-palette';
        },
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

        const elType = this.elementMoving()?.elementType;

        return elType && !wl.includes(elType);
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
        return this.getSpan(element);
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
        this.elementMoving.set({
            elementType: el.formGridOptions.type,
            inserting: false,
        });
    }

    public sortElements(event: ISortableEvent): void {
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

        // Creating a new element when a palette item has been dragged into the grid
        if (event.item.classList.contains('palette-item')) {
            const elType = event.item.getAttribute('data-element-type')!;
            const isControl = event.item.getAttribute('data-is-control') === 'true';
            const containerType = event.item.getAttribute('data-container-type');

            const createdEl = this.createNewElement(elType, isControl, containerType as any);

            if (isControl) {
                this.schema.update((data) => {
                    const copy = structuredClone(data);
                    copy.properties[createdEl.id] = {
                        type: elType,
                    };
                    return copy;
                });
            }
            this.elements.update((data) => {
                const copy = structuredClone(data);
                copy.splice(event.newDraggableIndex || event.newIndex!, 0, createdEl);
                return copy;
            });

            this.elementMoving.set(null);
            return;
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
            targetIndex: Math.max(0, event.newDraggableIndex! - 1),
        });

        this.elementMoving.set(null);
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

    private setSpan(el: any, span: number): void {
        if (!el.formGridOptions) {
            el.formGridOptions = {};
        }
        const min = this.getMinSpan(el);
        el.formGridOptions.numberOfColumns = Math.max(min, Math.min(12, span));
    }

    private createNewElement(
        type: string,
        isControl: boolean,
        containerType?: 'container' | 'aggregate' | null,
    ): FormElement {
        const elConf = isControl
            ? this.config().controls[type]
            : (this.config().blocks || {})[type];

        const el: FormElement = {
            id: uuidV4(),
            type: containerType || 'property',
            label: {},
            formGridOptions: {
                type,
                numberOfColumns: containerType ? 12 : 6,
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
        if (containerType) {
            el.elements = [];
        }

        for (const langCode of this.languages()) {
            const label = elConf?.labelI18n?.[langCode];
            if (label) {
                el.label[langCode] = label;
            }
        }

        return el;
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
}

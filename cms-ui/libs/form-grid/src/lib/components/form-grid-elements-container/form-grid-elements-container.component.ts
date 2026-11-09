import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    effect,
    input,
    model,
    NgZone,
    output,
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
    I18nString,
} from '@gentics/cms-models';
import { cancelEvent } from '@gentics/common';
import { ISortableEvent, ModalService, SortableGroup } from '@gentics/ui-core';
import { v4 as uuidV4 } from 'uuid';
import {
    ATTR_CONTAINER_ID,
    ATTR_CONTEXT_ID,
    ATTR_ELEMENT_ID,
    ElementContainerMoveEvent,
    ElementMoveData,
    ElementSelectionEvent,
    FormGridEditMode,
    UNIQUE_ID_GLUE,
} from '../../models';
import { blockDragForElement } from '../../utils/dragging';

interface DisplayItem {
    id: string;
    trackId: string;
    label: I18nString;
    element: FormElement;
    isBlock: boolean;
    isControl: boolean;
    isAggregate: boolean;
    isContainer: boolean;
    isUnknown: boolean;
    unknownType: string | null;
    whitelist: string[] | null;
    config: FormElementConfiguration | null;
    schema?: FormSchemaProperty;
    visible: boolean;
}

const MIN_SPAN_CONTAINER = 1;
const MIN_SPAN_ELEMENT = 1;

@Component({
    selector: 'gtx-form-grid-elements-container',
    templateUrl: './form-grid-elements-container.component.html',
    styleUrls: ['./form-grid-elements-container.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormGridElementsContainerComponent {

    public readonly FormGridEditMode = FormGridEditMode;

    /* BASIC INPUT/OUTPUT
     * ===================================================================== */

    /** Root element ID */
    public readonly rootId = input.required<string>();
    /** ID of this elements-container. */
    public readonly id = input.required<string>();
    /** ID of the context this container is in */
    public readonly contextId = input.required<string>();

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
    /** The ID of the parent container */
    public readonly parentId = input.required<string>();
    /** If this element/container is a aggregate */
    public readonly isAggregate = input.required<boolean>();

    /** The schema of the form */
    public readonly schema = model.required<FormSchema>();
    /** The elements of this container */
    public readonly elements = model.required<FormElement[]>();
    /** Map of all elements */
    public readonly elementMap = input.required<Record<string, FormElement>>();
    /** All schema properties */
    public readonly schemaPropertiesMap = input.required<FormSchemaProperties>();

    /** HTML element which indicates the resizing. */
    public readonly gridSurface = input.required<HTMLElement>();
    /** The whitelist of this container. */
    public readonly whitelist = input<string[] | null>(null);

    /** The currently selected element to be edited */
    public readonly selectedElement = input<FormElement | null>();
    /** The ID of the context element the selected element is in. */
    public readonly selectedElementContextId = input<string | null>();
    /** The type of the element that is being moved/dragged/re-ordererd around. */
    public readonly elementMoving = model<ElementMoveData | null>();

    /** Event to select a element. */
    public readonly elementSelect = output<ElementSelectionEvent | null>();
    /** Event to move an element to another elements-container component */
    public readonly moveToContainer = output<ElementContainerMoveEvent>();

    /* RESIZE
     * ===================================================================== */

    public readonly resizing = model<boolean>();

    /** Local overlay state — used when this is a nested container (level > 0). */
    public resizeOverlayActiveLocal = signal(false);
    public resizeOverlaySpanLocal = signal(12);

    private resizePointerId: number | null = null;
    private resizeStartX: number | null = null;
    private resizeTarget: FormElement | null = null;
    private resizeSurfaceEl: HTMLElement | null = null;
    private resizeRowBaseCols: number | null = null;
    private resizeRowMaxSpan: number | null = null;
    private resizeStartSpan: number | null = null;

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
            return !to.el.classList.contains('drag-blocked');
        },
    };

    /**
     * Dragging is blocked if the whitelist doesn't allow the element type,
     * or when the element would cause an ID collision.
     * ID collisions can only appear, when using aggregates, and an element
     * shares the ID on different levels.
     */
    public isDragBlocked = computed(() => {
        const wl = this.whitelist();
        const moveData = this.elementMoving();
        const contextId = this.selfContext();
        const rootId = this.rootId();
        const elMap = this.elementMap();

        // Check it in helper function, which checks the dragging element recursively
        return blockDragForElement(moveData, wl, contextId, rootId, elMap, true);
    });

    /* MISC
     * ===================================================================== */

    public readonly displayItems = signal<DisplayItem[]>([]);

    public readonly selfContext = computed(() => {
        // All signals called so they are correctly observed
        const id = this.id();
        const context = this.contextId();
        return this.isAggregate() ? id : context;
    });

    /* CONSTRUCTOR
     * ===================================================================== */

    constructor(
        private changeDetector: ChangeDetectorRef,
        private zone: NgZone,
        private i18n: I18nService,
        private modals: ModalService,
    ) {
        effect(() => {
            this.updateDisplayItems();
        });
    }

    /* TEMPLATE HANDLERS
     * ===================================================================== */

    public selectElement(element: FormElement, event?: MouseEvent): void {
        cancelEvent(event);

        if (this.resizing()) {
            return;
        }

        if (this.displayItems().find((item) => item.element === element)?.isUnknown) {
            return;
        }

        this.elementSelect.emit({
            elementId: element.id,
            contextId: this.selfContext(),
        });
    }

    public getDisplaySpan(element: FormElement, index: number): number {
        return this.getSpan(element);
    }

    public onResizeStart(
        event: PointerEvent,
        element: FormElement,
    ): void {
        cancelEvent(event);

        if (this.resizing() || !this.gridSurface()) {
            return;
        }

        // The rendered items (and therefore the DOM) are based on the display-items,
        // so the index has to be resolved from there and not from the elements.
        const items = this.displayItems();
        const index = items.findIndex((item) => item.element === element);
        if (index < 0) {
            return;
        }

        const handleEl = event.currentTarget as HTMLElement | null;
        // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
        const hostEl = handleEl?.closest('gtx-sortable-item') as HTMLElement;
        const parentEl = hostEl?.parentElement;

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
                    rowBase += this.getSpan(items[i].element);
                }
            }

            rowBase = Math.max(0, Math.min(11, rowBase));
            rowMax = Math.max(1, 12 - rowBase);
        }

        this.resizing.set(true);
        this.resizePointerId = event.pointerId;
        this.resizeStartX = event.clientX;
        this.resizeTarget = element;
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
        this.resizeOverlayActiveLocal.set(true);
        this.resizeOverlaySpanLocal.set(initialOverlaySpan);

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
                    this.resizeOverlaySpanLocal.set(nextOverlaySpan);
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

    public deleteElement(event: MouseEvent, element: FormElement): void {
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

                // Resolve the index by reference, as the element ID may be missing (broken/migrated data)
                // and the index of the rendered items may differ from the index in the elements.
                const index = this.elements().indexOf(element);
                if (index < 0) {
                    return;
                }

                if (
                    this.selectedElement()?.id === element.id
                    && this.selfContext() === this.selectedElementContextId()
                ) {
                    this.elementSelect.emit(null);
                }

                const newElements = this.elements().slice();
                newElements.splice(index, 1);
                this.elements.set(newElements);
                if (element.id) {
                    this.schema.update((data) => {
                        delete data.properties[element.id];
                        return data;
                    });
                }
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
            elementId: elId,
            contextId: this.selfContext(),
            elementType: el.formGridOptions.type,
            inserting: false,
        });
    }

    public sortElements(event: ISortableEvent): void {
        if (event.from !== event.to) {
            return;
        }

        // The indices of the event are based on the rendered (display) items, which may differ
        // from the elements. Therefore the moved element is resolved by reference and re-inserted
        // in front of the element which follows it in the rendered list.
        const items = this.displayItems();
        const moved = items[event.oldIndex]?.element;

        if (moved != null && event.oldIndex !== event.newIndex) {
            const remainingItems = items.filter((_, idx) => idx !== event.oldIndex);
            const following = remainingItems[event.newIndex]?.element;
            const sorted = this.elements().filter((el) => el !== moved);
            const followingIdx = following != null ? sorted.indexOf(following) : -1;
            sorted.splice(followingIdx < 0 ? sorted.length : followingIdx, 0, moved);

            // Persist back into the current page (this.pages is usually a reference to uiSchema.pages,
            // but we also explicitly write to uiSchema to be safe)
            this.elements.set(sorted);
        }
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
            const elType = event.item.getAttribute('data-element-type');
            const isControl = event.item.getAttribute('data-is-control') === 'true';
            const containerType = event.item.getAttribute('data-container-type');

            this.addNewElement(
                elType,
                this.toElementIndex(event.newDraggableIndex || event.newIndex),
                isControl,
                containerType as any,
            );
            return;
        }

        const fromId = event.from.getAttribute(ATTR_CONTAINER_ID);
        const fromCtx = event.from.getAttribute(ATTR_CONTEXT_ID);
        const toId = event.to.getAttribute(ATTR_CONTAINER_ID);
        const toCtx = event.to.getAttribute(ATTR_CONTEXT_ID);
        const elId = (event.item).getAttribute(ATTR_ELEMENT_ID);

        if (!fromId || !toId || !elId) {
            return;
        }

        this.moveToContainer.emit({
            pageIndex: this.pageIndex(),
            elementId: elId,
            fromContainerId: fromId,
            fromContextId: fromCtx,
            toContainerId: toId,
            toContextId: toCtx,
            targetIndex: Math.max(0, this.toElementIndex(event.newDraggableIndex) - 1),
        });

        this.elementMoving.set(null);
    }

    public setElementVisibility(item: DisplayItem, visibility: boolean, event?: Event): void {
        cancelEvent(event);

        this.elements.update((data) => {
            // Resolved by reference, as the element ID may be missing (broken/migrated data)
            const idx = data.indexOf(item.element);

            if (idx < 0) {
                return data;
            }

            const el = structuredClone(data[idx]);
            if (el.formGridOptions == null) {
                if (item.schema) {
                    el.formGridOptions = {
                        type: item.schema.type,
                    };
                }
            }
            el.formGridOptions.hidden = !visibility;
            const copy = data.slice();
            copy[idx] = el;

            return copy;
        });
    }

    /* INTERNALS
     * ===================================================================== */

    /**
     * Converts an index of the rendered (display) items - like the ones SortableJS provides from the DOM -
     * to the corresponding index in the elements of this container.
     * An index at (or past) the end of the rendered items is mapped to the end of the elements.
     */
    private toElementIndex(displayIndex: number | null | undefined): number {
        const elements = this.elements() || [];
        const items = this.displayItems();

        if (displayIndex == null || !Number.isInteger(displayIndex) || displayIndex <= 0) {
            return 0;
        }
        if (displayIndex >= items.length) {
            return elements.length;
        }

        const idx = elements.indexOf(items[displayIndex].element);
        return idx < 0 ? elements.length : idx;
    }

    private addNewElement(type: string, index: number, isControl: boolean, containerType: 'aggregate' | 'container' | null): void {
        const createdEl = this.createNewElement(type, isControl, containerType);

        if (isControl) {
            this.schema.update((data) => {
                const copy = structuredClone(data);

                copy.properties[createdEl.id] = {
                    type: type,
                    name: createdEl.id,
                    formPage: this.pageIndex(),
                };
                if (containerType === 'aggregate') {
                    copy.properties[createdEl.id].isList = true;
                    copy.properties[createdEl.id].properties = {};
                }

                return copy;
            });
        }
        this.elements.update((data) => {
            const copy = structuredClone(data);
            copy.splice(index, 0, createdEl);
            return copy;
        });

        this.elementMoving.set(null);
        this.selectElement(createdEl);
        this.updateDisplayItems();
    }

    private updateDisplayItems(): void {
        // Can't use computed, as it wouldn't update/call correctly when updating the elements manually.
        // Every element results in exactly one display item (unresolvable ones as placeholder),
        // so the rendered items always correspond to the elements.
        const usedTrackIds = new Set<string>();

        this.displayItems.set((this.elements() || []).map((el, index): DisplayItem => {
            let trackId = el.id;
            if (!trackId || usedTrackIds.has(trackId)) {
                trackId = `__unresolved_element__${index}`;
            }
            usedTrackIds.add(trackId);

            const uniqueElId = this.id() === this.rootId()
                ? el.id
                : `${this.selfContext()}${UNIQUE_ID_GLUE}${el.id}`;
            const itemSchema = this.schemaPropertiesMap()?.[uniqueElId];
            const visible = !el.formGridOptions?.['hidden'];

            const unknownItem = (isControl: boolean, unknownType: string | null): DisplayItem => ({
                id: el.id,
                trackId,
                label: el.label,
                element: el,
                isBlock: !isControl,
                isControl,
                isAggregate: false,
                isContainer: false,
                isUnknown: true,
                unknownType,
                whitelist: null,
                config: null,
                schema: itemSchema,
                visible,
            });

            if (itemSchema) {
                const itemConfig = this.config().controls[itemSchema?.type];

                if (!itemConfig) {
                    console.warn(`Could not find a control config for type "${itemSchema?.type}"! Is the form-type configuration correct? Element "${el.id}" is displayed as unknown element in the editor.`, el);
                    return unknownItem(true, itemSchema?.type ?? null);
                }

                const isAggregate = !!(itemConfig).aggregate;

                return {
                    id: el.id,
                    trackId,
                    label: itemConfig.labelI18n,
                    element: el,
                    isBlock: false,
                    isControl: true,
                    isAggregate,
                    isContainer: false,
                    isUnknown: false,
                    unknownType: null,
                    whitelist: isAggregate ? ((itemConfig as any).whitelist || null) : null,
                    config: itemConfig,
                    schema: itemSchema,
                    visible,
                };
            } else {
                const type = el.formGridOptions?.type;

                if (!type) {
                    console.warn('Could not determine a item type from element! Element is displayed as unknown element in the editor.', el);
                    return unknownItem(false, null);
                }

                const itemConfig = (this.config().blocks || {})[type];

                if (!itemConfig) {
                    console.warn(`Could not find a block config for type "${type}"! Is the form-type configuration correct? Element "${el.id}" is displayed as unknown element in the editor.`, el);
                    return unknownItem(false, type);
                }

                const isContainer = !!(itemConfig).container;

                return {
                    id: el.id,
                    trackId,
                    label: itemConfig.labelI18n,
                    element: el,
                    isControl: false,
                    isBlock: true,
                    isAggregate: false,
                    isContainer,
                    isUnknown: false,
                    unknownType: null,
                    whitelist: isContainer ? ((itemConfig as any).whitelist || null) : null,
                    config: itemConfig,
                    visible,
                };
            }
        }));
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
        containerType: 'container' | 'aggregate' | null,
    ): FormElement {
        const elConf = isControl
            ? this.config().controls[type]
            : (this.config().blocks || {})[type];

        const el: FormElement = {
            id: `${type}_${uuidV4().replaceAll('-', '')}`,
            type: containerType || 'property',
            label: {},
            formGridOptions: {
                type,
                numberOfColumns: containerType ? 12 : 6,
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

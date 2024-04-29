import {
    AfterContentInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ContentChildren,
    ElementRef,
    EventEmitter,
    HostBinding,
    Input,
    OnChanges,
    OnInit,
    Output,
    QueryList,
    SimpleChanges,
} from '@angular/core';
import { distinctUntilChanged } from 'rxjs/operators';
import Sortable from 'sortablejs/modular/sortable.core.esm.js';
import { ISortableEvent, ISortableMoveEvent, SortFunction, SortableGroup } from '../../common';
import { BaseComponent } from '../base-component/base.component';
import { SortableListDragHandleComponent } from '../drag-handle/drag-handle.component';

export function sortFactory(e: ISortableEvent): SortFunction<any> {
    return (source: any[], byReference: boolean = false) => {
        const result: any[] = byReference ? source : source.slice();
        const oldIndex: number = e.oldIndex;
        const newIndex: number = e.newIndex;

        // Check that index i is an integer
        const isInt = (i: any): boolean => Number(i) === i && i % 1 === 0;
        // Check that index i is within the bounds of the array
        const inBounds = (i: number): boolean => 0 <= i && i < result.length;
        // Valid if numeric and in bounds
        const valid = (i: any): boolean => isInt(i) && inBounds(i);

        if (oldIndex !== newIndex && valid(oldIndex) && valid(newIndex)) {
            result.splice(newIndex, 0, result.splice(oldIndex, 1)[0]);
        }

        return result;
    };
}

/**
 * Enables the creation of lists which can be re-ordered by dragging the items. Built on top of
 * [sortablejs](https://github.com/RubaXa/Sortable). Note that this component does not do the actual
 * sorting of the list data - this logic must be implemented by the consumer of the component. However,
 * this component provides a convenience `sort()` function which considerably simplifies this process - see below.
 *
 * ```html
 * <gtx-sortable-list (dragEnd)="sortList($event)">
 *     <gtx-sortable-item *ngFor="let item of items">
 *         <div>{{ item }}</div>
 *     </gtx-sortable-item>
 * </gtx-sortable-list>
 * ```
 *
 * ```typescript
 * items = ['foo', 'bar', 'baz'];
 * sortList(e: ISortableEvent): void {
 *     this.items = e.sort(this.items);
 * }
 * ```
 *
 * ## `ISortableEvent`
 *
 * The `dragEnd` event emits an `ISortableEvent` object. For a full listing of its properties, see the source. Below
 * are the more important properties:
 *
 * | Property       | Type         | Description |
 * | --------       | ------------ | ----------- |
 * | **oldIndex**   | `number`     | The index in the list that the item started from |
 * | **newIndex**   | `number`     | The index in the list that the item was dropped |
 * | **sort()**     | `Function`   | A pre-configured sort function - see below |
 *
 * ## `ISortableEvent.sort()`
 *
 * When the `dragEnd` event is fired, the event object exposes a `sort(array, byReference)` method. This is a convenience method for
 * sorting an array, so that the consumer of this component does not have to re-implement array sorting
 * each time the component is used.
 *
 * The sort function expects an array, and returns a new copy of that array, unless
 * `byReference === true`, in which case the original array is mutated and returned.
 *
 * ```typescript
 * items = [1, 2, 3, 4, 5];
 *
 * sortList(e: ISortableEvent): void {
 *     // assume that the 2nd item was dragged and dropped in the last place.
 *     this.items = e.sort(this.items);
 *     // this.items = [1, 3, 4, 5, 2]
 * }
 * ```
 */
@Component({
    selector: 'gtx-sortable-list',
    templateUrl: './sortable-list.component.html',
    styleUrls: ['./sortable-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SortableListComponent extends BaseComponent implements OnChanges, OnInit, AfterContentInit {

    /**
     * Specify a group to allow dragging items between SortableLists. See
     * [the Sortable docs](https://github.com/RubaXa/Sortable/blob/473bd8fecfd2f2834e4187fb033dfa6912eb3b98/README.md#group-option)
     * for more information.
     */
    @Input()
    public group: SortableGroup;

    /**
     * Explicitly enable/disable the handles for this list.
     * Handles may be set via the `gtx-drag-handle` component.
     * Set to `null` to auto-detect for handles and act accordingly (default).
     */
    @Input()
    public handles: boolean | null = null;

    /**
     * Invoked when an item is moved in the list or between lists. Return `false` to cancel the move.
     */
    @Input()
    public onMove: (e: ISortableMoveEvent) => boolean;

    /**
     * Fired when an item drag is started.
     */
    @Output()
    public dragStart = new EventEmitter<ISortableEvent>();

    /**
     * Fired when an item has been dragged and dropped to a new position in the list.
     */
    @Output()
    public dragEnd = new EventEmitter<ISortableEvent>();

    /**
     * Fired when an item has been dropped onto this list from a different list.
     */
    @Output()
    public addItem = new EventEmitter<ISortableEvent>();

    /**
     * Fired when creating a clone of element.
     */
    @Output()
    public cloneItem = new EventEmitter<ISortableEvent>();

    /**
     * Fired when an item has been remove from this list to a different list.
     */
    @Output()
    public removeItem = new EventEmitter<ISortableEvent>();

    @ContentChildren(SortableListDragHandleComponent)
    public handles$: QueryList<SortableListComponent>;

    @HostBinding('class.gtx-dragging')
    public dragging = false;

    private sortable: Sortable;

    constructor(
        changeDetector: ChangeDetectorRef,
        private elementRef: ElementRef<HTMLElement>,
    ) {
        super(changeDetector);
        this.booleanInputs.push('handles');
    }

    ngOnInit(): void {
        this.sortable = Sortable.create(this.elementRef.nativeElement, {
            animation: 150,
            setData: (dataTransfer: any, dragEl: Element): void => { },
            // dragging started
            onStart: (e: ISortableEvent): void => {
                this.dragging = true;
                this.dragStart.emit(e);
                this.changeDetector.markForCheck();
            },
            disabled: this.disabled,
            // dragging ended
            onEnd: (e: ISortableEvent): void => {
                e.sort = sortFactory(e);
                this.dragging = false;
                this.dragEnd.emit(e);
                this.changeDetector.markForCheck();
            },
            // Element is dropped into the list from another list
            onAdd: (e: ISortableEvent): void => {
                this.addItem.emit(e);
                this.changeDetector.markForCheck();
            },
            // Changed sorting within list
            onUpdate: (e: ISortableEvent): void => { },
            // Called by any change to the list (add / update / remove)
            onSort: (e: ISortableEvent): void => { },
            // Element is removed from the list into another list
            onRemove: (e: ISortableEvent): void => {
                this.removeItem.emit(e);
                this.changeDetector.markForCheck();
            },
            // Attempt to drag a filtered element
            onFilter: (e: ISortableEvent): void => { },
            // Event when you move an item in the list or between lists
            onMove: (e: ISortableMoveEvent): any => {
                this.changeDetector.markForCheck();
                if (typeof this.onMove === 'function') {
                    return this.onMove(e);
                }
            },
        });

        this.sortable.option('onClone', (e: ISortableEvent) => {
            this.cloneItem.emit(e);
            this.changeDetector.markForCheck();
        });

        if (this.group) {
            this.sortable.option('group', this.group);
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.group && this.sortable) {
            this.sortable.option('group', this.group);
        }
        if (changes.handle && !changes.handle.firstChange) {
            this.updateHandleOption();
        }
    }

    ngAfterContentInit(): void {
        this.updateHandleOption();

        this.subscriptions.push(this.handles$.changes.pipe(
            distinctUntilChanged(),
        ).subscribe(() => {
            this.updateHandleOption();
        }));
    }

    protected override onDisabledChange(): void {
        if (this.sortable) {
            this.sortable.option('disabled', this.disabled);
        }
    }

    protected updateHandleOption(): void {
        // Not mounted yet
        if (!this.elementRef?.nativeElement) {
            return;
        }

        const dragHandles = this.elementRef.nativeElement.querySelectorAll('gtx-drag-handle');
        if (this.handles === true || (this.handles == null && dragHandles && dragHandles.length > 0)) {
            this.sortable.option('handle', '.gtx-drag-handle');
        } else {
            this.sortable.option('handle', null);
        }
    }
}

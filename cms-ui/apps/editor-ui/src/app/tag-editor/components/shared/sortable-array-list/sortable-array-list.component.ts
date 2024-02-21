import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ContentChild, EventEmitter, Input, Output, TemplateRef } from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { ISortableEvent, generateFormProvider } from '@gentics/ui-core';

/**
 * Displays an array as a sortable list and allows the removal of items.
 *
 * The items are rendered using the specified content template, e.g.:
 * ```
 * <sortable-array-list [(ngModel)]="items">
 *      <ng-template let-item="item">
 *          {{ item.name }}
 *      </ng-template>
 * </sortable-array-list>
 * ```
 */
@Component({
    selector: 'sortable-array-list',
    templateUrl: './sortable-array-list.component.html',
    styleUrls: ['./sortable-array-list.component.scss'],
    providers: [ generateFormProvider(SortableArrayListComponent) ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SortableArrayListComponent<T> implements ControlValueAccessor {

    /**
     * Whether items may be removed by the user.
     */
    @Input()
    allowRemoval = true;

    /**
     * Whether items may be dragged by the user.
     */
    @Input()
    allowDrag = true;

    /**
     * The current array of items.
     */
    items: T[];

    /**
     * The template for rendering each item.
     */
    @ContentChild(TemplateRef)
    itemTemplate: TemplateRef<any>;

    /**
     * This is event is fired whenever the order of the items has been changed by the user.
     */
    @Output()
    orderChange  = new EventEmitter<T[]>();

    /**
     * This event is fired whever an item has been removed by the user.
     * The parameter is the index of the removed item.
     */
    @Output()
    itemRemove = new EventEmitter<number>();

    showWorkaroundHandle = true;

    /** ControlValueAccessor.onChange */
    private onChange: (items: T[]) => void;

    constructor(private changeDetector: ChangeDetectorRef) { }

    writeValue(items: T[]): void {
        this.items = items;
        this.showWorkaroundHandle = !items;
        this.changeDetector.markForCheck();
    }

    registerOnChange(fn: ((items: T[]) => void)): void {
        this.onChange = fn;
    }

    registerOnTouched(fn: any): void { }

    /**
     * Event handler that is called when the user has finished dragging an item in the list.
     */
    onDragEnd(e: ISortableEvent): void {
        this.items = e.sort(this.items);
        if (this.onChange) {
            this.onChange(this.items);
        }
        this.orderChange.emit(this.items);
        this.changeDetector.markForCheck();
    }

    /**
     * Event handler that is called when the user clicks the remove element button.
     */
    onRemoveClick(index: number): void {
        this.items = this.items.filter((element, i) => i !== index);
        if (this.onChange) {
            this.onChange(this.items);
        }
        this.itemRemove.emit(index);
        this.changeDetector.markForCheck();
    }

}

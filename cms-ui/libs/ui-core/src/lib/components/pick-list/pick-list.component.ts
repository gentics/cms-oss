import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
} from '@angular/core';
import { generateFormProvider } from '../../utils';
import { BaseFormElementComponent } from '../base-form-element/base-form-element.component';

export interface PickListItem {
    id: string | number;
    label: string;
    description?: string;
}

export type PickListValue = PickListItem[];

type PickListSide = 'available' | 'assigned';

/**
 * A dual-list form control which lets users move items between an available list and an assigned list.
 *
 * ```html
 * <gtx-pick-list
 *     [items]="allForms"
 *     [formControl]="formsControl"
 *     availableListLabel="Available forms"
 *     assignedListLabel="Assigned forms"
 * ></gtx-pick-list>
 * ```
 */
@Component({
    selector: 'gtx-pick-list',
    templateUrl: './pick-list.component.html',
    styleUrls: ['./pick-list.component.scss'],
    providers: [generateFormProvider(PickListComponent)],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class PickListComponent
    extends BaseFormElementComponent<PickListValue>
    implements OnChanges {

    /**
     * Label shown above the list containing items which are not currently assigned.
     */
    @Input()
    public availableListLabel = 'Available';

    /**
     * Label shown above the list containing the currently assigned items.
     */
    @Input()
    public assignedListLabel = 'Assigned';

    /**
     * If true, the number of items in each list is shown next to the corresponding list label.
     */
    @Input()
    public showCount = false;

    /**
     * Full set of available items. The current form value determines which of these items are shown
     * in the assigned list and which remain in the available list.
     */
    @Input()
    public items: PickListItem[] = [];

    /**
     * If true, users can move items between the lists using drag and drop.
     */
    @Input()
    public enableDragAndDrop = true;

    /**
     * If true, users can move only the currently selected items between the lists.
     */
    @Input()
    public allowMoveSelected = true;

    /**
     * If true, users can move all items from one list to the other in a single action.
     */
    @Input()
    public allowMoveAll = true;

    /**
     * Blur event.
     */
    @Output()
    public blur = new EventEmitter<PickListValue>();

    /**
     * Focus event.
     */
    @Output()
    public focus = new EventEmitter<PickListValue>();

    /**
     * Items currently shown in the available list.
     */
    public availableItems: PickListItem[] = [];

    /**
     * Items currently shown in the assigned list and therefore represented by the control value.
     */
    public assignedItems: PickListItem[] = [];

    /**
     * Currently selected items on the available side.
     */
    public selectedAvailableItems: PickListItem[] = [];

    /**
     * Currently selected items on the assigned side.
     */
    public selectedAssignedItems: PickListItem[] = [];

    private draggedItem: PickListItem | null = null;

    private draggedFrom: PickListSide | null = null;

    constructor(changeDetector: ChangeDetectorRef) {
        super(changeDetector);
        this.booleanInputs.push('showCount', 'enableDragAndDrop', 'allowMoveSelected', 'allowMoveAll');
    }

    /**
     * Recompute the derived list state whenever the available item source changes.
     */
    public ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.items) {
            this.onValueChange();
        }
    }

    /**
     * Check whether the given item is currently selected on the specified side.
     */
    public isSelected(side: PickListSide, item: PickListItem): boolean {
        const selection = side === 'available' ? this.selectedAvailableItems : this.selectedAssignedItems;
        return selection.some((selected) => this.isSame(selected, item));
    }

    /**
     * Toggle the selection state of an item on the given side and clear the selection on the opposite side.
     */
    public toggleItemSelection(side: PickListSide, item: PickListItem): void {
        if (this.disabled) {
            return;
        }
        const selection = side === 'available' ? this.selectedAvailableItems : this.selectedAssignedItems;
        const index = selection.findIndex((selected) => this.isSame(selected, item));

        if (index > -1) {
            selection.splice(index, 1);
        } else {
            selection.push(item);
        }

        if (side === 'available') {
            this.selectedAssignedItems = [];
        } else {
            this.selectedAvailableItems = [];
        }

        this.changeDetector.markForCheck();
    }

    /**
     * Move the selected available items into the assigned list.
     */
    public moveSelectedToRight(): void {
        if (this.disabled || !this.allowMoveSelected || this.selectedAvailableItems.length === 0) {
            return;
        }

        const newAssignedItems = [...this.assignedItems];
        this.selectedAvailableItems.forEach((item) => {
            if (!newAssignedItems.some((assigned) => this.isSame(assigned, item))) {
                newAssignedItems.push(item);
            }
        });

        this.triggerTouch();
        this.selectedAvailableItems = [];
        this.updateAssignedItems(newAssignedItems);
    }

    /**
     * Remove the selected assigned items from the assigned list.
     */
    public moveSelectedToLeft(): void {
        if (this.disabled || !this.allowMoveSelected || this.selectedAssignedItems.length === 0) {
            return;
        }

        const newAssignedItems = this.assignedItems.filter((assigned) => {
            return !this.selectedAssignedItems.some((selected) => this.isSame(selected, assigned));
        });

        this.triggerTouch();
        this.selectedAssignedItems = [];
        this.updateAssignedItems(newAssignedItems);
    }

    /**
     * Move all available items into the assigned list.
     */
    public moveAllToRight(): void {
        if (this.disabled || !this.allowMoveAll || this.availableItems.length === 0) {
            return;
        }

        this.triggerTouch();
        this.selectedAvailableItems = [];
        this.updateAssignedItems(this.items.slice());
    }

    /**
     * Remove all items from the assigned list.
     */
    public moveAllToLeft(): void {
        if (this.disabled || !this.allowMoveAll || this.assignedItems.length === 0) {
            return;
        }

        this.triggerTouch();
        this.selectedAssignedItems = [];
        this.updateAssignedItems([]);
    }

    /**
     * Start a drag operation and ensure the dragged item is part of the active selection.
     */
    public onDragStart(side: PickListSide, item: PickListItem, event: DragEvent): void {
        if (this.disabled || !this.enableDragAndDrop) {
            event.preventDefault();
            return;
        }

        if (!this.isSelected(side, item)) {
            if (side === 'available') {
                this.selectedAvailableItems = [item];
                this.selectedAssignedItems = [];
            } else {
                this.selectedAssignedItems = [item];
                this.selectedAvailableItems = [];
            }
        }

        this.draggedItem = item;
        this.draggedFrom = side;

        if (event.dataTransfer) {
            event.dataTransfer.effectAllowed = 'move';
            event.dataTransfer.setData('text/plain', 'pick-list-item');
        }

        this.changeDetector.markForCheck();
    }

    /**
     * Allow dropping by preventing the browser's default drag-over handling.
     */
    public onDragOver(event: DragEvent): void {
        if (this.disabled || !this.enableDragAndDrop) {
            return;
        }

        event.preventDefault();
        if (event.dataTransfer) {
            event.dataTransfer.dropEffect = 'move';
        }
    }

    /**
     * Complete a drag operation by moving the current selection to the target side.
     */
    public onDrop(targetSide: PickListSide, event: DragEvent): void {
        if (this.disabled || !this.enableDragAndDrop) {
            return;
        }

        event.preventDefault();

        if (!this.draggedFrom || this.draggedFrom === targetSide) {
            this.clearDragState();
            return;
        }

        this.triggerTouch();
        if (targetSide === 'assigned') {
            this.moveSelectedToRight();
        } else {
            this.moveSelectedToLeft();
        }

        this.clearDragState();
    }

    /**
     * Clear temporary drag state after a drag interaction finishes.
     */
    public onDragEnd(): void {
        if (this.disabled) {
            return;
        }

        this.clearDragState();
    }

    /**
     * Resolve the display label for an item.
     */
    public getItemLabel(item: PickListItem): string {
        return item?.label ?? '';
    }

    /**
     * Resolve the optional secondary description for an item.
     */
    public getItemDescription(item: PickListItem): string {
        return item?.description ?? '';
    }

    /**
     * Emit the current control value when one of the list containers receives focus.
     */
    public inputFocus(): void {
        if (this.disabled) {
            return;
        }

        this.focus.emit(this.value);
    }

    /**
     * Mark the control as touched and emit blur when focus leaves a list container.
     */
    public inputBlur(event: FocusEvent): void {
        event.stopPropagation();
        this.triggerTouch();
        this.blur.emit(this.value);
    }

    /**
     * Forward value changes to Angular forms and immediately rebuild the derived list state.
     */
    override triggerChange(value: PickListValue): void {
        super.triggerChange(value);
        this.onValueChange();
    }

    /**
     * Rebuild the derived available and assigned lists whenever the bound value or item source changes.
     */
    protected onValueChange(): void {
        const selectedValues = Array.isArray(this.value) ? this.value : [];

        this.assignedItems = this.items.filter((item) =>
            selectedValues.some((selected) => this.isSame(selected, item)),
        );

        this.availableItems = this.items.filter((item) =>
            !selectedValues.some((selected) => this.isSame(selected, item)),
        );

        this.selectedAvailableItems = this.selectedAvailableItems.filter((item) =>
            this.availableItems.some((available) => this.isSame(available, item)),
        );
        this.selectedAssignedItems = this.selectedAssignedItems.filter((item) =>
            this.assignedItems.some((assigned) => this.isSame(assigned, item)),
        );

        this.changeDetector.markForCheck();
    }

    /**
     * Compare two items by id while safely handling null values.
     */
    private isSame(value1: PickListItem | null, value2: PickListItem | null): boolean {
        if ((value1 == null && value2 != null) || (value1 != null && value2 == null)) {
            return false;
        }

        if (value1 == null && value2 == null) {
            return true;
        }

        return value1.id === value2.id;
    }

    /**
     * Normalize and emit the new assigned items based on the current `items` input order.
     */
    private updateAssignedItems(items: PickListItem[]): void {
        const normalizedAssignedItems = this.items.filter((item) =>
            items.some((assigned) => this.isSame(assigned, item)),
        );

        this.value = normalizedAssignedItems;
        this.triggerChange(normalizedAssignedItems);
    }

    /**
     * Reset transient drag-and-drop bookkeeping and request a view update.
     */
    private clearDragState(): void {
        this.draggedItem = null;
        this.draggedFrom = null;
        this.changeDetector.markForCheck();
    }
}

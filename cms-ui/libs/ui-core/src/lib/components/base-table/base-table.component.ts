import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, TemplateRef } from '@angular/core';
import {
    CHECKBOX_STATE_INDETERMINATE,
    FALLBACK_TABLE_COLUMN_RENDERER,
    TableAction,
    TableActionClickEvent,
    TableColumn,
    TableRow,
    TableSelection,
    TableSortOrder,
} from '../../common';
import { cancelEvent, toSelectionArray } from '../../utils';
import { BaseComponent } from '../base-component/base.component';

function selectionToMap(selection: string[] | TableSelection): TableSelection {
    const value = {};
    if (selection == null) {
        return value;
    }
    if (Array.isArray(selection)) {
        for (const id of selection) {
            value[id] = true;
        }
    } else if (typeof value === 'object') {
        return selection;
    }
    return value;
}

/**
 * INTERNAL BASE CLASS - Usage of this class outside of this project is heavily discouraged.
 *
 * Base table implementation which is used for the regular table and the trable components.
 * Allows for easier code sharing and normalized usage as they mostly behave the same.
 *
 * This implementation is mostly "pure" - All changes that occur are not directly performed
 * by this component, but have to be done by the parent component.
 */
@Component({
    template: '',
    standalone: false
})
export abstract class BaseTableComponent<T, R extends TableRow<T> = TableRow<T>> extends BaseComponent implements OnChanges {

    public readonly CHECKBOX_STATE_INDETERMINATE = CHECKBOX_STATE_INDETERMINATE;
    public readonly FALLBACK_TABLE_COLUMN_RENDERER = FALLBACK_TABLE_COLUMN_RENDERER;

    /** If this table's rows can be selected. */
    @Input()
    public selectable = false;

    /** If this table can select multiple rows. */
    @Input()
    public multiple = true;

    /** If the actions should not be displayed. */
    @Input()
    public hideActions = false;

    /** If the sortable column-headers should be active. */
    @Input()
    public sortable = true;

    /** Columns that this trable should display. */
    @Input()
    public columns: TableColumn<T>[] = [];

    /** Rows that this trable should display. */
    @Input()
    public rows: R[] = [];

    /** Actions this table can perform. */
    @Input()
    public actions: TableAction<T>[] = [];

    /** ID of a row which should be marked as active */
    @Input()
    public active: string;

    /** The ids of the selected items/rows. */
    @Input({ transform: selectionToMap })
    public selected: string[] | TableSelection = {};

    /** Flag to change the `selectedChange` event type from `string[]` to `TableSelection`. */
    @Input()
    public useSelectionMap = false;

    /** The id of the column which this table is being sorted by. */
    @Input()
    public sortBy: string;

    /** The sort order in which the items are sorted by. */
    @Input()
    public sortOrder: TableSortOrder = TableSortOrder.ASCENDING;

    /** Custom renderers for each column */
    @Input()
    public renderers: { [columnId: string]: TemplateRef<any> } = {};

    /** Filter columns which can be optionally rendered. */
    @Input()
    public filters: { [columnId: string]: TemplateRef<any> } = {};

    /** Event which emits when a clickable cell in a row has been clicked. */
    @Output()
    public rowClick = new EventEmitter<R>();

    /** Event which emits when a table action has been clicked. */
    @Output()
    public actionClick = new EventEmitter<TableActionClickEvent<T>>();

    /** Event which emits when the selection is changed. */
    @Output()
    public selectedChange = new EventEmitter<string[] | TableSelection>();

    /** Event which emits when a row has been selected. */
    @Output()
    public select = new EventEmitter<R>();

    /** Event which emits when a row has been de-selected */
    @Output()
    public deselect = new EventEmitter<R>();

    /** Event which emits when the column that is sorted by is changed. */
    @Output()
    public sortByChange = new EventEmitter<string>();

    /** Event which emits when the sort order is changed. */
    @Output()
    public sortOrderChange = new EventEmitter<TableSortOrder>();

    /** Flag if any actions are actually present. */
    public hasActions = false;

    /** Flag if any filters are visible. */
    public hasFilters = false;

    /** All actions which are for single rows */
    public singleActions: TableAction<T>[] = [];

    /** All actions which are for the selection */
    public multiActions: TableAction<T>[] = [];

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
        this.booleanInputs.push('selectable', 'hideActions', ['multiple', true], ['sortable', true], ['useSelectionMap', false]);
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.actions) {
            // Actions have to be defined and enabled for either single or multiple elements to be shown.
            this.hasActions = Array.isArray(this.actions)
                && (this.actions.filter(action => action.single || action.multiple)).length > 0;
            this.rebuildActions();
        }

        if (changes.columns || changes.filters) {
            this.determineFilterState();
        }
    }

    /** Function for `ngFor` to track items correctly by their ID. */
    public trackElement(index: number, element: TableColumn<T> | R | TableAction<T>): string {
        return element.id;
    }

    public trackRow(index: number, row: R): string {
        let id = row.id;
        if (row.hash) {
            id += `-${row.hash}`;
        }
        return id;
    }

    public updateSortBy(column: TableColumn<T>): void {
        if (!this.sortable || !column.sortable) {
            return;
        }

        // Click on the sort-column will change the sort-order
        if (this.sortBy === column.id) {
            this.sortOrderChange.emit(this.sortOrder === TableSortOrder.ASCENDING ? TableSortOrder.DESCENDING : TableSortOrder.ASCENDING);
        } else {
            this.sortByChange.emit(column.id);
        }
    }

    public updateRowSelection(row: R, event?: MouseEvent): void {
        cancelEvent(event);

        if (!this.multiple) {
            if (this.selected[row.id] === true) {
                this.deselect.emit(row);
                if (this.useSelectionMap) {
                    this.selectedChange.emit({ [row.id]: false });
                } else {
                    this.selectedChange.emit([]);
                }
                return;
            }

            this.select.emit(row);
            if (this.useSelectionMap) {
                this.selectedChange.emit({ [row.id]: true });
            } else {
                this.selectedChange.emit([row.id]);
            }
            return;
        }

        const copy = structuredClone(this.selected) as TableSelection;
        copy[row.id] = copy[row.id] === CHECKBOX_STATE_INDETERMINATE ? true : !copy[row.id];

        if (this.useSelectionMap) {
            this.selectedChange.emit(copy);
            return;
        }

        this.selectedChange.emit(toSelectionArray(copy));
    }

    public handleRowClick(row: R, event: MouseEvent): void {
        cancelEvent(event);
        if (this.disabled) {
            return;
        }

        this.rowClick.emit(row);
    }

    public handleCellClick(row: R, column: TableColumn<T>, event: MouseEvent): void {
        if (column.clickable === false || this.disabled) {
            cancelEvent(event);
            return;
        }

        if (typeof column.clickOverride === 'function') {
            column.clickOverride(row, column, event);
        }
    }

    public preventClick(event: MouseEvent): void {
        cancelEvent(event);
    }

    public handleSingleActionClick(action: TableAction<T>, row: R): void {
        this.actionClick.emit({
            actionId: action.id,
            selection: false,
            item: row.item,
        });
    }

    public handleMultiActionClick(action: TableAction<T>): void {
        this.actionClick.emit({
            actionId: action.id,
            selection: true,
        });
    }

    protected determineFilterState(): void {
        const filterKeys = Object.keys(this.filters || {});
        this.hasFilters = (this.columns || []).some(col => filterKeys.includes(col.id));
        this.changeDetector.markForCheck();
    }

    protected rebuildActions(): void {
        this.singleActions = (this.actions || [])
            .filter(action => action.single);

        this.multiActions = (this.actions || [])
            .filter(action => action.multiple);
    }
}

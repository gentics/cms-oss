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
import { TableRow, TableSelectAllType, TableSelection } from '../../common';
import { cancelEvent, randomId, toSelectionArray } from '../../utils';
import { BaseTableComponent } from '../base-table/base-table.component';

/**
 * A regular Table component which displays elements in a table view.
 */
@Component({
    selector: 'gtx-table',
    templateUrl: './table.component.html',
    styleUrls: ['./table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TableComponent<T> extends BaseTableComponent<T, TableRow<T>> implements OnChanges {

    public readonly TableSelectAllType = TableSelectAllType;

    public readonly UNIQUE_ID = `gtx-table-${randomId()}`;

    /** If this table's content is paginated and should display the pagination. */
    @Input()
    public paginated = true;

    /** How the selection handling should be handled when the checkbox in the header is clicked. */
    @Input()
    public selectAllType: TableSelectAllType = TableSelectAllType.PAGE;

    /** Which page this table is currently on. */
    @Input()
    public page = 0;

    /** How many rows are displayed per page. */
    @Input()
    public perPage = 10;

    /**
     * The total amount of items/rows that exist.
     * May only be set if the table should not automatically paginate the rows.
     */
    @Input()
    public totalCount: number | null = null;

    /** Event which emits when the page is changed. */
    @Output()
    public pageChange = new EventEmitter<number>();

    /** Event which emits when the `selectAllType` is `ALL` and should update the current selection. */
    @Output()
    public selectAll = new EventEmitter<boolean>();

    /** Flag if all rows are currently selected. */
    public allSelected = false;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
        this.booleanInputs.push(['paginated', true]);
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.selectAllType && this.selectAllType == null) {
            // Fix null values
            this.selectAllType = TableSelectAllType.NONE;
        }

        if (changes.selected || changes.rows) {
            this.recalculateAllSelected();
        }
    }

    public toggleAllSelections(event: MouseEvent): void {
        cancelEvent(event);

        if (this.selectAllType == null || this.selectAllType === TableSelectAllType.NONE) {
            // Nothing to do
            return;
        }

        if (this.selectAllType === TableSelectAllType.ALL) {
            this.selectAll.emit(!this.allSelected);
            return;
        }

        const copy = structuredClone(this.selected) as TableSelection;
        for (const row of this.rows) {
            copy[row.id] = !this.allSelected;
        }

        if (this.selectionMap) {
            this.selectedChange.emit(copy);
            return;
        }

        this.selectedChange.emit(toSelectionArray(copy));
    }

    public handlePageChange(toPage: number): void {
        if (this.totalCount == null) {
            this.page = toPage;
        }
        this.pageChange.emit(toPage)
    }

    protected recalculateAllSelected(): void {
        if (this.selectAllType == null || this.selectAllType !== TableSelectAllType.PAGE) {
            return;
        }

        this.allSelected = (this.rows || []).every(row => this.selected[row.id] === true);
    }
}

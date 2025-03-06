import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { TrableRow, TrableRowExpandEvent } from '../../common';
import { cancelEvent } from '../../utils';
import { BaseTableComponent } from '../base-table/base-table.component';

function createRowId(row: TrableRow<any>): string {
    let id = row.id;
    if (row.hash) {
        id += `-${row.hash}`;
    }

    if (row.expanded && row.hasChildren && row.children?.length > 0) {
        let childHash = ':';
        for (let i = 0; i < row.children.length; i++) {
            if (i > 0) {
                childHash += ',';
            }
            childHash += createRowId(row.children[i]);
        }
        id += childHash;
    }

    return id;
}

/**
 * Trable (Tree-Table) which displays hierachical elements in a Table.
 * Major difference to a regular table is, that a paginated view is not easily
 * done and therefore not supported.
 * Just like the table component, this component is "pure" - Changes to the rows
 * have to be performed by the parent element to properly take effect.
 */
@Component({
    selector: 'gtx-trable',
    templateUrl: './trable.component.html',
    styleUrls: ['./trable.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrableComponent<T> extends BaseTableComponent<T, TrableRow<T>> {

    /**
     * If the trable expansion toggle should be displayed inline in the first column
     * instead of creating an initial separate one.
     */
    @Input()
    public inlineExpansion = false;

    /**
     * If the trable selection checkboxes should be displayed inline in the first colunn
     * instead of creating an initial separate one.
     */
    @Input()
    public inlineSelection = false;

    /** Event which is triggered when a row should start loading. */
    @Output()
    public loadRow = new EventEmitter<TrableRow<T>>();

    /** Event which emits when a row is supposed to change the expansion flag. */
    @Output()
    public rowExpand = new EventEmitter<TrableRowExpandEvent<T>>();

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
        this.booleanInputs.push(['inlineExpansion', false], ['inlineSelection', false]);
    }

    public expandRow(row: TrableRow<T>, event?: MouseEvent): void {
        cancelEvent(event);

        if (!row.hasChildren) {
            return;
        }

        if (row.loaded) {
            this.rowExpand.emit({ row, expanded: !row.expanded });
            return;
        }

        this.loadRow.emit(row);
    }

    public override trackRow(index: number, row: TrableRow<T>): string {
        let id = createRowId(row);
        id += `:${row.loaded ? '0' : '1'}:${row.loading ? '0' : '1'}:${row.expanded ? '0' : '1'}`;

        return id;
    }
}

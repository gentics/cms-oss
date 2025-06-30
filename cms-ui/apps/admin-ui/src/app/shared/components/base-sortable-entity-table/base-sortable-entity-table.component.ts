import { BO_ID, BO_NEW_SORT_ORDER, SortableBusinessObject, TableSortEvent } from '@admin-ui/common';
import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { ChangesOf, TableActionClickEvent, TableRow, coerceToBoolean } from '@gentics/ui-core';
import { BaseEntityTableComponent } from '../base-entity-table/base-entity-table.component';

type MoveFn = (idx: number, total: number) => number;

export const MOVE_TO_TOP_ACTION = 'moveToTop';
export const MOVE_UP_ACTION = 'moveUp';
export const MOVE_DOWN_ACTION = 'moveDown';
export const MOVE_TO_BOTTOM_ACTION = 'moveToBottom';

@Component({ template: '' })
export abstract class BaseSortableEntityTableComponent<T, O = T & SortableBusinessObject, A = never>
    extends BaseEntityTableComponent<T, O, A>
    implements OnChanges {

    @Input()
    public sorting = false;

    @Input()
    public rows: TableRow<O>[] = [];

    @Output()
    public sort = new EventEmitter<TableSortEvent<O>>();

    public override ngOnChanges(changes: ChangesOf<this>): void {
        super.ngOnChanges(changes);

        if (changes.sorting) {
            this.sorting = coerceToBoolean(this.sorting);
            this.actionRebuildTrigger.next();
        }
    }

    protected findRowById(boId: string): TableRow<O> {
        return this.rows.find(row => row.id === boId);
    }

    protected getMoveFn(actionId: string): MoveFn {
        switch (actionId) {
            case MOVE_TO_TOP_ACTION:
                return () => 0;
            case MOVE_UP_ACTION:
                return (idx) => idx - 1;
            case MOVE_DOWN_ACTION:
                return (idx) => idx + 1;
            case MOVE_TO_BOTTOM_ACTION:
                return (idx, len) => len - 1;
        }
    }

    protected moveRow(row: TableRow<O>, fn: MoveFn): void {
        if (row == null) {
            return;
        }

        const from = row.item[BO_NEW_SORT_ORDER];
        const to = fn(from, this.rows.length);
        this.sort.emit({ row, from, to });
    }

    override handleAction(event: TableActionClickEvent<O>): void {
        switch (event.actionId) {
            case MOVE_TO_TOP_ACTION:
            case MOVE_UP_ACTION:
            case MOVE_DOWN_ACTION:
            case MOVE_TO_BOTTOM_ACTION:
                this.moveRow(this.findRowById(event.item[BO_ID]), this.getMoveFn(event.actionId));
                return;
        }

        super.handleAction(event);
    }
}

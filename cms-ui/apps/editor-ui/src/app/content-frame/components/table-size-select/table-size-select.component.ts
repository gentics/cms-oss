import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';

export interface TableSizeSelectEvent {
    columns: number;
    rows: number;
}

@Component({
    selector: 'gtx-table-size-select',
    templateUrl: './table-size-select.component.html',
    styleUrls: ['./table-size-select.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TableSizeSelectComponent {

    @Input()
    public maxColumns = 10;

    @Input()
    public maxRows = 10;

    @Output()
    public select = new EventEmitter<TableSizeSelectEvent>();

    constructor(
        public changeDetector: ChangeDetectorRef,
    ) {}

    public activeColumn = -1;
    public activeRow = -1;

    public setActivePosition(col: number, row: number): void {
        this.activeColumn = col;
        this.activeRow = row;
        this.changeDetector.markForCheck();
    }

    public triggerSelection(columnIdx: number, rowIdx: number): void {
        this.select.emit({ columns: columnIdx + 1, rows: rowIdx + 1 });
    }
}

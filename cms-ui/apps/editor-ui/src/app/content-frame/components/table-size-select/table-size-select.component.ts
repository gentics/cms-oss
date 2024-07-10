import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { TableSize } from '@gentics/aloha-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-table-size-select',
    templateUrl: './table-size-select.component.html',
    styleUrls: ['./table-size-select.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(TableSizeSelectComponent)],
})
export class TableSizeSelectComponent extends BaseFormElementComponent<TableSize> {

    @Input()
    public maxColumns = 10;

    @Input()
    public maxRows = 10;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    public activeColumn = -1;
    public activeRow = -1;

    public setActivePosition(col: number, row: number): void {
        this.activeColumn = col;
        this.activeRow = row;
        this.changeDetector.markForCheck();
    }

    public triggerSelection(columnIdx: number, rowIdx: number): void {
        const size = { columns: columnIdx + 1, rows: rowIdx + 1 };
        this.triggerChange(size);
    }

    protected onValueChange(): void {
        // No op
    }
}

import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { TableSize } from '@gentics/aloha-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-table-size-input',
    templateUrl: './table-size-input.component.html',
    styleUrls: ['./table-size-input.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(TableSizeInputComponent)],
})
export class TableSizeInputComponent extends BaseFormElementComponent<TableSize> implements OnChanges {

    @Input()
    public value: TableSize = { columns: 3, rows: 2 };

    @Input()
    public maxColumns = 10;

    @Input()
    public maxRows = 10;

    public max: TableSize;

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);
        if (changes.maxColumns || changes.maxRows) {
            this.max = { columns: this.maxColumns, rows: this.maxRows };
        }
    }

    protected onValueChange(): void {
        if (this.value == null) {
            this.value = { columns: 0, rows: 0 };
        }
    }

    public modify(key: keyof TableSize, value: number): void {
        const newValue = {
            ...this.value,
            // Clamp the value
            [key]: Math.max(Math.min(this.value[key] + value, this.max[key]), 0),
        };

        this.triggerChange(newValue);
    }
}

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { BaseControlsComponent } from '../base-controls/base-controls.component';

enum TablePart {
    CELL = 'cell',
    ROW = 'row',
    COLUMN = 'column',
    CAPTION = 'caption',
}

@Component({
    selector: 'gtx-table-controls',
    templateUrl: './table-controls.component.html',
    styleUrls: ['./table-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TableControlsComponent extends BaseControlsComponent {

    public readonly TablePart = TablePart;

    public active = false;
    public activePart: TablePart | null = null;

    public toggleActive(): void {
        if (!this.active) {
            this.cycleTablePart();
        }
        this.active = !this.active;
    }

    public cycleTablePart(): void {
        const entries = Object.entries(TablePart);
        let idx = entries.findIndex(e => e[1] === this.activePart);
        if (idx === -1 || (idx + 1) >= entries.length) {
            idx = 0;
        } else {
            idx++;
        }
        this.activePart = entries[idx][1];
    }
}

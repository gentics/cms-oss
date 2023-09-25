import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnChanges, SimpleChanges } from '@angular/core';
import {
    COMMAND_LINK,
    COMMAND_TABLE,
    LIST_COMMANDS,
    NODE_NAME_TO_COMMAND,
    STYLE_COMMANDS,
    TABLE_NODE_NAME,
    TYPOGRAPHY_COMMANDS,
} from '../../../common/models/aloha-integration';
import { BaseControlsComponent } from '../base-controls/base-controls.component';

enum TablePart {
    CELL = 'cell',
    ROW = 'row',
    COLUMN = 'column',
    CAPTION = 'caption',
}

interface TableSettings {
    activePart: TablePart;
}

@Component({
    selector: 'gtx-formatting-controls',
    templateUrl: './formatting-controls.component.html',
    styleUrls: ['./formatting-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormattingControlsComponent extends BaseControlsComponent implements OnChanges {

    public readonly TablePart = TablePart;
    public readonly STYLE_COMMANDS = STYLE_COMMANDS;
    public readonly LIST_COMMANDS = LIST_COMMANDS;
    public readonly TYPOGRAPHY_COMMANDS = TYPOGRAPHY_COMMANDS;
    public readonly COMMAND_LINK = COMMAND_LINK;
    public readonly COMMAND_TABLE = COMMAND_TABLE;

    public activeFormats: string[] = [];
    public linkActive = false;

    public table: TableSettings = {
        activePart: null,
    }

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    public ngOnChanges(changes: SimpleChanges): void {
        if (changes.range || changes.settings) {
            this.updateStateFromAloha();
        }
    }

    public updateStateFromAloha(): void {
        const newFormat = new Set<string>();

        if (!this.range) {
            this.activeFormats = Array.from(newFormat);
            return;
        }

        this.table.activePart = null;

        // eslint-disable-next-line @typescript-eslint/prefer-for-of
        for (let i = 0; i < this.range.markupEffectiveAtStart.length; i++) {
            const elem = this.range.markupEffectiveAtStart[i];
            const command = NODE_NAME_TO_COMMAND[elem.nodeName];

            if (command) {
                newFormat.add(command);
                continue;
            }

            if (elem.nodeName === TABLE_NODE_NAME && elem.classList.contains(TABLE_NODE_NAME)) {
                newFormat.add(COMMAND_TABLE);
                // Set CELL as default
                this.table.activePart = TablePart.CELL;

                // TODO: Figure out how table plugin determines which part is selected
                // for (let j = 0; j < i; j++) {
                //     const tmp = this.range.markupEffectiveAtStart[j];
                //     switch (tmp.nodeName) {
                //         case TABLE_CELL_NODE_NAME:
                //             this.table.activePart = TablePart.CELL;
                //             break;

                //         default:
                //             continue;
                //     }
                //     break;
                // }

                continue;
            }
        }

        this.activeFormats = Array.from(newFormat);
    }

    public toggleFormat(format: string): void {
        const idx = this.activeFormats.indexOf(format);

        // Push/Splice can't be used, as the change detection isn't triggered, because it's still
        // the same array. Even with `markForCheck`, it simply doesn't re-run the includes pipe.
        if (idx === -1) {
            this.activeFormats = [...this.activeFormats, format];
            if (format === 'table') {
                this.cycleTablePart();
            }
        } else {
            this.activeFormats = [
                ...this.activeFormats.slice(0, idx),
                ...this.activeFormats.slice(idx + 1),
            ];
        }
    }

    public setLinkActive(active: boolean): void {
        this.linkActive = active;
        this.changeDetector.markForCheck();
    }

    public cycleTablePart(): void {
        const entries = Object.entries(TablePart);
        let idx = entries.findIndex(e => e[1] === this.table.activePart);
        if (idx === -1 || (idx + 1) >= entries.length) {
            idx = 0;
        } else {
            idx++;
        }
        this.table.activePart = entries[idx][1];
    }
}

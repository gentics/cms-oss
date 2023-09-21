import { ChangeDetectionStrategy, Component } from '@angular/core';
import { BaseControlsComponent } from '../base-controls/base-controls.component';

interface LinkSettings {
    target?: string | {
        type: 'page' | 'file' | 'image';
        displayName: string;
        id: number;
        nodeId?: number;
    };
    anchor: string;
    newTab: boolean;
    title?: string;
    language?: string;
}

interface LinkCheckerSettings {
    enabled?: boolean;
    valid?: boolean;
    report?: LinkCheckerReport;
}

interface LinkCheckerReport {
    date: Date;
    url: string;
    text: string;
}

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
export class FormattingControlsComponent extends BaseControlsComponent {

    public readonly TablePart = TablePart;

    public activeFormats: string[] = [];

    public link: LinkSettings = {
        anchor: '',
        newTab: false,
    };

    public linkChecker: LinkCheckerSettings = {
        enabled: true,
        valid: true,
        report: {
            date: new Date(),
            url: 'https://example.copm',
            text: 'sample text',
        },
    };

    public table: TableSettings = {
        activePart: null,
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

    public setLinkNewTab(newTab: boolean): void {
        this.link.newTab = newTab;
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

    public toggleLinkCheckValidity(): void {
        this.linkChecker.valid = !this.linkChecker.valid;
    }

    public selectLinkTarget(): void {
        alert('todo, work in progress!');
    }
}

import { ChangeDetectionStrategy, Component, OnChanges, SimpleChanges, ViewChild, ChangeDetectorRef } from '@angular/core';
import {
    COMMAND_TABLE,
    NODE_NAME_TO_COMMAND,
    TABLE_CAPTION_NODE_NAME,
    TABLE_CELL_HEADER_NODE_NAME,
    TABLE_NODE_NAME,
    TABLE_SUMMARY_ATTRIBUTE,
} from '@editor-ui/app/common/models/aloha-integration';
import { AlohaTablePlugin, AlohaTableSelection, AlohaTableSelectionColumns, AlohaTableSelectionRows } from '@gentics/aloha-models';
import { DropdownListComponent } from '@gentics/ui-core';
import { BaseControlsComponent } from '../base-controls/base-controls.component';
import { TableSizeSelectEvent } from '../table-size-select/table-size-select.component';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';

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
export class TableControlsComponent extends BaseControlsComponent implements OnChanges {

    public readonly TablePart = TablePart;

    @ViewChild('dropdown', { static: false })
    public tableCreateDropdown: DropdownListComponent;

    public active = false;
    public allowed = false;
    public activePart: TablePart | null = null;
    public isHeader = false;
    public selectionActive = false;
    public canMergeCells = false;
    public canSplitCells = false;

    public summaryValue: string;

    public tableElement: HTMLTableElement | null;
    public captionElement: HTMLTableCaptionElement | null;

    public tablePlugin: AlohaTablePlugin;
    public tableSelection: AlohaTableSelection;

    constructor(
        changeDetector: ChangeDetectorRef,
        protected i18n: I18nService,
    ) {
        super(changeDetector);
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.aloha) {
            this.tablePlugin = this.safeRequire('table/table-plugin');
            this.tableSelection = this.safeRequire('table/table-selection');

            if (this.aloha) {
                this.aloha.bind('aloha-table-selection-changed', () => {
                    this.selectionOrEditableChanged();
                });
                this.aloha.bind('aloha-selection-changed', () => {
                    this.selectionOrEditableChanged();
                });
            }
        }
    }

    protected override selectionOrEditableChanged(): void {
        // Reset state
        this.active = false;
        this.allowed = false;
        this.activePart = null;
        this.isHeader = false;
        this.selectionActive = false;
        this.tableSelection = null;
        this.canMergeCells = false;
        this.canSplitCells = false;
        this.captionElement = null;
        this.tableElement = null;

        if (!this.range || !this.tablePlugin || !this.range.markupEffectiveAtStart) {
            return;
        }

        this.allowed = (this.tablePlugin.getEditableConfig(this.aloha.activeEditable?.obj) || [])
            .filter(nodeName => this.contentRules.isAllowed(this.aloha.activeEditable?.obj, nodeName))
            .map((nodeName: string) => NODE_NAME_TO_COMMAND[nodeName.toUpperCase()])
            .filter(val => val != null)
            .includes(COMMAND_TABLE);


        let captionFocused = false;
        // First we need to check if we're in a table with the cursor
        for (const elem of this.range.markupEffectiveAtStart) {
            if (elem.nodeName === TABLE_NODE_NAME && elem.classList.contains(this.tablePlugin.parameters.className)) {
                this.active = true;
                this.tableElement = elem as any;
            } else if (elem.nodeName === TABLE_CAPTION_NODE_NAME) {
                captionFocused = true;
            }
        }

        // Since we ain't in a table, ignore all other stuff
        if (!this.active) {
            return;
        }

        this.captionElement = this.tableElement?.querySelector?.('caption');
        this.summaryValue = this.tableElement?.getAttribute(TABLE_SUMMARY_ATTRIBUTE);

        // Now that we're in a table, we need to check which part of the table we're currently active in.
        // First we check for the tables selection data, then the aloha selection
        if (captionFocused) {
            this.activePart = TablePart.CAPTION;
        } else if (this.tablePlugin.activeTable?.selection?.selectionType != null) {
            this.tableSelection = this.tablePlugin.activeTable.selection;
            this.selectionActive = this.tableSelection.selectedCells?.length > 0;

            if (this.selectionActive) {
                this.canMergeCells = this.tableSelection.cellsAreMergeable();
                this.canSplitCells = this.tableSelection.cellsAreSplitable();
            }

            if (this.tableSelection.currentRectangle != null) {
                if ((this.tableSelection.currentRectangle as AlohaTableSelectionColumns).columns != null) {
                    this.activePart = TablePart.COLUMN;
                    this.isHeader = this.tableSelection.selectedCells.every(cell => cell.nodeName === TABLE_CELL_HEADER_NODE_NAME);
                } else if ((this.tableSelection.currentRectangle as AlohaTableSelectionRows).rows != null) {
                    this.activePart = TablePart.ROW;
                    this.isHeader = this.tableSelection.selectedCells.every(cell => cell.nodeName === TABLE_CELL_HEADER_NODE_NAME);
                } else {
                    this.activePart = TablePart.CELL;
                }
            } else {
                this.activePart = TablePart.CELL;
            }
        }
    }

    public toggleActive(): void {
        if (this.active) {
            this.deleteCurrentTable();
            return;
        }

        if (this.tableCreateDropdown) {
            this.tableCreateDropdown.openDropdown(true);
            return;
        }
    }

    public handleTableSizeSelect(event: TableSizeSelectEvent): void {
        this.createNewTable(event.columns, event.rows);
        if (this.tableCreateDropdown) {
            this.tableCreateDropdown.closeDropdown();
        }
    }

    public createNewTable(columnCount: number, rowCount: number): void {
        this.tablePlugin.createTable(columnCount, rowCount);
    }

    public deleteCurrentTable(): void {
        if (!this.tablePlugin.activeTable) {
            return;
        }
        // TODO: Open confirm dialog
        this.tablePlugin.activeTable.deleteTable();
    }

    public toggleCaption(): void {
        if (!this.captionElement) {
            this.createNewCaption();
        } else {
            this.captionElement.remove();
            this.captionElement = null;
            if (this.activePart === TablePart.CAPTION) {
                this.activePart = null;
            }
        }
    }

    public updateSummary(value: string): void {
        this.summaryValue = value;
        if (this.tablePlugin.summary) {
            this.tablePlugin.summary.setValue(value);
        }
        if (this.tableElement) {
            this.tableElement.setAttribute(TABLE_SUMMARY_ATTRIBUTE, value);
        }
        this.tablePlugin.updateWaiImage();
    }

    public mergeCells(): void {
        if (!this.tableSelection) {
            return;
        }

        this.tableSelection.mergeCells();
    }

    public splitCells(): void {
        if (!this.tableSelection) {
            return;
        }

        this.tableSelection.splitCells();
    }

    public addColumnLeft(): void {
        if (!this.tablePlugin.activeTable) {
            return;
        }

        this.tablePlugin.activeTable.addColumnsLeft();
    }

    public addColumnRight(): void {
        if (!this.tablePlugin.activeTable) {
            return;
        }

        this.tablePlugin.activeTable.addColumnsRight();
    }

    public deleteCurrentColumns(): void {
        if (!this.tablePlugin.activeTable) {
            return;
        }
        // TODO: Open confirm dialog
        this.tablePlugin.activeTable.deleteColumns();
    }

    public addRowAbove(): void {
        if (!this.tablePlugin.activeTable) {
            return;
        }

        this.tablePlugin.activeTable.addRowBeforeSelection();
    }

    public addRowBelow(): void {
        if (!this.tablePlugin.activeTable) {
            return;
        }

        this.tablePlugin.activeTable.addRowAfterSelection();
    }

    public deleteCurrentRows(): void {
        if (!this.tablePlugin.activeTable) {
            return;
        }
        // TODO: Open confirm dialog
        this.tablePlugin.activeTable.deleteRows();
    }

    public toggleSelectionAsHeader(scope: 'row' | 'col'): void {
        if (!this.tablePlugin.activeTable) {
            return;
        }

        this.tablePlugin.toggleHeaderStatus(this.tablePlugin.activeTable, scope);
    }

    protected createNewCaption(): void {
        const $caption: JQuery<HTMLTableCaptionElement> = this.aloha.jQuery('<caption></caption>');
        $caption.appendTo(this.tableElement);
        this.captionElement = $caption.get(0);
        this.tablePlugin.makeCaptionEditable($caption, this.i18n.translate('editor.format_table_caption'));

        // get the editable span within the caption and select it
        const cDiv = $caption.find('div').eq(0);
        const captionContent = cDiv.contents().eq(0);

        if (captionContent.length > 0) {
            const newRange = new this.aloha.Selection.SelectionRange();
            newRange.startContainer = newRange.endContainer = captionContent.get(0) as any;
            newRange.startOffset = 0;
            newRange.endOffset = captionContent.text().length;

            // blur all editables within the table
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            this.tablePlugin.activeTable.obj.find('div.aloha-table-cell-editable').blur();

            cDiv.focus();
            newRange.select();
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            this.aloha.Selection.updateSelection();
        }
    }
}

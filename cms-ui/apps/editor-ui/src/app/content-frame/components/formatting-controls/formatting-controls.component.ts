import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnChanges, SimpleChanges } from '@angular/core';
import { AlohaDOM, AlohaFormatPlugin } from '@gentics/aloha-models';
import {
    COMMAND_LINK,
    COMMAND_SPECIAL_STYLE_REMOVE_FORMAT,
    COMMAND_TABLE,
    COMMAND_TO_NODE_NAME,
    DEFAULT_COMMANDS,
    LIST_COMMANDS,
    NODE_NAME_TO_COMMAND,
    SPECIAL_STYLE_COMMANDS,
    STYLE_COMMANDS,
    TAG_ALIASES,
    TYPOGRAPHY_COMMANDS,
} from '../../../common/models/aloha-integration';
import { findAndToggleFormats } from '../../utils';
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
    public readonly SPECIAL_STYLE_COMMANDS = SPECIAL_STYLE_COMMANDS;
    public readonly LIST_COMMANDS = LIST_COMMANDS;
    public readonly TYPOGRAPHY_COMMANDS = TYPOGRAPHY_COMMANDS;
    public readonly COMMAND_LINK = COMMAND_LINK;
    public readonly COMMAND_TABLE = COMMAND_TABLE;

    public activeFormats: string[] = [];
    public allowedFormats: string[] = [];

    public linkActive = false;

    public table: TableSettings = {
        activePart: null,
    }

    protected alohaDom: AlohaDOM;
    protected formatPlugin: AlohaFormatPlugin;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    public ngOnChanges(changes: SimpleChanges): void {
        if (changes.aloha) {
            if (this.aloha) {
                try {
                    this.alohaDom = this.aloha.require('util/dom');
                } catch (err) {
                    this.alohaDom = null;
                    console.error('Could not load aloha-dom!', err);
                }
                try {
                    this.formatPlugin = this.aloha.require('format/format-plugin');
                } catch (err) {
                    this.formatPlugin = null;
                    console.error('Could not load aloha format-plugin!', err);
                }
            }
        }
        if (changes.range || changes.settings) {
            this.updateStateFromRange();
        }
    }

    public updateStateFromRange(): void {
        const newActiveFormat = new Set<string>();
        const pluginSettings = this.settings?.plugins?.format;
        let newAllowedFormats = pluginSettings?.config || DEFAULT_COMMANDS;
        const editableQueries = Object.keys(pluginSettings?.editables || {});
        let elementsToCheckForCommands: HTMLElement[] = [];
        let elementsToCheckForWhitelist: HTMLElement[] = [];

        if (this.range) {
            elementsToCheckForCommands = this.range.markupEffectiveAtStart || [];
            if (!elementsToCheckForCommands || elementsToCheckForCommands.length === 0) {
                elementsToCheckForCommands = [this.range.commonAncestorContainer];
            }
            elementsToCheckForWhitelist = this.range.unmodifiableMarkupAtStart || [];
        }

        for (const elem of elementsToCheckForCommands) {
            if (!elem) {
                continue;
            }
            const command = NODE_NAME_TO_COMMAND[elem.nodeName];

            if (command) {
                newActiveFormat.add(command);
                continue;
            }
        }

        for (const elem of elementsToCheckForWhitelist) {
            const editableSettings = editableQueries.find(selector => elem.matches(selector));
            if (editableSettings) {
                newAllowedFormats = pluginSettings.editables[editableSettings];
                break;
            }
        }

        this.activeFormats = Array.from(newActiveFormat)
            .filter(val => COMMAND_TO_NODE_NAME[val] != null);
        this.allowedFormats = newAllowedFormats.map(nodeOrCommand => {
            const nodeName = nodeOrCommand.toUpperCase();
            const alias = TAG_ALIASES[nodeName];
            return (alias ? NODE_NAME_TO_COMMAND[alias] : null) || NODE_NAME_TO_COMMAND[nodeName] || nodeOrCommand;
        }).filter(value => value != null);
    }

    public toggleFormat(format: string): void {
        if (format === COMMAND_SPECIAL_STYLE_REMOVE_FORMAT) {
            this.clearFormatFromSelection();
            return;
        }

        // The range may be out of date, as it's only being updated on block/element changes, but not on actual selection changes.
        const currentRange = this.aloha.Selection.getRangeObject();
        const nodeName = COMMAND_TO_NODE_NAME[format];
        const applied = findAndToggleFormats(
            currentRange,
            this.alohaDom,
            this.aloha.activeEditable,
            this.aloha.jQuery,
            [nodeName],
            {
                allowAdd: true,
                allowRemove: true,
            },
        );

        if (applied.length > 0) {
            // select the modified range and update our state
            currentRange.select();
            this.range = this.aloha.Selection.getRangeObject();
            this.updateStateFromRange();
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

    protected clearFormatFromSelection(): void {
        if (this.formatPlugin) {
            this.formatPlugin.removeFormat();
        }
    }
}

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnChanges, SimpleChanges } from '@angular/core';
import { AlohaDOM, AlohaFormatPlugin } from '@gentics/aloha-models';
import {
    COMMAND_LINK,
    COMMAND_SPECIAL_STYLE_REMOVE_FORMAT,
    COMMAND_TABLE,
    COMMAND_TO_NODE_NAME,
    COMMAND_TYPOGRAPHY_PARAGRAPH,
    NODE_NAME_TO_COMMAND,
    SPECIAL_NAME_TO_COMMAND,
    SPECIAL_STYLE_COMMANDS,
    STYLE_COMMANDS,
    TYPOGRAPHY_COMMANDS,
} from '../../../common/models/aloha-integration';
import { findAndToggleFormats } from '../../utils';
import { BaseControlsComponent } from '../base-controls/base-controls.component';

@Component({
    selector: 'gtx-formatting-controls',
    templateUrl: './formatting-controls.component.html',
    styleUrls: ['./formatting-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormattingControlsComponent extends BaseControlsComponent implements OnChanges {

    public readonly STYLE_COMMANDS = STYLE_COMMANDS;
    public readonly SPECIAL_STYLE_COMMANDS = SPECIAL_STYLE_COMMANDS;
    public readonly TYPOGRAPHY_COMMANDS = TYPOGRAPHY_COMMANDS;
    public readonly COMMAND_LINK = COMMAND_LINK;
    public readonly COMMAND_TABLE = COMMAND_TABLE;

    public activeFormats: string[] = [];
    public allowedFormats: string[] = [];

    public linkActive = false;

    protected alohaDom: AlohaDOM;
    protected formatPlugin: AlohaFormatPlugin;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.aloha) {
            this.alohaDom = this.safeRequire('util/dom');
            this.formatPlugin = this.safeRequire('format/format-plugin');
        }
    }

    protected selectionOrEditableChanged(): void {
        const newActiveFormat = new Set<string>();

        if (!this.formatPlugin || !this.range || !this.range.markupEffectiveAtStart || !this.aloha.activeEditable?.obj) {
            this.activeFormats = [];
            this.allowedFormats = [];
            return;
        }

        let foundOtherTypography = false;

        for (const elem of this.range.markupEffectiveAtStart ) {
            if (!elem) {
                continue;
            }
            const command = NODE_NAME_TO_COMMAND[elem.nodeName];

            if (!command) {
                continue;
            }

            // Special handling for typography types. There should always only be one
            // selected at any point, where the PARAGRAPH is the fallback/default.
            if (!TYPOGRAPHY_COMMANDS.includes(command)) {
                newActiveFormat.add(command);
                continue;
            }

            if (command !== COMMAND_TYPOGRAPHY_PARAGRAPH && !foundOtherTypography) {
                newActiveFormat.add(command);
                foundOtherTypography = true;
            }
        }

        if (!foundOtherTypography) {
            newActiveFormat.add(COMMAND_TYPOGRAPHY_PARAGRAPH);
        }

        this.activeFormats = Array.from(newActiveFormat)
            .filter(val => COMMAND_TO_NODE_NAME[val] != null);

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.allowedFormats = this.aloha?.activeEditable?.obj != null && (this.formatPlugin.getEditableConfig(this.aloha.activeEditable.obj) || [])
            .filter(nodeName => this.contentRules.isAllowed(this.aloha.activeEditable.obj, nodeName))
            .map((nodeName: string) => NODE_NAME_TO_COMMAND[nodeName.toUpperCase()] || SPECIAL_NAME_TO_COMMAND[nodeName])
            .filter(command => command != null);
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
            this.selectionOrEditableChanged();
        }
    }

    public setLinkActive(active: boolean): void {
        this.linkActive = active;
        this.changeDetector.markForCheck();
    }

    protected clearFormatFromSelection(): void {
        if (this.formatPlugin) {
            this.formatPlugin.removeFormat();
        }
    }
}

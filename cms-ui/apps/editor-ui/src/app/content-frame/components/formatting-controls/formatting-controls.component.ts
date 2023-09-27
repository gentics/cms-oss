import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnChanges, SimpleChanges } from '@angular/core';
import { AlohaDOM, AlohaFormatPlugin } from '@gentics/aloha-models';
import {
    COMMAND_LINK,
    COMMAND_SPECIAL_STYLE_REMOVE_FORMAT,
    COMMAND_TABLE,
    COMMAND_TO_NODE_NAME,
    NODE_NAME_TO_COMMAND,
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

    public ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

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
            } else {
                this.alohaDom = null;
                this.formatPlugin = null;
            }
        }
    }

    protected selectionOrEditableChanged(): void {
        const newActiveFormat = new Set<string>();

        if (!this.formatPlugin || !this.range || !this.range.markupEffectiveAtStart || !this.aloha.activeEditable?.obj) {
            this.activeFormats = [];
            this.allowedFormats = [];
            return;
        }

        for (const elem of this.range.markupEffectiveAtStart ) {
            if (!elem) {
                continue;
            }
            const command = NODE_NAME_TO_COMMAND[elem.nodeName];

            if (command) {
                newActiveFormat.add(command);
                continue;
            }
        }

        this.activeFormats = Array.from(newActiveFormat)
            .filter(val => COMMAND_TO_NODE_NAME[val] != null);

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.allowedFormats = (this.formatPlugin.getEditableConfig(this.aloha.activeEditable.obj) || [])
            .filter(nodeName => this.contentRules.isAllowed(this.aloha.activeEditable.obj, nodeName))
            .map((nodeName: string) => NODE_NAME_TO_COMMAND[nodeName.toUpperCase()])
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

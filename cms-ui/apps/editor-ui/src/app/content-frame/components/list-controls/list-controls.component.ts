import { ChangeDetectionStrategy, Component, OnChanges, SimpleChanges } from '@angular/core';
import {
    COMMAND_LIST_ORDERED,
    COMMAND_LIST_UNORDERED,
    COMMAND_TO_NODE_NAME,
    LIST_COMMANDS,
    NODE_NAME_TO_COMMAND,
} from '@editor-ui/app/common/models/aloha-integration';
import { AlohaListType, AlohaListPlugin } from '@gentics/aloha-models';
import { BaseControlsComponent } from '../base-controls/base-controls.component';

@Component({
    selector: 'gtx-list-controls',
    templateUrl: './list-controls.component.html',
    styleUrls: ['./list-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ListControlsComponent extends BaseControlsComponent implements OnChanges {

    public readonly LIST_COMMANDS = LIST_COMMANDS;

    /** The currently selected list */
    public currentElement: HTMLElement | null = null;
    /** If the selection is currently in a list. */
    public active = false;
    /** The list type that is currently active. */
    public activeCommand: typeof COMMAND_LIST_UNORDERED | typeof COMMAND_LIST_ORDERED | null = null;
    /** Allowed commands/lists to be used. */
    public availableCommands: typeof LIST_COMMANDS = [];

    /** Instance of the list plugin. */
    public listPlugin: AlohaListPlugin;

    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.aloha) {
            if (this.aloha) {
                try {
                    this.listPlugin = this.aloha.require('list/list-plugin');
                } catch (err) {
                    this.listPlugin = null;
                    console.error('Could not load aloha list-plugin!', err);
                }
            } else {
                this.listPlugin = null;
            }
        }
    }

    protected selectionOrEditableChanged(): void {
        if (!this.listPlugin || !this.range || !this.range.markupEffectiveAtStart || !this.aloha.activeEditable?.obj) {
            this.active = false;
            this.availableCommands = [];
            return;
        }

        const elementsToCheck = this.range.markupEffectiveAtStart || [];
        this.active = false;
        this.activeCommand = null;

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.availableCommands = (this.listPlugin.getEditableConfig(this.aloha.activeEditable.obj) || [])
            .filter(nodeName => this.contentRules.isAllowed(this.aloha.activeEditable.obj, nodeName))
            .map((nodeName: string) => NODE_NAME_TO_COMMAND[nodeName.toUpperCase()])
            .filter(command => command != null);

        for (const elem of elementsToCheck) {
            const command = NODE_NAME_TO_COMMAND[elem.nodeName];
            if (LIST_COMMANDS.includes(command)) {
                this.active = true;
                this.activeCommand = command as any;
                this.currentElement = elem;
                break;
            }
        }
    }

    toggleFormat(command: string): void {
        if (!this.listPlugin) {
            return;
        }

        const nodeName = COMMAND_TO_NODE_NAME[command];
        if (!nodeName) {
            return;
        }
        const listType = nodeName.toLowerCase() as AlohaListType;

        if (!this.active) {
            const newList = this.listPlugin.prepareNewList(listType);
            this.listPlugin.createList(listType, newList);
        } else {
            this.listPlugin.transformList(listType);
        }

        this.listPlugin.refreshSelection();
        this.range = this.aloha.Selection.getRangeObject();
        this.selectionOrEditableChanged();
    }

    increaseIndent(): void {
        if (!this.listPlugin) {
            return;
        }
        this.listPlugin.indentList();
    }

    decreaseIndent(): void {
        if (!this.listPlugin) {
            return;
        }
        this.listPlugin.outdentList();
    }
}

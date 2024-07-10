import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { InheritableItem, InheritanceRequest, Node } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';

/**
 * Dialog for setting inheritance settings for an item. Designed to work inside a modal dialog.
 * When closed, resolves the promise to an object like:
 * ```
 * {
 *   exclude: false,
 *   disinheritDefault: false,
 *   disinherit: [1, 2],
 *   reinherit: [4, 5]
 * }
 * ```
 * This object is ready to be posted to an `<type>/disinherit/<id>` endpoint.
 *
 * TODO: We need to implement some kind of constraint on which subchannels can be
 * disinherited based on the inheritance tree. Currently there is no way to get
 * the inheritance tree via the API, so this feature will need to wait until
 * https://jira.gentics.com/browse/GCU-29 is resolved.
 */
@Component({
    selector: 'gtx-inheritance-dialog',
    templateUrl: './inheritance-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InheritanceDialog implements OnInit, IModalDialog {

    item: InheritableItem;
    nodes: { [id: number]: Node };
    generalInheritance: boolean;
    disinheritDefault: boolean;
    inheritedChannels: { [id: number]: boolean } = {};

    constructor() {}

    ngOnInit(): void {
        this.generalInheritance = !this.item.excluded;
        this.disinheritDefault = this.item.disinheritDefault;

        this.item.inheritable.forEach(channelId => {
            this.inheritedChannels[channelId] = -1 === this.item.disinherit.indexOf(channelId);
        });
    }

    getChannelLabel(nodeId: number): string {
        return this.inheritedChannels[nodeId] ? 'modal.inherited_in_label' : 'modal.disinherited_from_label';
    }

    saveSettings(recursive: boolean = false): void {
        const returnValue: InheritanceRequest = {
            exclude: !this.generalInheritance,
            disinheritDefault: this.disinheritDefault,
            disinherit: [],
            reinherit: [],
        };
        if (recursive) {
            returnValue.recursive = true;
        }
        this.item.inheritable.forEach(channelId => {
            if (-1 < this.item.disinherit.indexOf(channelId) && this.inheritedChannels[channelId]) {
                returnValue.reinherit.push(channelId);
            }
            if (-1 === this.item.disinherit.indexOf(channelId) && !this.inheritedChannels[channelId]) {
                returnValue.disinherit.push(channelId);
            }
        });
        this.closeFn(returnValue);
    }

    closeFn(val: any): void { }
    cancelFn(val?: any): void { }

    registerCloseFn(close: (val: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }
}

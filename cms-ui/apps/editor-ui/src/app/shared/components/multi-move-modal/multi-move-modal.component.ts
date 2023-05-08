import { Component, Input } from '@angular/core';
import { Folder, InheritableItem, ItemType, Node as NodeModel } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { iconForItemType } from '../../../common/utils/icon-for-item-type';
import { itemIsLocalized } from '../../../common/utils/item-is-localized';

/**
 * A modal that tells the user when not all selected items can be moved (inherited / localized).
 */
@Component({
    selector: 'multi-move-modal-modal',
    templateUrl: './multi-move-modal.tpl.html',
    styleUrls: ['./multi-move-modal.scss']
})
export class MultiMoveModal implements IModalDialog {
    closeFn: (result: InheritableItem[]) => void;
    cancelFn: (val?: any) => void;

    @Input() items: InheritableItem[];
    @Input() targetFolder: Folder;
    @Input() targetNode: NodeModel;

    itemType: ItemType;
    canNotBeMoved: InheritableItem[];
    willBeMoved: InheritableItem[];

    iconForItemType = iconForItemType;

    ngOnInit(): void {
        this.willBeMoved = [];
        this.canNotBeMoved = [];
        this.itemType = this.items[0].type;

        for (let item of this.items) {
            if (item.inherited || itemIsLocalized(item)) {
                this.canNotBeMoved.push(item);
            } else {
                this.willBeMoved.push(item);
            }
        }
    }

    confirm(): void {
        this.closeFn(this.willBeMoved);
    }

    registerCloseFn(close: (result: InheritableItem[]) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }
}

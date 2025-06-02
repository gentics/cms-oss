import { Component } from '@angular/core';
import { Node, Raw } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';

@Component({
    selector: 'gtx-delete-nodes-modal',
    templateUrl: './delete-nodes-modal.component.html',
    styleUrls: ['./delete-nodes-modal.component.scss'],
    standalone: false
})
export class DeleteNodesModalComponent implements IModalDialog {

    get nodesToBeDeleted(): Node<Raw>[] {
        return this._nodesToBeDeleted;
    }

    set nodesToBeDeleted(v: Node<Raw>[]) {
        this._nodesToBeDeleted = [...v];
    }

    private _nodesToBeDeleted: Node<Raw>[] = [];

    closeFn = (userInput: Node<Raw>[]) => {};
    cancelFn = () => {};

    registerCloseFn(close: (userInput: Node<Raw>[]) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    onBtnOkayClicked(): void {
        this.closeFn(this.nodesToBeDeleted);
    }

    removeNodeFromList(nodeId: number): void {
        this.nodesToBeDeleted = this.nodesToBeDeleted.filter(node => node.id !== nodeId);
        // if no node is left for copy, close modal
        if (this.nodesToBeDeleted.length === 0) {
            this.cancelFn();
        }
    }

}

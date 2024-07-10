import { Component } from '@angular/core';
import { ItemWithObjectTags, ObjectTag } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';

/**
 * A modal for the user to confirm that he wants to apply an object property to all subitems of a certain type.
 */
@Component({
    selector: 'confirm-apply-to-subitems-modal',
    templateUrl: './confirm-apply-to-subitems-modal.component.html',
    styleUrls: ['./confirm-apply-to-subitems-modal.component.scss']
})
export class ConfirmApplyToSubitemsModalComponent implements IModalDialog {
    closeFn: (result: boolean) => void;
    cancelFn: (val?: any) => void;

    item: ItemWithObjectTags;
    objPropId: string;

    get objPropName(): string {
        return this.item && this.objPropId ? (this.item.tags[this.objPropId] as ObjectTag).displayName : '';
    }

    registerCloseFn(close: (val: boolean) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }
}

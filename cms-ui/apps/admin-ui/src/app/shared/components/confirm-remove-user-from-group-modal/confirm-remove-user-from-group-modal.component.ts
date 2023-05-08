import { Component } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-confirm-remove-user-from-group-modal',
    templateUrl: './confirm-remove-user-from-group-modal.component.html',
})
export class ConfirmRemoveUserFromGroupModalComponent extends BaseModal<boolean> {

    /** Name of the entity */
    groupName: string;
    userNames: string[];

    buttonDeleteConfirmClicked(): void {
        this.closeFn(true);
    }
}

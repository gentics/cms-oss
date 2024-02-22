import { Component } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { iconForItemType } from '../../../common/utils/icon-for-item-type';
import { ContentFrameComponent } from '../content-frame/content-frame.component';

/**
 * A modal for the user to choose what to do with the edited data before navigating.
 */
@Component({
    selector: 'confirm-navigation-modal',
    templateUrl: './confirm-navigation-modal.component.html',
    styleUrls: ['./confirm-navigation-modal.component.scss'],
})
export class ConfirmNavigationModal extends BaseModal<boolean> {

    allowSaving = true;
    contentFrame: ContentFrameComponent;
    iconForItemType = iconForItemType;

    saveAndClose(): void {
        const promiseOrUndefined = this.contentFrame.saveChanges();
        if (!(promiseOrUndefined instanceof Promise)) {
            this.closeFn(true);
            return;
        }

        promiseOrUndefined
            .then(() => this.closeFn(true))
            .catch(() => this.closeFn(false));
    }

    discardAndClose(): void {
        this.contentFrame.onChangesDiscarded();
        this.closeFn(true);
    }

    cancelAndClose(): void {
        this.closeFn(false);
    }
}

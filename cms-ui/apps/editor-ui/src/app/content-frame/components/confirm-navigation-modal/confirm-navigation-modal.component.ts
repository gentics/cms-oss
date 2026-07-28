import { Component } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { ContentFrameComponent } from '../content-frame/content-frame.component';

/**
 * A modal for the user to choose what to do with the edited data before navigating.
 */
@Component({
    selector: 'confirm-navigation-modal',
    templateUrl: './confirm-navigation-modal.component.html',
    styleUrls: ['./confirm-navigation-modal.component.scss'],
    standalone: false,
})
export class ConfirmNavigationModal extends BaseModal<boolean> {

    allowSaving = true;
    contentFrame: ContentFrameComponent;

    saveAndClose(): void {
        const promiseOrUndefined = this.contentFrame.saveChanges(true);
        if (!(promiseOrUndefined instanceof Promise)) {
            this.closeFn(true);
            return;
        }

        promiseOrUndefined
            .then((allow) => this.closeFn(allow))
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

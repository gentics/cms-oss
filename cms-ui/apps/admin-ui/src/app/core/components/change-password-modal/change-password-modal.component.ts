import {Component} from '@angular/core';
import {UntypedFormControl, UntypedFormGroup, Validators} from '@angular/forms';
import {IModalDialog} from '@gentics/ui-core';

import {AppStateService} from '@admin-ui/state';
import {PASSWORD_VALIDATORS} from '@admin-ui/shared';
import {AuthOperations} from '../../providers';


@Component({
    selector: 'gtx-change-password-modal',
    templateUrl: './change-password-modal.tpl.html'
})
export class ChangePasswordModalComponent implements IModalDialog {

    form: UntypedFormGroup;
    password1: UntypedFormControl;
    password2: UntypedFormControl;
    userId: number;

    constructor(
        private appState: AppStateService,
        private authOps: AuthOperations
    ) {

        this.password1 = new UntypedFormControl('', PASSWORD_VALIDATORS);
        this.password2 = new UntypedFormControl('', PASSWORD_VALIDATORS);

        this.form = new UntypedFormGroup({
            password1: this.password1,
            password2: this.password2
        }, this.areEqual);
    }

    /**
     * Validator which checks that both passwords contain equal values.
     */
    areEqual(group: UntypedFormGroup): { areEqual: boolean } {
        const valid = group.get('password1').value === group.get('password2').value;
        if (valid) {
            return null;
        } else {
            return { areEqual: true };
        }
    }

    changePassword(): void {
        const userId = this.userId || this.appState.now.auth.currentUserId;
        const password = this.form.get('password1').value;
        this.authOps.changePassword(userId, password)
            .then(() => this.closeFn());
    }

    closeFn = () => {};
    cancelFn = () => {};

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }
}

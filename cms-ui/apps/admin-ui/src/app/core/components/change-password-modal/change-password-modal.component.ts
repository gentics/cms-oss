import {Component, OnInit} from '@angular/core';
import {UntypedFormControl, UntypedFormGroup, Validators} from '@angular/forms';
import {IModalDialog} from '@gentics/ui-core';

import {PASSWORD_VALIDATORS} from '@admin-ui/shared';
import {AuthOperations} from '../../providers';


@Component({
    selector: 'gtx-change-password-modal',
    templateUrl: './change-password-modal.tpl.html',
    standalone: false
})
export class ChangePasswordModalComponent implements IModalDialog, OnInit {

    form: UntypedFormGroup;
    oldPassword: UntypedFormControl;
    password1: UntypedFormControl;
    password2: UntypedFormControl;

    /**
     * ID of the user, whose password is set by an administrator.
     * When not given, the current user changes the own password and has to enter the old password.
     */
    userId: number;

    /**
     * Whether the current user changes the own password
     */
    ownPassword: boolean;

    constructor(
        private authOps: AuthOperations
    ) {

        this.password1 = new UntypedFormControl('', PASSWORD_VALIDATORS);
        this.password2 = new UntypedFormControl('', PASSWORD_VALIDATORS);

        this.form = new UntypedFormGroup({
            password1: this.password1,
            password2: this.password2
        }, this.areEqual);
    }

    ngOnInit(): void {
        this.ownPassword = !this.userId;

        if (this.ownPassword) {
            // The old password may have been set before the current password rules applied, so it is only required
            this.oldPassword = new UntypedFormControl('', Validators.required);
            this.form.addControl('oldPassword', this.oldPassword);
        }
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
        const password = this.form.get('password1').value;

        if (this.ownPassword) {
            // keep the modal open on failure (e.g. wrong old password), so the user can correct the input
            this.authOps.changeOwnPassword(this.oldPassword.value, password)
                .then(success => {
                    if (success) {
                        this.closeFn();
                    }
                });
        } else {
            this.authOps.changePassword(this.userId, password)
                .then(() => this.closeFn());
        }
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

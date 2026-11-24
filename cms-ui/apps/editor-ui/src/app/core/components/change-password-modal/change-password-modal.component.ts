import { Component } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BaseModal } from '@gentics/ui-core';
import { AuthActionsService } from '../../../state';

@Component({
    selector: 'change-password-modal',
    templateUrl: './change-password-modal.component.html',
    styleUrls: ['./change-password-modal.component.scss'],
    standalone: false
})
export class ChangePasswordModal extends BaseModal<void> {

    form: UntypedFormGroup;
    oldPassword: UntypedFormControl;
    password1: UntypedFormControl;
    password2: UntypedFormControl;

    constructor(
        private authActions: AuthActionsService,
    ) {
        super();

        const passwordValidators = [
            Validators.required,
            Validators.minLength(4),
            Validators.maxLength(20),
            Validators.pattern('^[\\w\\-\\.@]+$'),
        ];

        // The old password may have been set before the current password rules applied, so it is only required
        this.oldPassword = new UntypedFormControl('', Validators.required);
        this.password1 = new UntypedFormControl('', passwordValidators);
        this.password2 = new UntypedFormControl('', passwordValidators);

        this.form = new UntypedFormGroup({
            oldPassword: this.oldPassword,
            password1: this.password1,
            password2: this.password2,
        }, this.areEqual);
    }

    /**
     * Validator which checks that both passwords contain equal values.
     */
    areEqual(group: UntypedFormGroup): { areEqual: boolean } {
        let valid = group.get('password1').value === group.get('password2').value;
        if (valid) {
            return null;
        } else {
            return { areEqual: true };
        }
    }

    changePassword(): void {
        const oldPassword = this.form.get('oldPassword').value;
        const password = this.form.get('password1').value;
        // keep the modal open on failure (e.g. wrong old password), so the user can correct the input
        this.authActions.changePassword(oldPassword, password)
            .then(success => {
                if (success) {
                    this.closeFn();
                }
            });
    }
}

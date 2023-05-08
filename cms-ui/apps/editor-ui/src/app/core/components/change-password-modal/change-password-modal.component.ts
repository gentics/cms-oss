import { Component } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BaseModal } from '@gentics/ui-core';
import { ApplicationStateService, AuthActionsService } from '../../../state';

@Component({
    selector: 'change-password-modal',
    templateUrl: './change-password-modal.component.html',
    styleUrls: ['./change-password-modal.component.scss']
    })
export class ChangePasswordModal extends BaseModal<void> {

    form: UntypedFormGroup;
    password1: UntypedFormControl;
    password2: UntypedFormControl;

    constructor(
        private appState: ApplicationStateService,
        private authActions: AuthActionsService,
    ) {
        super();

        const passwordValidators = [
            Validators.required,
            Validators.minLength(4),
            Validators.maxLength(20),
            Validators.pattern('^[\\w\\-\\.@]+$'),
        ];

        this.password1 = new UntypedFormControl('', passwordValidators);
        this.password2 = new UntypedFormControl('', passwordValidators);

        this.form = new UntypedFormGroup({
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
        const userId = this.appState.now.auth.currentUserId;
        const password = this.form.get('password1').value;
        this.authActions.changePassword(userId, password)
            .then(() => this.closeFn());
    }
}

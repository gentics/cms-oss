import { Component } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { BaseModal } from '@gentics/ui-core';
import { PASSWORD_VALIDATORS, passwordsAreEqual } from '../../../common/utils/password-validators';
import { ApplicationStateService, AuthActionsService } from '../../../state';

@Component({
    selector: 'change-password-modal',
    templateUrl: './change-password-modal.component.html',
    styleUrls: ['./change-password-modal.component.scss'],
    standalone: false
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

        this.password1 = new UntypedFormControl('', PASSWORD_VALIDATORS);
        this.password2 = new UntypedFormControl('', PASSWORD_VALIDATORS);

        this.form = new UntypedFormGroup({
            password1: this.password1,
            password2: this.password2,
        }, passwordsAreEqual);
    }

    changePassword(): void {
        const userId = this.appState.now.auth.user?.id;
        const password = this.form.get('password1').value;
        this.authActions.changePassword(userId, password)
            .then(() => this.closeFn());
    }
}

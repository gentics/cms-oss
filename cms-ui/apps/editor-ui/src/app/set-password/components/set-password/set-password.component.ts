import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { I18nNotificationService } from '@gentics/cms-components';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { BaseComponent } from '@gentics/ui-core';
import { PASSWORD_VALIDATORS, passwordsAreEqual } from '../../../common/utils/password-validators';

/** What the page shows */
export type SetPasswordState = 'checking' | 'invalid' | 'form';

/**
 * Public page on which a user sets the password with the token of a password link.
 */
@Component({
    selector: 'gtx-set-password',
    templateUrl: './set-password.component.html',
    styleUrls: ['./set-password.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class SetPasswordComponent extends BaseComponent implements OnInit {

    public state: SetPasswordState = 'checking';

    /** Message of the backend, why the token or the password was rejected */
    public errorMessage: string | null = null;

    public saving = false;

    public form: UntypedFormGroup;

    private token: string | null = null;

    constructor(
        changeDetector: ChangeDetectorRef,
        private route: ActivatedRoute,
        private router: Router,
        private client: GCMSRestClientService,
        private notification: I18nNotificationService,
    ) {
        super(changeDetector);
    }

    get password1(): UntypedFormControl {
        return this.form.get('password1') as UntypedFormControl;
    }

    get password2(): UntypedFormControl {
        return this.form.get('password2') as UntypedFormControl;
    }

    ngOnInit(): void {
        this.form = new UntypedFormGroup({
            password1: new UntypedFormControl('', PASSWORD_VALIDATORS),
            password2: new UntypedFormControl('', PASSWORD_VALIDATORS),
        }, passwordsAreEqual);

        this.token = this.route.snapshot.queryParamMap.get('token');
        if (!this.token) {
            this.showState('invalid');
            return;
        }

        void this.checkToken();
    }

    private async checkToken(): Promise<void> {
        try {
            await this.client.auth.validatePasswordToken(this.token).toPromise();
            this.showState('form');
        } catch (err) {
            this.errorMessage = err?.message || null;
            this.showState('invalid');
        }
    }

    async setPassword(): Promise<void> {
        if (!this.form.valid || this.saving) {
            return;
        }

        this.saving = true;
        this.errorMessage = null;
        this.changeDetector.markForCheck();

        try {
            await this.client.auth.setPassword({ token: this.token, password: this.password1.value }).toPromise();
            this.notification.show({
                message: 'message.updated_password',
                type: 'success',
            });
            await this.router.navigate(['/login']);
        } catch (err) {
            this.errorMessage = err?.message || null;
        } finally {
            this.saving = false;
            this.changeDetector.markForCheck();
        }
    }

    toLogin(): void {
        this.router.navigate(['/login']);
    }

    private showState(state: SetPasswordState): void {
        this.state = state;
        this.changeDetector.markForCheck();
    }
}

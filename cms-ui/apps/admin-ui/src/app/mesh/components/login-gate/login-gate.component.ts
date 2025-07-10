import { I18nNotificationService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ContentRepository, ContentRepositoryPasswordType, Response } from '@gentics/cms-models';
import { GCMSRestClientRequestError } from '@gentics/cms-rest-client';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { LoginRequest, User } from '@gentics/mesh-models';
import { MeshAPIVersion, MeshClientConnection, MeshRestClientRequestError } from '@gentics/mesh-rest-client';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { ChangesOf, FormProperties } from '@gentics/ui-core';
import { Subscription } from 'rxjs';

const NEEDS_NEW_PASSWORD_ERROR = 'auth_login_password_change_required';

@Component({
    selector: 'gtx-mesh-login-gate',
    templateUrl: './login-gate.component.html',
    styleUrls: ['./login-gate.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class LoginGateComponent implements OnInit, OnChanges, OnDestroy {

    @Input()
    public repository: ContentRepository;

    @Input()
    public loggedIn = false;

    @Output()
    public loggedInChange = new EventEmitter<{ loggedIn: boolean, user?: User }>();

    public initialized = false;
    public canLoginWithCR = false;
    public loading = false;
    public requiresLogin = false;

    public form: FormGroup<FormProperties<LoginRequest>>;

    protected sid: number;

    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected appState: AppStateService,
        protected cmsClient: GCMSRestClientService,
        protected meshClient: MeshRestClientService,
        protected notification: I18nNotificationService,
    ) { }

    ngOnInit(): void {
        this.form = new FormGroup<FormProperties<LoginRequest>>({
            /* eslint-disable @typescript-eslint/unbound-method */
            username: new FormControl('', Validators.required),
            password: new FormControl(''),
            newPassword: new FormControl({ value: '', disabled: true }, Validators.required),
            /* eslint-enable @typescript-eslint/unbound-method */
        });
        // Load it once instantly from the current state, and add reactivity.
        this.sid = this.appState.now.auth.sid;
        this.subscriptions.push(this.appState.select(state => state.auth.sid).subscribe(sid => {
            this.sid = sid;
        }));
        this.setupConnection();
    }

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.repository && !changes.repository.firstChange && !this.loggedIn) {
            this.setupConnection();
            // If we are no longer logged in, we have to show the login page again
        } else if (changes.loggedIn && changes.loggedIn.previousValue && !this.loggedIn) {
            this.requiresLogin = true;
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    setupConnection(): void {
        this.canLoginWithCR = (this.repository.passwordType === ContentRepositoryPasswordType.VALUE && this.repository.username?.length > 0)
            || (this.repository.passwordType === ContentRepositoryPasswordType.PROPERTY && this.repository.passwordProperty?.length > 0);

        const connection: MeshClientConnection = {
            absolute: false,
            basePath: `/rest/contentrepositories/${this.repository.id}/proxy/api/v2`,
            version: MeshAPIVersion.V2,
        };

        // HOW is this unsafe????
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.meshClient.init({
            connection,
            // We need this interceptor to always append the SID to the request, as it's getting proxied through the CMS.
            interceptors: [(data) => {
                return {
                    ...data,
                    params: {
                        ...data.params,
                        sid: `${this.sid}`,
                    },
                };
            }],
        });

        this.initialized = true;
        this.loggedIn = false;
        this.loading = true;

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.meshClient.auth.me().send().then(me => {
            this.loading = false;
            this.loggedIn = me.username !== 'anonymous';
            this.requiresLogin = !this.loggedIn;
            this.changeDetector.markForCheck();
            if (this.loggedIn) {
                this.loggedInChange.emit({ loggedIn: true, user: me });
            }
        }).catch(err => {
            // eslint-disable-next-line no-console
            console.debug('Error while loading user info from mesh', err);

            this.loading = false;
            this.loggedIn = false;
            this.requiresLogin = true;
            this.changeDetector.markForCheck();
        });
    }

    loginWithFormCredentials(): void {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.performLogin((req) => this.meshClient.auth.login(req).send());
    }

    loginWithContentRepository(): void {
        this.performLogin(() => this.cmsClient.contentRepository.proxyLogin(this.repository.id).toPromise());
    }

    protected performLogin(handler: (value: LoginRequest) => Promise<any>): void {
        const value = this.form.value as LoginRequest;
        this.form.disable();
        this.loading = true;
        this.changeDetector.markForCheck();

        handler(value).then(() => {
            this.loading = false;
            this.loggedIn = true;
            this.requiresLogin = false;
            this.form.enable();
            this.form.controls.newPassword.disable();

            this.changeDetector.markForCheck();
            this.loggedInChange.emit({ loggedIn: true });
        }).catch(err => {
            this.loading = false;
            this.loggedIn = false;
            this.requiresLogin = true;
            this.form.enable();

            if ((
                err instanceof GCMSRestClientRequestError
                || err instanceof MeshRestClientRequestError
            ) && err.data?.i18nKey === NEEDS_NEW_PASSWORD_ERROR) {
                this.form.controls.newPassword.enable();
                this.notification.show({
                    message: 'mesh.new_password_required_warning',
                    type: 'alert',
                    delay: 5_000,
                });
            } else {
                // eslint-disable-next-line no-console
                console.debug('Error while logging in to mesh', err);
                this.notification.show({
                    message: err.data?.message || (err.data as Response)?.responseInfo?.responseMessage || 'mesh.unknown_error',
                    type: 'alert',
                    delay: 5_000,
                });
                this.form.controls.newPassword.disable();
            }

            this.changeDetector.markForCheck();
        });
    }
}

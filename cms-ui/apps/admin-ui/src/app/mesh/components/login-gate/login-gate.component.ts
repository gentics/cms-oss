import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, Validators } from '@angular/forms';
import { ContentRepository } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { MeshAPIVersion, MeshClientConnection } from '@gentics/mesh-rest-client';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';

interface LoginForm {
    username: string;
    password: string;
}

@Component({
    selector: 'gtx-mesh-login-gate',
    templateUrl: './login-gate.component.html',
    styleUrls: ['./login-gate.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginGateComponent implements OnInit, OnChanges {

    @Input()
    public repository: ContentRepository;

    @Output()
    public login = new EventEmitter<void>();

    public initialized = false;
    public loggedIn = false;
    public canLoginWithCR = false;
    public loading = false;

    public form: FormGroup<Record<keyof LoginForm, AbstractControl<string>>>;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected cmsClient: GcmsApi,
        protected client: MeshRestClientService,
    ) {}

    ngOnInit(): void {
        this.form = new FormGroup<Record<keyof LoginForm, AbstractControl<string>>>({
            /* eslint-disable @typescript-eslint/unbound-method */
            username: new FormControl('', Validators.required),
            password: new FormControl('', Validators.required),
            /* eslint-enable @typescript-eslint/unbound-method */
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.repository) {
            this.setupConnection();
        }
    }

    setupConnection(): void {
        this.canLoginWithCR = this.repository.username?.length > 0 && this.repository.usePassword;

        const config: MeshClientConnection = {
            ssl: false,
            host: 'localhost',
            port: 4200,
            basePath: '/mesh/api/v2',
            version: MeshAPIVersion.V2,
        };

        // HOW is this unsafe????
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.client.init(config);
        this.initialized = true;
        this.loggedIn = false;
        this.loading = true;

        this.client.auth.me().then(me => {
            this.loading = false;
            this.loggedIn = me.username !== 'anonymous';
            this.changeDetector.markForCheck();
            if (this.loggedIn) {
                this.login.emit();
            }
        }).catch(err => {
            // eslint-disable-next-line no-console
            console.debug('Error while loading user info from mesh', err);

            this.loading = false;
            this.loggedIn = false;
            this.changeDetector.markForCheck();
        });
    }

    loginWithFormCredentials(): void {
        const { username, password } = this.form.value;
        this.performLogin(() => this.client.auth.login(username, password));
    }

    loginWithContentRepository(): void {
        this.performLogin(() => this.cmsClient.contentrepositories.loginToMeshInstance(this.repository.id).toPromise());
    }

    protected performLogin(handler: () => Promise<any>): void {
        this.form.disable();
        this.loading = true;
        this.changeDetector.markForCheck();

        handler().then(() => {
            this.loading = false;
            this.loggedIn = true;
            this.form.enable();
            this.changeDetector.markForCheck();
            this.login.emit();
        }).catch(err => {
            // eslint-disable-next-line no-console
            console.debug('Error while logging in to mesh', err);

            this.loading = false;
            this.loggedIn = false;
            this.form.enable();
            this.changeDetector.markForCheck();
        });
    }
}

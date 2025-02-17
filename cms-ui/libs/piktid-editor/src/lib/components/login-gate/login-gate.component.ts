import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { FormProperties } from '@gentics/ui-core';
import { map, Subscription, switchMap } from 'rxjs';
import { UserInfoResponse } from '../../common/models';
import { PiktidAPIService } from '../../providers';

interface LoginFormProperties {
    username: string;
    password: string;
}

const LOCAL_STORAGE_KEY = 'piktid-editor-auth';

@Component({
    selector: 'gtxpikt-login-gate',
    templateUrl: './login-gate.component.html',
    styleUrl: './login-gate.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginGateComponent implements OnInit, OnDestroy {

    /** Whether the user is logged in. */
    public loggedIn = false;

    /** The user info. */
    public userInfo: UserInfoResponse | null = null;

    public loginForm: FormGroup<FormProperties<LoginFormProperties>> | null = null;

    public loading = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private api: PiktidAPIService,
    ) {}

    ngOnInit(): void {
        this.loginForm = new FormGroup<FormProperties<LoginFormProperties>>({
            username: new FormControl('', Validators.required),
            password: new FormControl('', Validators.required),
        });

        const storedAuth = localStorage.getItem(LOCAL_STORAGE_KEY);
        if (storedAuth) {
            this.api.setAuth(storedAuth.split(':')[0], storedAuth.split(':')[1]);
        }

        this.loggedIn = this.api.isLoggedIn();

        if (this.loggedIn) {
            this.subscriptions.push(this.api.getUserInfo().subscribe({
                next: (response) => {
                    this.userInfo = response;
                    this.changeDetector.markForCheck();
                },
            }));
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public onLogin(): void {
        this.loading = true;
        this.changeDetector.markForCheck();

        this.subscriptions.push(this.api.authenticate(this.loginForm.value.username, this.loginForm.value.password).pipe(
            switchMap(auth => this.api.getUserInfo().pipe(
                map((userInfo) => ({ auth, userInfo })),
            )),
        ).subscribe({
            next: ({ auth, userInfo }) => {
                this.loggedIn = true;
                this.loading = false;
                this.userInfo = userInfo;
                this.changeDetector.markForCheck();

                localStorage.setItem(LOCAL_STORAGE_KEY, `${auth.access_token}:${auth.refresh_token}`);
            },
            error: (error) => {
                this.loading = false;
                console.error(error);
                this.changeDetector.markForCheck();
            },
        }));
    }
}

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { BaseComponent, FormProperties } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { distinctUntilChanged } from 'rxjs/operators';
import { AuthOperations } from '../../../core';
import { AppStateService } from '../../../state/providers/app-state/app-state.service';

interface LoginData {
    username: string;
    password: string;
}

@Component({
    selector: 'gtx-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class LoginComponent extends BaseComponent implements OnInit {

    public keycloakError?: string;
    public loginForm: FormGroup<FormProperties<LoginData>>;

    constructor(
        changeDetector: ChangeDetectorRef,
        private appState: AppStateService,
        private route: ActivatedRoute,
        private authOps: AuthOperations,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        this.loginForm = new FormGroup<FormProperties<LoginData>>({
            username: new FormControl('', Validators.required),
            password: new FormControl('', Validators.required),
        });

        this.subscriptions.push(this.appState.select((state) => state.auth.keycloakError).pipe(
            distinctUntilChanged(isEqual),
        ).subscribe((errMsg) => {
            this.keycloakError = errMsg;
            this.changeDetector.markForCheck();
        }));
    }

    login(): void {
        if (this.loginForm.valid) {
            const { username, password } = this.loginForm.value;

            // Get redirectUrl or fallback to the root route
            const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/';

            this.authOps.login(username, password, returnUrl);
        }
    }
}

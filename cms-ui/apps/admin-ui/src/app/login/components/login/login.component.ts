import { AuthOperations } from '@admin-ui/core';
import { SelectState } from '@admin-ui/state';
import { Component, Input, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { KEYCLOAK_ERROR_KEY } from '@gentics/cms-components';
import { Observable } from 'rxjs';

@Component({
    selector: 'gtx-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss'],
    standalone: false
})
export class LoginComponent implements OnInit {

    @SelectState(state => state.auth.lastError)
    errorMessage$: Observable<string>;

    loginForm: UntypedFormGroup;

    public keycloakError?: string;

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private authOps: AuthOperations,
    ) {
        const navigation = this.router.currentNavigation();

        if (navigation?.extras?.state) {
            this.keycloakError = navigation?.extras?.state[KEYCLOAK_ERROR_KEY];
        }
    }

    ngOnInit(): void {
        this.loginForm = new UntypedFormGroup({
            username: new UntypedFormControl('', Validators.required),
            password: new UntypedFormControl('', Validators.required),
        });
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

import { Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { ApplicationStateService, AuthActionsService } from '../../../state';

@Component({
    selector: 'login',
    templateUrl: './login.tpl.html',
    styleUrls: ['./login.scss'],
    standalone: false
})
export class LoginComponent implements OnInit {
    errorMessage$: Observable<string>;
    loginForm: UntypedFormGroup;
    public keycloakError?: string;

    constructor(
        private appState: ApplicationStateService,
        private route: ActivatedRoute,
        private router: Router,
        private authActions: AuthActionsService,
    ) {
        const navigation = this.router.currentNavigation();

        if (navigation?.extras?.state) {
            this.keycloakError = navigation?.extras?.state['keycloakError'];
        }
    }

    ngOnInit(): void {
        this.loginForm = new UntypedFormGroup({
            username: new UntypedFormControl('', Validators.required),
            password: new UntypedFormControl('', Validators.required),
        });
        this.errorMessage$ = this.appState.select(state => state.auth.lastError);
    }

    login(): void {
        if (this.loginForm.valid) {
            const { username, password } = this.loginForm.value;
            const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
            this.authActions.login(username, password, returnUrl);
        }
    }
}

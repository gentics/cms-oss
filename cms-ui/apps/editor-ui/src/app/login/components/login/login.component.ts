import { Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { ApplicationStateService, AuthActionsService } from '../../../state';

@Component({
    selector: 'login',
    templateUrl: './login.tpl.html',
    styleUrls: ['./login.scss'],
})
export class LoginComponent implements OnInit {
    errorMessage$: Observable<string>;
    loginForm: UntypedFormGroup;

    constructor(
        private appState: ApplicationStateService,
        private route: ActivatedRoute,
        private authActions: AuthActionsService
    ) { }

    ngOnInit(): void {
        this.loginForm = new UntypedFormGroup({
            username: new UntypedFormControl('', Validators.required),
            password: new UntypedFormControl('', Validators.required),
        });
        this.errorMessage$ = this.appState.select(state => state.auth.lastError);
    }

    login(): void {
        if (this.loginForm.valid) {
            let { username, password } = this.loginForm.value;
            let returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
            this.authActions.login(username, password, returnUrl);
        }
    }
}

import { AuthOperations } from '@admin-ui/core';
import { SelectState } from '@admin-ui/state';
import { Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { GtxVersion } from '@gentics/cms-models';
import { Observable } from 'rxjs';

@Component({
    selector: 'gtx-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit {

    @SelectState(state => state.auth.lastError)
    errorMessage$: Observable<string>;

    loginForm: UntypedFormGroup;

    constructor(
        private route: ActivatedRoute,
        private authOps: AuthOperations,
    ) {}

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

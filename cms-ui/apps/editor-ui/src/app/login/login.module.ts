import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AuthenticationModule } from '@gentics/cms-components/auth';
import { SharedModule } from '../shared/shared.module';
import { LoginComponent } from './components/login/login.component';
import { SingleSignOnComponent } from './components/single-sign-on/single-sign-on.component';
import { LOGIN_ROUTES } from './login.routes';

const COMPONENTS = [
    LoginComponent,
    SingleSignOnComponent,
];

@NgModule({
    imports: [
        SharedModule,
        RouterModule.forChild(LOGIN_ROUTES),
        AuthenticationModule,
    ],
    exports: [],
    declarations: COMPONENTS,
    providers: [],
})
export class LoginModule { }

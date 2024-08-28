import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { KeycloakService } from '@gentics/cms-components';
import { SharedModule } from '../shared/shared.module';
import { LoginComponent } from './components/login/login.component';
import { SingleSignOn } from './components/single-sign-on/single-sign-on.component';
import { LOGIN_ROUTES } from './login.routes';

const COMPONENTS = [
    LoginComponent,
    SingleSignOn,
];

@NgModule({
    imports: [
        SharedModule,
        RouterModule.forChild(LOGIN_ROUTES),
    ],
    exports: [],
    declarations: COMPONENTS,
    providers: [KeycloakService],
})
export class LoginModule { }

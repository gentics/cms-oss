import { NgModule } from '@angular/core';
import { RouterModule, provideRouter, withComponentInputBinding } from '@angular/router';
import { SharedModule } from '../shared/shared.module';
import { LoginComponent } from './components/login/login.component';
import { SingleSignOnComponent } from './components/single-sign-on/single-sign-on.component';
import { loginRoutes } from './login.routes';

const COMPONENTS = [
    LoginComponent,
    SingleSignOnComponent,
];

@NgModule({
    imports: [
        SharedModule,
        RouterModule.forChild(loginRoutes),
    ],
    exports: [],
    declarations: COMPONENTS,
    providers: [
        provideRouter(loginRoutes, withComponentInputBinding()),
    ],
})
export class LoginModule {}

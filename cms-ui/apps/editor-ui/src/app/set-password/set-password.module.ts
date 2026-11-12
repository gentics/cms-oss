import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../shared/shared.module';
import { SetPasswordComponent } from './components/set-password/set-password.component';
import { SET_PASSWORD_ROUTES } from './set-password.routes';

@NgModule({
    imports: [
        SharedModule,
        RouterModule.forChild(SET_PASSWORD_ROUTES),
    ],
    declarations: [SetPasswordComponent],
})
export class SetPasswordModule { }

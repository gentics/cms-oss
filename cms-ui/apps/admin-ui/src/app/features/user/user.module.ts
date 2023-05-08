import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { UserDetailComponent, UserMasterComponent } from './components';
import { CanActivateUserGuard } from './providers';
import { USER_ROUTES } from './user.routes';

@NgModule({
    declarations: [
        UserMasterComponent,
        UserDetailComponent,
    ],
    providers: [
        CanActivateUserGuard,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(USER_ROUTES),
    ],
})
export class UserModule {}

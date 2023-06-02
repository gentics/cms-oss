import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import {
    RoleDetailComponent,
    RoleMasterComponent,
    CreateRoleModalComponent,
    RoleTableComponent,
    RolePropertiesComponent,
} from './components';
import { CanActivateRoleGuard, RoleTableLoaderService } from './providers';
import { ROLE_ROUTES } from './role.routes';

@NgModule({
    declarations: [
        CreateRoleModalComponent,
        RoleMasterComponent,
        RoleDetailComponent,
        RolePropertiesComponent,
        RoleTableComponent,
    ],
    providers: [
        CanActivateRoleGuard,
        RoleTableLoaderService,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(ROLE_ROUTES),
    ],
})
export class RoleModule {}

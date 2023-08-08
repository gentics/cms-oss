import { SharedModule } from '@admin-ui/shared/shared.module';
import { CommonModule } from '@angular/common';
import { NgModule, Provider, Type } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MeshRestClientModule } from '@gentics/mesh-rest-client-angular';
import {
    CopyTokenModal,
    CopyValueComponent,
    LoginGateComponent,
    ManagementComponent,
    ManagementTabsComponent,
    MeshGroupModal,
    MeshGroupPropertiesComponent,
    MeshGroupTableComponent,
    MeshRoleModal,
    MeshRolePermissionsModal,
    MeshRolePermissionsTrableComponent,
    MeshRolePropertiesComponent,
    MeshRoleTableComponent,
    MeshUserModal,
    MeshUserPropertiesComponent,
    MeshUserTableComponent,
    SelectGroupModal,
    SelectRoleModal,
    SelectUserModal,
    ServerOverviewComponent,
} from './components';
import {
    MeshGroupHandlerService,
    MeshGroupTableLoaderService,
    MeshRoleHandlerService,
    MeshRoleTableLoaderService,
    MeshUserHandlerService,
    MeshUserTableLoaderService,
} from './providers';

const COMPONENTS: Type<any>[] = [
    CopyTokenModal,
    CopyValueComponent,
    LoginGateComponent,
    ManagementComponent,
    ManagementTabsComponent,
    MeshGroupModal,
    MeshGroupPropertiesComponent,
    MeshGroupTableComponent,
    MeshRoleModal,
    MeshRolePermissionsModal,
    MeshRolePermissionsTrableComponent,
    MeshRoleTableComponent,
    MeshRolePropertiesComponent,
    MeshUserModal,
    MeshUserPropertiesComponent,
    MeshUserTableComponent,
    SelectGroupModal,
    SelectRoleModal,
    SelectUserModal,
    ServerOverviewComponent,
];

const DECLARATIONS = [
    ...COMPONENTS,
];

const SERVICES: Provider[] = [
    MeshGroupHandlerService,
    MeshGroupTableLoaderService,
    MeshRoleHandlerService,
    MeshRoleTableLoaderService,
    MeshUserHandlerService,
    MeshUserTableLoaderService,
];

@NgModule({
    declarations: DECLARATIONS,
    providers: [
        ...SERVICES,
    ],
    exports: DECLARATIONS,
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        RouterModule.forChild([]),
        MeshRestClientModule,
        SharedModule,
    ],
})
export class MeshModule { }

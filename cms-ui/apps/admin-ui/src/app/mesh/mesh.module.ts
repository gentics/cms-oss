import { SharedModule } from '@admin-ui/shared/shared.module';
import { CommonModule } from '@angular/common';
import { NgModule, Provider, Type } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MeshRestClientModule } from '@gentics/mesh-rest-client-angular';
import {
    LoginGateComponent,
    ManagementComponent,
    ManagementTabsComponent,
    MeshGroupTableComponent,
    MeshRoleModal,
    MeshRolePropertiesComponent,
    MeshRoleTableComponent,
    SelectGroupModal,
    ServerOverviewComponent,
} from './components';
import { MeshGroupTableLoaderService, MeshRoleTableLoaderService } from './providers';

const COMPONENTS: Type<any>[] = [
    LoginGateComponent,
    ManagementComponent,
    ManagementTabsComponent,
    MeshGroupTableComponent,
    MeshRoleModal,
    MeshRolePropertiesComponent,
    MeshRoleTableComponent,
    SelectGroupModal,
    ServerOverviewComponent,
];

const DECLARATIONS = [
    ...COMPONENTS,
];

const SERVICES: Provider[] = [
    MeshGroupTableLoaderService,
    MeshRoleTableLoaderService,
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

import { SharedModule } from '@admin-ui/shared/shared.module';
import { CommonModule } from '@angular/common';
import { NgModule, Provider, Type } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MeshRestClientModule } from '@gentics/mesh-rest-client-angular';
import { LoginGateComponent, MeshManagementComponent } from './components';

const COMPONENTS: Type<any>[] = [
    LoginGateComponent,
    MeshManagementComponent,
];

const DECLARATIONS = [
    ...COMPONENTS,
];

const SERVICES: Provider[] = [

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
        MeshRestClientModule,
        SharedModule,
    ],
})
export class MeshModule { }

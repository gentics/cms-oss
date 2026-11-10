import { NgModule } from '@angular/core';
import { RouterModule, provideRouter, withComponentInputBinding } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import {
    CreatePortalImplementationModalComponent,
    PortalImplementationEditorComponent,
    PortalImplementationFileTableComponent,
    PortalImplementationMasterComponent,
    PortalImplementationPropertiesComponent,
} from './components';
import { PORTAL_IMPLEMENTATION_ROUTES } from './portal-implementation.routes';
import { PortalImplementationFileTableLoaderService } from './providers';

@NgModule({
    declarations: [
        CreatePortalImplementationModalComponent,
        PortalImplementationEditorComponent,
        PortalImplementationFileTableComponent,
        PortalImplementationMasterComponent,
        PortalImplementationPropertiesComponent,
    ],
    providers: [
        PortalImplementationFileTableLoaderService,
        provideRouter(PORTAL_IMPLEMENTATION_ROUTES, withComponentInputBinding()),
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(PORTAL_IMPLEMENTATION_ROUTES),
    ],
})
export class PortalImplementationModule {}

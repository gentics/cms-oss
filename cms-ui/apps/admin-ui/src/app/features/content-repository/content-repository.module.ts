import { MeshModule } from '@admin-ui/mesh';
import { NgModule } from '@angular/core';
import { RouterModule, provideRouter, withComponentInputBinding } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import {
    ContentRepositoryEditorComponent,
    ContentRepositoryMasterComponent,
    ContentRepositoryPropertiesComponent,
    CreateContentRepositoryModalComponent,
    ManageContentRepositoryRolesModal,
} from './components';
import { CONTENT_REPOSIROTY_ROUTES } from './content-repository.routes';

@NgModule({
    id: 'admin-ui_content-repository',
    declarations: [
        ContentRepositoryEditorComponent,
        ContentRepositoryMasterComponent,
        ContentRepositoryPropertiesComponent,
        CreateContentRepositoryModalComponent,
        ManageContentRepositoryRolesModal,
    ],
    providers: [
        provideRouter(CONTENT_REPOSIROTY_ROUTES, withComponentInputBinding()),
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(CONTENT_REPOSIROTY_ROUTES),
        MeshModule,
    ],
})
export class ContentRepositoryModule {}

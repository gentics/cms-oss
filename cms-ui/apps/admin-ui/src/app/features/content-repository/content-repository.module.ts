import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import {
    ContentRepositoryDetailComponent,
    ContentRepositoryMasterComponent,
    ContentRepositoryPropertiesComponent,
    CreateContentRepositoryModalComponent,
    ManageContentRepositoryRolesModal,
} from './components';
import { CONTENT_REPOSIROTY_ROUTES } from './content-repository.routes';
import { CanActivateContentRepositoryGuard } from './providers';

@NgModule({
    declarations: [
        ContentRepositoryDetailComponent,
        ContentRepositoryMasterComponent,
        ContentRepositoryPropertiesComponent,
        CreateContentRepositoryModalComponent,
        ManageContentRepositoryRolesModal,
    ],
    providers: [
        CanActivateContentRepositoryGuard,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(CONTENT_REPOSIROTY_ROUTES),
    ],
})
export class ContentRepositoryModule {}

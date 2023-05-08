import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { FolderDetailComponent, FolderMasterComponent } from './components';
import { FOLDER_ROUTES } from './folder.routes';
import { CanActivateFolderGuard } from './providers';

@NgModule({
    declarations: [
        FolderMasterComponent,
        FolderDetailComponent,
    ],
    providers: [
        CanActivateFolderGuard,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(FOLDER_ROUTES),
    ],
})
export class FolderModule {}

import { SharedModule } from '@admin-ui/shared/shared.module';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import {
    ContentPackageDetailComponent,
    ContentPackageImportErrorTableComponent,
    ContentPackageMasterComponent,
    ContentPackagePropertiesComponent,
    ContentPackageTableComponent,
    CreateContentPackageModalComponent,
    UploadContentPackageModalComponent,
} from './components';
import { CanActivateContentPackageGuard, ContentPackageImportErrorTableLoaderService, ContentPackageTableLoaderService } from './providers';
import { CONTENT_STAGING_ROUTES } from './content-staging.routes';

@NgModule({
    declarations: [
        ContentPackageDetailComponent,
        ContentPackageMasterComponent,
        ContentPackagePropertiesComponent,
        ContentPackageImportErrorTableComponent,
        ContentPackageTableComponent,
        CreateContentPackageModalComponent,
        UploadContentPackageModalComponent,
    ],
    providers: [
        CanActivateContentPackageGuard,
        ContentPackageTableLoaderService,
        ContentPackageImportErrorTableLoaderService,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(CONTENT_STAGING_ROUTES),
    ],
})
export class ContentStagingModule {}

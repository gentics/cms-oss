import { SharedModule } from '@admin-ui/shared/shared.module';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import {
    ContentPackageDetailComponent,
    ContentPackageMasterComponent,
    ContentPackagePropertiesComponent,
    ContentPackageTableComponent,
    CreateContentPackageModalComponent,
    UploadContentPackageModalComponent,
} from './components';
import { CanActivateContentPackageGuard, ContentPackageTableLoaderService } from './providers';
import { CONTENT_STAGING_ROUTES } from './content-staging.routes';

@NgModule({
    declarations: [
        ContentPackageDetailComponent,
        ContentPackageMasterComponent,
        ContentPackagePropertiesComponent,
        ContentPackageTableComponent,
        CreateContentPackageModalComponent,
        UploadContentPackageModalComponent,
    ],
    providers: [
        CanActivateContentPackageGuard,
        ContentPackageTableLoaderService,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(CONTENT_STAGING_ROUTES),
    ],
})
export class ContentStagingModule {}

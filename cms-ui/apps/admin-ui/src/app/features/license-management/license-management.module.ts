import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import {
    ContentRepositoryLicenseTableComponent,
    LicenseInfoComponent,
    LicenseManagementModuleMasterComponent,
    LicenseManagementComponent,
    LicenseUploadModal,
    ContentRepositoryLicenseInfoModal,
} from './components';
import { LicenseManagementRoutes } from './license-management.routes';
import { ContentRepositoryLicenseTableLoaderService } from './providers';

@NgModule({
    declarations: [
        ContentRepositoryLicenseInfoModal,
        ContentRepositoryLicenseTableComponent,
        LicenseInfoComponent,
        LicenseManagementModuleMasterComponent,
        LicenseManagementComponent,
        LicenseUploadModal,
    ],
    providers: [
        ContentRepositoryLicenseTableLoaderService,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(LicenseManagementRoutes),
    ],
})
export class LicenseManagementModeModule {}

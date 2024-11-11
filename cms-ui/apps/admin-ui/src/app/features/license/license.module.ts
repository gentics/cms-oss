import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { LicenseModuleMasterComponent, LicenseUploadModal } from './components';
import { LicenseRoutes } from './license.routes';

@NgModule({
    declarations: [
        LicenseModuleMasterComponent,
        LicenseUploadModal,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(LicenseRoutes),
    ],
})
export class LicenseModeModule {}

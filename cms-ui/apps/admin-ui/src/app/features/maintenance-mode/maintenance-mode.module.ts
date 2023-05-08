import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { MaintenanceModeViewComponent } from './components/maintenance-mode';
import { MaintenanceModeRoutes } from './maintenance-mode.routes';

@NgModule({
    declarations: [
        MaintenanceModeViewComponent,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(MaintenanceModeRoutes),
    ],
})
export class MaintenanceModeModule {}

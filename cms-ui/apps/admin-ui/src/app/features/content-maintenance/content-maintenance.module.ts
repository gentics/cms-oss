import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import {
    ContentMaintenanceComponent,
    DirtQueueItemTableComponent,
    MaintenanceActionModalComponent,
    NodePublishProcessTableComponent,
    PublishQueueSummaryComponent,
    DirtQueueSummaryComponent,
} from './components';
import { CONTENT_MAINTENANCE_ROUTES } from './content-maintenance.routes';
import { DirtQueueItemTableLoaderService } from './providers';

@NgModule({
    declarations: [
        ContentMaintenanceComponent,
        DirtQueueItemTableComponent,
        MaintenanceActionModalComponent,
        NodePublishProcessTableComponent,
        PublishQueueSummaryComponent,
        DirtQueueSummaryComponent,
    ],
    providers: [
        DirtQueueItemTableLoaderService,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(CONTENT_MAINTENANCE_ROUTES),
    ],
})
export class ContentmaintenanceModule {}

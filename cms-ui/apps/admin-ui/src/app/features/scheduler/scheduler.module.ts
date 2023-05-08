import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import {
    CreateScheduleModalComponent,
    CreateScheduleTaskModalComponent,
    FollowUpScheduleDataPropertiesComponent,
    IntervalScheduleDataPropertiesComponent,
    ScheduleDataPropertiesComponent,
    ScheduleDetailComponent,
    ScheduleExecutionDetailModalComponent,
    ScheduleExecutionsTableComponent,
    ScheduleMasterComponent,
    SchedulePropertiesComponent,
    SchedulerModuleMasterComponent,
    ScheduleTableComponent,
    ScheduleTaskDetailComponent,
    ScheduleTaskMasterComponent,
    ScheduleTaskPropertiesComponent,
    ScheduleTaskTableComponent,
} from './components';
import {
    CanActivateScheduleGuard,
    CanActivateScheduleTaskGuard,
    ScheduleExecutionsTableLoaderService,
    ScheduleTableLoaderService,
    ScheduleTaskTableLoaderService,
} from './providers';
import { SCHEDULER_ROUTES } from './scheduler.routes';

@NgModule({
    declarations: [
        CreateScheduleModalComponent,
        CreateScheduleTaskModalComponent,
        FollowUpScheduleDataPropertiesComponent,
        IntervalScheduleDataPropertiesComponent,
        ScheduleDataPropertiesComponent,
        ScheduleDetailComponent,
        ScheduleExecutionDetailModalComponent,
        ScheduleExecutionsTableComponent,
        ScheduleMasterComponent,
        SchedulePropertiesComponent,
        ScheduleTableComponent,
        ScheduleTaskDetailComponent,
        ScheduleTaskMasterComponent,
        ScheduleTaskPropertiesComponent,
        ScheduleTaskTableComponent,
        SchedulerModuleMasterComponent,
    ],
    providers: [
        CanActivateScheduleGuard,
        CanActivateScheduleTaskGuard,
        ScheduleExecutionsTableLoaderService,
        ScheduleTableLoaderService,
        ScheduleTaskTableLoaderService,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(SCHEDULER_ROUTES),
    ],
})
export class SchedulerModule {}

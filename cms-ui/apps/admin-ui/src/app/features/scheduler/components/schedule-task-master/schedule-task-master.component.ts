import { ScheduleTaskBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { NormalizableEntityType, ScheduleTask } from '@gentics/cms-models';

@Component({
    selector: 'gtx-schedule-task-master',
    templateUrl: './schedule-task-master.component.html',
    styleUrls: ['./schedule-task-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleTaskMasterComponent extends BaseTableMasterComponent<ScheduleTask, ScheduleTaskBO> {

    protected entityIdentifier: NormalizableEntityType = 'scheduleTask';
    protected detailPath = 'task';
}

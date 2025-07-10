import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { ScheduleExecution } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-schedule-exection-detail-modal',
    templateUrl: './schedule-execution-detail-modal.component.html',
    styleUrls: ['./schedule-execution-detail-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ScheduleExecutionDetailModalComponent extends BaseModal<void> {

    @Input()
    public execution: ScheduleExecution;

}

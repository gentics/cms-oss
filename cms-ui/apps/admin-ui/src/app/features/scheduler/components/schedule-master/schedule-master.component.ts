import { ScheduleBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Output,
    ViewChild,
} from '@angular/core';
import { NormalizableEntityType, Schedule } from '@gentics/cms-models';
import { Subscription } from 'rxjs';
import { ScheduleTableComponent } from '../schedule-table/schedule-table.component';

@Component({
    selector: 'gtx-schedule-master',
    templateUrl: './schedule-master.component.html',
    styleUrls: ['./schedule-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleMasterComponent extends BaseTableMasterComponent<Schedule, ScheduleBO> {

    @Output()
    public taskClick = new EventEmitter<void>();

    @ViewChild('table', { static: true })
    public tableInstance: ScheduleTableComponent;

    protected entityIdentifier: NormalizableEntityType = 'schedule';
    protected refreshSubscription: Subscription;

    public handleTaskClick(): void {
        this.taskClick.emit();
    }
}

import { ScheduleBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { NormalizableEntityType, Schedule } from '@gentics/cms-models';
import { coerceToBoolean } from '@gentics/ui-core';
import { interval, Subscription } from 'rxjs';
import { ScheduleTableComponent } from '../schedule-table/schedule-table.component';

@Component({
    selector: 'gtx-schedule-master',
    templateUrl: './schedule-master.component.html',
    styleUrls: ['./schedule-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleMasterComponent extends BaseTableMasterComponent<Schedule, ScheduleBO> implements OnInit, OnChanges, OnDestroy {

    @Input()
    public autoRefresh = false;

    @Output()
    public taskClick = new EventEmitter<void>();

    @ViewChild('table', { static: true })
    public tableInstance: ScheduleTableComponent;

    protected entityIdentifier: NormalizableEntityType = 'schedule';
    protected refreshSubscription: Subscription;

    public override ngOnInit(): void {
        super.ngOnInit();
        this.updateRefresh();
    }

    public ngOnChanges(changes: SimpleChanges): void {
        if (changes.autoRefresh) {
            this.autoRefresh = coerceToBoolean(this.autoRefresh);
            this.updateRefresh();
        }
    }

    public override ngOnDestroy(): void {
        super.ngOnDestroy();
        if (this.refreshSubscription) {
            this.refreshSubscription.unsubscribe();
        }
    }

    public handleTaskClick(): void {
        this.taskClick.emit();
    }

    protected updateRefresh(): void {
        if (!this.autoRefresh) {
            if (this.refreshSubscription) {
                this.refreshSubscription.unsubscribe();
                this.refreshSubscription = null;
            }
            return;
        }

        // Already active, nothing to do
        if (this.autoRefresh && this.refreshSubscription) {
            return;
        }

        this.refreshSubscription = interval(60_000).subscribe(() => {
            if (this.tableInstance) {
                this.tableInstance.reload();
            }
        });
    }
}

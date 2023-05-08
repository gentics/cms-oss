import { ScheduleExecutionBO } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AnyModelType, NormalizableEntityTypesMap, ScheduleExecution } from '@gentics/cms-models';
import { ModalService, TableColumn, TableRow, TableSortOrder } from '@gentics/ui-core';
import { ScheduleExecutionsTableLoaderOptions, ScheduleExecutionsTableLoaderService } from '../../providers';
import { ScheduleExecutionDetailModalComponent } from '../schedule-execution-detail-modal/schedule-execution-detail-modal.component';

@Component({
    selector: 'gtx-schedule-executions-table',
    templateUrl: './schedule-executions-table.component.html',
    styleUrls: ['./schedule-executions-table.components.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleExecutionsTableComponent
    extends BaseEntityTableComponent<ScheduleExecution, ScheduleExecutionBO, ScheduleExecutionsTableLoaderOptions>
    implements OnChanges {

    @Input()
    public scheduleId: string | number;

    public sortBy = 'startTime';
    public sortOrder = TableSortOrder.DESCENDING;

    protected rawColumns: TableColumn<ScheduleExecutionBO>[] = [
        {
            id: 'result',
            label: 'scheduler.execution_result',
            fieldPath: 'result',
            sortable: true,
        },
        {
            id: 'startTime',
            label: 'scheduler.execution_startTime',
            fieldPath: 'startTime',
            sortable: true,
        },
        {
            id: 'endTime',
            label: 'scheduler.execution_endTime',
            fieldPath: 'endTime',
            sortable: true,
        },
        {
            id: 'duration',
            label: 'scheduler.execution_duration',
            fieldPath: 'duration',
            sortable: true,
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'scheduleExecution';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: ScheduleExecutionsTableLoaderService,
        modalService: ModalService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.scheduleId) {
            this.loadTrigger.next();
        }
    }

    protected override createAdditionalLoadOptions(): ScheduleExecutionsTableLoaderOptions {
        return {
            scheduleId: this.scheduleId,
        };
    }

    public override handleRowClick(row: TableRow<ScheduleExecutionBO>): void {
        this.modalService.fromComponent(ScheduleExecutionDetailModalComponent, {
            width: '80%',
        }, {
            execution: row.item,
        }).then(dialog => dialog.open());

        super.handleRowClick(row);
    }
}

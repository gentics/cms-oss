import { BO_PERMISSIONS, ScheduleTaskBO } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AnyModelType, GcmsPermission, NormalizableEntityTypesMap, ScheduleTask } from '@gentics/cms-models';
import { ModalService, TableAction, TableColumn, TableSortOrder } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ScheduleTaskTableLoaderService } from '../../providers';
import { CreateScheduleTaskModalComponent } from '../create-schedule-task-modal/create-schedule-task-modal.component';

@Component({
    selector: 'gtx-schedule-task-table',
    templateUrl: './schedule-task-table.component.html',
    styleUrls: ['./schedule-task-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ScheduleTaskTableComponent extends BaseEntityTableComponent<ScheduleTask, ScheduleTaskBO> {

    public sortBy = 'name';
    public sortOrder = TableSortOrder.ASCENDING;

    protected rawColumns: TableColumn<ScheduleTaskBO>[] = [
        {
            id: 'name',
            label: 'common.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'description',
            label: 'common.description',
            fieldPath: 'description',
        },
        {
            id: 'internal',
            label: 'scheduler.internal',
            fieldPath: 'internal',
            align: 'center',
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'scheduleTask';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: ScheduleTaskTableLoaderService,
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

    public override async handleCreateButton(): Promise<void> {
        const dialog = await this.modalService.fromComponent(CreateScheduleTaskModalComponent, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
            width: '80%',
        }, {});
        const created = await dialog.open();

        if (!created) {
            return;
        }

        // Reload list once a new schedule was created
        this.loader.reload();
    }

    protected override createTableActionLoading(): Observable<TableAction<ScheduleTaskBO>[]> {
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<ScheduleTaskBO>[] = [
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('scheduler.delete_task'),
                        type: 'alert',
                        enabled: (task) => task == null || task[BO_PERMISSIONS].includes(GcmsPermission.DELETE),
                        single: true,
                        multiple: true,
                    },
                ];

                return actions;
            }),
        )
    }
}

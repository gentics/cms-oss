import { BO_PERMISSIONS, discard, ScheduleBO } from '@admin-ui/common';
import { I18nNotificationService, I18nService, PermissionsService, ScheduleOperations } from '@admin-ui/core';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, OnInit, Output } from '@angular/core';
import {
    AccessControlledType,
    AnyModelType,
    GcmsPermission,
    NormalizableEntityTypesMap,
    Schedule,
    SchedulerStatus,
    ScheduleSaveReqeust,
} from '@gentics/cms-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn, TableRow, TableSortOrder } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { combineLatest, forkJoin, interval, Observable, of } from 'rxjs';
import { distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { ScheduleTableLoaderService } from '../../providers';
import { CreateScheduleModalComponent } from '../create-schedule-modal/create-schedule-modal.component';
import { ScheduleExecutionDetailModalComponent } from '../schedule-execution-detail-modal/schedule-execution-detail-modal.component';

const RUN_SCHEDULE_ACTION = 'runSchedule';
const ACTIVATE_SCHEDULE_ACTION = 'activateSchedule';
const DEACTIVATE_SCHEDULE_ACTION = 'deactivateSchedule';

@Component({
    selector: 'gtx-schedule-table',
    templateUrl: './schedule-table.component.html',
    styleUrls: ['./schedule-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    })
export class ScheduleTableComponent extends BaseEntityTableComponent<Schedule, ScheduleBO> implements OnInit {

    @Output()
    public taskClick = new EventEmitter<number>();

    public sortBy = 'name';
    public sortOrder = TableSortOrder.ASCENDING;

    public canManageScheduler = false;
    public schedulerRunning = false;

    protected rawColumns: TableColumn<ScheduleBO>[] = [
        {
            id: 'name',
            label: 'common.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'active',
            label: 'scheduler.active',
            fieldPath: 'active',
            align: 'center',
        },
        {
            id: 'type',
            label: 'scheduler.scheduleType',
            fieldPath: 'scheduleData.type',
        },
        {
            id: 'task',
            label: 'scheduler.task',
            fieldPath: 'taskId',
            sortable: true,
            sortValue: 'taskId',
        },
        {
            id: 'status',
            label: 'scheduler.status',
            fieldPath: 'status',
        },
        {
            id: 'execStart',
            label: 'scheduler.execution_startTime',
            fieldPath: 'lastExecution.startTime',
        },
        {
            id: 'execResult',
            label: 'scheduler.execution_result',
            fieldPath: 'lastExecution.result',
        },
        {
            id: 'execDuration',
            label: 'scheduler.execution_duration',
            fieldPath: 'lastExecution.duration',
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'schedule';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: ScheduleTableLoaderService,
        modalService: ModalService,
        protected operations: ScheduleOperations,
        protected permissions: PermissionsService,
        protected notification: I18nNotificationService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(combineLatest([
            of(), // Load the status from the very beginning
            interval(10_000),
        ]).pipe(
            switchMap(() => this.operations.status()),
        ).subscribe(res => {
            this.schedulerRunning = res.status === SchedulerStatus.RUNNING;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.permissions.getTypePermissions(AccessControlledType.SCHEDULER).pipe(
            map(typePerm => typePerm.hasPermission(GcmsPermission.SUSPEND_SCHEDULER)),
            distinctUntilChanged(isEqual),
        ).subscribe(canManage => {
            this.canManageScheduler = canManage;
            this.changeDetector.markForCheck();
        }));
    }

    protected override createTableActionLoading(): Observable<TableAction<ScheduleBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
        ]).pipe(
            map(() => {
                const actions: TableAction<ScheduleBO>[] = [
                    {
                        id: RUN_SCHEDULE_ACTION,
                        icon: 'play_arrow',
                        label: this.i18n.instant('scheduler.run_schedule'),
                        type: 'primary',
                        enabled: (schedule) => schedule == null || schedule[BO_PERMISSIONS].includes(GcmsPermission.EDIT),
                        single: true,
                        multiple: true,
                    },
                    {
                        id: ACTIVATE_SCHEDULE_ACTION,
                        icon: 'done',
                        label: this.i18n.instant('scheduler.activate_schedule'),
                        type: 'success',
                        enabled: (schedule) => schedule == null || schedule[BO_PERMISSIONS].includes(GcmsPermission.EDIT),
                        single: true,
                        multiple: true,
                    },
                    {
                        id: DEACTIVATE_SCHEDULE_ACTION,
                        icon: 'close',
                        label: this.i18n.instant('scheduler.deactivate_schedule'),
                        type: 'warning',
                        enabled: (schedule) => schedule == null || schedule[BO_PERMISSIONS].includes(GcmsPermission.EDIT),
                        single: true,
                        multiple: true,
                    },
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('scheduler.delete_schedule'),
                        type: 'alert',
                        enabled: (schedule) => schedule == null || schedule[BO_PERMISSIONS].includes(GcmsPermission.DELETE),
                        single: true,
                        multiple: true,
                    },
                ];

                return actions;
            }),
        );
    }

    public override async handleCreateButton(): Promise<void> {
        const dialog = await this.modalService.fromComponent(CreateScheduleModalComponent, {
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

    public override handleAction(event: TableActionClickEvent<ScheduleBO>): void {
        switch (event.actionId) {
            case RUN_SCHEDULE_ACTION:
                this.runSchedules((this.loader as ScheduleTableLoaderService).getEntitiesByIds(this.getAffectedEntityIds(event)));
                return;

            case ACTIVATE_SCHEDULE_ACTION:
                this.activateSchedules((this.loader as ScheduleTableLoaderService).getEntitiesByIds(this.getAffectedEntityIds(event)));
                return;

            case DEACTIVATE_SCHEDULE_ACTION:
                this.deactivateSchedules((this.loader as ScheduleTableLoaderService).getEntitiesByIds(this.getAffectedEntityIds(event)));
                return;
        }

        super.handleAction(event);
    }

    public handleSchedulerResume(): void {
        this.subscriptions.push(this.operations.resumeExecutions().subscribe(res => {
            this.schedulerRunning = res.status === SchedulerStatus.RUNNING;
            this.notification.show({
                type: 'success',
                message: 'scheduler.resume_success',
            });
        }));
    }

    public handleSchedulerPause(): void {
        this.subscriptions.push(this.operations.suspendExecutions().subscribe(res => {
            this.schedulerRunning = res.status === SchedulerStatus.RUNNING;
            this.notification.show({
                type: 'success',
                message: 'scheduler.suspend_success',
            });
        }));
    }

    public openExecutionDetails(row: TableRow<ScheduleBO>, event?: MouseEvent): void {
        this.cancelEvent(event);
        if (row.item?.lastExecution == null) {
            return;
        }

        this.modalService.fromComponent(ScheduleExecutionDetailModalComponent, {
            width: '80%',
        }, {
            execution: row.item.lastExecution,
        }).then(dialog => dialog.open());
    }

    protected runSchedules(schedules: ScheduleBO[]): void {
        if (!Array.isArray(schedules)) {
            return;
        }

        let doAbort = false;
        schedules = schedules.filter(schedule => schedule);
        schedules.forEach(schedule => {
            if (!schedule[BO_PERMISSIONS].includes(GcmsPermission.EDIT)) {
                doAbort = true;
                this.notification.show({
                    type: 'alert',
                    message: 'scheduler.run_schedule_permission_required',
                    translationParams: {
                        scheduleName: schedule.name,
                    },
                });
            }
        })

        if (doAbort || schedules.length === 0) {
            return;
        }

        this.subscriptions.push(forkJoin(schedules
            .map(schedule => this.operations.execute(schedule.id),
            )).subscribe());
    }

    protected activateSchedules(schedules: ScheduleBO[]): void {
        if (!Array.isArray(schedules)) {
            return;
        }

        let doAbort = false;
        schedules = schedules.filter(schedule => schedule);
        schedules.forEach(schedule => {
            if (!schedule[BO_PERMISSIONS].includes(GcmsPermission.EDIT)) {
                doAbort = true;
                this.notification.show({
                    type: 'alert',
                    message: 'scheduler.activate_schedule_permission_required',
                    translationParams: {
                        scheduleName: schedule.name,
                    },
                });
            }
        })

        if (doAbort || schedules.length === 0) {
            return;
        }

        this.subscriptions.push(this.updateSchedules(schedules, {
            active: true,
        }).subscribe(() => {
            if (schedules.length === 1) {
                this.notification.show({
                    type: 'success',
                    message: 'scheduler.activate_schedule_success',
                    translationParams: {
                        scheduleName: schedules[0].name,
                    },
                });
            } else {
                this.notification.show({
                    type: 'success',
                    message: 'scheduler.activate_schedules_success',
                });
            }
        }));
    }

    protected deactivateSchedules(schedules: ScheduleBO[]): void {
        if (!Array.isArray(schedules)) {
            return;
        }

        let doAbort = false;
        schedules = schedules.filter(schedule => schedule);
        schedules.forEach(schedule => {
            if (!schedule[BO_PERMISSIONS].includes(GcmsPermission.EDIT)) {
                doAbort = true;
                this.notification.show({
                    type: 'alert',
                    message: 'scheduler.deactivate_schedule_permission_required',
                    translationParams: {
                        scheduleName: schedule.name,
                    },
                });
            }
        })

        if (doAbort || schedules.length === 0) {
            return;
        }

        this.subscriptions.push(this.updateSchedules(schedules, {
            active: false,
        }).subscribe(() => {
            if (schedules.length === 1) {
                this.notification.show({
                    type: 'success',
                    message: 'scheduler.deactivate_schedule_success',
                    translationParams: {
                        scheduleName: schedules[0].name,
                    },
                });
            } else {
                this.notification.show({
                    type: 'success',
                    message: 'scheduler.deactivate_schedules_success',
                });
            }
        }));
    }

    protected updateSchedules(schedules: ScheduleBO[], body: ScheduleSaveReqeust): Observable<void> {
        return forkJoin(schedules
            .map(schedule => this.operations.update(schedule.id, body, false)),
        ).pipe(
            discard(() => {
                // Reload the items, as they are out of date now and wouldn't update in the list otherwise
                this.loader.reload();
            }),
        );
    }
}

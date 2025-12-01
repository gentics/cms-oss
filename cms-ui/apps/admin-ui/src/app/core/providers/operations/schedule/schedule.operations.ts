import { applyInstancePermissions, discard } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import {
    EntityIdType,
    InstancePermissionMap,
    ModelType,
    Raw,
    Schedule,
    ScheduleBO,
    ScheduleCreateReqeust,
    ScheduleExecution,
    ScheduleExecutionListOptions,
    ScheduleListOptions,
    SchedulerStatusResponse,
    SchedulerSuspendRequest,
    ScheduleSaveReqeust,
    SingleInstancePermissionType,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { combineLatest, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class ScheduleOperations extends ExtendedEntityOperationsBase<'schedule'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entities: EntityManagerService,
        private notification: I18nNotificationService,
        private appState: AppStateService,
    ) {
        super(injector, 'schedule');
    }

    getAll(options?: ScheduleListOptions, parentId?: string | number): Observable<ScheduleBO<Raw>[]> {
        return this.api.scheduler.listSchedules(options).pipe(
            map((res) => applyInstancePermissions(res).items),
            map((items) => items.map((schedule) => this.mapToBusinessObject(schedule))),
            tap((schedules) => this.entities.addEntities(this.entityIdentifier, schedules)),
            this.catchAndRethrowError(),
        );
    }

    get(entityId: number, options?: any, parentId?: string | number): Observable<ScheduleBO<Raw>> {
        return combineLatest([
            this.api.scheduler.getSchedule(entityId),
            this.api.scheduler.getSchedulePermission(entityId, SingleInstancePermissionType.EDIT).pipe(
                map((res) => res.granted),
            ),
        ]).pipe(
            map(([res, canEdit]) => this.mapToBusinessObject(res.item, {
                [SingleInstancePermissionType.VIEW]: true,
                [SingleInstancePermissionType.EDIT]: canEdit,
                [SingleInstancePermissionType.DELETE]: canEdit,
            })),
            tap((schedule) => this.entities.addEntity(this.entityIdentifier, schedule)),
            this.catchAndRethrowError(),
        );
    }

    create(body: ScheduleCreateReqeust, notification: boolean = true): Observable<ScheduleBO<Raw>> {
        return this.api.scheduler.createSchedule(body).pipe(
            map((res) => this.mapToBusinessObject(res.item, {
                [SingleInstancePermissionType.VIEW]: true,
                [SingleInstancePermissionType.EDIT]: true,
                [SingleInstancePermissionType.DELETE]: true,
            })),
            tap((schedule) => {
                this.entities.addEntity(this.entityIdentifier, schedule);

                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_created',
                        translationParams: { name: schedule.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    update(entityId: EntityIdType, body: ScheduleSaveReqeust, notification: boolean = true): Observable<ScheduleBO<Raw>> {
        return this.api.scheduler.updateSchedule(entityId, body).pipe(
            map((res) => this.mapToBusinessObject(res.item, {
                [SingleInstancePermissionType.VIEW]: true,
                [SingleInstancePermissionType.EDIT]: true,
                [SingleInstancePermissionType.DELETE]: true,
            })),
            tap((schedule) => {
                this.entities.addEntity(this.entityIdentifier, schedule);

                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_updated',
                        translationParams: { name: schedule.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    delete(entityId: EntityIdType, notification: boolean = true): Observable<void> {
        const scheduleToBeDeleted = this.appState.now.entity[this.entityIdentifier][entityId];
        return this.api.scheduler.deleteSchedule(entityId).pipe(
            discard(() => {
                this.entities.deleteEntities(this.entityIdentifier, [entityId]);

                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_singular_deleted',
                        translationParams: { name: scheduleToBeDeleted?.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    execute(entityId: EntityIdType, notification: boolean = true): Observable<void> {
        return this.api.scheduler.executeSchedule(entityId).pipe(
            discard(() => {
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'scheduler.execute_schedule_success',
                        translationParams: {
                            scheduleName: this.appState.now.entity[this.entityIdentifier][entityId]?.name,
                        },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    listExecutions(scheduleId: number, options?: ScheduleExecutionListOptions): Observable<ScheduleExecution<Raw>[]> {
        return this.api.scheduler.listExecutions(scheduleId, options).pipe(
            map((res) => res.items),
            this.catchAndRethrowError(),
        );
    }

    getExecution(scheduleId: number, executionId: number): Observable<ScheduleExecution<Raw>> {
        return this.api.scheduler.getExecution(scheduleId, executionId).pipe(
            map((res) => res.item),
            this.catchAndRethrowError(),
        );
    }

    status(): Observable<SchedulerStatusResponse> {
        return this.api.scheduler.getStatus();
    }

    suspendExecutions(request?: SchedulerSuspendRequest): Observable<SchedulerStatusResponse> {
        return this.api.scheduler.suspend(request);
    }

    resumeExecutions(): Observable<SchedulerStatusResponse> {
        return this.api.scheduler.resume();
    }

    private mapToBusinessObject<T extends ModelType>(schedule: Schedule<T>, permissions?: InstancePermissionMap): ScheduleBO<T> {
        const bo: ScheduleBO<T> = {
            ...schedule,
            id: schedule.id + '',
        };

        if (permissions) {
            bo.permissions = permissions;
        }

        return bo;
    }

}

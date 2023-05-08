import { applyInstancePermissions, discard } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import {
    InstancePermissionMap,
    ModelType,
    Raw,
    ScheduleTask,
    ScheduleTaskBO,
    ScheduleTaskListOptions,
    ScheduleTaskSaveRequest,
    SingleInstancePermissionType,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { combineLatest, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class ScheduleTaskOperations extends ExtendedEntityOperationsBase<'scheduleTask'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entities: EntityManagerService,
        private notification: I18nNotificationService,
        private appState: AppStateService,
    ) {
        super(injector, 'scheduleTask');
    }

    getAll(options?: ScheduleTaskListOptions, parentId?: string | number): Observable<ScheduleTaskBO<Raw>[]> {
        return this.api.scheduler.listTasks(options).pipe(
            map(res => applyInstancePermissions(res).items),
            map(items => items.map(task => this.mapToBusinessObject(task))),
            tap(tasks => this.entities.addEntities(this.entityIdentifier, tasks)),
            this.catchAndRethrowError(),
        )
    }

    get(entityId: number, options?: any, parentId?: string | number): Observable<ScheduleTaskBO<Raw>> {
        return combineLatest([
            this.api.scheduler.getTask(entityId).pipe(
                map(res => res.item),
            ),
            this.api.scheduler.getTaskPermission(entityId, SingleInstancePermissionType.EDIT).pipe(
                map(res => res.granted),
            ),
        ]).pipe(
            map(([task, canEdit]) => this.mapToBusinessObject(task, {
                [SingleInstancePermissionType.VIEW]: true,
                [SingleInstancePermissionType.EDIT]: canEdit,
                [SingleInstancePermissionType.DELETE]: canEdit,
            })),
            tap(task => this.entities.addEntity(this.entityIdentifier, task)),
            this.catchAndRethrowError(),
        );
    }

    create(body: ScheduleTaskSaveRequest, notification: boolean = true): Observable<ScheduleTaskBO<Raw>> {
        return this.api.scheduler.createTask(body).pipe(
            map(res => this.mapToBusinessObject(res.item, {
                [SingleInstancePermissionType.VIEW]: true,
                [SingleInstancePermissionType.EDIT]: true,
                [SingleInstancePermissionType.DELETE]: true,
            })),
            tap(task => {
                this.entities.addEntity(this.entityIdentifier, task);
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_created',
                        translationParams: { name: task.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        )
    }

    update(entityId: number, body: ScheduleTaskSaveRequest, notification: boolean = true): Observable<ScheduleTaskBO<Raw>> {
        return this.api.scheduler.updateTask(entityId, body).pipe(
            map(res => this.mapToBusinessObject(res.item, {
                [SingleInstancePermissionType.VIEW]: true,
                [SingleInstancePermissionType.EDIT]: true,
                [SingleInstancePermissionType.DELETE]: true,
            })),
            tap(task => {
                this.entities.addEntity(this.entityIdentifier, task);
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_updated',
                        translationParams: { name: task.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    delete(entityId: string | number, notification: boolean = true): Observable<void> {
        const taskToBeDeleted = this.appState.now.entity[this.entityIdentifier][entityId];

        return this.api.scheduler.deleteTask(entityId).pipe(
            discard(() => {
                this.entities.deleteEntities(this.entityIdentifier, [entityId]);
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_singular_deleted',
                        translationParams: { name: taskToBeDeleted?.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    private mapToBusinessObject<T extends ModelType>(task: ScheduleTask<T>, permissions?: InstancePermissionMap): ScheduleTaskBO<T> {
        const bo: ScheduleTaskBO<T> = {
            ...task,
            id: task.id + '',
        };

        if (permissions) {
            bo.permissions = permissions;
        }

        return bo;
    }
}

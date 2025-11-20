import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_PERMISSIONS,
    discard,
    EditableEntity,
    EditableEntityBusinessObjects,
    EntityCreateRequestModel,
    EntityCreateRequestParams,
    EntityCreateResponseModel,
    EntityDeleteRequestModel,
    EntityDeleteRequestParams,
    EntityEditorHandler,
    EntityList,
    EntityListHandler,
    EntityListRequestModel,
    EntityListRequestParams,
    EntityListResponseModel,
    EntityLoadRequestParams,
    EntityLoadResponseModel,
    EntityUpdateRequestModel,
    EntityUpdateRequestParams,
    EntityUpdateResponseModel,
} from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import {
    AccessControlledType,
    GcmsPermission,
    ModelType,
    Schedule,
    SchedulerStatusResponse,
    SchedulerSuspendRequest,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { forkJoin, map, Observable, tap } from 'rxjs';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';

@Injectable()
export class ScheduleHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<EditableEntity.SCHEDULE>,
        EntityListHandler<EditableEntity.SCHEDULE> {

    constructor(
        errorHandler: ErrorHandler,
        private client: GCMSRestClientService,
        private notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: Schedule<ModelType.Raw>): string {
        return entity.name;
    }

    mapToBusinessObject(
        entity: Schedule<ModelType.Raw>,
        index?: number,
        context?: any,
    ): EditableEntityBusinessObjects[EditableEntity.SCHEDULE] {
        return {
            ...entity,
            [BO_ID]: `${entity.id}`,
            [BO_DISPLAY_NAME]: this.displayName(entity),
            [BO_PERMISSIONS]: [GcmsPermission.VIEW],
        };
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.SCHEDULE>,
        params?: EntityCreateRequestParams<EditableEntity.SCHEDULE>,
    ): Observable<EntityCreateResponseModel<EditableEntity.SCHEDULE>> {
        return this.client.scheduler.create(data).pipe(
            tap((res) => {
                const name = this.displayName(res.item);
                this.nameMap[res.item.id] = name;

                this.notification.show({
                    type: 'success',
                    message: 'shared.item_created',
                    translationParams: {
                        name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    createMapped(
        data: EntityCreateRequestModel<EditableEntity.SCHEDULE>,
        params?: EntityCreateRequestParams<EditableEntity.SCHEDULE>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.SCHEDULE]> {
        return this.create(data, params).pipe(
            map((res) => this.mapToBusinessObject(res.item)),
        );
    }

    get(
        id: string | number,
        params?: EntityLoadRequestParams<EditableEntity.SCHEDULE>,
    ): Observable<EntityLoadResponseModel<EditableEntity.SCHEDULE>> {
        return this.client.scheduler.get(id).pipe(
            tap((res) => {
                this.nameMap[res.item.id] = this.displayName(res.item);
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(
        id: string | number,
        params?: EntityLoadRequestParams<EditableEntity.SCHEDULE>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.SCHEDULE]> {
        return forkJoin([
            this.get(id, params),
            this.client.permission.check(GcmsPermission.EDIT, AccessControlledType.SCHEDULER, id),
        ]).pipe(
            map(([res, perms]) => {
                const bo = this.mapToBusinessObject(res.item);

                if (perms.granted) {
                    bo[BO_PERMISSIONS].push(GcmsPermission.EDIT, GcmsPermission.DELETE);
                }

                return bo;
            }),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.SCHEDULE>,
        params?: EntityUpdateRequestParams<EditableEntity.SCHEDULE>,
    ): Observable<EntityUpdateResponseModel<EditableEntity.SCHEDULE>> {
        return this.client.scheduler.update(id, data).pipe(
            tap((res) => {
                const name = this.displayName(res.item);
                this.nameMap[res.item.id] = name;

                this.notification.show({
                    type: 'success',
                    message: 'shared.item_updated',
                    translationParams: {
                        name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    updateMapped(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.SCHEDULE>,
        params?: EntityUpdateRequestParams<EditableEntity.SCHEDULE>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.SCHEDULE]> {
        return this.update(id, data, params).pipe(
            map((res) => {
                const bo = this.mapToBusinessObject(res.item);

                // Since we already updated it, we have permissions to do so
                bo[BO_PERMISSIONS].push(GcmsPermission.EDIT, GcmsPermission.DELETE);

                return bo;
            }),
        );
    }

    delete(
        id: string | number,
        data: EntityDeleteRequestModel<EditableEntity.SCHEDULE>,
        params?: EntityDeleteRequestParams<EditableEntity.SCHEDULE>,
    ): Observable<void> {
        return this.client.scheduler.delete(id).pipe(
            discard(() => {
                const name = this.nameMap[id];

                if (!name) {
                    return;
                }

                delete this.nameMap[id];
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_singular_deleted',
                    translationParams: {
                        name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    list(
        body?: EntityListRequestModel<EditableEntity.SCHEDULE>,
        params?: EntityListRequestParams<EditableEntity.SCHEDULE>,
    ): Observable<EntityListResponseModel<EditableEntity.SCHEDULE>> {
        return this.client.scheduler.list(params).pipe(
            tap((res) => {
                res.items.forEach((item) => {
                    this.nameMap[item.id] = this.displayName(item);
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listMapped(
        body?: EntityListRequestModel<EditableEntity.SCHEDULE>,
        params?: EntityListRequestParams<EditableEntity.SCHEDULE>,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.SCHEDULE]>> {
        return this.list(body, params).pipe(
            map((res) => ({
                items: res.items.map((item, index) => this.mapToBusinessObject(item, index)),
                totalItems: res.numItems,
            })),
        );
    }

    public status(): Observable<SchedulerStatusResponse> {
        return this.client.scheduler.status().pipe(
            this.catchAndRethrowError(),
        );
    }

    public resume(): Observable<SchedulerStatusResponse> {
        return this.client.scheduler.resume().pipe(
            this.catchAndRethrowError(),
        );
    }

    public suspend(body?: SchedulerSuspendRequest): Observable<SchedulerStatusResponse> {
        return this.client.scheduler.suspend(body).pipe(
            this.catchAndRethrowError(),
        );
    }
}

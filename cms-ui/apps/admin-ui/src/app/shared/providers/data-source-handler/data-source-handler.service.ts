/* eslint-disable @typescript-eslint/no-unused-vars */
import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_PERMISSIONS,
    DevToolEntityHandler,
    DevToolEntityListRequestModel,
    DevToolEntityListRequestParams,
    DevToolEntityListResponseModel,
    EditableEntity,
    EditableEntityBusinessObjects,
    EditableEntityModels,
    EntityCreateRequestModel,
    EntityCreateRequestParams,
    EntityCreateResponseModel,
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
import { DataSource, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../../providers/base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../../../core/providers/error-handler';
import { I18nNotificationService } from '../../../core/providers/i18n-notification';

@Injectable()
export class DataSourceHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<EditableEntity.DATA_SOURCE>,
        EntityListHandler<EditableEntity.DATA_SOURCE>,
        DevToolEntityHandler<EditableEntity.DATA_SOURCE> {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: EditableEntityModels[EditableEntity.DATA_SOURCE]): string {
        return entity.name;
    }

    public mapToBusinessObject(
        ds: EditableEntityModels[EditableEntity.DATA_SOURCE],
        index?: number,
    ): EditableEntityBusinessObjects[EditableEntity.DATA_SOURCE] {
        return {
            ...ds,
            [BO_ID]: String(ds.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: this.displayName(ds),
        };
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.DATA_SOURCE>,
        params?: EntityCreateRequestParams<EditableEntity.DATA_SOURCE>,
    ): Observable<EntityCreateResponseModel<EditableEntity.DATA_SOURCE>> {
        return this.api.dataSource.createDataSource(data).pipe(
            tap(res => {
                const name = this.displayName(res.datasource);
                this.nameMap[res.datasource.id] = name;

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
        data: EntityCreateRequestModel<EditableEntity.DATA_SOURCE>,
        options?: EntityCreateRequestParams<EditableEntity.DATA_SOURCE>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.DATA_SOURCE]> {
        return this.create(data, options).pipe(
            map(res => this.mapToBusinessObject(res.datasource)),
        );
    }

    get(
        id: string | number,
        params?: EntityLoadRequestParams<EditableEntity.DATA_SOURCE>,
    ): Observable<EntityLoadResponseModel<EditableEntity.DATA_SOURCE>> {
        return this.api.dataSource.getDataSource(id).pipe(
            tap(res => {
                const name = this.displayName(res.datasource);
                this.nameMap[res.datasource.id] = name;
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(id: string | number): Observable<EditableEntityBusinessObjects[EditableEntity.DATA_SOURCE]> {
        return this.get(id).pipe(
            map(res => this.mapToBusinessObject(res.datasource)),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.DATA_SOURCE>,
        params?: EntityUpdateRequestParams<EditableEntity.DATA_SOURCE>,
    ): Observable<EntityUpdateResponseModel<EditableEntity.DATA_SOURCE>> {
        return this.api.dataSource.updateDataSource(id, data).pipe(
            tap(res => {
                const name = this.displayName(res.datasource);
                this.nameMap[res.datasource.id] = name;

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
        data: EntityUpdateRequestModel<EditableEntity.DATA_SOURCE>,
        params?: EntityUpdateRequestParams<EditableEntity.DATA_SOURCE>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.DATA_SOURCE]> {
        return this.update(id, data, params).pipe(
            map(res => this.mapToBusinessObject(res.datasource)),
        );
    }

    delete(id: string | number): Observable<void> {
        return this.api.dataSource.deleteDataSource(id).pipe(
            tap(() => {
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
        body?: EntityListRequestModel<EditableEntity.DATA_SOURCE>,
        params?: EntityListRequestParams<EditableEntity.DATA_SOURCE>,
    ): Observable<EntityListResponseModel<EditableEntity.DATA_SOURCE>> {
        return this.api.dataSource.getDataSources(params).pipe(
            tap(res => {
                res.items.forEach(objCat => {
                    const name = this.displayName(objCat);
                    this.nameMap[objCat.id] = name;
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listMapped(
        body?: EntityListRequestModel<EditableEntity.DATA_SOURCE>,
        params?: EntityListRequestParams<EditableEntity.DATA_SOURCE>,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.DATA_SOURCE]>> {
        return this.list(body, params).pipe(
            map(res => ({
                items: res.items.map((item, index) => this.mapToBusinessObject(item, index)),
                totalItems: res.numItems,
            })),
        );
    }

    addToDevTool(
        devtoolPackage: string,
        entityId: string | number,
    ): Observable<void> {
        return this.api.devTools.addDataSourceToPackage(devtoolPackage, entityId).pipe(
            tap(() => {
                this.notification.show({
                    message: 'dataSource.dataSource_successfully_added_to_package',
                    type: 'success',
                    translationParams: {
                        name: this.nameMap[entityId],
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    removeFromDevTool(
        devtoolPackage: string,
        entityId: string | number,
    ): Observable<void> {
        return this.api.devTools.removeDataSourceFromPackage(devtoolPackage, entityId).pipe(
            tap(() => {
                this.notification.show({
                    message: 'dataSource.dataSource_successfully_removed_from_package',
                    type: 'success',
                    translationParams: {
                        name: this.nameMap[entityId],
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listFromDevTool(
        devtoolPackage: string,
        body?: DevToolEntityListRequestModel<EditableEntity.DATA_SOURCE>,
        params?: DevToolEntityListRequestParams<EditableEntity.DATA_SOURCE>,
    ): Observable<DevToolEntityListResponseModel<EditableEntity.DATA_SOURCE>> {
        return this.api.devTools.getDataSources(devtoolPackage, params).pipe(
            tap(res => {
                res.items.forEach(ds => {
                    const name = this.displayName(ds);
                    this.nameMap[ds.id] = name;
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listFromDevToolMapped(
        devtoolPackage: string,
        body?: DevToolEntityListRequestModel<EditableEntity.DATA_SOURCE>,
        params?: DevToolEntityListRequestParams<EditableEntity.DATA_SOURCE>,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.DATA_SOURCE]>> {
        return this.listFromDevTool(devtoolPackage, body, params).pipe(
            map(res => ({
                items: res.items.map((item, index) => this.mapToBusinessObject(item, index)),
                totalItems: res.numItems,
            })),
        );
    }

    getFromDevtoolMapped(packageId: string, entityId: string): Observable<EditableEntityBusinessObjects[EditableEntity.DATA_SOURCE]> {
        return this.api.devTools.getDataSource(packageId, entityId).pipe(
            map(res => this.mapToBusinessObject(res.datasource)),
            tap(con => {
                this.nameMap[con.id] = con[BO_DISPLAY_NAME];
            }),
            this.catchAndRethrowError(),
        );
    }
}

/* eslint-disable @typescript-eslint/no-unused-vars */
import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_PERMISSIONS,
    DevtoolEntityListHandler,
    DevtoolEntityListRequestModel,
    DevtoolEntityListRequestParams,
    DevtoolEntityListResponseModel,
    EditableEntity,
    EditableEntityBusinessObjects,
    EditableEntityModels,
    EntityCreateRequestModel,
    EntityCreateRequestParams,
    EntityCreateResponseModel,
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
    applyPermissions,
    discard,
} from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { AnyModelType, DataSourceConstructListOptions, DataSourceConstructListResponse, EntityIdType, Node, Raw, TagType } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';
import { I18nNotificationService } from '../i18n-notification';

@Injectable()
export class ConstructHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<EditableEntity.CONSTRUCT>,
        EntityListHandler<EditableEntity.CONSTRUCT>,
        DevtoolEntityListHandler<EditableEntity.CONSTRUCT> {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: EditableEntityModels[EditableEntity.CONSTRUCT]): string {
        return entity.name;
    }

    public mapToBusinessObject(
        construct: EditableEntityModels[EditableEntity.CONSTRUCT],
        index?: number,
    ): EditableEntityBusinessObjects[EditableEntity.CONSTRUCT] {
        return {
            ...construct,
            [BO_ID]: String(construct.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: this.displayName(construct),
        };
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.CONSTRUCT>,
        params?: EntityCreateRequestParams<EditableEntity.CONSTRUCT>,
    ): Observable<EntityCreateResponseModel<EditableEntity.CONSTRUCT>> {
        return this.api.tagType.createTagType(data, params).pipe(
            tap(res => {
                const name = this.displayName(res.construct);
                this.nameMap[res.construct.id] = name;

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
        data: EntityCreateRequestModel<EditableEntity.CONSTRUCT>,
        options?: EntityCreateRequestParams<EditableEntity.CONSTRUCT>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.CONSTRUCT]> {
        return this.create(data, options).pipe(
            map(res => this.mapToBusinessObject(res.construct)),
        );
    }

    get(
        id: string | number,
        params?: EntityLoadRequestParams<EditableEntity.CONSTRUCT>,
    ): Observable<EntityLoadResponseModel<EditableEntity.CONSTRUCT>> {
        return this.api.tagType.getTagType(id).pipe(
            tap(res => {
                const name = this.displayName(res.construct);
                this.nameMap[res.construct.id] = name;
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(
        id: string | number,
        params?: EntityLoadRequestParams<EditableEntity.CONSTRUCT>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.CONSTRUCT]> {
        return this.get(id, params).pipe(
            map(res => this.mapToBusinessObject(res.construct)),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.CONSTRUCT>,
        params?: EntityUpdateRequestParams<EditableEntity.CONSTRUCT>,
    ): Observable<EntityUpdateResponseModel<EditableEntity.CONSTRUCT>> {
        return this.api.tagType.updateTagType(id, data).pipe(
            tap(res => {
                const name = this.displayName(res.construct);
                this.nameMap[res.construct.id] = name;

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
        data: EntityUpdateRequestModel<EditableEntity.CONSTRUCT>,
        params?: EntityUpdateRequestParams<EditableEntity.CONSTRUCT>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.CONSTRUCT]> {
        return this.update(id, data, params).pipe(
            map(res => this.mapToBusinessObject(res.construct)),
        );
    }

    delete(id: string | number, parms?: EntityDeleteRequestParams<EditableEntity.CONSTRUCT>): Observable<void> {
        return this.api.tagType.deleteTagType(id).pipe(
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
        body?: EntityListRequestModel<EditableEntity.CONSTRUCT>,
        params?: EntityListRequestParams<EditableEntity.CONSTRUCT>,
    ): Observable<EntityListResponseModel<EditableEntity.CONSTRUCT>> {
        return this.api.tagType.getTagTypes(params).pipe(
            tap(res => {
                res.items.forEach(objCat => {
                    const name = this.displayName(objCat);
                    this.nameMap[objCat.id] = name;
                });
            }),
        );
    }

    listMapped(
        body?: EntityListRequestModel<EditableEntity.CONSTRUCT>,
        params?: EntityListRequestParams<EditableEntity.CONSTRUCT>,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.CONSTRUCT]>> {
        return this.list(body, params).pipe(
            map(res => {
                const items = res.items.map((item, index) => this.mapToBusinessObject(item, index));
                applyPermissions(items, res);

                return {
                    items,
                    totalItems: res.numItems,
                };
            }),
        );
    }

    listFromDevtool(
        devtoolPackage: string,
        body?: DevtoolEntityListRequestModel<EditableEntity.CONSTRUCT>,
        params?: DevtoolEntityListRequestParams<EditableEntity.CONSTRUCT>,
    ): Observable<DevtoolEntityListResponseModel<EditableEntity.CONSTRUCT>> {
        return this.api.devTools.getConstructs(devtoolPackage, params).pipe(
            tap(res => {
                res.items.forEach(objCat => {
                    const name = this.displayName(objCat);
                    this.nameMap[objCat.id] = name;
                });
            }),
        );
    }

    listFromDevtoolMapped(
        devtoolPackage: string,
        body?: DevtoolEntityListRequestModel<EditableEntity.CONSTRUCT>,
        params?: DevtoolEntityListRequestParams<EditableEntity.CONSTRUCT>,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.CONSTRUCT]>> {
        return this.listFromDevtool(devtoolPackage, body, params).pipe(
            map(res => {
                const items = res.items.map((item, index) => this.mapToBusinessObject(item, index));
                applyPermissions(items, res);

                return {
                    items,
                    totalItems: res.numItems,
                };
            }),
        );
    }

    getFromDevtoolMapped(packageId: string, entityId: string): Observable<EditableEntityBusinessObjects[EditableEntity.CONSTRUCT]> {
        return this.api.devTools.getConstruct(packageId, entityId).pipe(
            map(res => this.mapToBusinessObject(res.construct)),
            tap(con => {
                this.nameMap[con.id] = con[BO_DISPLAY_NAME];
            }),
            this.catchAndRethrowError(),
        );
    }

    getLinkedNodes(constructId: EntityIdType): Observable<Node[]> {
        return this.api.tagType.getLinkedNodes(constructId).pipe(
            map(res => res.items),
            this.catchAndRethrowError(),
        );
    }

    linkToNode(constructId: EntityIdType, nodeId: number): Observable<void> {
        return this.api.tagType.linkTagToNode({
            targetIds: [`${constructId}`],
            ids: [nodeId],
        }).pipe(discard());
    }

    unlinkFromNode(constructId: EntityIdType, nodeId: number): Observable<void> {
        return this.api.tagType.unlinkTagFromNode({
            targetIds: [`${constructId}`],
            ids: [nodeId],
        }).pipe(discard());
    }

    listFromDataSource(
        dataSourceId: string | number,
        body?: never,
        params?: DataSourceConstructListOptions,
    ): Observable<DataSourceConstructListResponse> {
        return this.api.dataSource.getConstructs(dataSourceId, params).pipe(
            tap(res => {
                res.items.forEach(con => {
                    const name = this.displayName(con);
                    this.nameMap[con.id] = name;
                });
            }),
        );
    }

    listFromDataSourceMapped(
        dataSourceId: string | number,
        body?: never,
        params?: DataSourceConstructListOptions,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.CONSTRUCT]>> {
        return this.listFromDataSource(dataSourceId, body, params).pipe(
            map(res => {
                const items = res.items.map((item, index) => this.mapToBusinessObject(item, index));
                applyPermissions(items, res);

                return {
                    items,
                    totalItems: res.numItems,
                };
            }),
        );
    }
}

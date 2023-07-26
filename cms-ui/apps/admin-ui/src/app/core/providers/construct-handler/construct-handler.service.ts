import {
    DevtoolEntityListHandler,
    DevtoolEntityListRequestModel,
    DevtoolEntityListRequestParams,
    DevtoolEntityListResponseModel,
    EditableEntity,
    EntityCreateRequestModel,
    EntityCreateRequestParams,
    EntityCreateResponseModel,
    EntityEditorHandler,
    EntityList,
    EntityListHandler,
    EntityListRequestModel,
    EntityListRequestParams,
    EntityListResponseModel,
    EntityLoadResponseModel,
    EntityUpdateRequestModel,
    EntityUpdateResponseModel,
    discard,
} from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { AnyModelType, EntityIdType, Node, Raw, TagType } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';
import { I18nNotificationService } from '../i18n-notification';

@Injectable()
export class ConstructHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<TagType<Raw>, EditableEntity.CONSTRUCT>,
        EntityListHandler<TagType<Raw>, EditableEntity.CONSTRUCT>,
        DevtoolEntityListHandler<TagType<Raw>, EditableEntity.CONSTRUCT> {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: TagType<AnyModelType>): string {
        return entity.name;
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.CONSTRUCT>,
        options?: EntityCreateRequestParams<EditableEntity.CONSTRUCT>,
    ): Observable<EntityCreateResponseModel<EditableEntity.CONSTRUCT>> {
        return this.api.tagType.createTagType(data, options).pipe(
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
    ): Observable<TagType> {
        return this.create(data, options).pipe(
            map(res => res.construct),
        );
    }

    get(id: string | number): Observable<EntityLoadResponseModel<EditableEntity.CONSTRUCT>> {
        return this.api.tagType.getTagType(id).pipe(
            tap(res => {
                const name = this.displayName(res.construct);
                this.nameMap[res.construct.id] = name;
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(id: string | number): Observable<TagType> {
        return this.get(id).pipe(
            map(res => res.construct),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.CONSTRUCT>,
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
    ): Observable<TagType> {
        return this.update(id, data).pipe(
            map(res => res.construct),
        );
    }

    delete(id: string | number): Observable<void> {
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
    ): Observable<EntityList<TagType<Raw>>> {
        return this.list(body, params).pipe(
            map(res => ({
                items: res.items,
                totalItems: res.numItems,
            })),
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
    ): Observable<EntityList<TagType<Raw>>> {
        return this.listFromDevtool(devtoolPackage, body, params).pipe(
            map(res => ({
                items: res.items,
                totalItems: res.numItems,
            })),
        );
    }

    getFromDevtoolMapped(packageId: string, entityId: string): Observable<TagType<Raw>> {
        return this.api.devTools.getConstruct(packageId, entityId).pipe(
            map(res => res.construct),
            tap(con => {
                this.nameMap[con.id] = this.displayName(con);
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
}

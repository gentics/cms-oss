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
    discard,
} from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { EntityIdType, Node, Raw } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';

@Injectable()
export class ObjectPropertyHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<EditableEntity.OBJECT_PROPERTY>,
        EntityListHandler<EditableEntity.OBJECT_PROPERTY>,
        DevToolEntityHandler<EditableEntity.OBJECT_PROPERTY> {

    constructor(
        errorHandler: ErrorHandler,
        protected client: GCMSRestClientService,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: EditableEntityModels[EditableEntity.OBJECT_PROPERTY]): string {
        return entity.name;
    }

    public mapToBusinessObject(
        property: EditableEntityModels[EditableEntity.OBJECT_PROPERTY],
        index?: number,
    ): EditableEntityBusinessObjects[EditableEntity.OBJECT_PROPERTY] {
        return {
            ...property,
            [BO_ID]: String(property.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: this.displayName(property),
        };
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.OBJECT_PROPERTY>,
        params?: EntityCreateRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EntityCreateResponseModel<EditableEntity.OBJECT_PROPERTY>> {
        return this.client.objectProperty.create(data).pipe(
            tap((res) => {
                const name = this.displayName(res.objectProperty);
                this.nameMap[res.objectProperty.id] = name;

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
        data: EntityCreateRequestModel<EditableEntity.OBJECT_PROPERTY>,
        params?: EntityCreateRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.OBJECT_PROPERTY]> {
        return this.create(data, params).pipe(
            map((res) => this.mapToBusinessObject(res.objectProperty)),
        );
    }

    get(
        id: string | number,
        params?: EntityLoadRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EntityLoadResponseModel<EditableEntity.OBJECT_PROPERTY>> {
        return this.client.objectProperty.get(id).pipe(
            tap((res) => {
                const name = this.displayName(res.objectProperty);
                this.nameMap[res.objectProperty.id] = name;
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(
        id: string | number,
        params?: EntityLoadRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.OBJECT_PROPERTY]> {
        return this.get(id, params).pipe(
            map((res) => this.mapToBusinessObject(res.objectProperty)),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.OBJECT_PROPERTY>,
        params?: EntityUpdateRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EntityUpdateResponseModel<EditableEntity.OBJECT_PROPERTY>> {
        return this.client.objectProperty.update(id, data).pipe(
            tap((res) => {
                const name = this.displayName(res.objectProperty);
                this.nameMap[res.objectProperty.id] = name;

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
        data: EntityUpdateRequestModel<EditableEntity.OBJECT_PROPERTY>,
        params?: EntityUpdateRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.OBJECT_PROPERTY]> {
        return this.update(id, data, params).pipe(
            map((res) => this.mapToBusinessObject(res.objectProperty)),
        );
    }

    delete(id: string | number, params?: EntityDeleteRequestParams<EditableEntity.OBJECT_PROPERTY>): Observable<void> {
        return this.client.objectProperty.delete(id).pipe(
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
        body?: EntityListRequestModel<EditableEntity.OBJECT_PROPERTY>,
        params?: EntityListRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EntityListResponseModel<EditableEntity.OBJECT_PROPERTY>> {
        return this.client.objectProperty.list(params).pipe(
            tap((res) => {
                res.items.forEach((objCat) => {
                    const name = this.displayName(objCat);
                    this.nameMap[objCat.id] = name;
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listMapped(
        body?: EntityListRequestModel<EditableEntity.OBJECT_PROPERTY>,
        params?: EntityListRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.OBJECT_PROPERTY]>> {
        return this.list(body, params).pipe(
            map((res) => ({
                items: res.items.map((item, index) => this.mapToBusinessObject(item, index)),
                totalItems: res.numItems,
            })),
        );
    }

    addToDevTool(
        devtoolPackage: string,
        entityId: string | number,
    ): Observable<void> {
        return this.client.devTools.assignObjectProperty(devtoolPackage, entityId).pipe(
            discard(() => {
                this.notification.show({
                    message: 'object_property.objectProperty_successfully_added_to_package',
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
        return this.client.devTools.unassignObjectProperty(devtoolPackage, entityId).pipe(
            discard(() => {
                this.notification.show({
                    message: 'object_property.objectProperty_successfully_removed_from_package',
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
        body?: DevToolEntityListRequestModel<EditableEntity.OBJECT_PROPERTY>,
        params?: DevToolEntityListRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<DevToolEntityListResponseModel<EditableEntity.OBJECT_PROPERTY>> {
        return this.client.devTools.listObjectProperties(devtoolPackage, params).pipe(
            tap((res) => {
                res.items.forEach((objCat) => {
                    const name = this.displayName(objCat);
                    this.nameMap[objCat.id] = name;
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listFromDevToolMapped(
        devtoolPackage: string,
        body?: DevToolEntityListRequestModel<EditableEntity.OBJECT_PROPERTY>,
        params?: DevToolEntityListRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.OBJECT_PROPERTY]>> {
        return this.listFromDevTool(devtoolPackage, body, params).pipe(
            map((res) => ({
                items: res.items.map((item, index) => this.mapToBusinessObject(item, index)),
                totalItems: res.numItems,
            })),
        );
    }

    getLinkedNodes(objectPropertyId: EntityIdType): Observable<Node<Raw>[]> {
        return this.client.objectProperty.listNodes(objectPropertyId).pipe(
            map((res) => res.items),
        );
    }

    addNodeRestriction(objectPropertyIds: number[], nodeIds: number[]): Observable<void> {
        return this.client.objectProperty.linkToNode({
            targetIds: objectPropertyIds,
            ids: nodeIds,
        }).pipe(discard(() => {
            this.notification.show({
                type: 'success',
                message: 'object_property.operation_linked_to_nodes_successful',
            });
        }));
    }

    removeNodeRestriction(objectPropertyIds: number[], nodeIds: number[]): Observable<void> {
        return this.client.objectProperty.unlinkFromNode({
            ids: nodeIds,
            targetIds: objectPropertyIds,
        }).pipe(discard(() => {
            this.notification.show({
                type: 'success',
                message: 'object_property.operation_linked_to_nodes_successful',
            });
        }));
    }
}

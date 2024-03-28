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
import { EntityIdType, Node, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable, forkJoin, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';
import { I18nNotificationService } from '../i18n-notification';

@Injectable()
export class ObjectPropertyHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<EditableEntity.OBJECT_PROPERTY>,
        EntityListHandler<EditableEntity.OBJECT_PROPERTY>,
        DevToolEntityHandler<EditableEntity.OBJECT_PROPERTY> {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
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
        return this.api.objectproperties.createObjectProperty(data).pipe(
            tap(res => {
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
            map(res => this.mapToBusinessObject(res.objectProperty)),
        );
    }

    get(
        id: string | number,
        params?: EntityLoadRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EntityLoadResponseModel<EditableEntity.OBJECT_PROPERTY>> {
        return this.api.objectproperties.getObjectProperty(id).pipe(
            tap(res => {
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
            map(res => this.mapToBusinessObject(res.objectProperty)),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.OBJECT_PROPERTY>,
        params?: EntityUpdateRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EntityUpdateResponseModel<EditableEntity.OBJECT_PROPERTY>> {
        return this.api.objectproperties.updateObjectProperty(id, data).pipe(
            tap(res => {
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
            map(res => this.mapToBusinessObject(res.objectProperty)),
        );
    }

    delete(id: string | number, params?: EntityDeleteRequestParams<EditableEntity.OBJECT_PROPERTY>): Observable<void> {
        return this.api.objectPropertycategories.deleteObjectPropertyCategory(id).pipe(
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
        body?: EntityListRequestModel<EditableEntity.OBJECT_PROPERTY>,
        params?: EntityListRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EntityListResponseModel<EditableEntity.OBJECT_PROPERTY>> {
        return this.api.objectproperties.getObjectProperties(params).pipe(
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
        body?: EntityListRequestModel<EditableEntity.OBJECT_PROPERTY>,
        params?: EntityListRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.OBJECT_PROPERTY]>> {
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
        return this.api.devTools.addObjectPropertyToPackage(devtoolPackage, entityId).pipe(
            tap(() => {
                this.notification.show({
                    message: 'objectProperty.objectProperty_successfully_added_to_package',
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
        return this.api.devTools.removeObjectPropertyFromPackage(devtoolPackage, entityId).pipe(
            tap(() => {
                this.notification.show({
                    message: 'objectProperty.objectProperty_successfully_removed_from_package',
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
        return this.api.devTools.getObjectproperties(devtoolPackage, params).pipe(
            tap(res => {
                res.items.forEach(objCat => {
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
            map(res => ({
                items: res.items.map((item, index) => this.mapToBusinessObject(item, index)),
                totalItems: res.numItems,
            })),
        );
    }

    getLinkedNodes(objectPropertyId: EntityIdType): Observable<Node<Raw>[]> {
        return this.api.objectproperties.getObjectPropertyLinkedNodes(objectPropertyId).pipe(
            map(res => res.items),
        );
    }

    changeNodeRestrictions(
        objectPropertyId: EntityIdType,
        nodeIdsToRestrict: EntityIdType[] = [],
        nodesCurrentlyRestricted: EntityIdType[] = [],
    ): Observable<void> {
        const reqs = [];
        const execute = (): Observable<void> => {
            if (reqs.length === 0) {
                // do nothing
                return of();
            }
            return forkJoin(reqs).pipe(
                // no need for explicit return value
                discard(),
                // display toast notification
                tap(() => this.notification.show({
                    type: 'success',
                    message: 'objectProperty.operation_linked_to_nodes_successful',
                })),
                this.catchAndRethrowError(),
            );
        };

        if (nodeIdsToRestrict.length === 0) {
            // if none selected
            reqs.push(this.api.objectproperties.unlinkObjectPropertiesFromNodes([objectPropertyId], nodesCurrentlyRestricted));
            return execute();
        }
        if (nodesCurrentlyRestricted.length > 0) {
            // don't remove nodes supposed to remain linked
            const filteredNodes = nodesCurrentlyRestricted.filter(i => nodeIdsToRestrict.some(j => i !== j));
            reqs.push(this.api.objectproperties.unlinkObjectPropertiesFromNodes([objectPropertyId], filteredNodes));
        }
        if (nodeIdsToRestrict.length > 0) {
            reqs.push(this.api.objectproperties.linkObjectPropertiesToNodes([objectPropertyId], nodeIdsToRestrict));
        }

        return execute();
    }
}

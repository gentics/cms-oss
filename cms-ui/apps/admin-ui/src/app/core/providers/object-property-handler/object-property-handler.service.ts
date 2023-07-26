import {
    DevtoolEntityListHandler,
    DevtoolEntityListRequestModel,
    DevtoolEntityListRequestParams,
    DevtoolEntityListResponseModel,
    EditableEntity,
    EntityCreateRequestModel,
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
import { EntityIdType, ModelType, Node, ObjectProperty, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable, of, forkJoin } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';
import { I18nNotificationService } from '../i18n-notification';

@Injectable()
export class ObjectPropertyHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<ObjectProperty<Raw>, EditableEntity.OBJECT_PROPERTY>,
        EntityListHandler<ObjectProperty<Raw>, EditableEntity.OBJECT_PROPERTY>,
        DevtoolEntityListHandler<ObjectProperty<Raw>, EditableEntity.OBJECT_PROPERTY> {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: ObjectProperty<ModelType.Raw>): string {
        return entity.name;
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.OBJECT_PROPERTY>,
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
    ): Observable<ObjectProperty<Raw>> {
        return this.create(data).pipe(
            map(res => res.objectProperty),
        );
    }

    get(id: string | number): Observable<EntityLoadResponseModel<EditableEntity.OBJECT_PROPERTY>> {
        return this.api.objectproperties.getObjectProperty(id).pipe(
            tap(res => {
                const name = this.displayName(res.objectProperty);
                this.nameMap[res.objectProperty.id] = name;
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(id: string | number): Observable<ObjectProperty<Raw>> {
        return this.get(id).pipe(
            map(res => res.objectProperty),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.OBJECT_PROPERTY>,
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
    ): Observable<ObjectProperty<Raw>> {
        return this.update(id, data).pipe(
            map(res => res.objectProperty),
        );
    }

    delete(id: string | number): Observable<void> {
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
        );
    }

    listMapped(
        body?: EntityListRequestModel<EditableEntity.OBJECT_PROPERTY>,
        params?: EntityListRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EntityList<ObjectProperty<ModelType.Raw>>> {
        return this.list(body, params).pipe(
            map(res => ({
                items: res.items,
                totalItems: res.numItems,
            })),
        );
    }

    listFromDevtool(
        devtoolPackage: string,
        body?: DevtoolEntityListRequestModel<EditableEntity.OBJECT_PROPERTY>,
        params?: DevtoolEntityListRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<DevtoolEntityListResponseModel<EditableEntity.OBJECT_PROPERTY>> {
        return this.api.devTools.getObjectproperties(devtoolPackage, params).pipe(
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
        body?: DevtoolEntityListRequestModel<EditableEntity.OBJECT_PROPERTY>,
        params?: DevtoolEntityListRequestParams<EditableEntity.OBJECT_PROPERTY>,
    ): Observable<EntityList<ObjectProperty<ModelType.Raw>>> {
        return this.listFromDevtool(devtoolPackage, body, params).pipe(
            map(res => ({
                items: res.items,
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

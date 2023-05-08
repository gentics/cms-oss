import { PackageEntityOperations } from '@admin-ui/common';
import { discard } from '@admin-ui/common/utils/rxjs-discard-operator/discard.opertator';
import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import {
    EntityIdType,
    ModelType,
    Node,
    ObjectProperty,
    ObjectPropertyBO,
    ObjectPropertyCreateRequest,
    ObjectPropertyListOptions,
    ObjectPropertyUpdateRequest,
    Raw,
    Response,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { forkJoin, Observable, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class ObjectPropertyOperations
    extends ExtendedEntityOperationsBase<'objectProperty'>
    implements PackageEntityOperations<ObjectPropertyBO<Raw>>
{

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entityManager: EntityManagerService,
        private notification: I18nNotificationService,
        private appState: AppStateService,
    ) {
        super(injector, 'objectProperty');
    }

    /**
     * Get a list of all objectPropertys and adds them to the AppState.
     */
    getAll(options?: ObjectPropertyListOptions): Observable<ObjectPropertyBO<Raw>[]> {
        return this.api.objectproperties.getObjectProperties(options).pipe(
            map(res => res.items.map(item => this.mapToBusinessObject(item))),
            tap(items => this.entityManager.addEntities('objectProperty', items)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single objectProperty and add it to the AppState.
     */
    get(objectPropertyId: string): Observable<ObjectPropertyBO<Raw>> {
        return this.api.objectproperties.getObjectProperty(objectPropertyId).pipe(
            map(res => this.mapToBusinessObject(res.objectProperty)),
            tap(item => this.entityManager.addEntity('objectProperty', item)),
            this.catchAndRethrowError(),
        );
    }

    getAllFromPackage(packageId: string, options?: any): Observable<ObjectPropertyBO<Raw>[]> {
        return this.api.devTools.getObjectproperties(packageId, options).pipe(
            map(res => res.items.map(item => this.mapToBusinessObject(item))),
            this.catchAndRethrowError(),
        );
    }

    getFromPackage(packageId: string, entityId: string): Observable<ObjectPropertyBO<Raw>> {
        return this.api.devTools.getObjectProperty(packageId, entityId).pipe(
            map(res => this.mapToBusinessObject(res.objectProperty)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Create a objectProperty.
     */
    create(objectProperty: ObjectPropertyCreateRequest, notify: boolean = true): Observable<ObjectPropertyBO<Raw>> {
        return this.api.objectproperties.createObjectProperty(objectProperty).pipe(
            map(response => this.mapToBusinessObject(response.objectProperty)),
            tap(property => {
                this.entityManager.addEntity(this.entityIdentifier, property);

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_created',
                        translationParams: { name: objectProperty.keyword },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change a single objectProperty
     */
    update(objectPropertyId: string, payload: ObjectPropertyUpdateRequest, notify: boolean = true): Observable<ObjectPropertyBO<Raw>> {
        return this.api.objectproperties.updateObjectProperty(objectPropertyId, payload).pipe(
            map(res => this.mapToBusinessObject(res.objectProperty)),
            tap(objectProperty => {
                // update state with server response
                this.entityManager.addEntity(this.entityIdentifier, objectProperty);

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_updated',
                        translationParams: { name: objectProperty.keyword },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Delete a single objectProperty
     */
    delete(objectPropertyId: string, notify: boolean = true): Observable<Response | void> {
        const propertyToBeDeleted = notify ? this.appState.now.entity[this.entityIdentifier][objectPropertyId] : null;

        return this.api.objectproperties.deleteObjectProperty(objectPropertyId).pipe(
            tap(() => {
                this.entityManager.deleteEntities(this.entityIdentifier, [objectPropertyId]);

                if (notify && propertyToBeDeleted) {
                    return this.notification.show({
                        type: 'success',
                        message: 'shared.item_singular_deleted',
                        translationParams: { name: propertyToBeDeleted.keyword },
                    });
                }
            }),
            this.catchAndRethrowError(),
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
            const _nodesCurrentlyRestrictedFiltered = nodesCurrentlyRestricted.filter(i => nodeIdsToRestrict.some(j => i !== j));
            reqs.push(this.api.objectproperties.unlinkObjectPropertiesFromNodes([objectPropertyId], _nodesCurrentlyRestrictedFiltered));
        }
        if (nodeIdsToRestrict.length > 0) {
            reqs.push(this.api.objectproperties.linkObjectPropertiesToNodes([objectPropertyId], nodeIdsToRestrict));
        }

        return execute();
    }

    private mapToBusinessObject<T extends ModelType>(property: ObjectProperty<T>): ObjectPropertyBO<T> {
        return {
            ...property,
            id: property.id + '',
        };
    }
}

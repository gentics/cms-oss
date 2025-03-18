import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EditableEntity, EntityEditorHandler, EntityList, EntityListHandler, NodeBO } from '@admin-ui/common';
import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import { BaseEntityHandlerService } from '@admin-ui/core/providers/base-entity-handler/base-entity-handler';
import { NodeFeaturesMap } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import {
    Language,
    ModelType,
    Node,
    NodeCreateRequest,
    NodeFeature,
    NodeFeatureModel,
    NodeListRequestOptions,
    NodeListResponse,
    NodeResponse,
    NodeSaveRequest,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { forkJoin, Observable } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';

@Injectable()
export class NodeHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<EditableEntity.NODE>,
        EntityListHandler<EditableEntity.NODE> {

    constructor(
        errorHandler: ErrorHandler,
        protected client: GCMSRestClientService,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: Node<ModelType.Raw>): string {
        return entity?.name || '';
    }

    mapToBusinessObject(entity: Node<ModelType.Raw>, index?: number, context?: any): NodeBO {
        return {
            ...entity,
            [BO_ID]: `${entity.id}`,
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: this.displayName(entity),
        };
    }

    create(data: NodeCreateRequest, params?: never): Observable<NodeResponse> {
        return this.client.node.create(data).pipe(
            tap(res => {
                const name = this.displayName(res.node);
                this.nameMap[res.node.id] = name;

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

    createMapped(data: NodeCreateRequest, params?: never): Observable<NodeBO> {
        return this.create(data, params).pipe(
            map(res => this.mapToBusinessObject(res.node)),
        );
    }

    get(id: string | number, options?: never): Observable<NodeResponse> {
        return this.client.node.get(id).pipe(
            tap(res => {
                this.nameMap[res.node.id] = this.displayName(res.node);
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(id: string | number, options?: never): Observable<NodeBO> {
        return this.get(id, options).pipe(
            map(res => this.mapToBusinessObject(res.node)),
        );
    }

    update(id: string | number, data: NodeSaveRequest, options?: never): Observable<NodeResponse> {
        return this.client.node.update(id, data).pipe(
            switchMap(() => this.client.node.get(id)),
            tap(res => {
                const name = this.displayName(res.node);
                this.nameMap[res.node.id] = name;

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

    updateMapped(id: string | number, data: NodeSaveRequest, options?: never): Observable<NodeBO> {
        return this.update(id, data, options).pipe(
            map(res => this.mapToBusinessObject(res.node)),
        );
    }

    delete(id: string | number, data?: never, options?: never): Observable<void> {
        return this.client.node.delete(id).pipe(
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

    list(body?: never, params?: NodeListRequestOptions): Observable<NodeListResponse> {
        return this.client.node.list(params).pipe(
            tap(res => {
                res.items.forEach(objCat => {
                    const name = this.displayName(objCat);
                    this.nameMap[objCat.id] = name;
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listMapped(body?: never, params?: NodeListRequestOptions): Observable<EntityList<NodeBO>> {
        return this.list(body, params).pipe(
            map(res => {
                const items = res.items.map((item, index) => this.mapToBusinessObject(item, index));

                return {
                    items,
                    totalItems: res.numItems,
                };
            }),
        );
    }

    listFeatures(): Observable<NodeFeatureModel[]> {
        return this.client.node.listFeatures({ sort: [ { attribute: 'id' } ] }).pipe(
            map(res => res.items),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Loads the activated node features for the specified node and updates the app state with the result.
     */
    getFeatures(nodeId: number): Observable<NodeFeaturesMap> {
        return this.client.node.instanceFeatures(nodeId).pipe(
            map(res => {
                return res.items.reduce((acc, feat) => {
                    acc[feat] = true;
                    return acc;
                }, {});
            }),
            this.catchAndRethrowError(),
        );
    }

    updateFeatures(nodeId: number, update: Partial<Record<NodeFeature, boolean>>): Observable<NodeFeaturesMap> {
        const updateQueries = [];

        Object.keys(update).forEach((feature: NodeFeature) => {
            if (update[feature]) {
                updateQueries.push(this.client.node.activateFeature(nodeId, feature));
            } else {
                updateQueries.push(this.client.node.deactivateFeature(nodeId, feature));
            }
        });

        return forkJoin(updateQueries).pipe(
            switchMap(() => this.getFeatures(nodeId)),
            tap(() => {
                this.notification.show({
                    type: 'success',
                    message: 'node.features_updated',
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    getLanguages(nodeId: number): Observable<Language[]> {
        return this.client.node.listLanguages(nodeId).pipe(
            map(response => response.items),
            this.catchAndRethrowError(),
        );
    }

    updateLanguages(nodeId: number, update: Language[]): Observable<Language[]> {
        return this.client.node.orderLanguages(nodeId, update).pipe(
            switchMap(() => this.getLanguages(nodeId)),
            tap(() => {
                this.notification.show({
                    type: 'success',
                    message: 'node.languages_updated',
                });
            }),
            this.catchAndRethrowError(),
        );
    }
}

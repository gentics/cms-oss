import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import {
    AccessControlledType,
    EntityIdType,
    GcmsPermission,
    Index,
    ItemDeleteResponse,
    Language,
    Node,
    NodeCopyRequest,
    NodeCopyRequestOptions,
    NodeCreateRequest,
    NodeDeleteRequestOptions,
    NodeFeature,
    NodeFeatureListRequestOptions,
    NodeFeatureModel,
    NodeFeatures,
    NodeListRequestOptions,
    NodeSaveRequest,
    PagedTemplateListResponse,
    Raw,
    Response,
    TemplateLinkResponse,
    TemplateListRequest,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { ActivityManagerService, GtxActivityManagerActivityConfig } from '../../activity-manager';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification/i18n-notification.service';
import { PermissionsService } from '../../permissions';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class NodeOperations extends ExtendedEntityOperationsBase<'node'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entityManager: EntityManagerService,
        private notification: I18nNotificationService,
        private activityManager: ActivityManagerService,
        private permissions: PermissionsService,
        private appState: AppStateService,
    ) {
        super(injector, 'node');
    }

    /**
     * Loads all nodes from the CMS and adds them to the EntityState.
     */
    getAll(options?: NodeListRequestOptions): Observable<Node<Raw>[]> {
        return this.api.node.getNodes(options).pipe(
            tap(res => {
                if (!res.perms) {
                    return;
                }

                // Store the permissions from the response into the permissions service
                Object.entries(res.perms).forEach(([nodeId, permissions]) => {
                    this.permissions.storePermissions(AccessControlledType.NODE, nodeId, permissions as any as GcmsPermission[]);
                });
            }),
            map(res => res.items),
            tap(nodes => this.entityManager.addEntities('node', nodes)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Loads a single node from the CMS and adds it to the EntityState.
     */
    get(nodeId: number): Observable<Node<Raw>> {
        return this.api.node.getNode(nodeId).pipe(
            map(res => res.node),
            tap(node => this.entityManager.addEntity(this.entityIdentifier, node)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Creates a new `Node` and adds it to the `EntityState`.
     */
    addNode(newNode: NodeCreateRequest): Observable<Node<Raw>> {
        return this.api.node.addNode(newNode).pipe(
            map(res => res.node),
            tap(node => this.entityManager.addEntity(this.entityIdentifier, node)),
            tap((node: Node<Raw>) => {
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_created',
                    translationParams: { name: node.name },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Deletes an existing `Node` and removes it from the `EntityState`.
     */
    removeNode(nodeId: string | number, options?: NodeDeleteRequestOptions): Promise<string> {
        const nodeTobeDeleted = this.appState.now.entity.node[nodeId];

        const promise = this.api.node.removeNode(nodeId, options).pipe(
            tap(() => {
                this.entityManager.deleteEntities(this.entityIdentifier, [nodeId]);

                if (nodeTobeDeleted) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_singular_deleted',
                        translationParams: { name: nodeTobeDeleted.name },
                    });
                }
            }),
        );

        return new Promise((resolve, reject) => {
            // configure and add activity to activity manager
            const multiple = false;
            const activityLabelInProgress = multiple ? 'shared.deleting_nodes_plural' : 'shared.deleting_node_singular';
            const activityConfig: GtxActivityManagerActivityConfig = {
                labelOnSuccess: {
                    label: multiple ? 'shared.items_plural_deleted' : 'shared.item_singular_deleted',
                    params: { name: multiple ? 'common.node_plural' : 'common.node_singular' },
                    translateParams: true,
                },
                callBackOnSuccess: (successMessage: string) => resolve(successMessage),
                callBackOnFailed: (errorMessage: string) => reject(errorMessage),
            };
            this.activityManager.activityAdd(
                activityLabelInProgress,
                promise,
                true,
                true,
                activityConfig,
            );
        });
    }

    /**
     * Copy given `Node` and add it to the `EntityState`.
     */
    copyNode(nodeId: number, payload?: NodeCopyRequest, options?: NodeCopyRequestOptions): Promise<string> {
        const promise =  this.api.node.copyNode(nodeId, payload, options).pipe(
            tap((res: Response) => {
                this.entityManager.getEntity(this.entityIdentifier, nodeId)
                    .toPromise()
                    .then(copiedNode => {
                        // try to read message from backend response as this contains most precise information
                        const message = Array.isArray(res.messages) && res.messages.length > 0 && res.messages[0].message || 'shared.item_singular_copied';
                        this.notification.show({
                            type: 'success',
                            message: message,
                            translationParams: { name: copiedNode.name },
                        });
                    });
            }),
        );

        return new Promise((resolve, reject) => {
            // configure and add activity to activity manager
            const multiple = payload.copies > 1;
            const activityLabelInProgress = multiple ? 'shared.copying_nodes_plural' : 'shared.copying_node_singular';
            const activityConfig: GtxActivityManagerActivityConfig = {
                labelOnSuccess: {
                    label: multiple ? 'shared.items_plural_copied' : 'shared.item_singular_copied',
                    params: { name: multiple ? 'common.node_plural' : 'common.node_singular' },
                    translateParams: true,
                },
                callBackOnSuccess: (successMessage: string) => resolve(successMessage),
                callBackOnFailed: (errorMessage: string) => reject(errorMessage),
            };
            this.activityManager.activityAdd(
                activityLabelInProgress,
                promise,
                true,
                true,
                activityConfig,
            );
        });
    }

    /**
     * Updates the `Node` with the specified `id`
     */
    update(id: number, update: NodeSaveRequest): Observable<Node<Raw>> {
        return this.api.node.updateNode(id, update).pipe(
            switchMap(() => this.get(id)),
            tap((node: Node<Raw>) => {
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_updated',
                    translationParams: { name: node.name },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    updateNodeFeatures(nodeId: number, update: Partial<Index<NodeFeature, boolean>>): Observable<(keyof NodeFeatures)[]> {
        const updateQueries = [];

        Object.keys(update).forEach((feature: NodeFeature) => {
            if (update[feature]) {
                updateQueries.push(this.api.node.activateNodeFeature(nodeId, feature));
            } else {
                updateQueries.push(this.api.node.deactivateNodeFeature(nodeId, feature));
            }
        });

        return forkJoin(updateQueries).pipe(
            switchMap(() => this.getNodeFeatures(nodeId)),
            tap((nodeFeatures: (keyof NodeFeatures)[]) => {
                this.notification.show({
                    type: 'success',
                    message: 'node.features_updated',
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    getAvailableFeatures(options?: NodeFeatureListRequestOptions): Observable<NodeFeatureModel[]> {
        return this.api.node.getNodeFeatureList(options).pipe(
            map(response => response.items),
            this.catchAndRethrowError(),
        );
    }

    getNodeFeatures(nodeId: number): Observable<(keyof NodeFeatures)[]> {
        return this.api.node.getNodeFeatures(nodeId).pipe(
            map(response => response.items),
            this.catchAndRethrowError(),
        );
    }

    getNodeLanguages(nodeId: number): Observable<Language[]> {
        return this.api.node.getNodeLanguageList(nodeId).pipe(
            map(response => response.items),
            this.catchAndRethrowError(),
        );
    }

    updateNodeLanguages(nodeId: number, update: Language[]): Observable<Language[]> {
        return this.api.node.updateNodeLanguages(nodeId, update).pipe(
            switchMap(() => this.getNodeLanguages(nodeId)),
            tap((nodeLanguages: Language[]) => {
                this.notification.show({
                    type: 'success',
                    message: 'node.languages_updated',
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    getTemplates(nodeId: number, options?: TemplateListRequest): Observable<PagedTemplateListResponse> {
        return this.api.node.getNodeTemplates(nodeId, options);
    }

    hasTemplateLinked(nodeId: number, templateId: EntityIdType): Observable<boolean> {
        return this.api.node.hasNodeTemplate(nodeId, templateId).pipe(
            map(() => true),
            catchError(() => of(false)),
        );
    }

    linkTemplate(nodeId: number | string, templateId: EntityIdType): Observable<TemplateLinkResponse> {
        return this.api.node.addNodeTemplate(nodeId, templateId);
    }

    unlinkTemplate(nodeId: number | string, templateId: EntityIdType): Observable<ItemDeleteResponse> {
        return this.api.node.removeNodeTemplate(nodeId, templateId);
    }
}

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
    applyPermissions,
    discard,
} from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { ContentRepositoryFragment, ContentRepositoryFragmentListOptions, EntityIdType, Node, Raw, TagmapEntryError } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';

@Injectable()
export class ContentRepositoryHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<EditableEntity.CONTENT_REPOSITORY>,
        EntityListHandler<EditableEntity.CONTENT_REPOSITORY>,
        DevToolEntityHandler<EditableEntity.CONTENT_REPOSITORY> {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: EditableEntityModels[EditableEntity.CONTENT_REPOSITORY]): string {
        return entity.name;
    }

    public mapToBusinessObject(
        cr: EditableEntityModels[EditableEntity.CONTENT_REPOSITORY],
        index?: number,
    ): EditableEntityBusinessObjects[EditableEntity.CONTENT_REPOSITORY] {
        return {
            ...cr,
            [BO_ID]: String(cr.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: cr.name,
        };
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.CONTENT_REPOSITORY>,
        params?: EntityCreateRequestParams<EditableEntity.CONTENT_REPOSITORY>,
    ): Observable<EntityCreateResponseModel<EditableEntity.CONTENT_REPOSITORY>> {
        return this.api.contentrepositories.createContentRepository(data).pipe(
            tap((res) => {
                const name = this.displayName(res.contentRepository);
                this.nameMap[res.contentRepository.id] = name;

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
        data: EntityCreateRequestModel<EditableEntity.CONTENT_REPOSITORY>,
        options?: EntityCreateRequestParams<EditableEntity.CONTENT_REPOSITORY>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.CONTENT_REPOSITORY]> {
        return this.create(data, options).pipe(
            map((res) => this.mapToBusinessObject(res.contentRepository)),
        );
    }

    get(
        id: string | number,
        params?: EntityLoadRequestParams<EditableEntity.CONTENT_REPOSITORY>,
    ): Observable<EntityLoadResponseModel<EditableEntity.CONTENT_REPOSITORY>> {
        return this.api.contentrepositories.getContentRepository(id).pipe(
            tap((res) => {
                const name = this.displayName(res.contentRepository);
                this.nameMap[res.contentRepository.id] = name;
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(
        id: string | number,
        params?: EntityLoadRequestParams<EditableEntity.CONTENT_REPOSITORY>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.CONTENT_REPOSITORY]> {
        return this.get(id, params).pipe(
            map((res) => this.mapToBusinessObject(res.contentRepository)),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.CONTENT_REPOSITORY>,
        params?: EntityUpdateRequestParams<EditableEntity.CONTENT_REPOSITORY>,
    ): Observable<EntityUpdateResponseModel<EditableEntity.CONTENT_REPOSITORY>> {
        return this.api.contentrepositories.updateContentRepository(id, data).pipe(
            tap((res) => {
                const name = this.displayName(res.contentRepository);
                this.nameMap[res.contentRepository.id] = name;

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
        data: EntityUpdateRequestModel<EditableEntity.CONTENT_REPOSITORY>,
        params?: EntityUpdateRequestParams<EditableEntity.CONTENT_REPOSITORY>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.CONTENT_REPOSITORY]> {
        return this.update(id, data, params).pipe(
            map((res) => this.mapToBusinessObject(res.contentRepository)),
        );
    }

    delete(id: string | number, parms?: EntityDeleteRequestParams<EditableEntity.CONTENT_REPOSITORY>): Observable<void> {
        return this.api.contentrepositories.deleteContentRepository(id).pipe(
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
        body?: EntityListRequestModel<EditableEntity.CONTENT_REPOSITORY>,
        params?: EntityListRequestParams<EditableEntity.CONTENT_REPOSITORY>,
    ): Observable<EntityListResponseModel<EditableEntity.CONTENT_REPOSITORY>> {
        return this.api.contentrepositories.getContentrepositories(params).pipe(
            tap((res) => {
                res.items.forEach((cr) => {
                    const name = this.displayName(cr);
                    this.nameMap[cr.id] = name;
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listMapped(
        body?: EntityListRequestModel<EditableEntity.CONTENT_REPOSITORY>,
        params?: EntityListRequestParams<EditableEntity.CONTENT_REPOSITORY>,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.CONTENT_REPOSITORY]>> {
        return this.list(body, params).pipe(
            map((res) => {
                const items = res.items.map((item, index) => this.mapToBusinessObject(item, index));
                applyPermissions(items, res);

                return {
                    items,
                    totalItems: res.numItems,
                };
            }),
        );
    }

    addToDevTool(
        devtoolPackage: string,
        entityId: string | number,
    ): Observable<void> {
        return this.api.devTools.addContentRepositoryToPackage(devtoolPackage, entityId).pipe(
            tap(() => {
                this.notification.show({
                    message: 'content_repository.added_to_package',
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
        return this.api.devTools.removeContentRepositoryFromPackage(devtoolPackage, entityId).pipe(
            tap(() => {
                this.notification.show({
                    message: 'content_repository.added_to_package',
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
        body?: DevToolEntityListRequestModel<EditableEntity.CONTENT_REPOSITORY>,
        params?: DevToolEntityListRequestParams<EditableEntity.CONTENT_REPOSITORY>,
    ): Observable<DevToolEntityListResponseModel<EditableEntity.CONTENT_REPOSITORY>> {
        return this.api.devTools.getContentrepositories(devtoolPackage, params).pipe(
            tap((res) => {
                res.items.forEach((cr) => {
                    const name = this.displayName(cr);
                    this.nameMap[cr.id] = name;
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listFromDevToolMapped(
        devtoolPackage: string,
        body?: DevToolEntityListRequestModel<EditableEntity.CONTENT_REPOSITORY>,
        params?: DevToolEntityListRequestParams<EditableEntity.CONTENT_REPOSITORY>,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.CONTENT_REPOSITORY]>> {
        return this.listFromDevTool(devtoolPackage, body, params).pipe(
            map((res) => {
                const items = res.items.map((item, index) => this.mapToBusinessObject(item, index));
                applyPermissions(items, res);

                return {
                    items,
                    totalItems: res.numItems,
                };
            }),
        );
    }

    getFromDevtoolMapped(packageId: string, entityId: string): Observable<EditableEntityBusinessObjects[EditableEntity.CONTENT_REPOSITORY]> {
        return this.api.devTools.getContentRepository(packageId, entityId).pipe(
            map((res) => this.mapToBusinessObject(res.contentRepository)),
            tap((con) => {
                this.nameMap[con.id] = con[BO_DISPLAY_NAME];
            }),
            this.catchAndRethrowError(),
        );
    }

    // CHECK & REPAIR //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Check the data in the given contentRepository.
     */
    checkData(repositoryId: string | number, notify: boolean = true): Observable<EditableEntityBusinessObjects[EditableEntity.CONTENT_REPOSITORY]> {
        return this.api.contentrepositories.checkContentRepositoryData(repositoryId).pipe(
            map((res) => this.mapToBusinessObject(res.contentRepository)),
            tap((repository) => {
                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'content_repository.result_data_checked',
                        translationParams: { name: repository[BO_DISPLAY_NAME] },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Check and repair the data in the given contentRepository.
     */
    repairData(repositoryId: string | number, notify: boolean = true): Observable<EditableEntityBusinessObjects[EditableEntity.CONTENT_REPOSITORY]> {
        return this.api.contentrepositories.repairContentRepositoryData(repositoryId).pipe(
            map((res) => this.mapToBusinessObject(res.contentRepository)),
            tap((repository) => {
                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'content_repository.result_data_repaired',
                        translationParams: { name: repository[BO_DISPLAY_NAME] },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Check the connectivity and structure of the given contentRepository.
     */
    checkStructure(repositoryId: string | number, notify: boolean = true): Observable<EditableEntityBusinessObjects[EditableEntity.CONTENT_REPOSITORY]> {
        return this.api.contentrepositories.checkContentRepositoryStructure(repositoryId).pipe(
            map((res) => this.mapToBusinessObject(res.contentRepository)),
            tap((repository) => {
                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'content_repository.result_structure_checked',
                        translationParams: { name: repository[BO_DISPLAY_NAME] },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Check the connectivity and structure of the given contentRepository.
     */
    repairStructure(repositoryId: string | number, notify: boolean = true): Observable<EditableEntityBusinessObjects[EditableEntity.CONTENT_REPOSITORY]> {
        return this.api.contentrepositories.repairContentRepositoryStructure(repositoryId).pipe(
            map((res) => this.mapToBusinessObject(res.contentRepository)),
            tap((repository) => {
                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'content_repository.result_structure_repaired',
                        translationParams: { name: repository[BO_DISPLAY_NAME] },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Check consistency of tagmap entries and return inconsistencies.
     */
    checkTagmapEntries(repositoryId: string | number): Observable<TagmapEntryError[]> {
        return this.api.contentrepositories.checkContentRepositoryTagmapEntries(repositoryId).pipe(
            map((res) => res.items),
            this.catchAndRethrowError(),
        );
    }

    // ASSIGNED NODES //////////////////////////////////////////////////////////////////////////////////////////////////

    getAssignedNodes(contentRepositoryId: number | string): Observable<Node[]> {
        const crId = Number(contentRepositoryId);

        return this.api.node.getNodes().pipe(
            map((res) => {
                const nodes = res.items.filter((node) => node.contentRepositoryId === crId);
                return nodes;
            }),
        );
    }

    addContentRepositoryToNode(
        contentRepositoryId: number | string,
        nodeId: number,
    ): Observable<void> {
        const numberId = typeof contentRepositoryId === 'number'
            ? contentRepositoryId
            : parseInt(contentRepositoryId, 10);

        return this.api.node.updateNode(nodeId, { node: { contentRepositoryId: numberId } }).pipe(
            discard(),
        );
    }

    removeContentRepositoryFromNode(
        nodeId: number,
    ): Observable<void> {
        return this.api.node.updateNode(nodeId, { node: { contentRepositoryId: 0 } }).pipe(
            discard(),
        );
    }

    /**
     * Change the node assigned to a contentRepository
     * @param contentRepositoryId to be changed
     * @param nodeIdsSelected to be changed
     */
    changeAssignedNodesOfContentRepository(contentRepositoryId: number | string, nodeIdsSelected?: number[]): Observable<void> {
        return this.api.node.getNodes().pipe(
            map((res) => res.items),
            // assign desired groups and unassign unwanted groups
            switchMap((allNodes) => {
                const crId = Number(contentRepositoryId);
                const nodesCurrentlyLinked = allNodes
                    .filter((node) => node.contentRepositoryId === crId)
                    .map((node) => node.id);

                // calculate minimal amount of requests required
                const nodesShallBeLinked = nodeIdsSelected;
                const nodesShallNotBeLinked = allNodes
                    .map((node) => node.id)
                    .filter((nodeId) => !nodesShallBeLinked.includes(nodeId));
                const nodesCurrentlyNotLinked = allNodes
                    .map((node) => node.id)
                    .filter((nodeId) => !nodesCurrentlyLinked.includes(nodeId));

                const nodesToLink = nodesShallBeLinked.filter((id) => !nodesCurrentlyLinked.includes(id));
                const nodesToUnlink = nodesShallNotBeLinked.filter((id) => !nodesCurrentlyNotLinked.includes(id));

                const assignRequests: Observable<any>[] = nodesToLink.map((nodeId) => {
                    return this.addContentRepositoryToNode(contentRepositoryId, nodeId);
                });
                const unassignRequests: Observable<any>[] = nodesToUnlink.map((nodeId) => {
                    return this.removeContentRepositoryFromNode(nodeId);
                });

                const requestChanges = (requests: Observable<void>[]) => {
                    if (requests.length > 0) {
                        return forkJoin(requests).pipe(
                            catchError(() => {
                                this.notification.show({
                                    type: 'alert',
                                    message: 'shared.assign_contentRepository_to_crnodes_error',
                                });
                                return of(null);
                            }),
                        );
                    } else {
                        // complete Observable
                        return of(null);
                    }
                };

                // request assign changes before unassign changes to avoid that a user has no group
                return requestChanges(assignRequests).pipe(
                    switchMap(() => requestChanges(unassignRequests)),
                    discard(),
                );
            }),
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.assign_contentRepository_to_nodes_success',
            })),
        );
    }

    // CR FRAGMENTS //////////////////////////////////////////////////////////////////////////////////////////////////#

    getAssignedFragments(
        contentRepositoryId: number | string,
        options?: ContentRepositoryFragmentListOptions,
    ): Observable<ContentRepositoryFragment<Raw>[]> {
        return this.api.contentrepositories.getContentRepositoryFragments(contentRepositoryId, options).pipe(
            map((res) => res.items),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Assign a ContentRepository Fragment to the ContentRepository.
     */
    addContentRepositoryToFragment(
        contentRepositoryId: string | number,
        crFragmentId: string | number,
    ): Observable<void> {
        return this.api.contentrepositories.addContentRepositoryToFragment(contentRepositoryId, crFragmentId).pipe(
            this.catchAndRethrowError(),
        );
    }

    /**
     * Unassign the ContentRepository Fragment from the ContentRepository.
     */
    removeContentRepositoryFromFragment(
        contentRepositoryId: string | number,
        crFragmentId: string | number,
    ): Observable<void> {
        return this.api.contentrepositories.removeContentRepositoryFromFragment(contentRepositoryId, crFragmentId).pipe(
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change the groups assigned to a user
     * @param contentRepositoryId of user whose groups shall be changed
     * @param crfragmentIds of groups to be assigned to the user
     * @returns user with updated group assignments
     */
    changeFragmentsOfContentRepository(contentRepositoryId: number | string, crfragmentIds: (number | string)[]): Observable<void> {
        return forkJoin([
            this.api.contentRepositoryFragments.getContentRepositoryFragments().pipe(
                map((res) => res.items.map((fragment) => fragment.id)),
            ),
            this.getAssignedFragments(contentRepositoryId).pipe(
                map((fragments) => fragments.map((fragment) => fragment.id)),
            ),
        ]).pipe(
            // assign desired groups and unassign unwanted groups
            switchMap(([allFragments, fragmentsCurrentlyLinked]) => {
                const fragmentIds = crfragmentIds.map((id) => Number(id));

                // calculate minimal amount of requests required
                const fragmentsShallBeLinked = fragmentIds;
                const fragmentsShallNotBeLinked = allFragments
                    .filter((fragmentId) => !fragmentsShallBeLinked.includes(fragmentId));
                const fragmentsCurrentlyNotLinked = allFragments
                    .filter((fragmentId) => !fragmentsCurrentlyLinked.includes(fragmentId));

                const fragmentsToLink = fragmentsShallBeLinked.filter((id) => !fragmentsCurrentlyLinked.includes(id));
                const fragmentsToUnlink = fragmentsShallNotBeLinked.filter((id) => !fragmentsCurrentlyNotLinked.includes(id));

                const assignRequests: Observable<void>[] = fragmentsToLink.map((fragmentId) => {
                    return this.addContentRepositoryToFragment(contentRepositoryId, fragmentId);
                });
                const unassignRequests: Observable<void>[] = fragmentsToUnlink.map((fragmentId) => {
                    return this.removeContentRepositoryFromFragment(contentRepositoryId, fragmentId);
                });

                const requestChanges = (requests: Observable<void>[]) => {
                    if (requests.length === 0) {
                        return of(undefined);
                    }

                    return forkJoin(requests).pipe(
                        catchError(() => {
                            this.notification.show({
                                type: 'alert',
                                message: 'shared.assign_contentRepository_to_crfragments_error',
                            });
                            return of(null);
                        }),
                    );
                };

                // request assign changes before unassign changes to avoid that a user has no group
                return requestChanges(assignRequests).pipe(
                    switchMap(() => requestChanges(unassignRequests)),
                    discard(),
                );
            }),
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.assign_contentRepository_to_crfragments_success',
            })),
        );
    }

    // MESH INTERACTIONS //////////////////////////////////////////////////////////////////////////////////////////////////#

    public getAvailableMeshRoles(contentRepositoryId: EntityIdType): Observable<string[]> {
        return this.api.contentrepositories.getAvailableContentRepositoryRoles(contentRepositoryId).pipe(
            map((res) => res.roles || []),
            this.catchAndRethrowError(),
        );
    }

    public getAssignedMeshRoles(contentRepositoryId: EntityIdType): Observable<string[]> {
        return this.api.contentrepositories.getAssignedContentRepositoryRoles(contentRepositoryId).pipe(
            map((res) => res.roles || []),
            this.catchAndRethrowError(),
        );
    }

    public assignMeshRoles(contentRepositoryId: EntityIdType, roles: string[]): Observable<string[]> {
        return this.api.contentrepositories.updateAssignedContentRepositoryRoles(contentRepositoryId, roles).pipe(
            map((res) => res.roles || []),
            tap(() => this.notification.show({
                type: 'success',
                message: 'content_repository.assign_mesh_roles_success',
            })),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Normalize the given entity before storing sending it to the REST API
     * @param entity entity to be normalized
     * @returns normalized entity
     */
    public normalizeForREST(
        entity: EditableEntityModels[EditableEntity.CONTENT_REPOSITORY],
    ): EntityUpdateRequestModel<EditableEntity.CONTENT_REPOSITORY> {
        if (typeof entity.elasticsearch === 'string') {
            try {
                entity.elasticsearch = JSON.parse(entity.elasticsearch as any);
            } catch (err) {
                entity.elasticsearch = null;
            }
        }

        // Don't update the password if it's blank/just whitespace!
        if (typeof entity.password === 'string' && entity.password.trim() === '') {
            entity.password = null;
        }

        return entity;
    }
}

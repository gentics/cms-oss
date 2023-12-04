import { discard, LOAD_FOR_PACKAGE_LIST, PackageEntityOperations } from '@admin-ui/common';
import { ContentRepositoryFragmentDataService, NodeDataService } from '@admin-ui/shared/providers';
import { Injectable, Injector } from '@angular/core';
import {
    ContentRepository,
    ContentRepositoryBO,
    ContentRepositoryCreateRequest,
    ContentRepositoryFragment,
    ContentRepositoryFragmentBO,
    ContentRepositoryFragmentListOptions,
    ContentRepositoryListOptions,
    ContentRepositoryUpdateRequest,
    EntityIdType,
    ModelType,
    Node,
    Normalized,
    Raw,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, first, map, switchMap, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification/i18n-notification.service';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';
import { NodeOperations } from '../node';

@Injectable()
export class ContentRepositoryOperations
    extends ExtendedEntityOperationsBase<'contentRepository'>
    implements PackageEntityOperations<ContentRepositoryBO<Raw>>
{

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entityManager: EntityManagerService,
        private notification: I18nNotificationService,
        private crFragments: ContentRepositoryFragmentDataService,
        private nodeDataService: NodeDataService,
        private nodeOperations: NodeOperations,
    ) {
        super(injector, 'contentRepository');
    }

    /**
     * Loads all content repositories from the CMS and adds them to the EntityState.
     */
    getAll(options?: ContentRepositoryListOptions): Observable<ContentRepositoryBO<Raw>[]> {
        return this.api.contentrepositories.getContentrepositories(options).pipe(
            map(res => res.items.map(item => this.mapToBusinessObject(item, !!(options?.[LOAD_FOR_PACKAGE_LIST])))),
            // eslint-disable-next-line @typescript-eslint/no-misused-promises
            tap(items => this.entityManager.addEntities(this.entityIdentifier, items)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Loads a single content repository from the CMS and adds it to the EntityState.
     */
    get(contentRepositoryId: EntityIdType): Observable<ContentRepositoryBO<Raw>> {
        return this.api.contentrepositories.getContentRepository(contentRepositoryId).pipe(
            map(res => this.mapToBusinessObject(res.contentRepository)),
            tap(contentRepository => this.entityManager.addEntity(this.entityIdentifier, contentRepository)),
            this.catchAndRethrowError(),
        );
    }

    getAllFromPackage(packageId: string, options?: any): Observable<ContentRepositoryBO<ModelType.Raw>[]> {
        return this.api.devTools.getContentrepositories(packageId, options).pipe(
            map(res => res.items.map(item => this.mapToBusinessObject(item, true))),
            this.catchAndRethrowError(),
        );
    }

    getFromPackage(packageId: string, entityId: string): Observable<ContentRepositoryBO<ModelType.Raw>> {
        return this.api.devTools.getContentRepository(packageId, entityId).pipe(
            map(res => this.mapToBusinessObject(res.contentRepository, true)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Create a repository.
     */
    create(repository: ContentRepositoryCreateRequest, notify: boolean = true): Observable<ContentRepositoryBO<Raw>> {
        return this.api.contentrepositories.createContentRepository(repository).pipe(
            map(res => this.mapToBusinessObject(res.contentRepository)),
            tap(repository => {
                this.entityManager.addEntity(this.entityIdentifier, repository);

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_created',
                        translationParams: { name: repository.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change a single repository
     */
    update(repositoryId: string, payload: ContentRepositoryUpdateRequest, notify: boolean = true): Observable<ContentRepositoryBO<Raw>> {
        return this.api.contentrepositories.updateContentRepository(repositoryId, payload).pipe(
            map(res => this.mapToBusinessObject(res.contentRepository)),
            tap(repository => {
                // update state with server response
                this.entityManager.addEntity(this.entityIdentifier, repository);

                // display toast notification
                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_updated',
                        translationParams: { name: repository.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Delete a single repository
     */
    delete(repositoryId: string | number): Observable<Response | void> {
        return this.api.contentrepositories.deleteContentRepository(repositoryId).pipe(
            // display toast notification
            tap(() => {
                this.entityManager.getEntity('contentRepository', repositoryId)
                    .toPromise()
                    .then((repositoryDeleted: ContentRepositoryBO<Normalized>) => {
                        return this.notification.show({
                            type: 'success',
                            message: 'shared.item_singular_deleted',
                            translationParams: { name: repositoryDeleted.name },
                        });
                    })
                    // remove entity from state
                    .then(() => this.entityManager.deleteEntities(this.entityIdentifier, [repositoryId]));
            }),
            this.catchAndRethrowError(),
        );
    }

    // ACTIONS


    /**
     * Check the data in the given contentRepository.
     */
    checkData(repositoryId: string | number, notify: boolean = true): Observable<ContentRepositoryBO<Raw>> {
        return this.api.contentrepositories.checkContentRepositoryData(repositoryId).pipe(
            map(res => this.mapToBusinessObject(res.contentRepository)),
            tap(repository => {
                this.entityManager.addEntity(this.entityIdentifier, repository);

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'contentRepository.result_data_checked',
                        translationParams: { name: repository.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Check and repair the data in the given contentRepository.
     */
    repairData(repositoryId: string | number, notify: boolean = true): Observable<ContentRepositoryBO<Raw>> {
        return this.api.contentrepositories.repairContentRepositoryData(repositoryId).pipe(
            map(res => this.mapToBusinessObject(res.contentRepository)),
            tap(repository => {
                this.entityManager.addEntity(this.entityIdentifier, repository);

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'contentRepository.result_data_repaired',
                        translationParams: { name: repository.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Check the connectivity and structure of the given contentRepository.
     */
    checkStructure(repositoryId: string | number, notify: boolean = true): Observable<ContentRepositoryBO<Raw>> {
        return this.api.contentrepositories.checkContentRepositoryStructure(repositoryId).pipe(
            map(res => this.mapToBusinessObject(res.contentRepository)),
            tap(repository => {
                this.entityManager.addEntity(this.entityIdentifier, repository);

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'contentRepository.result_structure_checked',
                        translationParams: { name: repository.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Check the connectivity and structure of the given contentRepository.
     */
    repairStructure(repositoryId: string | number, notify: boolean = true): Observable<ContentRepositoryBO<Raw>> {
        return this.api.contentrepositories.repairContentRepositoryStructure(repositoryId).pipe(
            map(res => this.mapToBusinessObject(res.contentRepository)),
            tap(repository => {
                this.entityManager.addEntity(this.entityIdentifier, repository);

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'contentRepository.result_structure_repaired',
                        translationParams: { name: repository.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Check consistency of tagmap entries and return inconsistencies.
     */
    checkTagmapEntries(repositoryId: string): Observable<void> {
        return this.api.contentrepositories.checkContentRepositoryTagmapEntries(repositoryId).pipe(
            discard(() => {
                this.entityManager.getEntity('contentRepository', repositoryId)
                    .toPromise()
                    .then((repositoryDeleted: ContentRepositoryBO<Normalized>) => {
                        return this.notification.show({
                            type: 'success',
                            message: 'contentRepository.result_entries_checked',
                            translationParams: { name: repositoryDeleted.name },
                        });
                    })
                    // remove entity from state
                    .then(() => this.entityManager.deleteEntities(this.entityIdentifier, [repositoryId]));
            }),
            this.catchAndRethrowError(),
        );
    }

    // ASSIGNED NODES //////////////////////////////////////////////////////////////////////////////////////////////////

    getAssignedNodes(contentRepositoryId: string): Observable<Node<Normalized>[]> {
        return this.nodeDataService.getNormalizedEntitiesFromState().pipe(
            map((nodes: Node<Normalized>[]) => nodes.filter(n => n.contentRepositoryId.toString() === contentRepositoryId)),
        );
    }

    addContentRepositoryToNode(
        contentRepositoryId: EntityIdType,
        nodeId: number,
    ): Observable<Node<Raw>> {
        const numberId = typeof contentRepositoryId === 'number'
            ? contentRepositoryId
            : parseInt(contentRepositoryId, 10);

        return this.nodeOperations.update(nodeId, { node: { contentRepositoryId: numberId } }).pipe(
            tap(updatedNode => this.entityManager.addEntity('node', updatedNode)),
        );
    }

    removeContentRepositoryFromNode(
        contentRepositoryId: EntityIdType,
        nodeId: number,
    ): Observable<Node<Raw>> {
        const numberId = typeof contentRepositoryId === 'number'
            ? contentRepositoryId
            : parseInt(contentRepositoryId, 10);

        return this.nodeOperations.update(nodeId, { node: { contentRepositoryId: numberId } }).pipe(
            tap(updatedNode => this.entityManager.addEntity('node', updatedNode)),
        );
    }


    /**
     * Change the node assigned to a contentRepository
     * @param contentRepositoryId to be changed
     * @param nodeIdsSelected to be changed
     */
    changeAssignedNodesOfContentRepository(contentRepositoryId: string, nodeIdsSelected?: number[]): Observable<void> {
        return forkJoin([
            this.nodeDataService.getNormalizedEntitiesFromState().pipe(first()),
            this.getAssignedNodes(contentRepositoryId).pipe(first()),
        ]).pipe(
            // assign desired groups and unassign unwanted groups
            switchMap(([allNodes, nodesCurrentlyLinked]) => {
                // calculate minimal amount of requests required
                const nodesShallNotBeLinked = allNodes.filter((node: Node<Normalized>) => !nodeIdsSelected.includes(node.id));
                const nodesCurrentlyNotLinked = allNodes.filter((node: Node<Normalized>) => !nodesCurrentlyLinked.includes(node));

                const nodesToLink = nodeIdsSelected.filter(id => !nodesCurrentlyLinked.map(fcl => fcl.id).includes(id));
                const nodesToUnlink = nodesShallNotBeLinked.filter(id => !nodesCurrentlyNotLinked.includes(id));

                const assignRequests: Observable<any>[] = nodesToLink.map(nodeId => {
                    return this.addContentRepositoryToNode(contentRepositoryId, nodeId);
                });
                const unassignRequests: Observable<any>[] = nodesToUnlink.map((node: Node<Normalized>) => {
                    return this.removeContentRepositoryFromNode(contentRepositoryId, node.id);
                });

                const requestChanges = (requests: Observable<void>[]) => {
                    if (requests.length > 0) {
                        return forkJoin(requests).pipe(
                            catchError(() => of(this.notification.show({
                                type: 'alert',
                                message: 'shared.assign_contentRepository_to_crnodes_error',
                            })),
                            ));
                    } else {
                        // complete Observable
                        return of(undefined);
                    }
                };

                // request assign changes before unassign changes to avoid that a user has no group
                return requestChanges(assignRequests).pipe(
                    switchMap(() => requestChanges(unassignRequests)),
                );
            }),
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.assign_contentRepository_to_nodes_success',
            })),
        );
    }

    // CR FRAGMENTS //////////////////////////////////////////////////////////////////////////////////////////////////#

    /**
     * Loads all cr fragments assigned to a content repository and stores them in Entity State.
     */
    getAssignedFragments(
        contentRepositoryId: string,
        options?: ContentRepositoryFragmentListOptions,
    ): Observable<ContentRepositoryFragmentBO<Raw>[]> {
        return this.api.contentrepositories.getContentRepositoryFragments(contentRepositoryId, options).pipe(
            map(res => res.items.map(item => this.mapFragmentToBusinessObject(item))),
            // eslint-disable-next-line @typescript-eslint/no-misused-promises
            tap(items => this.entityManager.addEntities('contentRepositoryFragment', items)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Assign a ContentRepository Fragment to the ContentRepository.
     */
    addContentRepositoryToFragment(
        contentRepositoryId: string,
        crFragmentId: string,
    ): Observable<void> {
        return this.api.contentrepositories.addContentRepositoryToFragment(contentRepositoryId, crFragmentId).pipe(
            this.catchAndRethrowError(),
        );
    }

    /**
     * Unassign the ContentRepository Fragment from the ContentRepository.
     */
    removeContentRepositoryFromFragment(
        contentRepositoryId: string,
        crFragmentId: string,
    ): Observable<void> {
        return this.api.contentrepositories.removeContentRepositoryFromFragment(contentRepositoryId, crFragmentId).pipe(
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change the groups assigned to a user
     * @param contentRepositoryId of user whose groups shall be changed
     * @param crFragmentIds of groups to be assigned to the user
     * @returns user with updated group assignments
     */
    changeFragmentsOfContentRepository(contentRepositoryId: string, crFragmentIds: string[]): Observable<void> {
        return forkJoin([
            this.crFragments.getEntitiesFromApi(),
            this.getAssignedFragments(contentRepositoryId).pipe(first()),
        ]).pipe(
            // assign desired groups and unassign unwanted groups
            switchMap(([allFragments, fragmentsCurrentlyLinked]) => {
                // calculate minimal amount of requests required
                const fragmentsShallNotBeLinked = allFragments
                    .filter((fragment: ContentRepositoryFragmentBO<Raw>) => !crFragmentIds.includes(fragment.id))
                    .map(e => e.id);
                const fragmentsCurrentlyNotLinked = allFragments
                    .filter((fragment: ContentRepositoryFragmentBO<Raw>) => !fragmentsCurrentlyLinked.map(e => e.id).includes(fragment.id))
                    .map(e => e.id);

                const fragmentsToLink = crFragmentIds.filter(id => !fragmentsCurrentlyLinked.map(fcl => fcl.id).includes(id));
                const fragmentsToUnlink = fragmentsShallNotBeLinked.filter(id => !fragmentsCurrentlyNotLinked.includes(id));

                const assignRequests: Observable<void>[] = fragmentsToLink.map(fragmentId => {
                    return this.addContentRepositoryToFragment(contentRepositoryId, fragmentId);
                });
                const unassignRequests: Observable<void>[] = fragmentsToUnlink.map((fragmentId: string) => {
                    return this.removeContentRepositoryFromFragment(contentRepositoryId, fragmentId);
                });

                const requestChanges = (requests: Observable<void>[]) => {
                    if (requests.length === 0) {
                        return of(undefined);
                    }

                    return forkJoin(requests).pipe(
                        catchError(() => of(this.notification.show({
                            type: 'alert',
                            message: 'shared.assign_contentRepository_to_crfragments_error',
                        }))),
                    );
                };

                // request assign changes before unassign changes to avoid that a user has no group
                return requestChanges(assignRequests).pipe(
                    switchMap(() => requestChanges(unassignRequests)),
                );
            }),
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.assign_contentRepository_to_crfragments_success',
            })),
        );
    }

    // MESH ROLES //////////////////////////////////////////////////////////////////////////////////////////////////#

    public getAvailableMeshRoles(contentRepositoryId: EntityIdType): Observable<string[]> {
        return this.api.contentrepositories.getAvailableContentRepositoryRoles(contentRepositoryId).pipe(
            map(res => res.roles || []),
            this.catchAndRethrowError(),
        );
    }

    public getAssignedMeshRoles(contentRepositoryId: EntityIdType): Observable<string[]> {
        return this.api.contentrepositories.getAssignedContentRepositoryRoles(contentRepositoryId).pipe(
            map(res => res.roles || []),
            this.catchAndRethrowError(),
        );
    }

    public assignMeshRoles(contentRepositoryId: EntityIdType, roles: string[]): Observable<string[]> {
        return this.api.contentrepositories.updateAssignedContentRepositoryRoles(contentRepositoryId, roles).pipe(
            map(res => res.roles || []),
            tap(() => this.notification.show({
                type: 'success',
                message: 'contentRepository.assign_mesh_roles_success',
            })),
            this.catchAndRethrowError(),
        );
    }

    // MAPPERS //////////////////////////////////////////////////////////////////////////////////////////////////#

    public mapToBusinessObject<T extends ModelType>(cr: ContentRepository<T>, forPackage: boolean = false): ContentRepositoryBO<T> {
        return {
            ...cr,
            id: forPackage ? cr.globalId?.toString?.() : cr.id?.toString?.(),
        };
    }

    public mapFragmentToBusinessObject(fragment: ContentRepositoryFragment<Raw>, forPackage: boolean = false): ContentRepositoryFragmentBO<Raw> {
        return {
            ...fragment,
            id: forPackage ? fragment.globalId?.toString?.() : fragment.id?.toString?.(),
        };
    }
}

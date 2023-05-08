import { applyInstancePermissions, discard, PackageEntityOperations } from '@admin-ui/common';
import { Injectable, Injector } from '@angular/core';
import {
    ConstructCreateRequest,
    ConstructUpdateRequest,
    EntityIdType,
    InstancePermissionMap,
    ModelType,
    Node,
    PagedConstructListRequestOptions,
    Raw,
    SingleInstancePermissionType,
    TagType,
    TagTypeBO,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { combineLatest, Observable } from 'rxjs';
import { map, switchMap, take, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class ConstructOperations
    extends ExtendedEntityOperationsBase<'construct'>
    implements PackageEntityOperations<TagTypeBO<Raw>>
{

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entityManager: EntityManagerService,
        private notification: I18nNotificationService,
    ) {
        super(injector, 'construct');
    }

    /**
     * Get a list of all constructs and adds them to the AppState.
     */
    getAll(options?: PagedConstructListRequestOptions): Observable<TagTypeBO<Raw>[]> {
        return this.api.tagType.getTagTypes(options).pipe(
            map(res => applyInstancePermissions(res)),
            map(res => res.items.map(item => this.mapToBusinessObject(item))),
            tap(items => this.entityManager.addEntities(this.entityIdentifier, items)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single construct and add it to the AppState.
     */
    get(constructId: string): Observable<TagTypeBO<Raw>> {
        return combineLatest([
            this.api.tagType.getTagType(constructId),
            this.api.tagType.getTagPermission(constructId, SingleInstancePermissionType.EDIT).pipe(
                map(res => res.granted),
            ),
            this.api.tagType.getTagPermission(constructId, SingleInstancePermissionType.DELETE).pipe(
                map(res => res.granted),
            ),
        ]).pipe(
            map(([res, canEdit, canDelete]) => this.mapToBusinessObject(res.construct, {
                [SingleInstancePermissionType.VIEW]: true,
                [SingleInstancePermissionType.EDIT]: canEdit,
                [SingleInstancePermissionType.DELETE]: canDelete,
            })),
            tap(item => this.entityManager.addEntity(this.entityIdentifier, item)),
            this.catchAndRethrowError(),
        );
    }

    getAllFromPackage(packageId: string, options?: any): Observable<TagTypeBO<Raw>[]> {
        return this.api.devTools.getConstructs(packageId, options).pipe(
            map(res => res.items.map(item => this.mapToBusinessObject(item))),
            this.catchAndRethrowError(),
        );
    }

    getFromPackage(packageId: string, entityId: string): Observable<TagTypeBO<Raw>> {
        return this.api.devTools.getConstruct(packageId, entityId).pipe(
            map(res => this.mapToBusinessObject(res.construct)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Create a construct.
     */
    create(construct: ConstructCreateRequest, nodeIds: number[]): Observable<TagTypeBO<Raw>> {
        return this.api.tagType.createTagType(construct, { nodeId: nodeIds }).pipe(
            map(res => this.mapToBusinessObject(res.construct, {
                [SingleInstancePermissionType.VIEW]: true,
                [SingleInstancePermissionType.EDIT]: true,
                [SingleInstancePermissionType.DELETE]: true,
            })),
            tap(construct => this.entityManager.addEntity(this.entityIdentifier, construct)),
            tap(construct => {
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_created',
                    translationParams: { name: construct.keyword },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change a single construct
     */
    update(constructId: string, payload: ConstructUpdateRequest): Observable<TagTypeBO<Raw>> {
        return this.api.tagType.updateTagType(constructId, payload).pipe(
            map(res => this.mapToBusinessObject(res.construct, {
                [SingleInstancePermissionType.VIEW]: true,
                [SingleInstancePermissionType.EDIT]: true,
                [SingleInstancePermissionType.DELETE]: true,
            })),
            // update state with server response
            tap(construct => this.entityManager.addEntity(this.entityIdentifier, construct)),
            // display toast notification
            tap(construct => {
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_updated',
                    translationParams: { name: construct.keyword },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Delete a single construct
     */
    delete(constructId: string | number): Observable<void> {
        return this.api.tagType.deleteTagType(constructId).pipe(
            // Load the item from the entity-manager, so we can display it in the notification
            switchMap(() => this.entityManager.getEntity(this.entityIdentifier, constructId).pipe(
                // Only load it once
                take(1),
                // display toast notification
                tap((constructDeleted: TagTypeBO) => {
                    if (!constructDeleted) {
                        return;
                    }

                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_singular_deleted',
                        translationParams: {
                            name: constructDeleted.keyword,
                        },
                    });
                }),
            )),
            // Remove the entity from the store/manager
            tap(() => this.entityManager.deleteEntities(this.entityIdentifier, [constructId])),
            discard(),
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
            targetIds: [constructId + ''],
            ids: [nodeId],
        }).pipe(discard());
    }

    unlinkFromNode(constructId: EntityIdType, nodeId: number): Observable<void> {
        return this.api.tagType.unlinkTagFromNode({
            targetIds: [constructId + ''],
            ids: [nodeId],
        }).pipe(discard());
    }

    mapToBusinessObject<T extends ModelType>(construct: TagType<T>, permissions?: InstancePermissionMap): TagTypeBO<T> {
        const bo: TagTypeBO = {
            ...construct,
            id: construct.id + '',
        };

        if (permissions) {
            bo.permissions = permissions;
        }

        return bo;
    }
}

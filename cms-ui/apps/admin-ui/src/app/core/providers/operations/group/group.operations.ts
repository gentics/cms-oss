import { removeEntries, removeEntryIfPresent } from '@admin-ui/common/utils/list-utils/list-utils';
import { AppStateService, UpdateEntities } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import {
    AccessControlledType,
    EntityIdType,
    Group,
    GroupCreateRequest,
    GroupPermissionsListOptions,
    GroupResponse,
    GroupSetPermissionsRequest,
    GroupUpdateRequest,
    GroupUserCreateRequest,
    GroupUserCreateResponse,
    GroupUsersListOptions,
    Normalized,
    PermissionInfo,
    PermissionsSet,
    Raw,
    RecursivePartial,
    User,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { combineLatest, Observable, of as observableOf } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

/**
 * Operations on Groups.
 *
 * All operations that modify something first make the change using the REST API
 * and then update the AppState accordingly.
 */
@Injectable()
export class GroupOperations extends ExtendedEntityOperationsBase<'group'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entities: EntityManagerService,
        private appState: AppStateService,
        private notification: I18nNotificationService,
    ) {
        super(injector, 'group');
    }

    /**
     * Gets the nested list of all groups that are visible to the current user.
     */
    getAll(): Observable<Group<Raw>[]> {
        return this.api.group.getGroupsTree().pipe(
            map(response => response.groups),
            tap(groups => this.entities.addEntities(this.entityIdentifier, groups)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Gets the flat list of all groups that are visible to the current user.
     */
    getFlattned(): Observable<Group<Raw>[]> {
        return this.api.group.listGroups().pipe(
            map(response => response.items),
            tap(groups => this.entities.addEntities(this.entityIdentifier, groups)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Gets the list of all permissions of groups.
     */
    getGroupPermissions(): Observable<{[key: number]: string[]}> {
        return this.api.group.listGroups().pipe(
            map(response => response.perms),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Gets the nested list of all groups that are visible to the current user.
     */
    getSubgroups(parentId: number): Observable<Group<Raw>[]> {
        return this.api.group.getSubgroups(parentId).pipe(
            map(response => response.items),
            tap(groups => this.entities.addEntities(this.entityIdentifier, groups)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single group and add it to the AppState.
     */
    get(userId: number): Observable<Group<Raw>> {
        return this.api.group.getGroup(userId).pipe(
            map((res: GroupResponse) => res.group),
            // update state with server response
            tap((group: Group<Raw>) => this.entities.addEntity(this.entityIdentifier, group)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Creates a new subgroup under the group specified by `parentId`.
     *
     * Groups can only be created as subgroups. There is always one root group in the CMS instance.
     */
    createSubgroup(parentId: number, subgroup: GroupCreateRequest): Observable<Group<Raw>> {
        return this.api.group.createSubgroup(parentId, subgroup).pipe(
            map(response => response.group),
            switchMap(group => {
                this.entities.addEntity(this.entityIdentifier, group);
                return this.addSubgroupToParent(parentId, group).pipe(
                    map(() => group),
                );
            }),
            tap(group => {
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_created',
                    translationParams: { name: group.name },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Moves the subgroup with the specified `id` to the parent group indicated through `parentTargetId`
     * and then refreshes all groups to make sure that the former and the new parent groups are also updated in the AppState.
     *
     * If this operation is successful, the new parent group of `id` will be `parentTargetId`.
     * @param id The ID of the `Group` to be moved.
     * @param parentTargetId The ID of the `Group` that should be the new parent group.
     */
    moveSubgroup(id: number, parentTargetId: number): Observable<Group<Raw>> {
        return this.api.group.moveSubgroup(id, parentTargetId).pipe(
            map(response => response.group),
            switchMap(movedGroup =>
                this.getAll().pipe(
                    map(() => movedGroup),
                ),
            ),
            tap((group) => this.notification.show({
                type: 'success',
                message: 'shared.item_moved',
                translationParams: { name: group.name },
            })),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Deletes the `Group` with the specified `id`.
     */
    delete(id: number): Observable<void> {
        const groupToBeDeleted = this.appState.now.entity.group[id];

        return this.api.group.deleteGroup(id).pipe(
            switchMap(() => this.removeGroupFromAppState(id)),
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.item_singular_deleted',
                translationParams: { name: groupToBeDeleted ? groupToBeDeleted.name : id },
            })),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Updates the `Group` with the specified `id`
     */
    update(id: number, update: GroupUpdateRequest): Observable<Group<Raw>> {
        return this.api.group.updateGroup(id, update).pipe(
            map(response => response.group),
            tap(group => {
                this.entities.addEntity(this.entityIdentifier, group);
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_updated',
                    translationParams: { name: group.name },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Gets the users of the `Group` with the specified `id`.
     */
    getGroupUsers(id: number, options?: GroupUsersListOptions): Observable<User<Raw>[]> {
        return this.api.group.getGroupUsers(id, options).pipe(
            map(response => response.items),
            switchMap(users => combineLatest([
                observableOf(users),
                this.entities.addEntities('user', users),
            ])),
            map(([users]) => users),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Create a new user in group with id {groupId}
     */
    createUser(groupId: number, payload: GroupUserCreateRequest): Observable<User<Raw>> {
        return this.api.group.createUser(groupId, payload).pipe(
            // get user from response
            map((res: GroupUserCreateResponse) => res.user),
            // get group of created user to assemble raw user (presume that group user has been created in is already in app state)
            map((user: User<Raw>) => {
                const userGroupIdInitial = this.entities.denormalizeEntity('group', this.appState.now.entity.group[groupId]);
                return { ...user, groups: [ userGroupIdInitial ] };
            }),
            tap((userWithGroups: User<Raw>) => this.entities.addEntity('user', userWithGroups)),
            // display toast notification
            tap((user: User<Raw>) => this.notification.show({
                type: 'success',
                message: 'shared.item_created',
                translationParams: { name: user.login },
            })),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Add the existing user indicated by `userId` to the group with `groupId`.
     */
    addUserToGroup(groupId: number, userId: number): Observable<User<Raw>> {
        const group = this.appState.now.entity.group[groupId];

        return this.api.group.addUserToGroup(groupId, userId).pipe(
            map(response => response.user),
            tap(user => {
                this.entities.addEntity('user', user);
                this.notification.show({
                    type: 'success',
                    message: 'group.user_added_to_group',
                    translationParams: { user: user.login, group: group ? group.name : groupId },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Removes the user indicated by `userId` from the group with `groupId`.
     */
    removeUserFromGroup(groupId: number, userId: number): Observable<void> {
        const group = this.appState.now.entity.group[groupId];
        const user = this.appState.now.entity.user[userId];

        return this.api.group.removeUserFromGroup(groupId, userId).pipe(
            tap(() => {
                if (user) {
                    const newGroups = removeEntries(user.groups, [groupId]);
                    this.appState.dispatch(new UpdateEntities({
                        user: {
                            [userId]: { groups: newGroups },
                        },
                    }));
                }
                this.notification.show({
                    type: 'success',
                    message: 'group.user_removed_from_group',
                    translationParams: { user: user.login, group: group ? group.name : groupId },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Gets the PermissionSets for the group with the specified `groupId` on
     * all AccessControlledTypes or their instances with the parentType and/or parentId specified in the `options`.
     *
     * If no `options.parentType` is given, only root-level permissions will be returned.
     * Otherwise, the permissions on child types/instances of the given parentType/parentId will be returned.
     */
    getPermissionsSets(groupId: number, options: GroupPermissionsListOptions = {}): Observable<PermissionsSet[]> {
        return this.api.group.getGroupPermissions(groupId, options).pipe(
            map(response => response.items),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Gets the permissions of the group with the specified `groupId` on a particular `type`.
     */
    getGroupTypePermissions(groupId: number, type: AccessControlledType): Observable<PermissionInfo[]> {
        return this.api.group.getGroupTypePermissions(groupId, type).pipe(
            map(response => response.perms),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Gets the permissions of the group with the specified `groupId` on a particular instance of a `type`.
     */
    getGroupInstancePermissions(groupId: number, type: AccessControlledType, instanceId: number): Observable<PermissionInfo[]> {
        return this.api.group.getGroupInstancePermissions(groupId, type, instanceId).pipe(
            map(response => response.perms),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Sets the permissions of the group indicated by `groupId` on the specified `type`.
     */
    setGroupTypePermissions(groupId: number, type: AccessControlledType, request: GroupSetPermissionsRequest): Observable<Group<Normalized>> {
        return this.api.group.setGroupTypePermissions(groupId, type, request).pipe(
            map(() => this.appState.now.entity.group[groupId]),
            tap((group: Group<Normalized>) => {
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_updated',
                    translationParams: { name: group.name },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Sets the permissions of the group indicated by `groupId` on the specified  instance of `type`.
     */
    setGroupInstancePermissions(
        groupId: number,
        type: AccessControlledType,
        instanceId: number | string,
        request: GroupSetPermissionsRequest,
    ): Observable<Group<Normalized>> {
        return this.api.group.setGroupInstancePermissions(groupId, type, instanceId, request).pipe(
            map(() => this.appState.now.entity.group[groupId]),
            tap((group: Group<Normalized>) => {
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_updated',
                    translationParams: { name: group.name },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    private addSubgroupToParent(parentId: number, subGroup: Group<Raw>): Observable<void> {
        const parentGroup = this.appState.now.entity.group[parentId];
        const children = parentGroup.children ? [ ...parentGroup.children ] : [];
        children.push(subGroup.id);
        const update: Record<EntityIdType, RecursivePartial<Group<Normalized>>> = {
            [parentId]: { children },
        };

        return this.appState.dispatch(new UpdateEntities({ group: update }));
    }

    private removeGroupFromAppState(deletedGroupId: number): Observable<void> {
        this.entities.deleteEntities(this.entityIdentifier, [deletedGroupId]);
        const allGroups = this.appState.now.entity.group;
        const allGroupIds = Object.keys(allGroups) as any as number[];
        const updatedParentGroups: Record<EntityIdType, RecursivePartial<Group<Normalized>>> = {};

        allGroupIds.forEach(currGroupId => {
            const currGroup = allGroups[currGroupId];
            const newChildren = removeEntryIfPresent(currGroup.children, deletedGroupId) as number[];
            if (newChildren !== currGroup.children) {
                updatedParentGroups[currGroupId] = {
                    children: newChildren,
                };
            }
        });

        return this.appState.dispatch(new UpdateEntities({ group: updatedParentGroups }));
    }

}

import { detailLoading } from '@admin-ui/common';
import { EntityManagerService, GroupOperations, I18nNotificationService, I18nService, PermissionsService, UserOperations } from '@admin-ui/core';
import { AppStateService, SelectState } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import {
    AccessControlledType,
    GcmsPermission,
    Group,
    NormalizableEntityType,
    Normalized,
    Raw,
    TypePermissions,
    User,
    UserGroupNodeRestrictionsResponse,
    UserListOptions,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { combineLatest, forkJoin, Observable, of, OperatorFunction } from 'rxjs';
import { catchError, filter, first, map, switchMap, tap } from 'rxjs/operators';
import { ConfirmRemoveUserFromGroupModalComponent } from '../../components/confirm-remove-user-from-group-modal/confirm-remove-user-from-group-modal.component';
import { GroupDataService } from '../group-data/group-data.service';
import { UserDataService } from '../user-data/user-data.service';

@Injectable()
export class GroupUserDataService extends UserDataService {

    @SelectState(state => state.ui.focusEntityType)
    focusEntityType$: Observable<NormalizableEntityType>;

    @SelectState(state => state.ui.focusEntityId)
    focusEntityId$: Observable<number>;

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: UserOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        groupData: GroupDataService,
        protected permissionsService: PermissionsService,
        protected modalService: ModalService,
        protected groupOperations: GroupOperations,
    ) {
        super(
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
            groupData,
            permissionsService,
        );
    }

    getParentEntityId(): Observable<number> {
        return combineLatest([
            this.focusEntityType$,
            this.focusEntityId$,
        ]).pipe(
            map(([focusEntityType, focusEntityId]) => focusEntityType === 'group' ? focusEntityId : undefined),
            filter((id: number | undefined) => id != null),
        );
    }

    removeUsersFromGroup(groupId: number, userIds: number[]): Promise<void> {
        const groupName = this.state.now.entity.group[groupId].name;
        const userNames = userIds.map(id => this.state.now.entity.user[id].login);

        // open modal
        return this.modalService.fromComponent(
            ConfirmRemoveUserFromGroupModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            {
                groupName,
                userNames,
            },
        )
            .then(modal => modal.open())
            .then(() => {
                const removeOps = userIds.map(userId => this.groupOperations.removeUserFromGroup(groupId, userId).toPromise());
                return Promise.all(removeOps)
                    .then(() => {});
            });
    }

    getEntitiesFromApi(): Observable<User<Raw>[]> {
        // check if allowed to read groups
        return this.permissionsService.getPermissions(AccessControlledType.GROUP_ADMIN).pipe(
            switchMap((typePermissions: TypePermissions) => {
                // then get users with groups
                return this.getParentEntityId().pipe(
                    filter((parentGroupId: number | undefined) => Number.isInteger(parentGroupId)),
                    switchMap(parentGroupId => this.groupOperations.getGroupUsers(parentGroupId).pipe(
                        this.getLoadingOperator(),
                    )),
                );
            }),
        );
    }

    getRawEntitiesFromState(): Observable<User<Raw>[]> {
        return combineLatest([
            this.getParentEntityId(),
            super.getRawEntitiesFromState(),
        ]).pipe(
            filter(([parentGroupId, users]: [number, User<Raw>[]]) => Number.isInteger(parentGroupId)),
            map(([parentGroupId, users]: [number, User<Raw>[]]) => {
                return users.filter(user => Array.isArray(user.groups) && user.groups.find(group => group.id === parentGroupId));
            }),
        );
    }

    /**
     * Change a group assigned to the users
     *
     * @param groupId of group whose users shall be changed
     * @param userIds of users to be assigned to a group
     * @returns group with updated group assignments
     */
    changeGroupOfUsers(groupId: number, userIds: number[]): Observable<User<Raw>[]> {
        return forkJoin([
            this.entityManager.getEntity('group', groupId).pipe(first()),
            this.getRawEntitiesFromState().pipe(first()),
        ]).pipe(
            // assign desired groups and unassign unwanted groups
            switchMap(([group, allUsers]: [Group<Normalized>, User<Raw>[]]) => {
                // calculate minimal amount of requests required
                const usersShallBeLinked = userIds;
                const usersShallNotBeLinked = allUsers.filter((user: User<Raw>) => !usersShallBeLinked.includes(user.id));
                const usersCurrentlyLinked = allUsers.filter((user: User<Raw>) =>
                    user.groups.find(userGroup => userGroup.id === group.id)).map(user => user.id);
                const usersCurrentlyNotLinked = allUsers.filter((user: User<Raw>) => !usersCurrentlyLinked.includes(user.id));

                const usersToLink = usersShallBeLinked.filter(id => !usersCurrentlyLinked.includes(id));
                const usersToUnlink = usersShallNotBeLinked.filter(id => !usersCurrentlyNotLinked.includes(id));

                const assignRequests: Observable<Group<Raw>>[] = usersToLink.map(userId => this.entityOperations.addToGroup(userId, groupId));
                const unassignRequests: Observable<void>[] = usersToUnlink.map((user: User<Raw>) =>
                    this.entityOperations.removeFromGroup(user.id, groupId));

                const requestChanges = (requests: Observable<void | Group<Raw>>[]) => {
                    if (requests.length > 0) {
                        return forkJoin(requests).pipe(
                            // return assigned users
                            map((responses: Array<Group<Raw> | void>) => responses.filter((response: Group<Raw> | void) => response instanceof Object)),
                            catchError(() => of(this.displayNotificationError('shared.assign_group_to_users_error', group && group.name))),
                        );
                    } else {
                        // complete Observable
                        return of(undefined);
                    }
                };

                // request assign changes before unassign changes to avoid that a user has no group
                return requestChanges(assignRequests).pipe(switchMap(() => requestChanges(unassignRequests)));
            }),
            tap(() => this.displayNotificationSuccess('shared.assign_group_to_users_success')),
        );
    }

    /**
     * Get node restrictions for the assignment of the user to the group
     */
    getUserNodeRestrictions(
        userId: number,
        groupId: number,
    ): Observable<UserGroupNodeRestrictionsResponse> {
        return this.entityOperations.getUserNodeRestrictions(userId, groupId);
    }

    changeUserNodeRestrictions(
        userId: number,
        groupId: number,
        nodeIdsToRestrict: number[],
        nodesCurrentlyRestricted: number[] = [],
    ): Observable<UserGroupNodeRestrictionsResponse> {
        return this.state.select(state => state.entity.node).pipe(
            // get node ids as array
            map(nodesIndexed => Object.values(nodesIndexed).map(node => node.id)),
            // restrict desired nodes and unrestrict unwanted nodes
            switchMap((allNodeIds: number[]) => {
                // calculate minimal amount of requests required
                const nodesShallBeRestricted = nodeIdsToRestrict;
                const nodesShallNotBeRestricted = allNodeIds.filter((id: number) => !nodesShallBeRestricted.includes(id));

                const nodesCurrentlyNotRestricted = allNodeIds.filter((id: number) => !nodesCurrentlyRestricted.includes(id));

                const nodesToRestrict = nodesShallBeRestricted.filter(id => !nodesCurrentlyRestricted.includes(id));
                const nodesToUnrestrict = nodesShallNotBeRestricted.filter(id => !nodesCurrentlyNotRestricted.includes(id));

                const restrictRequests: Observable<UserGroupNodeRestrictionsResponse>[]
                    = nodesToRestrict.map(nodeId => this.entityOperations.addUserNodeRestrictions(userId, groupId, nodeId));
                const unrestrictRequests: Observable<UserGroupNodeRestrictionsResponse>[]
                    = nodesToUnrestrict.map(nodeId => this.entityOperations.removeUserNodeRestrictions(userId, groupId, nodeId));

                // request restrict changes before unrestrict changes
                return forkJoin([ ...restrictRequests, ...unrestrictRequests ]).pipe(
                    catchError(() => of(this.displayNotificationError('shared.restrict_group_to_users_error', userId.toString()))),
                ).pipe(
                    // return final state
                    switchMap(() => this.entityOperations.getUserNodeRestrictions(userId, groupId)),
                );
            }),
            tap(() => this.displayNotificationSuccess('shared.restrict_group_to_users_success')),
        );
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return detailLoading(this.state);
    }
}

import { detailLoading, discard } from '@admin-ui/common';
import {
    EntityManagerService,
    GroupOperations,
    PermissionsService,
    UserOperations,
} from '@admin-ui/core';
import { AppStateService, SelectState } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import {
    AccessControlledType,
    NormalizableEntityType,
    Raw,
    User,
    UserGroupNodeRestrictionsResponse,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { I18nService } from '@gentics/cms-components';
import { Observable, OperatorFunction, combineLatest, forkJoin, of } from 'rxjs';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';
import { GroupDataService } from '../group-data/group-data.service';
import { UserDataService } from '../user-data/user-data.service';

@Injectable()
export class GroupUserDataService extends UserDataService {

    @SelectState((state) => state.ui.focusEntityType)
    focusEntityType$: Observable<NormalizableEntityType>;

    @SelectState((state) => state.ui.focusEntityId)
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

    getEntitiesFromApi(): Observable<User<Raw>[]> {
        // check if allowed to read groups
        return this.permissionsService.getPermissions(AccessControlledType.GROUP_ADMIN).pipe(
            switchMap(() => {
                // then get users with groups
                return this.getParentEntityId().pipe(
                    filter((parentGroupId: number | undefined) => Number.isInteger(parentGroupId)),
                    switchMap((parentGroupId) => this.groupOperations.getGroupUsers(parentGroupId).pipe(
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
            filter(([parentGroupId]: [number, User<Raw>[]]) => Number.isInteger(parentGroupId)),
            map(([parentGroupId, users]: [number, User<Raw>[]]) => {
                return users.filter((user) => Array.isArray(user.groups) && user.groups.find((group) => group.id === parentGroupId));
            }),
        );
    }

    /**
     * Change a group assigned to the users
     * @param groupId of group whose users shall be changed
     * @param userIds of users to be assigned to a group
     * @returns group with updated group assignments
     */
    changeGroupOfUsers(groupId: number, userIds: number[], replace: boolean = false): Observable<void> {
        let worker: Observable<void>;

        if (replace) {
            worker = this.groupOperations.getGroupUsers(groupId).pipe(
                switchMap((assignedUsers) => {
                    const assignedIds = assignedUsers.map((user) => user.id);
                    const toAssignIds = userIds.filter((id) => !assignedIds.includes(id));
                    const toRemoveIds = assignedIds.filter((id) => !userIds.includes(id));

                    const assignWorker = toAssignIds.length === 0 ? of() : forkJoin(toAssignIds.map((id) => this.entityOperations.addToGroup(id, groupId)));
                    const removeWorker = toRemoveIds.length === 0 ? of() : forkJoin(toRemoveIds.map((id) => this.entityOperations.removeFromGroup(id, groupId)));

                    return assignWorker.pipe(switchMap(() => removeWorker));
                }),
                discard(),
            );
        } else {
            worker = forkJoin(userIds.map((id) => this.entityOperations.addToGroup(id, groupId))).pipe(
                discard(),
            );
        }

        return worker.pipe(
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
        const toAssignIds = nodeIdsToRestrict.filter((id) => !nodesCurrentlyRestricted.includes(id));
        const toRemoveIds = nodesCurrentlyRestricted.filter((id) => !nodeIdsToRestrict.includes(id));

        const assignWorker = toAssignIds.length === 0
            ? of()
            : forkJoin(
                toAssignIds.map((id) => this.entityOperations.addUserNodeRestrictions(userId, groupId, id)),
            );
        const removeWorker = toRemoveIds.length === 0
            ? of()
            : forkJoin(
                toRemoveIds.map((id) => this.entityOperations.removeUserNodeRestrictions(userId, groupId, id)),
            );

        return assignWorker.pipe(
            switchMap(() => removeWorker),
            tap(() => this.displayNotificationSuccess('shared.restrict_group_to_users_success')),
            catchError(() => of(this.displayNotificationError('shared.restrict_group_to_users_error', userId.toString()))),
            switchMap(() => this.entityOperations.getUserNodeRestrictions(userId, groupId)),
        );
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return detailLoading(this.state);
    }
}

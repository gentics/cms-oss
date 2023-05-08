import { EntityManagerService, I18nNotificationService, I18nService, PermissionsService, UserOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { AccessControlledType, Group, Normalized, Raw, TypePermissions, User, UserListOptions } from '@gentics/cms-models';
import { Observable, OperatorFunction, forkJoin, of } from 'rxjs';
import { catchError, first, map, switchMap, tap } from 'rxjs/operators';
import { masterLoading } from '../../../common/utils/rxjs-loading-operators/master-loading.operator';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';
import { GroupDataService } from '../group-data/group-data.service';

@Injectable()
export class UserDataService extends ExtendedEntityDataServiceBase<'user', UserOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: UserOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected groupData: GroupDataService,
        protected permissionsService: PermissionsService,
    ) {
        super(
            'user',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: User<Raw>): number {
        return entity.id;
    }

    getEntitiesFromApi(): Observable<User<Raw>[]> {
        // check if allowed to read groups
        return this.permissionsService.getPermissions(AccessControlledType.GROUP_ADMIN).pipe(
            switchMap((typePermissions: TypePermissions) => {
                const entityListRequestOptions: UserListOptions = {
                    // groups: typePermissions.hasPermission(GcmsPermission.READ),
                };
                // then get users with groups
                return this.entityOperations.getAll(entityListRequestOptions).pipe(this.getLoadingOperator());
            }),
        );
    }

    getRawEntitiesFromState(): Observable<User<Raw>[]> {
        return super.getRawEntitiesFromState().pipe(
            map(
                // Some users, which we are not allowed to access, are added to the entity state by the message service.
                // These don't have the login property set.
                users => users.filter(user => !!user.login),
            ),
        );
    }

    /**
     * Change the groups assigned to a user
     * @param userId of user whose groups shall be changed
     * @param groupIds of groups to be assigned to the user
     * @param replace if the provided groupIds should replace the existing groups
     * @returns user with updated group assignments
     */
    changeGroupsOfUser(userId: number, groupIds: number[], replace: boolean = false): Observable<User<Raw>> {
        let worker: Observable<void>;

        if (replace) {
            worker = forkJoin([
                this.entityManager.getEntity('user', userId).pipe(
                    first(),
                ),
                this.groupData.getRawEntitiesFromState().pipe(
                    first(),
                ),
            ]).pipe(
                // assign desired groups and unassign unwanted groups
                switchMap(([user, allGroups]) => this.replaceGroupsOfUser(user, groupIds, allGroups)),
            );
        } else {
            worker = this.entityManager.getEntity('user', userId).pipe(
                first(),
                switchMap(user => this.appendGroupsToUser(user, groupIds)),
            );
        }

        return worker.pipe(
            // request changed user to update user group data in state
            switchMap(() => this.entityOperations.get(userId)),
            tap((user: User<Raw>) => this.displayNotificationSuccess('shared.assign_users_to_groups_success', user && user.login)),
        );
    }

    /**
     * Helper function which replaces the current groups of a user with the newly provided ones.
     * It'll determine which groups are already present, and skip these.
     * Groups which are assigned to the user, but aren't listed in `groupIds` will be removed from the user.
     * Groups which are not yet assigned to the user, will be added.
     *
     * @param user the user of which the groups need to be updated
     * @param groupIds the new group ids which will be set
     * @param allGroups all available groups from the cms
     * @returns an observable which completes once all groups have been assigned/removed properly
     */
    replaceGroupsOfUser(user: User<Normalized>, groupIds: number[], allGroups: Group<Raw>[]): Observable<void> {
        // calculate minimal amount of requests required
        const groupsShallBeLinked = groupIds;
        const groupsShallNotBeLinked = allGroups.filter((group: Group<Raw>) => !groupsShallBeLinked.includes(group.id));
        const groupsCurrentlyLinked = user.groups;
        const groupsCurrentlyNotLinked = allGroups.filter((group: Group<Raw>) => !groupsCurrentlyLinked.includes(group.id));

        const groupsToLink = groupsShallBeLinked.filter(id => !groupsCurrentlyLinked.includes(id));
        const groupsToUnlink = groupsShallNotBeLinked.filter(id => !groupsCurrentlyNotLinked.includes(id));

        const assignRequests: Observable<Group<Raw>>[] = groupsToLink.map(groupId => this.entityOperations.addToGroup(user.id, groupId));
        const unassignRequests: Observable<void>[] = groupsToUnlink.map((group: Group<Raw>) => this.entityOperations.removeFromGroup(user.id, group.id));

        const requestChanges = (requests: Observable<void | Group<Raw>>[]) => {
            if (requests.length === 0) {
                // complete Observable
                return of(undefined);
            }

            return forkJoin(requests).pipe(
                // return assigned groups
                map((responses: Array<Group<Raw> | void>) => responses.filter((response: Group<Raw> | void) => response instanceof Object)),
                catchError(() => of(this.displayNotificationError('shared.assign_users_to_groups_error', user && user.login))),
            );
        };

        // request assign changes before unassign changes to avoid that a user has no group
        return requestChanges(assignRequests).pipe(switchMap(() => requestChanges(unassignRequests)));
    }

    /**
     * Helper function to add a user to groups.
     * Will skip all groups which are already assigned to the user.
     *
     * @param user the user to which the groups should be added to
     * @param groupIds the ids of the groups to add
     * @returns an observable which completes once all groups have been assigned
     */
    appendGroupsToUser(user: User<Normalized>, groupIds: number[]): Observable<void> {
        const toAdd = new Set<number>(groupIds);
        for (const userGroupId of user.groups) {
            toAdd.delete(userGroupId);
        }

        if (toAdd.size === 0) {
            return of(null);
        }

        return forkJoin(Array.from(toAdd)
            .map(groupToAdd => this.entityOperations.addToGroup(user.id, groupToAdd)),
        ).pipe(
            map(() => { }),
        );
    }

    getGroupPermissions(): Observable<{[key: number]: string[]}> {
        return this.groupData.getGroupPermissions();
    }

    /**
     * Change the groups assigned to users
     * @param userIds of users whose groups shall be changed
     * @param groupIds of groups to be assigned to the users
     * @param replace if the provided groupIds should replace the existing groups
     * @returns users with updated group assignments if all operations were successful, otherwise FALSE
     */
    changeGroupsOfUsers(userIds: number[], groupIds: number[], replace: boolean = false): Observable<User<Raw>[] | boolean> {
        return forkJoin(userIds.map(userId => this.changeGroupsOfUser(userId, groupIds, replace))).pipe(
            catchError(() => of(false)),
        );
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return masterLoading(this.state);
    }
}

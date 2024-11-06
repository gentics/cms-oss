import { discard } from '@admin-ui/common';
import { EntityManagerService, I18nNotificationService, I18nService, PermissionsService, UserOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { AccessControlledType, Normalized, Raw, User, UserListOptions } from '@gentics/cms-models';
import { Observable, OperatorFunction, forkJoin, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
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
            switchMap(() => {
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
            worker = this.entityOperations.groups(userId).pipe(
                switchMap(assignedGroups => {
                    const assignedIds = assignedGroups.map(group => group.id);
                    const toAssignIds = Array.from(new Set(groupIds.filter(id => !assignedIds.includes(id))));
                    const toRemoveIds = Array.from(new Set(assignedIds.filter(id => !groupIds.includes(id))));

                    const assignWork = toAssignIds.length === 0 ? of([]) : forkJoin(toAssignIds.map(id => this.entityOperations.addToGroup(userId, id)));
                    const removeWork = toRemoveIds.length === 0 ? of([]) : forkJoin(toRemoveIds.map(id => this.entityOperations.removeFromGroup(userId, id)));

                    /*
                     * Not using a forkJoin here on purpose.
                     * First we want to assign all new groups, before we remove some.
                     * This is to prevent an edge case, where we could potentially try to remove
                     * all groups from a user before assigning new ones.
                     * This would cause an error, because we can't unassign the last group of a user.
                     */
                    return assignWork.pipe(switchMap(() => removeWork));
                }),
                discard(),
            );
        } else {
            worker = forkJoin(groupIds.map(id => this.entityOperations.addToGroup(userId, id))).pipe(
                discard(),
            );
        }

        return worker.pipe(
            // request changed user to update user group data in state
            switchMap(() => this.entityOperations.get(userId)),
            tap((user: User<Raw>) => this.displayNotificationSuccess('shared.assign_users_to_groups_success', user && user.login)),
        );
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
            discard(),
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

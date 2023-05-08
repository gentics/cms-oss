import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import {
    Group,
    GroupResponse,
    Raw,
    Response,
    User,
    UserGroupNodeRestrictionsResponse,
    UserGroupsRequestOptions,
    UserGroupsResponse,
    UserListOptions,
    UserRequestOptions,
    UserResponse,
    UserUpdateRequest,
    UserUpdateResponse,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class UserOperations extends ExtendedEntityOperationsBase<'user'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entities: EntityManagerService,
        private notification: I18nNotificationService,
        private appState: AppStateService,
    ) {
        super(injector, 'user');
    }

    /**
     * Get a list of all users and adds them to the AppState.
     *
     * **Important:** A large list of entities is added to the AppState in batches.
     * Thus the AppState may not yet contain all loaded users when the returned observable emits.
     */
    getAll(options?: UserListOptions): Observable<User<Raw>[]> {
        return this.api.user.getUsers(options).pipe(
            map(res => res.items),
            tap(users => this.entities.addEntities('user', users)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single user and add it to the AppState.
     */
    get(userId: number, options?: UserRequestOptions): Observable<User<Raw>> {
        return this.api.user.getUser(userId, options).pipe(
            map((res: UserResponse) => res.user),
            // update state with server response
            tap((user: User<Raw>) => this.entities.addEntity('user', user)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change a single user
     */
    update(userId: number, payload: UserUpdateRequest): Observable<User<Raw>> {
        return this.api.user.updateUser(userId, payload).pipe(
            map((res: UserUpdateResponse) => res.user),
            // update state with server response
            tap((user: User<Raw>) => this.entities.addEntity('user', user)),
            // display toast notification
            tap((user: User<Raw>) => this.notification.show({
                type: 'success',
                message: 'shared.item_updated',
                translationParams: { name: user.login },
            })),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Delete a single user
     */
    delete(userId: number): Observable<Response | void> {
        return this.api.user.deleteUser(userId).pipe(
            // display toast notification
            tap(() => {
                const user = this.appState.now.entity.user[userId];
                return this.notification.show({
                    type: 'success',
                    message: 'shared.item_singular_deleted',
                    translationParams: { name: user.login },
                });
            }),
            // remove entity from state
            tap(() => this.entities.deleteEntities('user', [userId])),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get groups a user is assigned to
     */
    groups(userId: number, groupId?: number): Observable<Group<Raw>[]> {
        const options: UserGroupsRequestOptions = groupId ? { id: groupId } : {};
        return this.api.user.getUserGroups(userId, options).pipe(
            map((res: UserGroupsResponse) => res.items),
            tap((groups: Group<Raw>[]) => this.entities.addEntities('group', groups)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Add user to group
     */
    addToGroup(userId: number, groupId: number): Observable<Group<Raw>> {
        return this.api.user.addUserToGroup(userId, groupId).pipe(
            map((res: GroupResponse) => res.group),
            // update state with server response
            tap(() => this.get(userId, { groups: true }).pipe(
                tap((user: User<Raw>) => this.entities.addEntity('user', user)),
            )),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Remove user from group
     */
    removeFromGroup(userId: number, groupId: number): Observable<void> {
        return this.api.user.removeUserFromGroup(userId, groupId).pipe(
            // update state with server response
            tap(() => this.get(userId, { groups: true }).pipe(
                tap((user: User<Raw>) => this.entities.addEntity('user', user)),
            )),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get node restrictions for the assignment of the user to the group
     */
    getUserNodeRestrictions(userId: number, groupId: number): Observable<UserGroupNodeRestrictionsResponse> {
        return this.api.user.getUserNodeRestrictions(userId, groupId);
    }

    /**
     * Add node restriction to the assignment of the user to the group
     */
    addUserNodeRestrictions(userId: number, groupId: number, nodeId: number): Observable<UserGroupNodeRestrictionsResponse> {
        return this.api.user.addUserNodeRestrictions(userId, groupId, nodeId);
    }

    /**
     * Remove node restriction from the assignment of the user to the group
     */
    removeUserNodeRestrictions(userId: number, groupId: number, nodeId: number): Observable<UserGroupNodeRestrictionsResponse> {
        return this.api.user.removeUserNodeRestrictions(userId, groupId, nodeId);
    }

    /**
     * Get user groups with permissions
     */
    getUserGroupsWithPermissions(userId: number): Observable<UserGroupsResponse> {
        return this.api.user.getUserGroups(userId, { perms: true });
    }

}

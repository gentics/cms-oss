import {
    GroupResponse,
    User,
    UserGroupNodeRestrictionsResponse,
    UserGroupsRequestOptions,
    UserGroupsResponse,
    UserListOptions,
    UserListResponse,
    UserRequestOptions,
    UserResponse,
    UserUpdateRequest,
    UserUpdateResponse,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyEmbedOptions, stringifyPagingSortOptions } from '../util/sort-options/sort-options';

export type UserSearchFilterMap = { [P in keyof User]?: User[P] | null; };

/**
 * API methods related to the user resource.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/resource_UserResource.html
 *
 * Note: Creating new users is done via the `/group` endpoint.
 */
export class UserApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    /**
     * Get list of users
     */
    getUsers(options?: UserListOptions): Observable<UserListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }
        stringifyEmbedOptions(options);

        return this.apiBase.get('user', options);
    }

    /**
     * Get a single user
     */
    getUser(userId: number, options?: UserRequestOptions): Observable<UserResponse> {
        return this.apiBase.get(`user/${userId}`, options);
    }

    /**
     * Change a single user
     */
    updateUser(userId: number, payload: UserUpdateRequest): Observable<UserUpdateResponse> {
        return this.apiBase.put(`user/${userId}`, payload);
    }

    /**
     * Delete a single user
     */
    deleteUser(userId: number): Observable<void> {
        return this.apiBase.delete(`user/${userId}`);
    }

    /**
     * Get groups of a single user
     */
    getUserGroups(userId: number, options?: UserGroupsRequestOptions): Observable<UserGroupsResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get(`user/${userId}/groups`, options);
    }

    /**
     * Add user to group
     */
    addUserToGroup(userId: number, groupId: number): Observable<GroupResponse> {
        return this.apiBase.put(`user/${userId}/groups/${groupId}`, {});
    }

    /**
     * remove user from group
     */
    removeUserFromGroup(userId: number, groupId: number): Observable<void> {
        return this.apiBase.delete(`user/${userId}/groups/${groupId}`);
    }

    /**
     * Get node restrictions for the assignment of the user to the group
     */
    getUserNodeRestrictions(userId: number, groupId: number): Observable<UserGroupNodeRestrictionsResponse> {
        return this.apiBase.get(`user/${userId}/groups/${groupId}/nodes`);
    }

    /**
     * Add node restriction to the assignment of the user to the group
     */
    addUserNodeRestrictions(userId: number, groupId: number, nodeId: number): Observable<UserGroupNodeRestrictionsResponse> {
        return this.apiBase.put(`user/${userId}/groups/${groupId}/nodes/${nodeId}`, {});
    }

    /**
     * Remove node restriction from the assignment of the user to the group
     */
    removeUserNodeRestrictions(userId: number, groupId: number, nodeId: number): Observable<UserGroupNodeRestrictionsResponse> {
        return this.apiBase.delete(`user/${userId}/groups/${groupId}/nodes/${nodeId}`);
    }

}

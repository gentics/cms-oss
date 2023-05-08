import {
    AccessControlledType,
    BaseListOptionsWithPaging,
    Group,
    GroupCreateRequest,
    GroupListOptions,
    GroupListResponse,
    GroupPermissionsListOptions,
    GroupPermissionsListResponse,
    GroupResponse,
    GroupSetPermissionsRequest,
    GroupTreeOptions,
    GroupTreeResponse,
    GroupTypeOrInstancePermissionsResponse,
    GroupUpdateRequest,
    GroupUserCreateRequest,
    GroupUserCreateResponse,
    GroupUsersListOptions,
    Response,
    UserGroupsResponse,
    UserListResponse,
    UserResponse,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyEmbedOptions, stringifyPagingSortOptions } from '../util/sort-options/sort-options';

export type GroupSearchFilterMap = { [P in keyof Group]?: Group[P] | null; };

/**
 * API methods related to the group resource.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/guides/restapi/resource_GroupResource.html
 *
 */
export class GroupApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    /**
     * Gets the nested list of all groups that are visible to the current user.
     */
    getGroupsTree(options?: GroupTreeOptions): Observable<GroupTreeResponse> {
        return this.apiBase.get('group/load', options);
    }

    /**
     * Gets a flat list (`children` property is not set) of all groups visible to the current user,
     * optionally matching the criteria defined in `options`.
     */
    listGroups(options?: GroupListOptions): Observable<GroupListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }
        stringifyEmbedOptions(options);

        return this.apiBase.get('group', options);
    }

    /**
     * Gets the group with the specified `id` (without its `children`).
     */
    getGroup(id: number): Observable<GroupResponse> {
        return this.apiBase.get(`group/${id}`);
    }

    /**
     * Gets a flat list (`children` property is not set) of the subgroups of the
     * parent group specified by `parentId`.
     */
    getSubgroups(parentId: number, options?: BaseListOptionsWithPaging<Group>): Observable<UserGroupsResponse> {
        if (options?.sort) {
            const copy: any = { ...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get(`group/${parentId}/groups`, options);
    }

    /**
     * Creates a new subgroup under the group specified by `parentId`.
     *
     * Groups can only be created as subgroups. There is always one root group in the CMS instance.
     */
    createSubgroup(parentId: number, subgroup: GroupCreateRequest): Observable<GroupResponse> {
        return this.apiBase.put(`group/${parentId}/groups`, subgroup);
    }

    /**
     * Moves the subgroup with the specified `id` to the parent group indicated through `parentTargetId`.
     *
     * If this operation is successful, the new parent group of `id` will be `parentTargetId`.
     *
     * @param id The ID of the `Group` to be moved.
     * @param parentTargetId The ID of the `Group` that should be the new parent group.
     */
    moveSubgroup(id: number, parentTargetId: number): Observable<GroupResponse> {
        return this.apiBase.put(`group/${parentTargetId}/groups/${id}`, {});
    }

    /**
     * Deletes the `Group` with the specified `id`.
     */
    deleteGroup(id: number): Observable<void> {
        return this.apiBase.delete(`group/${id}`);
    }

    /**
     * Updates the `Group` with the specified `id`.
     */
    updateGroup(id: number, update: GroupUpdateRequest): Observable<GroupResponse> {
        return this.apiBase.post(`group/${id}`, update);
    }

    /**
     * Gets the users of the `Group` with the specified `id`.
     */
    getGroupUsers(id: number, options?: GroupUsersListOptions): Observable<UserListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get(`group/${id}/users`, options);
    }

    /**
     * Create a new user in the group with the specified `groupId`.
     */
    createUser(groupId: number, payload: GroupUserCreateRequest): Observable<GroupUserCreateResponse> {
        return this.apiBase.put(`group/${groupId}/users`, payload);
    }

    /**
     * Add the existing user indicated by `userId` to the group with `groupId`.
     */
    addUserToGroup(groupId: number, userId: number): Observable<UserResponse> {
        return this.apiBase.put(`group/${groupId}/users/${userId}`, {});
    }

    /**
     * Removes the user indicated by `userId` from the group with `groupId`.
     */
    removeUserFromGroup(groupId: number, userId: number): Observable<void> {
        return this.apiBase.delete(`group/${groupId}/users/${userId}`);
    }

    /**
     * Gets the permissions set of the group with the specified `groupId`.
     *
     * If no `options.parentType` is given, only root-level permissions will be returned.
     * Otherwise, the permissions on child types/instances of the given parentType/parentId will be returned.
     */
    getGroupPermissions(groupId: number, options?: GroupPermissionsListOptions): Observable<GroupPermissionsListResponse> {
        return this.apiBase.get(`group/${groupId}/perms`, options);
    }

    /**
     * Gets the permissions of the group with the specified `groupId` on a particular `type`.
     */
    getGroupTypePermissions(groupId: number, type: AccessControlledType): Observable<GroupTypeOrInstancePermissionsResponse> {
        return this.apiBase.get(`group/${groupId}/perms/${type}`);
    }

    /**
     * Gets the permissions of the group with the specified `groupId` on a particular instance of a `type`.
     */
    getGroupInstancePermissions(groupId: number, type: AccessControlledType, instanceId: number | string): Observable<GroupTypeOrInstancePermissionsResponse> {
        return this.apiBase.get(`group/${groupId}/perms/${type}/${instanceId}`);
    }

    /**
     * Sets the permissions of the group indicated by `groupId` on the specified `type`.
     */
    setGroupTypePermissions(groupId: number, type: AccessControlledType, payload: GroupSetPermissionsRequest): Observable<Response> {
        return this.apiBase.post(`group/${groupId}/perms/${type}`, payload);
    }

    /**
     * Sets the permissions of the group indicated by `groupId` on the specified  instance of `type`.
     */
    setGroupInstancePermissions(
        groupId: number,
        type: AccessControlledType,
        instanceId: number | string,
        payload: GroupSetPermissionsRequest): Observable<Response> {
        return this.apiBase.post(`group/${groupId}/perms/${type}/${instanceId}`, payload);
    }

}

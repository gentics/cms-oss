/* eslint-disable @typescript-eslint/naming-convention */
import { BasicListOptions, PagingMetaInfo, PermissionInfo } from './common';
import { RoleReference } from './roles';
import { UserReference } from './users';

export interface GroupCreateRequest {
    /** Name of the group. */
    name: string;
}

export interface GroupListOptions extends BasicListOptions { }

export interface GroupListResponse {
    /** Paging information of the list result. */
    _metainfo: PagingMetaInfo;
    /** Array which contains the found elements. */
    data: GroupResponse[];
}

export interface GroupReference {
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface GroupResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: UserReference;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: UserReference;
    /** Name of the group */
    name: string;
    permissions: PermissionInfo;
    rolePerms: PermissionInfo;
    /** List of role references */
    roles: RoleReference[];
    /** Uuid of the element */
    uuid: string;
}

export interface GroupUpdateRequest {
    /** New name of the group */
    name: string;
}

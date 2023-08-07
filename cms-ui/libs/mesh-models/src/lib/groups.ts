/* eslint-disable @typescript-eslint/naming-convention */
import { BasicListOptions, Entity, PagingMetaInfo, PermissionInfo } from './common';
import { RoleReference } from './roles';

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

export interface Group {
    /** Name of the group */
    name: string;
}

export interface GroupResponse extends Entity, Group {
    permissions: PermissionInfo;
    rolePerms: PermissionInfo;
    /** List of role references */
    roles: RoleReference[];
}

export interface GroupUpdateRequest {
    /** New name of the group */
    name: string;
}

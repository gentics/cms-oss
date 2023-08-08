/* eslint-disable @typescript-eslint/naming-convention */
import { BasicListOptions, Entity, PagingMetaInfo, PermissionInfo, PermissionListOptions } from './common';
import { RoleReference } from './roles';

export interface GroupCreateRequest {
    /** Name of the group. */
    name: string;
}

export interface GroupListOptions extends BasicListOptions, PermissionListOptions { }

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

export interface EditableGroupProperties {
    /** Name of the group */
    name: string;
}

export interface Group extends EditableGroupProperties, Entity {}

export interface GroupResponse extends Group {
    permissions: PermissionInfo;
    rolePerms: PermissionInfo;
    /** List of role references */
    roles: RoleReference[];
}

export interface GroupUpdateRequest extends EditableGroupProperties { }

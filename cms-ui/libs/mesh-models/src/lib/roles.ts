/* eslint-disable @typescript-eslint/naming-convention */
import { BasicListOptions, Entity, PagingMetaInfo, PermissionInfo } from './common';
import { GroupReference } from './groups';

export interface RoleCreateRequest {
    /** New name of the role */
    name: string;
}

export interface RoleListOptions extends BasicListOptions { }

export interface RoleListResponse {
    /** Paging information of the list result. */
    _metainfo: PagingMetaInfo;
    /** Array which contains the found elements. */
    data: RoleResponse[];
}

export interface RolePermissionRequest {
    permissions: PermissionInfo;
    /** Flag which indicates whether the permission update should be applied recursively. */
    recursive?: boolean;
}

export interface RolePermissionResponse {
    /** Flag which indicates whether the create permission is granted. */
    create: boolean;
    /** Flag which indicates whether the delete permission is granted. */
    delete: boolean;
    /** Flag which indicates whether the publish permission is granted. */
    publish?: boolean;
    /** Flag which indicates whether the read permission is granted. */
    read: boolean;
    /** Flag which indicates whether the read published permission is granted. */
    readPublished?: boolean;
    /** Flag which indicates whether the update permission is granted. */
    update: boolean;
}

export interface RoleReference {
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface EditableRoleProperties {
    /** Name of the role. */
    name: string;
}

export interface Role extends EditableRoleProperties, Entity { }

export interface RoleResponse extends Role {
    /** List of groups which are assigned to the role. */
    groups: GroupReference[];
    permissions: PermissionInfo;
    rolePerms: PermissionInfo;
}

export interface RoleUpdateRequest extends Role { }

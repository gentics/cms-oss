import { BusinessObject } from '@admin-ui/common';
import { GroupResponse, Permission, RoleResponse, User, UserResponse } from '@gentics/mesh-models';

export enum MeshType {
    PROJECT = 'project',
    TAG = 'tag',
    TAG_FAMILY = 'tag-family',
    NODE = 'node',
    SCHEMA = 'schema',
    MICROSCHEMA = 'microschema',
    USER = 'user',
    GROUP = 'group',
    ROLE = 'role',
}

export const MBO_TYPE = Symbol('mesh-type');
export const MBO_PERMISSION_PATH = Symbol('mesh-permission-path');
export const MBO_ROLE_PERMISSIONS = Symbol('mesh-role-permissions');
export const MBO_AVILABLE_PERMISSIONS = Symbol('mesh-available-perms');

export interface MeshBusinessObject extends BusinessObject {
    [MBO_PERMISSION_PATH]?: string;
    [MBO_ROLE_PERMISSIONS]?: Permission[];
    [MBO_AVILABLE_PERMISSIONS]?: Permission[];
    [MBO_TYPE]?: MeshType;
}

export type MeshRoleBO = RoleResponse & MeshBusinessObject;
export type MeshGroupBO = GroupResponse & MeshBusinessObject & {
    users?: User[];
};
export type MeshUserBO = UserResponse & MeshBusinessObject;

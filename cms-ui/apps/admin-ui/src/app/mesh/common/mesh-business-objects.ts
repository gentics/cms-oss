import { BusinessObject } from '@admin-ui/common';
import {
    GroupResponse,
    MicroschemaResponse,
    NodeResponse,
    Permission,
    ProjectResponse,
    RoleResponse,
    SchemaResponse,
    TagFamilyResponse,
    TagResponse,
    User,
    UserResponse,
} from '@gentics/mesh-models';

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
export const MBO_PROJECT_CONTEXT = Symbol('mesh-project');

export const EDITABLE_ENTITY_PERMISSIONS: Permission[] = [Permission.READ, Permission.UPDATE, Permission.DELETE];
export const BASIC_ENTITY_PERMISSIONS: Permission[] = [Permission.CREATE, ...EDITABLE_ENTITY_PERMISSIONS];
export const NODE_PERMISSIONS: Permission[] = [...BASIC_ENTITY_PERMISSIONS, Permission.READ_PUBLISHED, Permission.PUBLISH];

export interface MeshBusinessObject extends BusinessObject {
    [MBO_PERMISSION_PATH]?: string;
    [MBO_ROLE_PERMISSIONS]?: Permission[];
    [MBO_AVILABLE_PERMISSIONS]?: Permission[];
    [MBO_TYPE]?: MeshType;
    [MBO_PROJECT_CONTEXT]?: string;
}

export type MeshRoleBO = RoleResponse & MeshBusinessObject;
export type MeshGroupBO = GroupResponse & MeshBusinessObject & {
    users?: User[];
};
export type MeshUserBO = UserResponse & MeshBusinessObject;
export type MeshSchemaBO = SchemaResponse & MeshBusinessObject;
export type MeshMicroschemaBO = MicroschemaResponse & MeshBusinessObject;
export type MeshProjectBO = ProjectResponse & MeshBusinessObject;
export type MeshTagBO = TagResponse & MeshBusinessObject;
export type MeshTagFamilyBO = TagFamilyResponse & MeshBusinessObject & {
    tags?: MeshTagBO[];
};
export type MeshNodeBO = NodeResponse & MeshBusinessObject;

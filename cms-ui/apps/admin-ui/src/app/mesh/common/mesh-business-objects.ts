import { BusinessObject } from '@admin-ui/common';
import { GroupResponse, RoleResponse } from '@gentics/mesh-models';

export type MeshRoleBO = RoleResponse & BusinessObject;
export type MeshGroupBO = GroupResponse & BusinessObject;

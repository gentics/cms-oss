import { BusinessObject } from '@admin-ui/common';
import { GroupResponse, RoleResponse, User, UserResponse } from '@gentics/mesh-models';

export type MeshRoleBO = RoleResponse & BusinessObject;
export type MeshGroupBO = GroupResponse & BusinessObject & {
    users?: User[];
};
export type MeshUserBO = UserResponse & BusinessObject;

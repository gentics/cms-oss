import { BusinessObject } from '@admin-ui/common';
import { GroupResponse, RoleResponse, User } from '@gentics/mesh-models';

export type MeshRoleBO = RoleResponse & BusinessObject;
export type MeshGroupBO = GroupResponse & BusinessObject & {
    users?: User[];
};

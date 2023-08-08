import { Permission, PermissionInfo } from '@gentics/mesh-models';

export function toPermissionArray(info?: PermissionInfo): Permission[] {
    return Object.entries((info || {}))
        .filter(([, value]) => value)
        .map(([key]) => key as Permission);
}

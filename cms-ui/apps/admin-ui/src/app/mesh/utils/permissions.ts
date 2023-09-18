import { Permission, PermissionInfo } from '@gentics/mesh-models';

export function toPermissionArray(info?: PermissionInfo): Permission[] {
    return Object.entries((info || {}))
        .filter(([, value]) => value)
        .map(([key]) => key as Permission);
}

export function toPermissionInfo(perms: Permission[]): PermissionInfo {
    return Object.entries(Permission).reduce((acc, [key, value]) => {
        acc[key] = perms.includes(value);
        return acc;
    }, {} as PermissionInfo);
}

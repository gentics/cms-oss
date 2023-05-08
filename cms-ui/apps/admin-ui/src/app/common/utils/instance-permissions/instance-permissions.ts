import { InstancePermissionItem, SingleInstancePermissionType } from '@gentics/cms-models';
import { PermissionsCheckResult } from '../../models';

export function hasInstancePermission(item: InstancePermissionItem, perm: SingleInstancePermissionType): boolean {
    return (item?.permissions ?? [])[perm];
}

export function btnInstancePermissionCheck(perm: SingleInstancePermissionType): (item: InstancePermissionItem) => PermissionsCheckResult {
    return (item) => ({
        actionPerms: null,
        granted: hasInstancePermission(item, perm),
    });
}

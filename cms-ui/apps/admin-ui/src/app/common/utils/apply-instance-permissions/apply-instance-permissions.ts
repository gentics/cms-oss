import { InstancePermissionItem, InstancePermissionMap, PermissionListResponse, SingleInstancePermissionType } from '@gentics/cms-models'

export function applyInstancePermissions<T extends InstancePermissionItem>(list: PermissionListResponse<T>): PermissionListResponse<T> {
    list.items = list.items.map(element => {
        if (list.perms && list.perms[element.id]) {
            const permObj: InstancePermissionMap = {} as any;

            for (const key of Object.values(SingleInstancePermissionType)) {
                permObj[key] = (list.perms[element.id] || []).includes(key);
            }

            element.permissions = permObj;
        }
        return element;
    });
    return list;
}

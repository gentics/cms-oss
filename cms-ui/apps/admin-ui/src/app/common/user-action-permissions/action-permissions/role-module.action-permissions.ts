import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes
export const ROLE_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'createRole': {
        typePermissions: [
            {
                type: AccessControlledType.ROLE,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.create_role_permission_required',
    },

    'updateRole': {
        typePermissions: [
            {
                type: AccessControlledType.ROLE,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.update_role_permission_required',
    },

    'deleteRole': {
        typePermissions: [
            {
                type: AccessControlledType.ROLE,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.delete_role_permission_required',
    },
};

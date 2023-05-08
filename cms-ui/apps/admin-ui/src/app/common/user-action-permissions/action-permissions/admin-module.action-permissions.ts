import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes

export const ADMIN_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'readAdmin': {
        typePermissions: [
            {
                type: AccessControlledType.ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        // this would never be displayed
        disabledTooltip: null,
    },

    'setPermAdmin': {
        typePermissions: [
            {
                type: AccessControlledType.ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.CREATE_GROUP,
                ],
            },
        ],
        disabledTooltip: 'common.setperm_permission_required',
    },

};

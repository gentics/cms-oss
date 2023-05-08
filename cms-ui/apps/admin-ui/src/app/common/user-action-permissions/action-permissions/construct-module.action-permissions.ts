import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes
export const CONSTRUCT_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'createConstruct': {
        typePermissions: [
            {
                type: AccessControlledType.CONSTRUCT_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.create_construct_permission_required',
    },

    'updateConstructInstance': {
        typePermissions: [
            {
                type: AccessControlledType.CONSTRUCT_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.update_construct_permission_required',
    },

    'deleteConstruct': {
        // permissions of this entity only work with explicit instances
        typePermissions: [],
        disabledTooltip: 'common.delete_construct_permission_required',
    },

    'deleteConstructInstance': {
        typePermissions: [
            {
                type: AccessControlledType.CONSTRUCT_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.delete_construct_permission_required',
    },
};

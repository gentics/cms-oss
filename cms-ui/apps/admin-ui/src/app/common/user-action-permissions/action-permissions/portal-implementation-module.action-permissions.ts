import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes
export const PORTAL_IMPLEMENTATION_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'createPortalImplementation': {
        typePermissions: [
            {
                type: AccessControlledType.PORTAL_IMPLEMENTATION_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.CREATE,
                ],
            },
        ],
        disabledTooltip: 'common.create_portalImplementation_permission_required',
    },

    'updatePortalImplementationInstance': {
        typePermissions: [
            {
                type: AccessControlledType.PORTAL_IMPLEMENTATION_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        instancePermissions: {
            type: AccessControlledType.PORTAL_IMPLEMENTATION,
            permissions: [
                GcmsPermission.UPDATE,
            ],
        },
        disabledTooltip: 'common.update_portalImplementation_permission_required',
    },

    'deletePortalImplementation': {
        // permissions of this entity only work with explicit instances
        typePermissions: [
            {
                type: AccessControlledType.PORTAL_IMPLEMENTATION_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.delete_portalImplementation_permission_required',
    },

    'deletePortalImplementationInstance': {
        typePermissions: [
            {
                type: AccessControlledType.PORTAL_IMPLEMENTATION_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        instancePermissions: {
            type: AccessControlledType.PORTAL_IMPLEMENTATION,
            permissions: [
                GcmsPermission.DELETE,
            ],
        },
        disabledTooltip: 'common.delete_portalImplementation_permission_required',
    },

    'assignPortalImplementationToNodes': {
        // the permission to change the assignment is checked on the nodes by the backend
        typePermissions: [
            {
                type: AccessControlledType.PORTAL_IMPLEMENTATION_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.assign_portalImplementation_permission_required',
    },
};

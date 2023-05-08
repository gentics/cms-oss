import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes

export const USER_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'createUser': {
        typePermissions: [
            {
                type: AccessControlledType.USER_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.CREATE_USER,
                ],
            },
        ],
        disabledTooltip: 'common.create_user_permission_required',
    },

    'setUserPassword': {
        typePermissions: [
            {
                type: AccessControlledType.USER_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.UPDATE_USER,
                ],
            },
        ],
        disabledTooltip: 'common.update_user_permission_required',
    },

    'updateUser': {
        typePermissions: [
            {
                type: AccessControlledType.USER_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.UPDATE_USER,
                ],
            },
        ],
        disabledTooltip: 'common.update_user_permission_required',
    },

    'updateUserInstance': {
        // Change this to instance permissions and the correct AccessControlledType after https://jira.gentics.com/browse/GTXPE-676
        // has been implemented.
        typePermissions: [
            {
                type: AccessControlledType.USER_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.UPDATE_USER,
                ],
            },
        ],
        disabledTooltip: 'common.update_user_permission_required',
    },

    'deleteUser': {
        typePermissions: [
            {
                type: AccessControlledType.USER_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.DELETE_USER,
                ],
            },
        ],
        disabledTooltip: 'common.delete_user_permission_required',
    },

    'assignUserToGroup': {
        typePermissions: [
            {
                type: AccessControlledType.GROUP_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.USER_ASSIGNMENT,
                ],
            },
        ],
        disabledTooltip: 'common.assign_user_to_group',
    },

    'removeUserFromGroup': {
        typePermissions: [
            {
                type: AccessControlledType.GROUP_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.USER_ASSIGNMENT,
                ],
            },
        ],
        disabledTooltip: 'common.remove_user_from_group_permission_required',
    },

    'assignUserToRole': {
        typePermissions: [
            {
                type: AccessControlledType.ROLE,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.ASSIGN_ROLES,
                ],
            },
        ],
        disabledTooltip: 'common.assign_user_to_role',
    },

};

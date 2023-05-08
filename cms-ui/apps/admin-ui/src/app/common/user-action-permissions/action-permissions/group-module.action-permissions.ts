import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes

export const GROUP_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'createGroup': {
        typePermissions: [
            {
                type: AccessControlledType.GROUP_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.CREATE_GROUP,
                ],
            },
        ],
        disabledTooltip: 'common.create_group_permission_required',
    },

    'updateGroup': {
        typePermissions: [
            {
                type: AccessControlledType.GROUP_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.UPDATE_GROUP,
                ],
            },
        ],
        disabledTooltip: 'common.update_group_permission_required',
    },

    'updateGroupInstance': {
        // Change this to instance permissions and the correct AccessControlledType after https://jira.gentics.com/browse/GTXPE-676
        // has been implemented.
        typePermissions: [
            {
                type: AccessControlledType.GROUP_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.UPDATE_GROUP,
                ],
            },
        ],
        disabledTooltip: 'common.update_group_permission_required',
    },

    'deleteGroup': {
        typePermissions: [
            {
                type: AccessControlledType.GROUP_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.DELETE_GROUP,
                ],
            },
        ],
        disabledTooltip: 'common.delete_group_permission_required',
    },

    'editPermissions': {
        typePermissions: [
            {
                type: AccessControlledType.GROUP_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.SET_USER_PERMISSIONS,
                ],
            },
        ],
        disabledTooltip: 'common.editperm_permission_required',
    },

    'moveGroup': {
        typePermissions: [
            {
                type: AccessControlledType.GROUP_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.UPDATE_GROUP,
                ],
            },
        ],
        disabledTooltip: 'common.move_group_permission_required',
    },

    'userassignment': {
        typePermissions: [
            {
                type: AccessControlledType.GROUP_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.USER_ASSIGNMENT,
                ],
            },
        ],
        disabledTooltip: 'common.update_group_permission_required',
    },

};

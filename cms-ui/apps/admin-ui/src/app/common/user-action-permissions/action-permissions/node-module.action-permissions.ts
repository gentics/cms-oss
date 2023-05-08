import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes
export const NODE_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'createNode': {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'node.create_node_permission_required',
    },

    /**
     * Due to a legacy design decision in the CMS, we must not check permissions for the
     * node's ID (`node.id`) here, but the node's root folder ID (`node.folderId`),
     * using, however, `AccessControlledType.node`.
     */
    'updateNodeInstance': {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        instancePermissions: {
            type: AccessControlledType.NODE,
            permissions: [
                GcmsPermission.READ,
                GcmsPermission.UPDATE_FOLDER,
            ],
        },
        disabledTooltip: 'node.update_node_permission_required',
    },

    /**
     * Due to a legacy design decision in the CMS, we must not check permissions for the
     * node's ID (`node.id`) here, but the node's root folder ID (`node.folderId`),
     * using, however, `AccessControlledType.node`.
     */
    'deleteNode': {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        instancePermissions: {
            type: AccessControlledType.NODE,
            permissions: [
                GcmsPermission.READ,
                GcmsPermission.DELETE_FOLDER,
            ],
        },
        disabledTooltip: 'node.delete_node_permission_required',
    },

    'copyNode': {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'node.copy_node_permission_required',
    },
};

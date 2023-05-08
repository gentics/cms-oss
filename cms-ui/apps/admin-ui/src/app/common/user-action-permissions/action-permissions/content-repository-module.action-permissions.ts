import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes
export const CONTENTREPOSITORY_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'createContentRepository': {
        // permissions of this entity only work with explicit instances
        typePermissions: [],
        disabledTooltip: 'common.create_contentRepository_permission_required',
    },

    'updateContentRepositoryInstance': {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT_REPOSITORY_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        instancePermissions: {
            type: AccessControlledType.CONTENT_REPOSITORY_ADMIN,
            permissions: [
                GcmsPermission.READ,
            ],
        },
        disabledTooltip: 'common.update_contentRepository_permission_required',
    },

    'deleteContentRepository': {
        // permissions of this entity only work with explicit instances
        typePermissions: [],
        disabledTooltip: 'common.delete_contentRepository_permission_required',
    },

    'deleteContentRepositoryInstance': {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT_REPOSITORY_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        instancePermissions: {
            type: AccessControlledType.CONTENT_REPOSITORY_ADMIN,
            permissions: [
                GcmsPermission.READ,
            ],
        },
        disabledTooltip: 'common.delete_contentRepository_permission_required',
    },

    'addContentRepositoryToFragment': {
        typePermissions: [
            {
                type: AccessControlledType.CR_FRAGMENT_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.update_contentRepository_entry_permission_required',
    },

    'removeContentRepositoryFromFragment': {
        typePermissions: [
            {
                type: AccessControlledType.CR_FRAGMENT_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.delete_contentRepository_entry_permission_required',
    },
};

import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes
export const CONSTRUCTCATEGORY_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'createCategory': {
        // permissions of this entity only work with explicit instances
        typePermissions: [
            {
                type: AccessControlledType.CONSTRUCT_CATEGORY_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.create_constructCategory_permission_required',
    },

    'updateCategoryInstance': {
        typePermissions: [
            {
                type: AccessControlledType.CONSTRUCT_CATEGORY_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.update_constructCategory_permission_required',
    },

    'deleteCategory': {
        // permissions of this entity only work with explicit instances
        typePermissions: [],
        disabledTooltip: 'common.delete_constructCategory_permission_required',
    },

    'deleteCategoryInstance': {
        typePermissions: [
            {
                type: AccessControlledType.CONSTRUCT_CATEGORY_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.delete_constructCategory_permission_required',
    },
};

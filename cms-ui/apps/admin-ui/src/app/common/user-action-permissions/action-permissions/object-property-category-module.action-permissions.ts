import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes
export const OBJECTPROPERTYCATEGORY_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'createObjectpropertyCategory': {
        // permissions of this entity only work with explicit instances
        typePermissions: [],
        disabledTooltip: 'common.create_objectPropertyCategory_permission_required',
    },

    'updateObjectpropertyCategoryInstance': {
        typePermissions: [
            {
                type: AccessControlledType.OBJECT_PROPERTY_CATEGORY,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.update_objectPropertyCategory_permission_required',
    },

    'deleteObjectpropertyCategory': {
        // permissions of this entity only work with explicit instances
        typePermissions: [],
        disabledTooltip: 'common.delete_objectPropertyCategory_permission_required',
    },

    'deleteObjectpropertyCategoryInstance': {
        typePermissions: [
            {
                type: AccessControlledType.OBJECT_PROPERTY_CATEGORY,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.delete_objectPropertyCategory_permission_required',
    },
};

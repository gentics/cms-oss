import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes
export const OBJECTPROPERTY_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'createObjectproperty': {
        // permissions of this entity only work with explicit instances
        typePermissions: [],
        disabledTooltip: 'common.create_objectProperty_permission_required',
    },

    'updateObjectpropertyInstance': {
        typePermissions: [
            {
                type: AccessControlledType.OBJECT_PROPERTY_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.update_objectProperty_permission_required',
    },

    'deleteObjectproperty': {
        // permissions of this entity only work with explicit instances
        typePermissions: [],
        disabledTooltip: 'common.delete_objectProperty_permission_required',
    },

    'deleteObjectpropertyInstance': {
        typePermissions: [
            {
                type: AccessControlledType.OBJECT_PROPERTY_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.delete_objectProperty_permission_required',
    },
};

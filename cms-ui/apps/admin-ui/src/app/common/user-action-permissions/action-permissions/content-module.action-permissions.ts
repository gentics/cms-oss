import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes

export const CONTENT_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'readContent': {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        // this would never be displayed
        disabledTooltip: null,
    },

    'updateContent': {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        // this would never be displayed
        disabledTooltip: null,
    },

    'deleteContent': {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        // this would never be displayed
        disabledTooltip: null,
    },

    'setPermContent': {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT,
                permissions: [
                    GcmsPermission.SET_PERMISSION,
                ],
            },
        ],
        // this would never be displayed
        disabledTooltip: null,
    },

};

import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes
export const LANGUAGE_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'createLanguage': {
        typePermissions: [
            {
                type: AccessControlledType.LANGUAGE_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.create_language_permission_required',
    },

    'updateLanguageInstance': {
        typePermissions: [
            {
                type: AccessControlledType.LANGUAGE_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.update_language_permission_required',
    },

    'deleteLanguage': {
        typePermissions: [
            {
                type: AccessControlledType.LANGUAGE_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.delete_language_permission_required',
    },
};

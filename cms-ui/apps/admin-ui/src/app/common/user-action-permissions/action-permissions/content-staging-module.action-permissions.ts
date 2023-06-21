import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes
export const CONTENT_STAGING_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    createContentPackage: {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT_STAGING_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.CREATE,
                ],
            },
        ],
    },

    readContentPackage: {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT_STAGING_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
    },

    updateContentPackage: {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT_STAGING_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.UPDATE,
                ],
            },
        ],

    },

    deleteContentPackage: {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT_STAGING_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.DELETE,
                ],
            },
        ],
    },

    modifyContentPackageContent: {
        typePermissions: [
            {
                type: AccessControlledType.CONTENT_STAGING_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.MODIFY_CONTENT,
                ],
            },
        ],
    },
};

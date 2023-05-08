import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes

/**
 * Contains the action permissions for global actions.
 *
 * @note This should stay as small as possible. We should only add actions here
 * that really do not fit into any other module.
 */
export const GLOBAL_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'viewInbox': {
        typePermissions: [
            {
                type: AccessControlledType.INBOX,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
    },

};

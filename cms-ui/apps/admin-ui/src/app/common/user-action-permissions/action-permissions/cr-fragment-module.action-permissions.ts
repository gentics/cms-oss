import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

export const CR_FRAGMENTS_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    createFragment: {
        typePermissions: [
            // {
            //     type: AccessControlledType.crfragmentadmin,
            //     permissions: [
            //         GcmsPermission.read,
            //         GcmsPermission.createcrfragment,
            //     ],
            // },
        ],
        disabledTooltip: 'common.create_cr_fragment_permission_required',
    },

    updateFragment: {
        typePermissions: [
            // {
            //     type: AccessControlledType.crfragmentadmin,
            //     permissions: [
            //         GcmsPermission.read,
            //         GcmsPermission.updatecrfragment,
            //     ],
            // },
        ],
        disabledTooltip: 'common.update_cr_fragment_permission_required',
    },

    deleteFragment: {
        typePermissions: [
            // {
            //     type: AccessControlledType.crfragmentadmin,
            //     permissions: [
            //         GcmsPermission.read,
            //         GcmsPermission.deletecrfragment,
            //     ],
            // },
        ],
        disabledTooltip: 'common.delete_cr_fragment_permission_required',
    },

}

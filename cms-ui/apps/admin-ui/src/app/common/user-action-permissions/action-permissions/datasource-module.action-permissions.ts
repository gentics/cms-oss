import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

// tslint:disable: object-literal-key-quotes
export const DATASOURCE_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    'createDataSource': {
        typePermissions: [
            {
                type: AccessControlledType.DATA_SOURCE_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.create_dataSource_permission_required',
    },

    'updateDataSourceInstance': {
        typePermissions: [
            {
                type: AccessControlledType.DATA_SOURCE_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        instancePermissions: {
            type: AccessControlledType.DATA_SOURCE,
            permissions: [
                GcmsPermission.READ,
            ],
        },
        disabledTooltip: 'common.update_dataSource_permission_required',
    },

    'deleteDataSource': {
        typePermissions: [
            {
                type: AccessControlledType.DATA_SOURCE_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.delete_dataSource_permission_required',
    },

    'deleteDataSourceInstance': {
        typePermissions: [
            {
                type: AccessControlledType.DATA_SOURCE_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        instancePermissions: {
            type: AccessControlledType.DATA_SOURCE,
            permissions: [
                GcmsPermission.READ,
            ],
        },
        disabledTooltip: 'common.delete_dataSource_permission_required',
    },

    'createDataSourceEntry': {
        typePermissions: [
            {
                type: AccessControlledType.DATA_SOURCE_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.create_dataSource_entry_permission_required',
    },

    'updateDataSourceEntries': {
        typePermissions: [
            {
                type: AccessControlledType.DATA_SOURCE_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.update_dataSource_entry_permission_required',
    },

    'deleteDataSourceEntry': {
        typePermissions: [
            {
                type: AccessControlledType.DATA_SOURCE_ADMIN,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
        ],
        disabledTooltip: 'common.delete_dataSource_entry_permission_required',
    },
};

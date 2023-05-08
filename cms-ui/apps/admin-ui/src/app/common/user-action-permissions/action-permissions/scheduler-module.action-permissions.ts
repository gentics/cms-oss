import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { SingleModuleUserActionPermissions } from '../user-action-permissions';

export const SCHEDULER_MODULE_ACTION_PERMISSIONS: SingleModuleUserActionPermissions = {

    readSchedule: {
        typePermissions: [
            {
                type: AccessControlledType.SCHEDULER,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.READ_SCHEDULES,
                ],
            },
        ],
    },

    createSchedule: {
        typePermissions: [
            {
                type: AccessControlledType.SCHEDULER,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.READ_SCHEDULES,
                    GcmsPermission.UPDATE_SCHEDULES,
                ],
            },
        ],
    },

    updateSchedule: {
        typePermissions: [
            {
                type: AccessControlledType.SCHEDULER,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.READ_SCHEDULES,
                    GcmsPermission.UPDATE_SCHEDULES,
                ],
            },
        ],
        instancePermissions: {
            type: AccessControlledType.SCHEDULER,
            permissions: [
                GcmsPermission.READ,
            ],
        },
    },

    deleteSchedule: {
        typePermissions: [
            {
                type: AccessControlledType.SCHEDULER,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.READ_SCHEDULES,
                    GcmsPermission.UPDATE_SCHEDULES,
                ],
            },
        ],
        instancePermissions: {
            type: AccessControlledType.SCHEDULER,
            permissions: [
                GcmsPermission.READ,
            ],
        },
    },

    readTask: {
        typePermissions: [
            {
                type: AccessControlledType.SCHEDULER,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.READ_TASKS,
                ],
            },
        ],
    },

    createTask: {
        typePermissions: [
            {
                type: AccessControlledType.SCHEDULER,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.READ_TASKS,
                    GcmsPermission.UPDATE_TASKS,
                ],
            },
        ],
    },

    updateTask: {
        typePermissions: [
            {
                type: AccessControlledType.SCHEDULER,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.READ_TASKS,
                    GcmsPermission.UPDATE_TASKS,
                ],
            },
        ],
        instancePermissions: {
            type: AccessControlledType.SCHEDULER,
            permissions: [
                GcmsPermission.READ,
            ],
        },
    },

    deleteTask: {
        typePermissions: [
            {
                type: AccessControlledType.SCHEDULER,
                permissions: [
                    GcmsPermission.READ,
                    GcmsPermission.READ_TASKS,
                    GcmsPermission.UPDATE_TASKS,
                ],
            },
        ],
        instancePermissions: {
            type: AccessControlledType.SCHEDULER,
            permissions: [
                GcmsPermission.READ,
            ],
        },
    },
};

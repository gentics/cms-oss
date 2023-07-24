import { ScheduleDetailTabs, ScheduleTaskDetailTabs } from '@admin-ui/common';
import { AdminUIEntityDetailRoutes, GcmsAdminUiRoute } from '@admin-ui/common/routing/gcms-admin-ui-route';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import {
    ScheduleDetailComponent,
    ScheduleTaskDetailComponent,
    SchedulerModuleMasterComponent,
} from './components';
import { CanActivateScheduleGuard, CanActivateScheduleTaskGuard } from './providers';

export const SCHEDULER_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: SchedulerModuleMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.SCHEDULE,
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: ScheduleDetailComponent,
                data: {
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
                canActivate: [CanActivateScheduleGuard],
                canDeactivate: [DiscardChangesGuard],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: `:id/${ScheduleDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
    {
        path: AdminUIEntityDetailRoutes.SCHEDULE_TASK,
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: ScheduleTaskDetailComponent,
                data: {
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
                canActivate: [CanActivateScheduleTaskGuard],
                canDeactivate: [DiscardChangesGuard],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: `:id/${ScheduleTaskDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];

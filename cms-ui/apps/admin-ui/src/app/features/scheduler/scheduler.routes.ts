import { AdminUIEntityDetailRoutes, EDITOR_TAB, GcmsAdminUiRoute, ROUTE_DETAIL_OUTLET, ScheduleDetailTabs, ScheduleTaskDetailTabs } from '@admin-ui/common';
import { BreadcrumbResolver, DiscardChangesGuard } from '@admin-ui/core';
import { inject } from '@angular/core';
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
        outlet: ROUTE_DETAIL_OUTLET,
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
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
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
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
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

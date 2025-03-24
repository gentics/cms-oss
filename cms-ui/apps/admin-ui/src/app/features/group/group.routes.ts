import { AdminUIEntityDetailRoutes, EDITOR_TAB, GcmsAdminUiRoute, GroupDetailTabs, ROUTE_DETAIL_OUTLET } from '@admin-ui/common';
import { BreadcrumbResolver, DiscardChangesGuard } from '@admin-ui/core';
import { inject } from '@angular/core';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { GroupDetailComponent, GroupMasterComponent } from './components';
import { CanActivateGroupGuard } from './providers';

export const GROUP_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: GroupMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.GROUP,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: GroupDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.GROUP_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateGroupGuard],
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: `:id/${GroupDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];

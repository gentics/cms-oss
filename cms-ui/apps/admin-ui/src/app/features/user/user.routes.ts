import { AdminUIEntityDetailRoutes, EDITOR_TAB, GcmsAdminUiRoute, ROUTE_DETAIL_OUTLET, UserDetailTabs } from '@admin-ui/common';
import { BreadcrumbResolver, DiscardChangesGuard } from '@admin-ui/core';
import { inject } from '@angular/core';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { UserDetailComponent, UserMasterComponent } from './components';
import { CanActivateUserGuard } from './providers';

export const USER_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: UserMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.USER,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: UserDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.USER_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateUserGuard],
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: `:id/${UserDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];

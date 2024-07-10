import { AdminUIEntityDetailRoutes, GcmsAdminUiRoute, ROUTE_DETAIL_OUTLET, RoleDetailTabs } from '@admin-ui/common';
import { BreadcrumbResolver, DiscardChangesGuard, EDITOR_TAB } from '@admin-ui/core';
import { inject } from '@angular/core';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { RoleDetailComponent, RoleMasterComponent } from './components';
import { CanActivateRoleGuard } from './providers';

export const ROLE_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: RoleMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.ROLE,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: RoleDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.ROLE,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateRoleGuard],
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: `:id/${RoleDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];

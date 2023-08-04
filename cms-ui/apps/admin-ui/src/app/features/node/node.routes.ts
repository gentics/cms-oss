import { AdminUIEntityDetailRoutes, GcmsAdminUiRoute, NodeDetailTabs, ROUTE_DETAIL_OUTLET } from '@admin-ui/common';
import { BreadcrumbResolver, DiscardChangesGuard, EDITOR_TAB } from '@admin-ui/core';
import { inject } from '@angular/core';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { NodeDetailComponent, NodeMasterComponent } from './components';
import { CanActivateNodeGuard } from './providers';

export const NODE_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: NodeMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.NODE,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: NodeDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.CONTENT,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateNodeGuard],
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: `:id/${NodeDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },

];

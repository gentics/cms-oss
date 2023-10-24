import { ROUTE_DETAIL_OUTLET } from '@admin-ui/common';
import { AdminUIEntityDetailRoutes, GcmsAdminUiRoute } from '@admin-ui/common/models/routing';
import { BreadcrumbResolver, DiscardChangesGuard, EDITOR_TAB } from '@admin-ui/core';
import { inject } from '@angular/core';
import { MeshBrowserEditorComponent, MeshBrowserMasterComponent } from './components';


export const MESH_BROWSER_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: MeshBrowserMasterComponent,
        data: {
            typePermissions: [],
        },
    },
    {
        path: ':repository',
        component: MeshBrowserEditorComponent,
        data: {
            typePermissions: [],
        },
    },
    {
        path: ':repository/:project/:branch/:parent/:language',
        component: MeshBrowserMasterComponent,
        data: {
            typePermissions: [],
        },
    },
    {
        path: AdminUIEntityDetailRoutes.MESH_BROWSER,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: MeshBrowserEditorComponent,
                data: {
                    typePermissions: [
                    ],
                },
                canActivate: [],
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
        ],
    },
];

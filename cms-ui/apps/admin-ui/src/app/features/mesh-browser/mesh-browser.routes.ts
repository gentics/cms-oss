import { ROUTE_DETAIL_OUTLET } from '@admin-ui/common';
import {
    AdminUIEntityDetailRoutes,
    GcmsAdminUiRoute,
    ROUTE_MESH_BRANCH_ID,
    ROUTE_MESH_CURRENT_NODE_ID,
    ROUTE_MESH_LANGUAGE,
    ROUTE_MESH_PARENT_NODE_ID,
    ROUTE_MESH_PROJECT_ID,
    ROUTE_MESH_REPOSITORY_ID,
} from '@admin-ui/common/models/routing';
import { BreadcrumbResolver, DiscardChangesGuard } from '@admin-ui/core';
import { inject } from '@angular/core';
import {
    MeshBrowserEditorComponent,
    MeshBrowserMasterComponent,
} from './components';

export const MESH_BROWSER_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: MeshBrowserMasterComponent,
        data: {
            typePermissions: [],
        },
    },
    {
        path: `:${ROUTE_MESH_REPOSITORY_ID}`,
        component: MeshBrowserEditorComponent,
        data: {
            typePermissions: [],
        },
    },
    {
        path: `:${ROUTE_MESH_REPOSITORY_ID}/:${ROUTE_MESH_PROJECT_ID}/:${ROUTE_MESH_BRANCH_ID}/:${ROUTE_MESH_PARENT_NODE_ID}/:${ROUTE_MESH_LANGUAGE}`,
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
                path: `:${ROUTE_MESH_PROJECT_ID}/:${ROUTE_MESH_BRANCH_ID}/:${ROUTE_MESH_CURRENT_NODE_ID}/:${ROUTE_MESH_LANGUAGE}`,
                component: MeshBrowserEditorComponent,
                data: {
                    typePermissions: [],
                },
                canActivate: [],
                canDeactivate: [
                    (routeComponent) =>
                        inject(DiscardChangesGuard).canDeactivate(
                            routeComponent,
                        ),
                ],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
        ],
    },
];

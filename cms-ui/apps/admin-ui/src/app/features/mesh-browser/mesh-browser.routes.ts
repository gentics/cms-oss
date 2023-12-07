import {
    ROUTE_DETAIL_OUTLET,
    ROUTE_MESH_BROWSER_OUTLET,
} from '@admin-ui/common';
import {
    AdminUIEntityDetailRoutes,
    GcmsAdminUiRoute,
    ROUTE_DATA_MESH_REPO_ITEM,
    ROUTE_MESH_BRANCH_ID,
    ROUTE_MESH_CURRENT_NODE_ID,
    ROUTE_MESH_LANGUAGE,
    ROUTE_MESH_PARENT_NODE_ID,
    ROUTE_MESH_PROJECT_ID,
    ROUTE_PERMISSIONS_KEY,
} from '@admin-ui/common/models/routing';
import {
    DiscardChangesGuard,
    RouteEntityResolverService,
} from '@admin-ui/core';
import { inject } from '@angular/core';
import {
    MeshBrowserEditorComponent,
    MeshBrowserMasterComponent,
    MeshBrowserModuleMasterComponent,
} from './components';


export const MESH_BROWSER_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: MeshBrowserModuleMasterComponent,
        data: {
            [ROUTE_PERMISSIONS_KEY]: [],
        },
        children: [
            {
                path: 'list',
                outlet: ROUTE_MESH_BROWSER_OUTLET,
                data: {
                    [ROUTE_PERMISSIONS_KEY]: [],
                },
                children: [
                    {
                        path: '',
                        component: MeshBrowserMasterComponent,
                        data: {
                            [ROUTE_PERMISSIONS_KEY]: [],
                        },
                        resolve: {
                            [ROUTE_DATA_MESH_REPO_ITEM]: (route) => inject(RouteEntityResolverService).resolveMeshRoute(route),
                        },
                    },
                    {
                        path: `:${ROUTE_MESH_PROJECT_ID}/:${ROUTE_MESH_BRANCH_ID}/:${ROUTE_MESH_PARENT_NODE_ID}/:${ROUTE_MESH_LANGUAGE}`,
                        component: MeshBrowserMasterComponent,
                        resolve: {
                            [ROUTE_DATA_MESH_REPO_ITEM]: (route) => inject(RouteEntityResolverService).resolveMeshRoute(route),
                        },
                        data: {
                            [ROUTE_PERMISSIONS_KEY]: [],
                        },
                    },
                ],
            },
        ],
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
                    [ROUTE_PERMISSIONS_KEY]: [],
                },
                canActivate: [
                ],
                canDeactivate: [
                    (routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent),
                ],
            },
        ],
    },
];

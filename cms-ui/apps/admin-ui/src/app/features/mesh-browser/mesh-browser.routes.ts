import { ROUTE_DETAIL_OUTLET, TemplateDetailTabs } from '@admin-ui/common';
import { AdminUIEntityDetailRoutes, GcmsAdminUiRoute } from '@admin-ui/common/models/routing';
import { BreadcrumbResolver, DiscardChangesGuard, EDITOR_TAB } from '@admin-ui/core';
import { inject } from '@angular/core';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
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
        path: AdminUIEntityDetailRoutes.TEMPLATE,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:nodeId/:id/:${EDITOR_TAB}`,
                component: MeshBrowserEditorComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
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

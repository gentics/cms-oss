import { AdminUIEntityDetailRoutes, FolderDetailTabs, GcmsAdminUiRoute, ROUTE_DETAIL_OUTLET } from '@admin-ui/common';
import { BreadcrumbResolver, DiscardChangesGuard, EDITOR_TAB } from '@admin-ui/core';
import { inject } from '@angular/core';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { FolderDetailComponent, FolderMasterComponent } from './components';
import { CanActivateFolderGuard } from './providers';

export const FOLDER_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: FolderMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.FOLDER,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: FolderDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.CONTENT_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateFolderGuard],
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: `:id/${FolderDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];


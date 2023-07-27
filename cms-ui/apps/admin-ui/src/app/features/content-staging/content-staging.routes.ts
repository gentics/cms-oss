import { AdminUIEntityDetailRoutes, ContentPackageDetailTabs, GcmsAdminUiRoute, ROUTE_DETAIL_OUTLET } from '@admin-ui/common';
import { BreadcrumbResolver, DiscardChangesGuard, EDITOR_TAB } from '@admin-ui/core';
import { inject } from '@angular/core';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import {
    ContentPackageDetailComponent,
    ContentPackageMasterComponent,
} from './components';
import { CanActivateContentPackageGuard } from './providers';

export const CONTENT_STAGING_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: ContentPackageMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.CONTENT_PACKAGE,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: ContentPackageDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.CONTENT_STAGING_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateContentPackageGuard],
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            // Default the tab to properties
            {
                path: ':id',
                redirectTo: `:id/${ContentPackageDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];

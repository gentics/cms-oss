import { AdminUIEntityDetailRoutes, ContentRepositoryFragmentDetailTabs, EDITOR_TAB, GcmsAdminUiRoute, ROUTE_DETAIL_OUTLET } from '@admin-ui/common';
import { BreadcrumbResolver, DiscardChangesGuard } from '@admin-ui/core';
import { inject } from '@angular/core';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import {
    ContentRepositoryFragmentDetailComponent,
    ContentRepositoryFragmentMasterComponent,
} from './components';
import { CanActivateCRFragmentGuard } from './providers';

export const CR_FRAGMENT_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: ContentRepositoryFragmentMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.CR_FRAGMENT,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: ContentRepositoryFragmentDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.CONTENT_REPOSITORY_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateCRFragmentGuard],
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            // Default the tab to properties
            {
                path: ':id',
                redirectTo: `:id/${ContentRepositoryFragmentDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];

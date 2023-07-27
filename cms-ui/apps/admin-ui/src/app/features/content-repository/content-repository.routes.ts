import { AdminUIEntityDetailRoutes, ContentRepositoryDetailTabs, GcmsAdminUiRoute, ROUTE_DETAIL_OUTLET } from '@admin-ui/common';
import { BreadcrumbResolver, DiscardChangesGuard, EDITOR_TAB } from '@admin-ui/core';
import { inject } from '@angular/core';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { ContentRepositoryDetailComponent, ContentRepositoryMasterComponent } from './components';
import { CanActivateContentRepositoryGuard } from './providers';

export const CONTENT_REPOSIROTY_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: ContentRepositoryMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.CONTENT_REPOSITORY,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: ContentRepositoryDetailComponent,
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
                canActivate: [CanActivateContentRepositoryGuard],
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: `:id/${ContentRepositoryDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];

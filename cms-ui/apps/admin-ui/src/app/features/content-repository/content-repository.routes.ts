import { ContentRepositoryDetailTabs } from '@admin-ui/common';
import { AdminUIEntityDetailRoutes, GcmsAdminUiRoute } from '@admin-ui/common/routing/gcms-admin-ui-route';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
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
        outlet: 'detail',
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
                canDeactivate: [DiscardChangesGuard],
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

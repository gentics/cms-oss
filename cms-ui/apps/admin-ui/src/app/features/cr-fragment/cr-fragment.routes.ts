import { GcmsAdminUiRoute } from '@admin-ui/common/routing/gcms-admin-ui-route';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import {
    ContentRepositoryFragmentDetailComponent,
    ContentRepositoryFragmentDetailTabs,
    ContentRepositoryFragmentMasterComponent,
} from './components';
import { CanActivateCRFragmentGuard } from './providers';

export const CR_FRAGMENT_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: ContentRepositoryFragmentMasterComponent,
    },
    {
        path: 'cr-fragment',
        outlet: 'detail',
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
                canDeactivate: [DiscardChangesGuard],
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

import { GcmsAdminUiRoute } from '@admin-ui/common/routing/gcms-admin-ui-route';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import {
    ContentPackageDetailComponent,
    ContentPackageDetailTabs,
    ContentPackageMasterComponent,
} from './components';
import { CanActivateContentPackageGuard } from './providers';

export const CONTENT_STAGING_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: ContentPackageMasterComponent,
    },
    {
        path: 'package',
        outlet: 'detail',
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
                canDeactivate: [DiscardChangesGuard],
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

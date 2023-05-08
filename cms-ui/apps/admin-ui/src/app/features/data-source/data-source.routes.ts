import { GcmsAdminUiRoute } from '@admin-ui/common/routing/gcms-admin-ui-route';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { DataSourceDetailComponent, DataSourceDetailTabs, DataSourceMasterComponent } from './components';
import { CanActivateDataSourceGuard } from './providers';

export const DATA_SOURCE_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: DataSourceMasterComponent,
    },
    {
        path: 'data-source',
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: DataSourceDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.DATA_SOURCE_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateDataSourceGuard],
                canDeactivate: [DiscardChangesGuard],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: `:id/${DataSourceDetailTabs.properties}`,
                pathMatch: 'full',
            },
        ],
    },
];

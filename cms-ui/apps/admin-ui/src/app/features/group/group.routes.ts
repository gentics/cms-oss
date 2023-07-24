import { GroupDetailTabs } from '@admin-ui/common';
import { AdminUIEntityDetailRoutes, GcmsAdminUiRoute } from '@admin-ui/common/routing/gcms-admin-ui-route';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { GroupDetailComponent, GroupMasterComponent } from './components';
import { CanActivateGroupGuard } from './providers';

export const GROUP_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: GroupMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.GROUP,
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: GroupDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.GROUP_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateGroupGuard],
                canDeactivate: [DiscardChangesGuard],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: `:id/${GroupDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];

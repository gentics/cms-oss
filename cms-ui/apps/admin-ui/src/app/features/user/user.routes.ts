import { UserDetailTabs } from '@admin-ui/common';
import { AdminUIEntityDetailRoutes, GcmsAdminUiRoute } from '@admin-ui/common/routing/gcms-admin-ui-route';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { UserDetailComponent, UserMasterComponent } from './components';
import { CanActivateUserGuard } from './providers';

export const USER_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: UserMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.USER,
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: UserDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.USER_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateUserGuard],
                canDeactivate: [DiscardChangesGuard],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: `:id/${UserDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];

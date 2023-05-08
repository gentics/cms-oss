import { GcmsAdminUiRoute } from '@admin-ui/common';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { RoleDetailComponent, RoleMasterComponent } from './components';
import { CanActivateRoleGuard } from './providers';

export const ROLE_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: RoleMasterComponent,
    },
    {
        path: 'role',
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: RoleDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.ROLE,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateRoleGuard],
                canDeactivate: [DiscardChangesGuard],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: ':id/properties',
                pathMatch: 'full',
            },
        ],
    },
];

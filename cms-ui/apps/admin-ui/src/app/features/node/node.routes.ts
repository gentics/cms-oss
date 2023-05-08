import { GcmsAdminUiRoute } from '@admin-ui/common';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { NodeDetailComponent, NodeMasterComponent } from './components';
import { CanActivateNodeGuard } from './providers';

export const NODE_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: NodeMasterComponent,
    },
    {
        path: 'node',
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: NodeDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.CONTENT,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateNodeGuard],
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

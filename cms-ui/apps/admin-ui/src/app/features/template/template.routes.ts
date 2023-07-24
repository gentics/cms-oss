import { TemplateDetailTabs } from '@admin-ui/common';
import { AdminUIEntityDetailRoutes, GcmsAdminUiRoute } from '@admin-ui/common/routing/gcms-admin-ui-route';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { TemplateDetailComponent, TemplateMasterComponent } from './components';
import { CanActivateTemplateGuard } from './providers';

export const TEMPLATE_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: TemplateMasterComponent,
        data: {
            typePermissions: [],
        },
    },
    {
        path: AdminUIEntityDetailRoutes.TEMPLATE,
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:nodeId/:id/:${EDITOR_TAB}`,
                component: TemplateDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateTemplateGuard],
                canDeactivate: [DiscardChangesGuard],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            // Default the tab to properties
            {
                path: ':nodeId/:id',
                redirectTo: `:nodeId/:id/${TemplateDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];

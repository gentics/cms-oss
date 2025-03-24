import { AdminUIEntityDetailRoutes, EDITOR_TAB, GcmsAdminUiRoute, ROUTE_DETAIL_OUTLET, TemplateDetailTabs } from '@admin-ui/common';
import { BreadcrumbResolver, DiscardChangesGuard } from '@admin-ui/core';
import { inject } from '@angular/core';
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
        outlet: ROUTE_DETAIL_OUTLET,
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
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
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

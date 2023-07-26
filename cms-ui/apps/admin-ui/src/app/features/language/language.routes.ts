import { AdminUIEntityDetailRoutes, GcmsAdminUiRoute, LanguageDetailTabs } from '@admin-ui/common';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { inject } from '@angular/core';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { LanguageDetailComponent, LanguageMasterComponent } from './components';
import { CanActivateLanguageGuard } from './providers';

export const LANGUAGE_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: LanguageMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.LANGUAGE,
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: LanguageDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.LANGUAGE_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateLanguageGuard],
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: `:id/${LanguageDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];


import { GcmsAdminUiRoute } from '@admin-ui/common';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { LanguageDetailComponent, LanguageMasterComponent } from './components';
import { CanActivateLanguageGuard } from './providers';

export const LANGUAGE_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: LanguageMasterComponent,
    },
    {
        path: 'language',
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


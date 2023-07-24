import { AdminUIEntityDetailRoutes, ConstructDetailTabs, GcmsAdminUiRoute } from '@admin-ui/common';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import {
    ConstructCategoryDetailComponent,
    ConstructDetailComponent,
    ConstructModuleMasterComponent,
} from './components';
import { CanActivateConstructCategoryGuard, CanActivateConstructGuard } from './providers';

export const CONSTRUCT_ROUTES: GcmsAdminUiRoute[] = [
    // {
    //     path: `:${MASTER_TAB_ID}`,
    //     component: ConstructMasterComponent,
    //     data: {
    //         typePermissions: [],
    //     },
    // },
    // {
    //     path: '',
    //     redirectTo: ConstructModuleTabs.CONSTRUCTS,
    //     pathMatch: 'full',
    // },
    {
        path: '',
        component: ConstructModuleMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.CONSTRUCT_CATEGORY,
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: ':id',
                component: ConstructCategoryDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.CONSTRUCT_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateConstructCategoryGuard],
                canDeactivate: [DiscardChangesGuard],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
        ],
    },
    {
        path: AdminUIEntityDetailRoutes.CONSTRUCT,
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: ConstructDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.CONSTRUCT_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivateConstructGuard],
                canDeactivate: [DiscardChangesGuard],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            // Default the tab to properties
            {
                path: ':id',
                redirectTo: `:id/${ConstructDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];

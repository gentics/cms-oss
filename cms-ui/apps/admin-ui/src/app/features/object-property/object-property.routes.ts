import { AdminUIEntityDetailRoutes, GcmsAdminUiRoute } from '@admin-ui/common';
import { BreadcrumbResolver } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import {
    ObjectPropertyCategoryDetailComponent,
    ObjectPropertyDetailComponent,
    ObjectPropertyModuleMasterComponent,
} from './components';
import { CanActivateObjectPropertyCategoryGuard, CanActivateObjectPropertyGuard } from './providers';

export const OBJECT_PROPERTY_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: ObjectPropertyModuleMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.OBJECT_PROPERTY,
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: ':id',
                component: ObjectPropertyDetailComponent,
                data: {
                    typePermissions: [],
                },
                canActivate: [CanActivateObjectPropertyGuard],
                canDeactivate: [DiscardChangesGuard],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
        ],
    },
    {
        path: AdminUIEntityDetailRoutes.OBJECT_PROPERTY_CATEGORY,
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: ':id',
                component: ObjectPropertyCategoryDetailComponent,
                data: {
                    typePermissions: [],
                },
                canActivate: [CanActivateObjectPropertyCategoryGuard],
                canDeactivate: [DiscardChangesGuard],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
        ],
    },
];

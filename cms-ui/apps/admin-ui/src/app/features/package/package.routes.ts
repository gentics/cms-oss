import { AdminUIEntityDetailRoutes, DevtoolPackageDetailTabs, GcmsAdminUiRoute } from '@admin-ui/common';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { inject } from '@angular/core';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { PackageDetailComponent, PackageMasterComponent } from './components';
import { CanActivatePackageGuard } from './providers';

export const PACKAGE_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: PackageMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.DEVTOOL_PACKAGE,
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: PackageDetailComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.CONTENT_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
                canActivate: [CanActivatePackageGuard],
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: `:id/${DevtoolPackageDetailTabs.CONSTRUCTS}`,
                pathMatch: 'full',
            },
        ],
    },
];

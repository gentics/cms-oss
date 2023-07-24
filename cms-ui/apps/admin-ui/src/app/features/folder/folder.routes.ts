import { AdminUIEntityDetailRoutes, FolderDetailTabs, GcmsAdminUiRoute } from '@admin-ui/common';
import { BreadcrumbResolver, EDITOR_TAB } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { FolderDetailComponent, FolderMasterComponent } from './components';
import { CanActivateFolderGuard } from './providers';

export const FOLDER_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: FolderMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.FOLDER,
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:id/:${EDITOR_TAB}`,
                component: FolderDetailComponent,
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
                canActivate: [CanActivateFolderGuard],
                canDeactivate: [DiscardChangesGuard],
                resolve: {
                    breadcrumb: BreadcrumbResolver,
                },
            },
            {
                path: ':id',
                redirectTo: `:id/${FolderDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];


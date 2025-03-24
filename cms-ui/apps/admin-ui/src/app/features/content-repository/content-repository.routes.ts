import {
    AdminUIEntityDetailRoutes,
    ContentRepositoryDetailTabs,
    EditableEntity,
    GcmsAdminUiRoute,
    MeshMangementTabs,
    ROUTE_DETAIL_OUTLET,
    ROUTE_MANAGEMENT_OUTLET,
    ROUTE_PARAM_MESH_TAB,
    ROUTE_PATH_MESH,
    ROUTE_PERMISSIONS_KEY,
} from '@admin-ui/common';
import { createEntityEditorRoutes } from '@admin-ui/core';
import { ManagementTabsComponent } from '@admin-ui/mesh';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { ContentRepositoryEditorComponent, ContentRepositoryMasterComponent } from './components';

export const CONTENT_REPOSIROTY_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: ContentRepositoryMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.CONTENT_REPOSITORY,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            [ROUTE_PERMISSIONS_KEY]: [],
        },
        children: [
            ...createEntityEditorRoutes(EditableEntity.CONTENT_REPOSITORY, ContentRepositoryEditorComponent, ContentRepositoryDetailTabs.PROPERTIES, {
                typePermissions: [
                    {
                        type: AccessControlledType.CONTENT_REPOSITORY_ADMIN,
                        permissions: [
                            GcmsPermission.READ,
                        ],
                    },
                ],
            }, [
                {
                    path: ROUTE_PATH_MESH,
                    outlet: ROUTE_MANAGEMENT_OUTLET,
                    data: {
                        [ROUTE_PERMISSIONS_KEY]: [],
                    },
                    children: [
                        {
                            path: `:${ROUTE_PARAM_MESH_TAB}`,
                            component: ManagementTabsComponent,
                            data: {
                                [ROUTE_PERMISSIONS_KEY]: [],
                            },
                        },
                        {
                            path: '',
                            redirectTo: MeshMangementTabs.OVERVIEW,
                            pathMatch: 'full',
                        },
                    ],
                },
            ]),
        ],
    },
];

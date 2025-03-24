import {
    AdminUIEntityDetailRoutes,
    DataSourceDetailTabs,
    EditableEntity,
    GcmsAdminUiRoute,
    ROUTE_DETAIL_OUTLET,
    ROUTE_PERMISSIONS_KEY,
} from '@admin-ui/common';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { DataSourceEditorComponent, DataSourceMasterComponent } from './components';
import { createEntityEditorRoutes } from '@admin-ui/core';

export const DATA_SOURCE_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: DataSourceMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.DATA_SOURCE,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            [ROUTE_PERMISSIONS_KEY]: [],
        },
        children: [
            ...createEntityEditorRoutes(EditableEntity.DATA_SOURCE, DataSourceEditorComponent, DataSourceDetailTabs.PROPERTIES, {
                [ROUTE_PERMISSIONS_KEY]: [
                    {
                        type: AccessControlledType.DATA_SOURCE_ADMIN,
                        permissions: [
                            GcmsPermission.READ,
                        ],
                    },
                ],
            }),
        ],
    },
];

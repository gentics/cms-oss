import {
    AdminUIEntityDetailRoutes,
    EditableEntity,
    GcmsAdminUiRoute,
    NodeDetailTabs,
    ROUTE_DETAIL_OUTLET,
    ROUTE_PERMISSIONS_KEY,
} from '@admin-ui/common';
import { createEntityEditorRoutes } from '@admin-ui/core';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { NodeEditorComponent, NodeMasterComponent } from './components';

export const NODE_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: NodeMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.NODE,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            typePermissions: [],
        },
        children: [
            ...createEntityEditorRoutes(EditableEntity.NODE, NodeEditorComponent, NodeDetailTabs.PROPERTIES, {
                [ROUTE_PERMISSIONS_KEY]: [
                    {
                        type: AccessControlledType.CONTENT,
                        permissions: [
                            GcmsPermission.READ,
                        ],
                    },
                ],
            }),
        ],
    },

];

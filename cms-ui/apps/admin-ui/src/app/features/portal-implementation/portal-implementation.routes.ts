import {
    AdminUIEntityDetailRoutes,
    EditableEntity,
    GcmsAdminUiRoute,
    PortalImplementationDetailTabs,
    ROUTE_DETAIL_OUTLET,
    ROUTE_PERMISSIONS_KEY,
    createEntityEditorRoutes,
} from '@admin-ui/common';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { PortalImplementationEditorComponent, PortalImplementationMasterComponent } from './components';

export const PORTAL_IMPLEMENTATION_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: PortalImplementationMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.PORTAL_IMPLEMENTATION,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            [ROUTE_PERMISSIONS_KEY]: [],
        },
        children: [
            ...createEntityEditorRoutes(
                EditableEntity.PORTAL_IMPLEMENTATION,
                PortalImplementationEditorComponent,
                PortalImplementationDetailTabs.PROPERTIES,
                {
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.PORTAL_IMPLEMENTATION_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            ),
        ],
    },
];

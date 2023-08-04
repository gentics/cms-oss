import {
    AdminUIEntityDetailRoutes,
    ConstructCategoryDetailTabs,
    ConstructDetailTabs,
    EditableEntity,
    GcmsAdminUiRoute,
    ROUTE_DETAIL_OUTLET,
    ROUTE_PERMISSIONS_KEY,
    createEntityEditorRoutes,
} from '@admin-ui/common';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import {
    ConstructCategoryEditorComponent,
    ConstructEditorComponent,
    ConstructModuleMasterComponent,
} from './components';

export const CONSTRUCT_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: ConstructModuleMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.CONSTRUCT_CATEGORY,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            [ROUTE_PERMISSIONS_KEY]: [],
        },
        children: [
            ...createEntityEditorRoutes(EditableEntity.CONSTRUCT_CATEGORY, ConstructCategoryEditorComponent, ConstructCategoryDetailTabs.PROPERTIES, {
                [ROUTE_PERMISSIONS_KEY]: [
                    {
                        type: AccessControlledType.CONSTRUCT_ADMIN,
                        permissions: [
                            GcmsPermission.READ,
                        ],
                    },
                ],
            }),
        ],
    },
    {
        path: AdminUIEntityDetailRoutes.CONSTRUCT,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            [ROUTE_PERMISSIONS_KEY]: [],
        },
        children: [
            ...createEntityEditorRoutes(EditableEntity.CONSTRUCT, ConstructEditorComponent, ConstructDetailTabs.PROPERTIES, {
                [ROUTE_PERMISSIONS_KEY]: [
                    {
                        type: AccessControlledType.CONSTRUCT_ADMIN,
                        permissions: [
                            GcmsPermission.READ,
                        ],
                    },
                ],
            }),
        ],
    },
];

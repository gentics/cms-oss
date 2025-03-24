import {
    AdminUIEntityDetailRoutes,
    EditableEntity,
    GcmsAdminUiRoute,
    ObjectPropertyCategoryDetailTabs,
    ObjectPropertyDetailTabs,
    ROUTE_DETAIL_OUTLET,
    ROUTE_PERMISSIONS_KEY,
} from '@admin-ui/common';
import { createEntityEditorRoutes } from '@admin-ui/core';
import {
    ObjectPropertyCategoryEditorComponent,
    ObjectPropertyEditorComponent,
    ObjectPropertyModuleMasterComponent,
} from './components';

export const OBJECT_PROPERTY_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: ObjectPropertyModuleMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.OBJECT_PROPERTY,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            [ROUTE_PERMISSIONS_KEY]: [],
        },
        children: [
            ...createEntityEditorRoutes(EditableEntity.OBJECT_PROPERTY, ObjectPropertyEditorComponent, ObjectPropertyDetailTabs.PROPERTIES),
        ],
    },
    {
        path: AdminUIEntityDetailRoutes.OBJECT_PROPERTY_CATEGORY,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            [ROUTE_PERMISSIONS_KEY]: [],
        },
        children: [
            ...createEntityEditorRoutes(
                EditableEntity.OBJECT_PROPERTY_CATEGORY,
                ObjectPropertyCategoryEditorComponent,
                ObjectPropertyCategoryDetailTabs.PROPERTIES,
            ),
        ],
    },
];

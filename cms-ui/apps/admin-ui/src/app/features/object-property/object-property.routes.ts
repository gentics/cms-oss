import {
    AdminUIEntityDetailRoutes,
    EditableEntity,
    GcmsAdminUiRoute,
    ObjectPropertyCategoryDetailTabs,
    ObjectPropertyDetailTabs,
    ROUTE_ENTITY_RESOLVER_KEY,
    ROUTE_ENTITY_TYPE_KEY,
    ROUTE_IS_EDITOR_ROUTE,
    ROUTE_PARAM_ENTITY_ID,
} from '@admin-ui/common';
import { EDITOR_TAB, RouteEntityResolverService, runEntityResolver } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { inject } from '@angular/core';
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
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:${ROUTE_PARAM_ENTITY_ID}/:${EDITOR_TAB}`,
                component: ObjectPropertyEditorComponent,
                data: {
                    typePermissions: [],
                    [ROUTE_IS_EDITOR_ROUTE]: true,
                    [ROUTE_ENTITY_TYPE_KEY]: EditableEntity.OBJECT_PROPERTY,
                },
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    [ROUTE_ENTITY_RESOLVER_KEY]: (route) => inject(RouteEntityResolverService).resolve(route),
                },
                runGuardsAndResolvers: (from, to) => runEntityResolver(from, to),
            },
            {
                path: `:${ROUTE_PARAM_ENTITY_ID}`,
                redirectTo: `:${ROUTE_PARAM_ENTITY_ID}/${ObjectPropertyDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
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
                path: `:${ROUTE_PARAM_ENTITY_ID}/:${EDITOR_TAB}`,
                component: ObjectPropertyCategoryEditorComponent,
                data: {
                    typePermissions: [],
                    [ROUTE_IS_EDITOR_ROUTE]: true,
                    [ROUTE_ENTITY_TYPE_KEY]: EditableEntity.OBJECT_PROPERTY_CATEGORY,
                },
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    [ROUTE_ENTITY_RESOLVER_KEY]: (route) => inject(RouteEntityResolverService).resolve(route),
                },
                runGuardsAndResolvers: (from, to) => runEntityResolver(from, to),
            },
            {
                path: `:${ROUTE_PARAM_ENTITY_ID}`,
                redirectTo: `:${ROUTE_PARAM_ENTITY_ID}/${ObjectPropertyCategoryDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];

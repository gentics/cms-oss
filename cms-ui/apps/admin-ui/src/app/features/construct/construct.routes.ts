import {
    AdminUIEntityDetailRoutes,
    ConstructCategoryDetailTabs,
    ConstructDetailTabs,
    EditableEntity,
    GcmsAdminUiRoute,
    ROUTE_ENTITY_RESOLVER_KEY,
    ROUTE_ENTITY_TYPE_KEY,
    ROUTE_IS_EDITOR_ROUTE,
    ROUTE_PARAM_ENTITY_ID,
} from '@admin-ui/common';
import { EDITOR_TAB, RouteEntityResolverService, runEntityResolver } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { inject } from '@angular/core';
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
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:${ROUTE_PARAM_ENTITY_ID}/:${EDITOR_TAB}`,
                component: ConstructCategoryEditorComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.CONSTRUCT_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                    [ROUTE_IS_EDITOR_ROUTE]: true,
                    [ROUTE_ENTITY_TYPE_KEY]: EditableEntity.CONSTRUCT_CATEGORY,
                },
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    [ROUTE_ENTITY_RESOLVER_KEY]: (route) => inject(RouteEntityResolverService).resolve(route),
                },
                runGuardsAndResolvers: (from, to) => runEntityResolver(from, to),
            },
            {
                path: `:${ROUTE_PARAM_ENTITY_ID}`,
                redirectTo: `:${ROUTE_PARAM_ENTITY_ID}/${ConstructCategoryDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
    {
        path: AdminUIEntityDetailRoutes.CONSTRUCT,
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:${ROUTE_PARAM_ENTITY_ID}/:${EDITOR_TAB}`,
                component: ConstructEditorComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.CONSTRUCT_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                    [ROUTE_IS_EDITOR_ROUTE]: true,
                    [ROUTE_ENTITY_TYPE_KEY]: EditableEntity.CONSTRUCT,
                },
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    [ROUTE_ENTITY_RESOLVER_KEY]: (route) => inject(RouteEntityResolverService).resolve(route),
                },
                runGuardsAndResolvers: (from, to) => runEntityResolver(from, to),
            },
            {
                path: `:${ROUTE_PARAM_ENTITY_ID}`,
                redirectTo: `:${ROUTE_PARAM_ENTITY_ID}/${ConstructDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];

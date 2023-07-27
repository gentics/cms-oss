import {
    AdminUIEntityDetailRoutes,
    EditableEntity,
    GcmsAdminUiRoute,
    LanguageDetailTabs,
    ROUTE_ENTITY_RESOLVER_KEY,
    ROUTE_ENTITY_TYPE_KEY,
    ROUTE_IS_EDITOR_ROUTE,
    ROUTE_PARAM_ENTITY_ID,
} from '@admin-ui/common';
import { EDITOR_TAB, RouteEntityResolverService, runEntityResolver } from '@admin-ui/core';
import { DiscardChangesGuard } from '@admin-ui/core/providers/guards/discard-changes';
import { inject } from '@angular/core';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { LanguageEditorComponent, LanguageMasterComponent } from './components';

export const LANGUAGE_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: LanguageMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.LANGUAGE,
        outlet: 'detail',
        data: {
            typePermissions: [],
        },
        children: [
            {
                path: `:${ROUTE_PARAM_ENTITY_ID}/:${EDITOR_TAB}`,
                component: LanguageEditorComponent,
                data: {
                    typePermissions: [
                        {
                            type: AccessControlledType.LANGUAGE_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                    [ROUTE_IS_EDITOR_ROUTE]: true,
                    [ROUTE_ENTITY_TYPE_KEY]: EditableEntity.LANGUAGE,
                },
                canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
                resolve: {
                    [ROUTE_ENTITY_RESOLVER_KEY]: (route) => inject(RouteEntityResolverService).resolve(route),
                },
                runGuardsAndResolvers: (from, to) => runEntityResolver(from, to),
            },
            {
                path: `:${ROUTE_PARAM_ENTITY_ID}`,
                redirectTo: `:${ROUTE_PARAM_ENTITY_ID}/${LanguageDetailTabs.PROPERTIES}`,
                pathMatch: 'full',
            },
        ],
    },
];


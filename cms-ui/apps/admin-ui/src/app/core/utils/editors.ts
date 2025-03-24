import {
    EDITOR_TAB,
    EditableEntity,
    EditableEntityDetailTabs,
    GcmsAdminUiRoute,
    ROUTE_ENTITY_RESOLVER_KEY,
    ROUTE_ENTITY_TYPE_KEY,
    ROUTE_IS_EDITOR_ROUTE,
    ROUTE_PARAM_ENTITY_ID,
    ROUTE_PERMISSIONS_KEY,
    RouteData,
} from '@admin-ui/common';
import { Type, inject } from '@angular/core';
import { BaseEntityEditorComponent } from '../components/base-entity-editor/base-entity-editor.component';
import { DiscardChangesGuard } from '../guards/discard-changes/discard-changes.guard';
import { RouteEntityResolverService, runEntityResolver } from '../providers/route-entity-resolver/route-entity-resolver.service';

export function createEntityEditorRoutes<K extends EditableEntity>(
    type: K,
    component: Type<BaseEntityEditorComponent<K>>,
    defaultTab: EditableEntityDetailTabs[K],
    data?: Partial<RouteData>,
    children?: GcmsAdminUiRoute[],
): GcmsAdminUiRoute<BaseEntityEditorComponent<K>>[] {
    return [
        {
            path: `:${ROUTE_PARAM_ENTITY_ID}/:${EDITOR_TAB}`,
            component: component,
            data: {
                [ROUTE_PERMISSIONS_KEY]: [],
                [ROUTE_IS_EDITOR_ROUTE]: true,
                [ROUTE_ENTITY_TYPE_KEY]: type,
                ...data,
            },
            canDeactivate: [(routeComponent) => inject(DiscardChangesGuard).canDeactivate(routeComponent)],
            resolve: {
                [ROUTE_ENTITY_RESOLVER_KEY]: (route) => inject(RouteEntityResolverService).resolve(route),
            },
            runGuardsAndResolvers: (from, to) => runEntityResolver(from, to),
            children,
        },
        {
            path: `:${ROUTE_PARAM_ENTITY_ID}`,
            redirectTo: `:${ROUTE_PARAM_ENTITY_ID}/${defaultTab}`,
            pathMatch: 'full',
        },
    ]
}

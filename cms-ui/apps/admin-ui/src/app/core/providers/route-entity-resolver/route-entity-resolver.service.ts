import {
    EditableEntity,
    EditableEntityModels,
    EntityEditorHandler,
    ROUTE_ENTITY_TYPE_KEY,
    ROUTE_IS_EDITOR_ROUTE,
    ROUTE_PARAM_ENTITY_ID,
    ROUTE_PARAM_NODE_ID,
} from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot } from '@angular/router';
import { ConstructHandlerService } from '../construct-handler/construct-handler.service';
import { ObjectPropertyCategoryHandlerService } from '../object-property-category-handler/object-property-category-handler.service';
import { ObjectPropertyHandlerService } from '../object-property-handler/object-property-handler.service';
import { ConstructCategoryHandlerService } from '../construct-category-handler/construct-category-handler.service';

export function runEntityResolver(from: ActivatedRouteSnapshot, to: ActivatedRouteSnapshot): boolean {
    if (from.component !== to.component) {
        return true;
    }
    if (!from.data[ROUTE_IS_EDITOR_ROUTE] && !to.data[ROUTE_IS_EDITOR_ROUTE]) {
        return true;
    }
    if (from.data[ROUTE_ENTITY_TYPE_KEY] !== to.data[ROUTE_ENTITY_TYPE_KEY]) {
        return true;
    }

    if ((from.params[ROUTE_PARAM_ENTITY_ID] === to.params[ROUTE_PARAM_ENTITY_ID])
        && (from.params[ROUTE_PARAM_NODE_ID] === to.params[ROUTE_PARAM_NODE_ID])
    ) {
        return false;
    }

    return true;
}

@Injectable()
export class RouteEntityResolverService {

    constructor(
        private construct: ConstructHandlerService,
        private constructCat: ConstructCategoryHandlerService,
        private objPropCat: ObjectPropertyCategoryHandlerService,
        private objPro: ObjectPropertyHandlerService,
    ) {}

    async resolve<K extends EditableEntity>(route: ActivatedRouteSnapshot): Promise<EditableEntityModels[K]> {
        const id = route.paramMap.get(ROUTE_PARAM_ENTITY_ID);
        const type: K = route.data[ROUTE_ENTITY_TYPE_KEY];

        const handler = this.getHandler(type);

        if (handler == null) {
            return null;
        }

        const entity = await handler.getMapped(id).toPromise() as EditableEntityModels[K];

        return entity;
    }

    public getHandler<K extends EditableEntity>(type: EditableEntity): EntityEditorHandler<EditableEntityModels[K], K> {
        switch (type) {
            case EditableEntity.CONSTRUCT:
                return this.construct as any;

            case EditableEntity.CONSTRUCT_CATEGORY:
                return this.constructCat as any;

            case EditableEntity.OBJECT_PROPERTY:
                return this.objPro as any;

            case EditableEntity.OBJECT_PROPERTY_CATEGORY:
                return this.objPropCat as any;

            default:
                return null
        }
    }
}

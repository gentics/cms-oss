import { EditableEntity, EditableEntityModels, EntityEditorHandler, ROUTE_ENTITY_TYPE_KEY, ROUTE_PARAM_ENTITY_ID } from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot } from '@angular/router';
import { ObjectPropertyCategoryHandlerService } from '../object-property-category-handler/object-property-category-handler.service';
import { ObjectPropertyHandlerService } from '../object-property-handler/object-property-handler.service';

@Injectable()
export class RouteEntityResolverService {

    constructor(
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
            case EditableEntity.OBJECT_PROPERTY_CATEGORY:
                return this.objPropCat as any;

            case EditableEntity.OBJECT_PROPERTY:
                return this.objPro as any;

            default:
                return null
        }
    }
}

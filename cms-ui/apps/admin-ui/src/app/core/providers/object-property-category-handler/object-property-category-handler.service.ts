/* eslint-disable @typescript-eslint/no-unused-vars */
import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_PERMISSIONS,
    EditableEntity,
    EditableEntityBusinessObjects,
    EditableEntityModels,
    EntityCreateRequestModel,
    EntityCreateRequestParams,
    EntityCreateResponseModel,
    EntityDeleteRequestParams,
    EntityEditorHandler,
    EntityList,
    EntityListHandler,
    EntityListRequestModel,
    EntityListRequestParams,
    EntityListResponseModel,
    EntityLoadRequestParams,
    EntityLoadResponseModel,
    EntityUpdateRequestModel,
    EntityUpdateRequestParams,
    EntityUpdateResponseModel,
} from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';

@Injectable()
export class ObjectPropertyCategoryHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
        EntityListHandler<EditableEntity.OBJECT_PROPERTY_CATEGORY> {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: EditableEntityModels[EditableEntity.OBJECT_PROPERTY_CATEGORY]): string {
        return entity.name;
    }

    public mapToBusinessObject(
        category: EditableEntityModels[EditableEntity.OBJECT_PROPERTY_CATEGORY],
        index?: number,
    ): EditableEntityBusinessObjects[EditableEntity.OBJECT_PROPERTY_CATEGORY] {
        return {
            ...category,
            [BO_ID]: String(category.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: this.displayName(category),
        };
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
        params?: EntityCreateRequestParams<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
    ): Observable<EntityCreateResponseModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>> {
        return this.api.objectPropertycategories.createObjectPropertyCategory(data).pipe(
            tap((res) => {
                const name = this.displayName(res.objectPropertyCategory);
                this.nameMap[res.objectPropertyCategory.id] = name;

                this.notification.show({
                    type: 'success',
                    message: 'shared.item_created',
                    translationParams: {
                        name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    createMapped(
        data: EntityCreateRequestModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
        params?: EntityCreateRequestParams<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.OBJECT_PROPERTY_CATEGORY]> {
        return this.create(data, params).pipe(
            map((res) => this.mapToBusinessObject(res.objectPropertyCategory)),
        );
    }

    get(
        id: string | number,
        params?: EntityLoadRequestParams<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
    ): Observable<EntityLoadResponseModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>> {
        return this.api.objectPropertycategories.getObjectPropertyCategory(id).pipe(
            tap((res) => {
                const name = this.displayName(res.objectPropertyCategory);
                this.nameMap[res.objectPropertyCategory.id] = name;
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(
        id: string | number,
        params?: EntityLoadRequestParams<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.OBJECT_PROPERTY_CATEGORY]> {
        return this.get(id, params).pipe(
            map((res) => this.mapToBusinessObject(res.objectPropertyCategory)),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
        params?: EntityUpdateRequestParams<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
    ): Observable<EntityUpdateResponseModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>> {
        return this.api.objectPropertycategories.updateObjectPropertyCategory(id, data).pipe(
            tap((res) => {
                const name = this.displayName(res.objectPropertyCategory);
                this.nameMap[res.objectPropertyCategory.id] = name;

                this.notification.show({
                    type: 'success',
                    message: 'shared.item_updated',
                    translationParams: {
                        name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    updateMapped(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
        params?: EntityUpdateRequestParams<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.OBJECT_PROPERTY_CATEGORY]> {
        return this.update(id, data, params).pipe(
            map((res) => this.mapToBusinessObject(res.objectPropertyCategory)),
        );
    }

    delete(id: string | number, params?: EntityDeleteRequestParams<EditableEntity.OBJECT_PROPERTY_CATEGORY>): Observable<void> {
        return this.api.objectPropertycategories.deleteObjectPropertyCategory(id).pipe(
            tap(() => {
                const name = this.nameMap[id];

                if (!name) {
                    return;
                }

                delete this.nameMap[id];
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_singular_deleted',
                    translationParams: {
                        name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    list(
        body?: EntityListRequestModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
        params?: EntityListRequestParams<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
    ): Observable<EntityListResponseModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>> {
        return this.api.objectPropertycategories.getObjectPropertyCategories(params).pipe(
            tap((res) => {
                res.items.forEach((objCat) => {
                    const name = this.displayName(objCat);
                    this.nameMap[objCat.id] = name;
                });
            }),
        );
    }

    listMapped(
        body?: EntityListRequestModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
        params?: EntityListRequestParams<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.OBJECT_PROPERTY_CATEGORY]>> {
        return this.list(body, params).pipe(
            map((res) => ({
                items: res.items.map((item, index) => this.mapToBusinessObject(item, index)),
                totalItems: res.numItems,
            })),
        );
    }
}

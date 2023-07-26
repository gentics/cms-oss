import {
    EditableEntity,
    EntityCreateRequestModel,
    EntityCreateResponseModel,
    EntityEditorHandler,
    EntityList,
    EntityListHandler,
    EntityListRequestModel,
    EntityListRequestParams,
    EntityListResponseModel,
    EntityLoadResponseModel,
    EntityUpdateRequestModel,
    EntityUpdateResponseModel,
} from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { ModelType, ObjectPropertyCategory, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';
import { I18nNotificationService } from '../i18n-notification';

@Injectable()
export class ObjectPropertyCategoryHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<ObjectPropertyCategory<Raw>, EditableEntity.OBJECT_PROPERTY_CATEGORY>,
        EntityListHandler<ObjectPropertyCategory<Raw>, EditableEntity.OBJECT_PROPERTY_CATEGORY> {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: ObjectPropertyCategory<ModelType.Raw>): string {
        return entity.name;
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
    ): Observable<EntityCreateResponseModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>> {
        return this.api.objectPropertycategories.createObjectPropertyCategory(data).pipe(
            tap(res => {
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
    ): Observable<ObjectPropertyCategory<Raw>> {
        return this.create(data).pipe(
            map(res => res.objectPropertyCategory),
        );
    }

    get(id: string | number): Observable<EntityLoadResponseModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>> {
        return this.api.objectPropertycategories.getObjectPropertyCategory(id).pipe(
            tap(res => {
                const name = this.displayName(res.objectPropertyCategory);
                this.nameMap[res.objectPropertyCategory.id] = name;
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(id: string | number): Observable<ObjectPropertyCategory<Raw>> {
        return this.get(id).pipe(
            map(res => res.objectPropertyCategory),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
    ): Observable<EntityUpdateResponseModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>> {
        return this.api.objectPropertycategories.updateObjectPropertyCategory(id, data).pipe(
            tap(res => {
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
    ): Observable<ObjectPropertyCategory<Raw>> {
        return this.update(id, data).pipe(
            map(res => res.objectPropertyCategory),
        );
    }

    delete(id: string | number): Observable<void> {
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
            tap(res => {
                res.items.forEach(objCat => {
                    const name = this.displayName(objCat);
                    this.nameMap[objCat.id] = name;
                });
            }),
        );
    }

    listMapped(
        body?: EntityListRequestModel<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
        params?: EntityListRequestParams<EditableEntity.OBJECT_PROPERTY_CATEGORY>,
    ): Observable<EntityList<ObjectPropertyCategory<ModelType.Raw>>> {
        return this.list(body, params).pipe(
            map(res => ({
                items: res.items,
                totalItems: res.numItems,
            })),
        );
    }
}

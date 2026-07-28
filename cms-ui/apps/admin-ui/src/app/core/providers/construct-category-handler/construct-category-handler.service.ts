/* eslint-disable @typescript-eslint/no-unused-vars */
import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_NEW_SORT_ORDER,
    BO_ORIGINAL_SORT_ORDER,
    BO_PERMISSIONS,
    EditableEntity,
    EditableEntityBusinessObjects,
    EditableEntityModels,
    EntityCreateRequestModel,
    EntityCreateRequestParams,
    EntityCreateResponseModel,
    EntityEditorHandler,
    EntityList,
    EntityListHandler,
    EntityListRequestModel,
    EntityListRequestParams,
    EntityListResponseModel,
    EntityLoadResponseModel,
    EntityUpdateRequestModel,
    EntityUpdateRequestParams,
    EntityUpdateResponseModel,
    discard,
} from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';

@Injectable()
export class ConstructCategoryHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<EditableEntity.CONSTRUCT_CATEGORY>,
        EntityListHandler<EditableEntity.CONSTRUCT_CATEGORY> {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: EditableEntityModels[EditableEntity.CONSTRUCT_CATEGORY]): string {
        return entity.name;
    }

    public mapToBusinessObject(
        category: EditableEntityModels[EditableEntity.CONSTRUCT_CATEGORY],
        index?: number,
    ): EditableEntityBusinessObjects[EditableEntity.CONSTRUCT_CATEGORY] {
        return {
            ...category,
            [BO_ID]: String(category.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: this.displayName(category),
            [BO_ORIGINAL_SORT_ORDER]: index,
            [BO_NEW_SORT_ORDER]: index,
        };
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.CONSTRUCT_CATEGORY>,
        options?: EntityCreateRequestParams<EditableEntity.CONSTRUCT_CATEGORY>,
    ): Observable<EntityCreateResponseModel<EditableEntity.CONSTRUCT_CATEGORY>> {
        return this.api.constructCategory.createConstructCategoryCategory(data).pipe(
            tap((res) => {
                const name = this.displayName(res.constructCategory);
                this.nameMap[res.constructCategory.id] = name;

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
        data: EntityCreateRequestModel<EditableEntity.CONSTRUCT_CATEGORY>,
        options?: EntityCreateRequestParams<EditableEntity.CONSTRUCT_CATEGORY>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.CONSTRUCT_CATEGORY]> {
        return this.create(data, options).pipe(
            map((res) => this.mapToBusinessObject(res.constructCategory)),
        );
    }

    get(id: string | number): Observable<EntityLoadResponseModel<EditableEntity.CONSTRUCT_CATEGORY>> {
        return this.api.constructCategory.getConstructCategoryCategory(id).pipe(
            tap((res) => {
                const name = this.displayName(res.constructCategory);
                this.nameMap[res.constructCategory.id] = name;
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(id: string | number): Observable<EditableEntityBusinessObjects[EditableEntity.CONSTRUCT_CATEGORY]> {
        return this.get(id).pipe(
            map((res) => this.mapToBusinessObject(res.constructCategory)),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.CONSTRUCT_CATEGORY>,
        parms?: EntityUpdateRequestParams<EditableEntity.CONSTRUCT_CATEGORY>,
    ): Observable<EntityUpdateResponseModel<EditableEntity.CONSTRUCT_CATEGORY>> {
        return this.api.constructCategory.updateConstructCategoryCategory(id, data).pipe(
            tap((res) => {
                const name = this.displayName(res.constructCategory);
                this.nameMap[res.constructCategory.id] = name;

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
        data: EntityUpdateRequestModel<EditableEntity.CONSTRUCT_CATEGORY>,
        params?: EntityUpdateRequestParams<EditableEntity.CONSTRUCT_CATEGORY>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.CONSTRUCT_CATEGORY]> {
        return this.update(id, data).pipe(
            map((res) => this.mapToBusinessObject(res.constructCategory)),
        );
    }

    delete(id: string | number): Observable<void> {
        return this.api.constructCategory.deleteConstructCategoryCategory(id).pipe(
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
        body?: EntityListRequestModel<EditableEntity.CONSTRUCT_CATEGORY>,
        params?: EntityListRequestParams<EditableEntity.CONSTRUCT_CATEGORY>,
    ): Observable<EntityListResponseModel<EditableEntity.CONSTRUCT_CATEGORY>> {
        return this.api.constructCategory.getConstructCategoryCategories(params).pipe(
            tap((res) => {
                res.items.forEach((objCat) => {
                    const name = this.displayName(objCat);
                    this.nameMap[objCat.id] = name;
                });
            }),
        );
    }

    listMapped(
        body?: EntityListRequestModel<EditableEntity.CONSTRUCT_CATEGORY>,
        params?: EntityListRequestParams<EditableEntity.CONSTRUCT_CATEGORY>,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.CONSTRUCT_CATEGORY]>> {
        return this.list(body, params).pipe(
            map((res) => ({
                items: (res.items || []).map((item, index) => this.mapToBusinessObject(item, index)),
                totalItems: res.numItems,
            })),
        );
    }

    sort(categoryIds: string[]): Observable<void> {
        return this.api.constructCategory.sortConstructCategories({
            ids: categoryIds,
        }).pipe(
            discard(),
            this.catchAndRethrowError(),
        );
    }
}

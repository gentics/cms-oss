import {
    EditableEntity,
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
    EntityUpdateResponseModel,
    discard,
} from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { ConstructCategory, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';
import { I18nNotificationService } from '../i18n-notification';

@Injectable()
export class ConstructCategoryHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<ConstructCategory<Raw>, EditableEntity.CONSTRUCT_CATEGORY>,
        EntityListHandler<ConstructCategory<Raw>, EditableEntity.CONSTRUCT_CATEGORY> {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: ConstructCategory<Raw>): string {
        return entity.name;
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.CONSTRUCT_CATEGORY>,
        options?: EntityCreateRequestParams<EditableEntity.CONSTRUCT_CATEGORY>,
    ): Observable<EntityCreateResponseModel<EditableEntity.CONSTRUCT_CATEGORY>> {
        return this.api.constructCategory.createConstructCategoryCategory(data).pipe(
            tap(res => {
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
    ): Observable<ConstructCategory> {
        return this.create(data, options).pipe(
            map(res => res.constructCategory),
        );
    }

    get(id: string | number): Observable<EntityLoadResponseModel<EditableEntity.CONSTRUCT_CATEGORY>> {
        return this.api.constructCategory.getConstructCategoryCategory(id).pipe(
            tap(res => {
                const name = this.displayName(res.constructCategory);
                this.nameMap[res.constructCategory.id] = name;
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(id: string | number): Observable<ConstructCategory> {
        return this.get(id).pipe(
            map(res => res.constructCategory),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.CONSTRUCT_CATEGORY>,
    ): Observable<EntityUpdateResponseModel<EditableEntity.CONSTRUCT_CATEGORY>> {
        return this.api.constructCategory.updateConstructCategoryCategory(id, data).pipe(
            tap(res => {
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
    ): Observable<ConstructCategory> {
        return this.update(id, data).pipe(
            map(res => res.constructCategory),
        );
    }

    delete(id: string | number): Observable<void> {
        return this.api.tagType.deleteTagType(id).pipe(
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
            tap(res => {
                res.items.forEach(objCat => {
                    const name = this.displayName(objCat);
                    this.nameMap[objCat.id] = name;
                });
            }),
        );
    }

    listMapped(
        body?: EntityListRequestModel<EditableEntity.CONSTRUCT_CATEGORY>,
        params?: EntityListRequestParams<EditableEntity.CONSTRUCT_CATEGORY>,
    ): Observable<EntityList<ConstructCategory<Raw>>> {
        return this.list(body, params).pipe(
            map(res => ({
                items: res.items,
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

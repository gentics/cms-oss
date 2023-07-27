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
} from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { I18nLanguage, Language, Response } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable, forkJoin } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';
import { I18nNotificationService } from '../i18n-notification';

@Injectable()
export class LanguageHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<Language, EditableEntity.LANGUAGE>,
        EntityListHandler<Language, EditableEntity.LANGUAGE> {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: Language): string {
        return entity.name;
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.LANGUAGE>,
        options?: EntityCreateRequestParams<EditableEntity.LANGUAGE>,
    ): Observable<EntityCreateResponseModel<EditableEntity.LANGUAGE>> {
        return this.api.language.createLanguage(data).pipe(
            tap(res => {
                const name = this.displayName(res.language);
                this.nameMap[res.language.id] = name;

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
        data: EntityCreateRequestModel<EditableEntity.LANGUAGE>,
        options?: EntityCreateRequestParams<EditableEntity.LANGUAGE>,
    ): Observable<Language> {
        return this.create(data, options).pipe(
            map(res => res.language),
        );
    }

    get(id: string | number): Observable<EntityLoadResponseModel<EditableEntity.LANGUAGE>> {
        return this.api.language.getLanguage(id).pipe(
            tap(res => {
                const name = this.displayName(res.language);
                this.nameMap[res.language.id] = name;
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(id: string | number): Observable<Language> {
        return this.get(id).pipe(
            map(res => res.language),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.LANGUAGE>,
    ): Observable<EntityUpdateResponseModel<EditableEntity.LANGUAGE>> {
        return this.api.language.updateLanguage(id, data).pipe(
            tap(res => {
                const name = this.displayName(res.language);
                this.nameMap[res.language.id] = name;

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
        data: EntityUpdateRequestModel<EditableEntity.LANGUAGE>,
    ): Observable<Language> {
        return this.update(id, data).pipe(
            map(res => res.language),
        );
    }

    delete(id: string | number): Observable<void> {
        return this.api.language.deleteLanguage(id).pipe(
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
        body?: EntityListRequestModel<EditableEntity.LANGUAGE>,
        params?: EntityListRequestParams<EditableEntity.LANGUAGE>,
    ): Observable<EntityListResponseModel<EditableEntity.LANGUAGE>> {
        return this.api.language.getLanguages(params).pipe(
            tap(res => {
                res.items.forEach(objCat => {
                    const name = this.displayName(objCat);
                    this.nameMap[objCat.id] = name;
                });
            }),
        );
    }

    listMapped(
        body?: EntityListRequestModel<EditableEntity.LANGUAGE>,
        params?: EntityListRequestParams<EditableEntity.LANGUAGE>,
    ): Observable<EntityList<Language>> {
        return this.list(body, params).pipe(
            map(res => ({
                items: res.items,
                totalItems: res.numItems,
            })),
        );
    }

    setActiveUiLanguage(language: Language | string): Observable<Response> {
        if (typeof language === 'string') {
            language = { code: language } as any;
        }

        return this.api.i18n.setActiveUiLanguage(language as Language).pipe(
            this.catchAndRethrowError(),
        );
    }

    getActiveBackendLanguage(): Observable<string> {
        return this.api.i18n.getActiveUiLanguage().pipe(
            map(response => response.code),
        );
    }

    getBackendLanguages(): Observable<I18nLanguage[]> {
        return this.api.i18n.getAvailableUiLanguages().pipe(
            map(response => response.items),
        );
    }

    watchSupportedLanguages(params?: EntityListRequestParams<EditableEntity.LANGUAGE>): Observable<Language[]> {
        return forkJoin([
            this.listMapped(null as never, params),
            this.getBackendLanguages(),
        ]).pipe(
            map(([allLangs, avilableLangs]) => allLangs.items.filter(lang => avilableLangs.map(i => i.code).includes(lang.code))),
        );
    }
}

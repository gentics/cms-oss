/* eslint-disable @typescript-eslint/no-unused-vars */
import {
    EditableEntity,
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
import { I18nLanguage, Language, NodeLanguageListRequest, NodeLanguagesListResponse, Response } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable, forkJoin } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';
import { I18nNotificationService } from '../i18n-notification';

@Injectable()
export class LanguageHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<EditableEntity.LANGUAGE>,
        EntityListHandler<EditableEntity.LANGUAGE> {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: EditableEntityModels[EditableEntity.LANGUAGE]): string {
        return entity.name;
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.LANGUAGE>,
        params?: EntityCreateRequestParams<EditableEntity.LANGUAGE>,
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
    ): Observable<EditableEntityModels[EditableEntity.LANGUAGE]> {
        return this.create(data, options).pipe(
            map(res => res.language),
        );
    }

    get(id: string | number, params?: EntityLoadRequestParams<EditableEntity.LANGUAGE>): Observable<EntityLoadResponseModel<EditableEntity.LANGUAGE>> {
        return this.api.language.getLanguage(id).pipe(
            tap(res => {
                const name = this.displayName(res.language);
                this.nameMap[res.language.id] = name;
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(id: string | number, params?: EntityLoadRequestParams<EditableEntity.LANGUAGE>): Observable<EditableEntityModels[EditableEntity.LANGUAGE]> {
        return this.get(id, params).pipe(
            map(res => res.language),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.LANGUAGE>,
        params?: EntityUpdateRequestParams<EditableEntity.LANGUAGE>,
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
        params?: EntityUpdateRequestParams<EditableEntity.LANGUAGE>,
    ): Observable<EditableEntityModels[EditableEntity.LANGUAGE]> {
        return this.update(id, data, params).pipe(
            map(res => res.language),
        );
    }

    delete(id: string | number, params?: EntityDeleteRequestParams<EditableEntity.LANGUAGE>): Observable<void> {
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
                res.items.forEach(lang => {
                    const name = this.displayName(lang);
                    this.nameMap[lang.id] = name;
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listMapped(
        body?: EntityListRequestModel<EditableEntity.LANGUAGE>,
        params?: EntityListRequestParams<EditableEntity.LANGUAGE>,
    ): Observable<EntityList<EditableEntityModels[EditableEntity.LANGUAGE]>> {
        return this.list(body, params).pipe(
            map(res => ({
                items: res.items,
                totalItems: res.numItems,
            })),
        );
    }

    listFromNode(
        nodeId: number | string,
        body?: never,
        params?: NodeLanguageListRequest,
    ): Observable<NodeLanguagesListResponse> {
        return this.api.node.getNodeLanguageList(nodeId, params).pipe(
            tap(res => {
                res.items.forEach(lang => {
                    const name = this.displayName(lang);
                    this.nameMap[lang.id] = name;
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listFromNodeMapped(
        nodeId: number | string,
        body?: never,
        params?: NodeLanguageListRequest,
    ): Observable<EntityList<EditableEntityModels[EditableEntity.LANGUAGE]>> {
        return this.listFromNode(nodeId, body, params).pipe(
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
            this.catchAndRethrowError(),
        );
    }

    getBackendLanguages(): Observable<I18nLanguage[]> {
        return this.api.i18n.getAvailableUiLanguages().pipe(
            map(response => response.items),
            this.catchAndRethrowError(),
        );
    }

    getSupportedLanguages(params?: EntityListRequestParams<EditableEntity.LANGUAGE>): Observable<Language[]> {
        return forkJoin([
            this.listMapped(null as never, params),
            this.getBackendLanguages(),
        ]).pipe(
            map(([allLangs, avilableLangs]) => allLangs.items.filter(lang => avilableLangs.map(i => i.code).includes(lang.code))),
        );
    }
}

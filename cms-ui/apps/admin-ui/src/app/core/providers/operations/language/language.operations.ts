import { UpdateEntities } from '@admin-ui/state/entity/entity.actions';
import { AppStateService } from '@admin-ui/state/providers/app-state/app-state.service';
import { Injectable, Injector } from '@angular/core';
import {
    GcmsUiLanguage,
    I18nLanguage,
    ItemDeleteResponse,
    Language,
    LanguageCreateRequest,
    LanguageResponse,
    LanguageUpdateRequest,
    Response,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class LanguageOperations extends ExtendedEntityOperationsBase<'language'> {


    constructor(
        injector: Injector,
        private api: GcmsApi,
        private appState: AppStateService,
        private entityManager: EntityManagerService,
        private notification: I18nNotificationService,
    ) {
        super(injector, 'language');
    }

    /**
     * Loads all languages from the CMS and adds them to the EntityState.
     */
    getAll(): Observable<Language[]> {
        return this.api.language.getLanguages().pipe(
            map(res => res.items),
            tap(languages => this.entityManager.addEntities('language', languages)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Create the `Language`
     */
    createLanguage(language: LanguageCreateRequest, notification: boolean = true): Observable<Language> {
        return this.api.language.createLanguage(language).pipe(
            map((response: LanguageResponse) => response.language),
            tap((language: Language) => {
                this.entityManager.addEntity(this.entityIdentifier, language);
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_created',
                        translationParams: { name: language.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Deletes the `Language` with the specified `id`.
     */
    deleteLanguage(id: number): Observable<void> {
        const languageToBeDeleted = this.appState.now.entity.language[id];

        return this.api.language.deleteLanguage(id).pipe(
            switchMap(() => this.removeLanguageFromAppState(id)),
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.item_singular_deleted',
                translationParams: { name: languageToBeDeleted ? languageToBeDeleted.name : id },
            })),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Updates the `Language` with the specified `id`
     */
    updateLanguage(id: number, update: LanguageUpdateRequest, notification: boolean = true): Observable<Language> {
        return this.api.language.updateLanguage(id, update).pipe(
            map((response: LanguageResponse) => response.language),
            tap((language: Language) => {
                this.entityManager.addEntity(this.entityIdentifier, language);
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_updated',
                        translationParams: { name: language.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    get(id: number): Observable<Language> {
        return this.api.language.getLanguage(id).pipe(
            map((response: LanguageResponse) => response.language),
        );
    }

    private removeLanguageFromAppState(deletedLanguageId: number): Observable<void> {
        this.entityManager.deleteEntities(this.entityIdentifier, [deletedLanguageId]);
        return this.appState.dispatch(new UpdateEntities({ language: this.appState.now.entity.language }));
    }

    setActiveUiLanguage(language: GcmsUiLanguage): Observable<Response> {
        return this.api.i18n.setActiveUiLanguage({ code: language }).pipe(
            this.catchAndRethrowError(),
        );
    }

    unassignLanguage(nodeId: number, languageId: number): Observable<ItemDeleteResponse>{
        return this.api.node.removeNodeLanguage(nodeId, languageId).pipe(
            tap(response => {
                this.notification.show({
                    type: 'success',
                    message: response.responseInfo.responseMessage,
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    getActiveBackendLanguage(): Observable<GcmsUiLanguage> {
        return this.api.i18n.getActiveUiLanguage().pipe(
            map(response => response.code),
        );
    }

    getBackendLanguages(): Observable<I18nLanguage[]> {
        return this.api.i18n.getAvailableUiLanguages().pipe(
            map(response => response.items),
        );
    }
}

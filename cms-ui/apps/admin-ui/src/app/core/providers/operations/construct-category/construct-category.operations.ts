import { Injectable, Injector } from '@angular/core';
import {
    ConstructCategory,
    ConstructCategoryBO,
    ConstructCategoryCreateRequest,
    ConstructCategoryCreateResponse,
    ConstructCategoryListOptions,
    ConstructCategoryListResponse,
    ConstructCategoryLoadResponse,
    ConstructCategoryUpdateRequest,
    ConstructCategoryUpdateResponse,
    Normalized,
    Raw
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';
import { discard } from '@admin-ui/common';

@Injectable()
export class ConstructCategoryOperations extends ExtendedEntityOperationsBase<'constructCategory'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entityManager: EntityManagerService,
        private notification: I18nNotificationService,
    ) {
        super(injector, 'constructCategory');
    }

    /**
     * Get a list of all constructCategorys and adds them to the AppState.
     */
    getAll(options?: ConstructCategoryListOptions): Observable<ConstructCategoryBO<Raw>[]> {
        return this.api.constructCategory.getConstructCategoryCategories(options).pipe(
            map((res: ConstructCategoryListResponse) => {
                return res.items.map(item => item as any as ConstructCategoryBO<Raw>);
            }),
            tap((items: ConstructCategoryBO<Raw>[]) => this.entityManager.addEntities(this.entityIdentifier, items)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single constructCategory and add it to the AppState.
     */
    get(constructCategoryId: string): Observable<ConstructCategoryBO<Raw>> {
        return this.api.constructCategory.getConstructCategoryCategory(constructCategoryId).pipe(
            map((res: ConstructCategoryLoadResponse) => res.constructCategory),
            map((item: ConstructCategory<Raw>) => item as any as ConstructCategoryBO<Raw>),
            tap((item: ConstructCategoryBO<Raw>) => this.entityManager.addEntity(this.entityIdentifier, item)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Create a constructCategory.
     */
    create(constructCategory: ConstructCategoryCreateRequest): Observable<ConstructCategoryBO<Raw>> {
        return this.api.constructCategory.createConstructCategoryCategory(constructCategory).pipe(
            map((response: ConstructCategoryCreateResponse) => response.constructCategory),
            // fake entity's `id` property to enforce internal application entity uniformity
            map((constructCategory: ConstructCategory<Raw>) => Object.assign(constructCategory, { id: `${constructCategory.id}` })),
            tap((constructCategory: ConstructCategoryBO<Raw>) => {
                this.entityManager.addEntity(this.entityIdentifier, constructCategory);
            }),
            tap((constructCategory: ConstructCategoryBO<Raw>) => {
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_created',
                    translationParams: { name: constructCategory.name },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change a single constructCategory
     */
    update(constructCategoryId: string, payload: ConstructCategoryUpdateRequest): Observable<ConstructCategoryBO<Raw>> {
        return this.api.constructCategory.updateConstructCategoryCategory(constructCategoryId, payload).pipe(
            map((res: ConstructCategoryUpdateResponse) => res.constructCategory),
            // fake entity's `id` property to enforce internal application entity uniformity
            map((constructCategory: ConstructCategory<Raw>) => Object.assign(constructCategory, { id: `${constructCategory.id}` })),
            // update state with server response
            tap((constructCategory: ConstructCategoryBO<Raw>) => {
                this.entityManager.addEntity(this.entityIdentifier, constructCategory);
            }),
            // display toast notification
            tap((constructCategory: ConstructCategoryBO<Raw>) => {
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_updated',
                    translationParams: { name: constructCategory.name },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Delete a single constructCategory
     */
    delete(constructCategoryId: string | number): Observable<Response | void> {
        return this.api.constructCategory.deleteConstructCategoryCategory(constructCategoryId).pipe(
            // display toast notification
            tap(() => {
                this.entityManager.getEntity(this.entityIdentifier, constructCategoryId)
                    .toPromise()
                    .then((constructCategoryDeleted: ConstructCategoryBO<Normalized>) => {
                        return this.notification.show({
                            type: 'success',
                            message: 'shared.item_singular_deleted',
                            translationParams: { name: constructCategoryDeleted.name },
                        });
                    })
                    // remove entity from state
                    .then(() => this.entityManager.deleteEntities(this.entityIdentifier, [constructCategoryId]));
            }),
            this.catchAndRethrowError(),
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

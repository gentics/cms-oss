import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import {
    ModelType,
    ObjectPropertyCategory,
    ObjectPropertyCategoryBO,
    ObjectPropertyCategoryCreateRequest,
    ObjectPropertyCategoryListOptions,
    ObjectPropertyCategoryUpdateRequest,
    Raw,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { filter, map, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class ObjectPropertyCategoryOperations extends ExtendedEntityOperationsBase<'objectPropertyCategory'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entityManager: EntityManagerService,
        private notification: I18nNotificationService,
        private appState: AppStateService,
    ) {
        super(injector, 'objectPropertyCategory');
    }

    /**
     * Get a list of all objectPropertyCategorys and adds them to the AppState.
     */
    getAll(options?: ObjectPropertyCategoryListOptions): Observable<ObjectPropertyCategoryBO<Raw>[]> {
        return this.api.objectPropertycategories.getObjectPropertyCategories(options).pipe(
            filter(res => Array.isArray(res.items)),
            map(res  => res.items.map(item => this.mapToBusinessObject(item))),
            tap(items => this.entityManager.addEntities('objectPropertyCategory', items)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single objectPropertyCategory and add it to the AppState.
     */
    get(entityId: string): Observable<ObjectPropertyCategoryBO<Raw>> {
        return this.api.objectPropertycategories.getObjectPropertyCategory(entityId).pipe(
            map(res => this.mapToBusinessObject(res.objectPropertyCategory)),
            tap(item => this.entityManager.addEntity('objectPropertyCategory', item)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Create a objectPropertyCategory.
     */
     create(body: ObjectPropertyCategoryCreateRequest, notify: boolean = true): Observable<ObjectPropertyCategoryBO<Raw>> {
        return this.api.objectPropertycategories.createObjectPropertyCategory(body).pipe(
            map(response => this.mapToBusinessObject(response.objectPropertyCategory)),
            tap(created => {
                this.entityManager.addEntity(this.entityIdentifier, created);

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_created',
                        translationParams: {
                            name: created.name,
                        },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change a single objectPropertyCategory
     */
    update(entityId: string, payload: ObjectPropertyCategoryUpdateRequest, notify: boolean = true): Observable<ObjectPropertyCategoryBO<Raw>> {
        return this.api.objectPropertycategories.updateObjectPropertyCategory(entityId, payload).pipe(
            map(res => this.mapToBusinessObject(res.objectPropertyCategory)),
            // update state with server response
            tap(objectPropertyCategory => {
                this.entityManager.addEntity(this.entityIdentifier, objectPropertyCategory);

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_updated',
                        translationParams: {
                            name: objectPropertyCategory.name,
                        },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Delete a single objectPropertyCategory
     */
    delete(entityId: string, notify: boolean = true): Observable<Response | void> {
        const categoryToBeDeleted = this.appState.now.entity[this.entityIdentifier][entityId];

        return this.api.objectPropertycategories.deleteObjectPropertyCategory(entityId).pipe(
            // display toast notification
            tap(() => {
                if (notify && categoryToBeDeleted) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_singular_deleted',
                        translationParams: {
                            name: categoryToBeDeleted.name,
                        },
                    });
                }

                this.entityManager.deleteEntities(this.entityIdentifier, [entityId]);
            }),
            this.catchAndRethrowError(),
        );
    }

    private mapToBusinessObject<T extends ModelType>(category: ObjectPropertyCategory<T>): ObjectPropertyCategoryBO<T> {
        // Noop as the models are the same
        return category as any;
    }

}

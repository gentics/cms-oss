import { LOAD_FOR_PACKAGE_LIST, PackageEntityOperations } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import {
    ContentRepositoryFragment,
    ContentRepositoryFragmentBO,
    ContentRepositoryFragmentCreateRequest,
    ContentRepositoryFragmentListOptions,
    ContentRepositoryFragmentUpdateRequest,
    EntityIdType,
    Raw,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class ContentRepositoryFragmentOperations
    extends ExtendedEntityOperationsBase<'contentRepositoryFragment'>
    implements PackageEntityOperations<ContentRepositoryFragmentBO<Raw>>
{

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entityManager: EntityManagerService,
        private appState: AppStateService,
        private notification: I18nNotificationService,
    ) {
        super(injector, 'contentRepositoryFragment');
    }

    create(payload: ContentRepositoryFragmentCreateRequest, notification: boolean = true): Observable<ContentRepositoryFragmentBO<Raw>> {
        return this.api.contentRepositoryFragments.createContentRepositoryFragment(payload).pipe(
            map(res => this.mapToBusinessObject(res.contentRepositoryFragment)),
            tap(fragment => {
                this.entityManager.addEntity(this.entityIdentifier, fragment);

                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_created',
                        translationParams: {
                            name: fragment.name,
                        },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Loads all content repositories from the CMS and adds them to the EntityState.
     */
    getAll(options?: ContentRepositoryFragmentListOptions): Observable<ContentRepositoryFragmentBO<Raw>[]> {
        return this.api.contentRepositoryFragments.getContentRepositoryFragments(options).pipe(
            map(res => res.items.map(item => this.mapToBusinessObject(item, !!(options?.[LOAD_FOR_PACKAGE_LIST])))),
            tap(fragments => this.entityManager.addEntities(this.entityIdentifier, fragments)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Loads a single content repository from the CMS and adds it to the EntityState.
     */
    get(fragmentId: EntityIdType): Observable<ContentRepositoryFragmentBO<Raw>> {
        return this.api.contentRepositoryFragments.getContentRepositoryFragment(fragmentId).pipe(
            map(res => this.mapToBusinessObject(res.contentRepositoryFragment)),
            tap(fragment => this.entityManager.addEntity(this.entityIdentifier, fragment)),
            this.catchAndRethrowError(),
        );
    }

    getAllFromPackage(packageId: string, options?: any): Observable<ContentRepositoryFragmentBO<Raw>[]> {
        return this.api.devTools.getContentRepositoryFragments(packageId, options).pipe(
            map(res => res.items.map(item => this.mapToBusinessObject(item, true))),
            this.catchAndRethrowError(),
        );
    }

    getFromPackage(packageId: string, entityId: string): Observable<ContentRepositoryFragmentBO<Raw>> {
        return this.api.devTools.getContentRepositoryFragment(packageId, entityId).pipe(
            map(res => this.mapToBusinessObject(res.contentRepositoryFragment, true)),
            this.catchAndRethrowError(),
        );
    }

    update(
        fragmentId: EntityIdType,
        payload: ContentRepositoryFragmentUpdateRequest,
        notification: boolean = true,
    ): Observable<ContentRepositoryFragmentBO<Raw>> {
        return this.api.contentRepositoryFragments.updateContentRepositoryFragment(fragmentId, payload).pipe(
            map(res => this.mapToBusinessObject(res.contentRepositoryFragment)),
            tap(fragment => {
                this.entityManager.addEntity(this.entityIdentifier, fragment);

                // display toast notification
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_updated',
                        translationParams: {
                            name: fragment.name,
                        },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    delete(fragmentId: EntityIdType, notification: boolean = true): Observable<void> {
        const entityToBeDeleted = this.appState.now.entity.contentRepositoryFragment[fragmentId];

        return this.api.contentRepositoryFragments.deleteContentRepositoryFragment(fragmentId).pipe(
            // display toast notification
            tap(() => {
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_singular_deleted',
                        translationParams: {
                            name: entityToBeDeleted.name,
                        },
                    });
                }

                // remove entity from state
                this.entityManager.deleteEntities(this.entityIdentifier, [fragmentId]);
            }),
            this.catchAndRethrowError(),
        )
    }

    addToDevTool(
        devtoolPackage: string,
        entityId: string | number,
    ): Observable<void> {
        const entity = this.appState.now.entity.contentRepositoryFragment[entityId];

        return this.api.devTools.addContentRepositoryToPackage(devtoolPackage, entityId).pipe(
            tap(() => {
                this.notification.show({
                    message: 'contentRepositoryFragment.contentRepositoryFragment_successfully_added_to_package',
                    type: 'success',
                    translationParams: {
                        name: entity.name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    removeFromDevTool(
        devtoolPackage: string,
        entityId: string | number,
    ): Observable<void> {
        const entity = this.appState.now.entity.contentRepositoryFragment[entityId];

        return this.api.devTools.removeContentRepositoryFromPackage(devtoolPackage, entityId).pipe(
            tap(() => {
                this.notification.show({
                    message: 'contentRepositoryFragment.contentRepositoryFragment_successfully_removed_from_package',
                    type: 'success',
                    translationParams: {
                        name: entity.name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    public mapToBusinessObject(fragment: ContentRepositoryFragment<Raw>, forPackage: boolean = false): ContentRepositoryFragmentBO<Raw> {
        return {
            ...fragment,
            id: forPackage ? fragment.globalId?.toString?.() : fragment.id?.toString?.(),
        };
    }
}

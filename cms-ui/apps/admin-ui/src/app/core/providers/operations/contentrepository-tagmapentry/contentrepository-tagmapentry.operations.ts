import { ContentRepositoryOperations, EntityManagerService, I18nNotificationService } from '@admin-ui/core';
import { Injectable, Injector } from '@angular/core';
import {
    EntityIdType,
    Normalized,
    Raw,
    TagmapEntry,
    TagmapEntryBO,
    TagmapEntryCreateRequest,
    TagmapEntryCreateResponse,
    TagmapEntryListOptions,
    TagmapEntryListResponse,
    TagmapEntryUpdateRequest,
    TagmapEntryUpdateResponse,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class ContentRepositoryTagmapEntryOperations extends ExtendedEntityOperationsBase<'tagmapEntry'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entityManager: EntityManagerService,
        private notification: I18nNotificationService,
    ) {
        super(injector, 'tagmapEntry');
    }

    /**
     * Get a list of all entries of a contentRepository.
     * @param options Set to null.
     * @param contentRepositoryId Although ExtendedEntityOperationsBase suggest optionality, this argument must be supplied
     */
    getAll(
        options?: TagmapEntryListOptions,
        contentRepositoryId?: EntityIdType,
    ): Observable<TagmapEntryBO<Raw>[]> {
        if (!contentRepositoryId) {
            throw new Error('Invalid Argument: ContentRepository ID has to be provided');
        }
        return this.api.contentrepositories.getContentRepositoryTagmapEntries(contentRepositoryId, options).pipe(
            map((res: TagmapEntryListResponse) => {
                // fake entity's `id` property to enforce internal application entity uniformity
                return res.items.map(item => Object.assign(item, { id: `${item.id}` }) as TagmapEntryBO<Raw>);
            }),
            tap(items => this.entityManager.addEntities('tagmapEntry', items)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single entry of a contentRepository.
     * @param tagmapEntryId Entry ID.
     * @param contentRepositoryId Although EntityOperationsBase suggest optionality, this argument must be supplied
     */
    get(
        contentRepositoryId: string,
        tagmapId: string,
    ): Observable<TagmapEntryBO<Normalized>> {
        return this.api.contentrepositories.getContentRepositoryTagmapEntry(contentRepositoryId, tagmapId).pipe(
            map(res => res.entry),
            // fake entity's `id` property to enforce internal application entity uniformity
            map((item: TagmapEntry<Raw>) => Object.assign(item, { id: `${item.id}` })),
            tap(contentRepository => this.entityManager.addEntity('tagmapEntry', contentRepository)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Create a contentRepository entry.
     * @param contentRepositoryId parentId
     * @param payload Tagmap Entry data
     * @returns created entity
     */
    create(
        contentRepositoryId: string,
        payload: TagmapEntryCreateRequest,
    ): Observable<TagmapEntryBO<Normalized>> {
        return this.api.contentrepositories.createContentRepositoryTagmapEntry(contentRepositoryId, payload).pipe(
            map((response: TagmapEntryCreateResponse) => response.entry),
            // fake entity's `id` property to enforce internal application entity uniformity
            map((item: TagmapEntry<Raw>) => Object.assign(item, { id: `${item.id}` })),
            tap((item: TagmapEntryBO<Raw>) => this.entityManager.addEntity('tagmapEntry', item)),
            tap((item: TagmapEntryBO<Raw>) => {
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_created',
                    translationParams: { name: item.tagname },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change a single tagmap entry.
     * @param contentRepositoryId parentId
     * @param tagmapId child Id
     * @param payload Tagmap Entry data
     */
    update(
        contentRepositoryId: string,
        tagmapId: string | number,
        payload: TagmapEntryUpdateRequest,
    ): Observable<TagmapEntryBO<Normalized>> {
        return this.api.contentrepositories.updateContentRepositoryTagmapEntry(contentRepositoryId, tagmapId, payload).pipe(
            map((res: TagmapEntryUpdateResponse) => res.entry),
            // fake entity's `id` property to enforce internal application entity uniformity
            map((item: TagmapEntry<Raw>) => Object.assign(item, { id: `${item.id}` })),
            // update state with server response
            tap((item: TagmapEntryBO<Raw>) => this.entityManager.addEntity('tagmapEntry', item)),
            // display toast notification
            tap((item: TagmapEntryBO<Raw>) => {
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_updated',
                    translationParams: { name: item.tagname },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Delete a single contentRepository entry.
     * @param contentRepositoryId parentId
     * @param tagmapId child Id
     */
    delete(
        contentRepositoryId: string,
        tagmapId: string,
    ): Observable<void> {
        return this.api.contentrepositories.deleteContentRepositoryTagmapEntry(contentRepositoryId, tagmapId).pipe(
            // display toast notification
            tap(() => {
                this.entityManager.getEntity('tagmapEntry', tagmapId)
                    .toPromise()
                    .then((item: TagmapEntryBO<Normalized>) => {
                        return this.notification.show({
                            type: 'success',
                            message: 'shared.item_singular_deleted',
                            translationParams: { name: item.tagname },
                        });
                    })
                    // remove entity from state
                    .then(() => this.entityManager.deleteEntities('tagmapEntry', [tagmapId]));
            }),
            this.catchAndRethrowError(),
        );
    }

}

import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, TableLoadOptions, TagMapEntryBO } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { TagmapEntry, TagmapEntryListResponse, TagmapEntryParentType } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable, throwError } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../entity-manager';

export interface TagMapEntryTableLoaderOptions {
    parentType: TagmapEntryParentType;
    parentId: string | number;
}

@Injectable()
export class TagMapEntryTableLoaderService extends BaseTableLoaderService<TagmapEntry, TagMapEntryBO, TagMapEntryTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
    ) {
        super('tagmapEntry', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number, options?: TagMapEntryTableLoaderOptions): Promise<void> {
        try {
            this.validateOptions(options);
        } catch (err) {
            return Promise.reject(err);
        }

        if (options.parentType === 'contentRepository') {
            return this.api.contentrepositories.deleteContentRepositoryTagmapEntry(options.parentId, entityId)
                .toPromise();
        } else {
            return this.api.contentRepositoryFragments.deleteContentRepositoryFragmentTagmapEntry(options.parentId, entityId)
                .toPromise();
        }
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: TagMapEntryTableLoaderOptions,
    ): Observable<EntityPageResponse<TagMapEntryBO>> {
        try {
            this.validateOptions(additionalOptions);
        } catch (err) {
            return throwError(err);
        }

        const loadOptions = this.createDefaultOptions(options);
        let loader: Observable<TagmapEntryListResponse>;

        if (additionalOptions?.parentType === 'contentRepository') {
            loader = this.api.contentrepositories.getContentRepositoryTagmapEntries(additionalOptions.parentId, loadOptions);
        } else {
            loader = this.api.contentRepositoryFragments.getContentRepositoryFragmentTagmapEntries(additionalOptions.parentId, loadOptions);
        }

        return loader.pipe(
            map(response => {
                const entities = response.items.map(entry => this.mapToBusinessObject(entry));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    protected validateOptions(options: TagMapEntryTableLoaderOptions): void {
        if (!options?.parentId || !options.parentType) {
            throw Error('TagMapLoader requires a parent-id and parent-type to be set/provided!');
        }

        if (options.parentType !== 'contentRepository' && options.parentType !== 'contentRepositoryFragment') {
            // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
            throw Error(`Invalid parent-type "${options.parentType}" provided!`);
        }
    }

    public mapToBusinessObject(entry: TagmapEntry): TagMapEntryBO {
        return {
            ...entry,
            [BO_ID]: String(entry.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: entry.mapname,
        };
    }
}

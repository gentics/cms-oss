import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, ContentRepositoryBO, discard, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ContentRepository, ContentRepositoryListResponse } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../entity-manager';
import { ContentRepositoryOperations } from '../operations';

export interface ContentRepositoryTableLoaderOptions {
    packageName?: string;
}

@Injectable()
export class ContentRepositoryTableLoaderService
    extends BaseTableLoaderService<ContentRepository, ContentRepositoryBO, ContentRepositoryTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: ContentRepositoryOperations,
    ) {
        super('contentRepository', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.operations.delete(entityId).pipe(discard()).toPromise();
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions: ContentRepositoryTableLoaderOptions,
    ): Observable<EntityPageResponse<ContentRepositoryBO>> {
        const loadOptions = this.createDefaultOptions(options);
        let loader: Observable<ContentRepositoryListResponse>;

        if (additionalOptions?.packageName) {
            loader = this.api.devTools.getContentrepositories(additionalOptions.packageName, loadOptions);
        } else {
            loader = this.api.contentrepositories.getContentrepositories(loadOptions);
        }

        return loader.pipe(
            map(response => {
                const entities = response.items.map(cr => this.mapToBusinessObject(cr));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        )
    }

    public mapToBusinessObject(cr: ContentRepository): ContentRepositoryBO {
        return {
            ...cr,
            [BO_ID]: String(cr.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: cr.name,
        };
    }

}

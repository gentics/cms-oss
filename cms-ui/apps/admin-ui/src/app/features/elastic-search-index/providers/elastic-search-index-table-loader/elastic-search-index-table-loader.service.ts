import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, ElasticSearchIndexBO, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ElasticSearchIndex, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable()
export class ElasticSearchIndexTableLoaderService extends BaseTableLoaderService<ElasticSearchIndex, ElasticSearchIndexBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
    ) {
        super('elasticSearchIndex', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(false);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return Promise.reject(new Error('Deletion of indicies is not supported/allowed!'));
    }

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<ElasticSearchIndexBO>> {
        const loadOptions = this.createDefaultOptions(options);

        return this.api.elasticSearchIndex.getItems(loadOptions).pipe(
            map(response => {
                const entities = response.items.map(index => this.mapToBusinessObject(index));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(index: ElasticSearchIndex<Raw>): ElasticSearchIndexBO {
        return {
            ...index,
            [BO_ID]: index.name,
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: index.name,
        };
    }
}

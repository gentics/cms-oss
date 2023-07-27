import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, DataSourceBO, discard, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { DataSource, DataSourceListResponse, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { DataSourceHandlerService } from '../data-source-handler/data-source-handler.service';
import { EntityManagerService } from '../entity-manager';

export interface DataSourceTableLoaderOptions {
    packageName?: string;
}

@Injectable()
export class DataSourceTableLoaderService extends BaseTableLoaderService<DataSource<Raw>, DataSourceBO, DataSourceTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected handler: DataSourceHandlerService,
    ) {
        super('dataSource', entityManager, appState);
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: DataSourceTableLoaderOptions,
    ): Observable<EntityPageResponse<DataSourceBO>> {
        const loadOptions = this.createDefaultOptions(options);
        let loader: Observable<DataSourceListResponse>;

        if (additionalOptions?.packageName) {
            loader = this.api.devTools.getDataSources(additionalOptions.packageName, loadOptions);
        } else {
            loader = this.api.dataSource.getDataSources(loadOptions);
        }

        return loader.pipe(
            map(response => {
                const entities = response.items.map(ds => this.mapToBusinessObject(ds));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.handler.delete(Number(entityId)).pipe(discard()).toPromise();
    }

    protected mapToBusinessObject(ds: DataSource<Raw>): DataSourceBO {
        return {
            ...ds,
            [BO_ID]: String(ds.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: ds.name,
        };
    }

}

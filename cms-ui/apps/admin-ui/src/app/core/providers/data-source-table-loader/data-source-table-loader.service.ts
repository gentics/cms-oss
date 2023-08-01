import { DataSourceBO, discard, EntityList, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { DataSource, Raw } from '@gentics/cms-models';
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
        let loader: Observable<EntityList<DataSourceBO>>;

        if (additionalOptions?.packageName) {
            loader = this.handler.listFromDevtoolMapped(additionalOptions.packageName, null as never, loadOptions);
        } else {
            loader = this.handler.listMapped(null as never, loadOptions);
        }

        return loader.pipe(
            map(response => {
                return {
                    entities: response.items,
                    totalCount: response.totalItems,
                };
            }),
        );
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.handler.delete(entityId).pipe(discard()).toPromise();
    }
}

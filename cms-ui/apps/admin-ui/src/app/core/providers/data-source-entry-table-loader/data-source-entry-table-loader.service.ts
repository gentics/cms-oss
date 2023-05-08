import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_PERMISSIONS,
    DataSourceEntryBO,
    BO_NEW_SORT_ORDER,
    BO_ORIGINAL_SORT_ORDER,
    EntityPageResponse,
    TableLoadOptions,
    discard,
} from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { DataSourceEntry, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { TableRow } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../entity-manager';
import { DataSourceEntryOperations } from '../operations';

export interface DataSourceEntryTableLoaderOptions {
    dataSourceId: string | number;
}

@Injectable()
export class DataSourceEntryTableLoaderService
    extends BaseTableLoaderService<DataSourceEntry<Raw>, DataSourceEntryBO, DataSourceEntryTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: DataSourceEntryOperations,
    ) {
        super('dataSourceEntry', entityManager, appState);
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions: DataSourceEntryTableLoaderOptions,
    ): Observable<EntityPageResponse<DataSourceEntryBO>> {
        // No pagination available

        return this.api.dataSource.getEntries(additionalOptions.dataSourceId).pipe(
            map(response => {
                const entities = response.items.map((entry, index) => this.mapToBusinessObject(entry, index));

                return  {
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
        return Promise.reject('Use #deleteEntry instead!');
    }

    public deleteEntry(dsId: string | number, dsEntryId: string | number): Promise<void> {
        return this.operations.delete(String(dsEntryId), String(dsId)).pipe(discard()).toPromise();
    }

    public mapToBusinessObject(entry: DataSourceEntry<Raw>, index: number): DataSourceEntryBO {
        return {
            ...entry,
            [BO_ID]: String(entry.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: entry.key,
            [BO_ORIGINAL_SORT_ORDER]: index,
            [BO_NEW_SORT_ORDER]: index,
        };
    }

    public override mapToTableRow(bo: DataSourceEntryBO): TableRow<DataSourceEntryBO> {
        return {
            id: bo[BO_ID],
            hash: bo[BO_NEW_SORT_ORDER],
            item: bo,
        }
    }
}

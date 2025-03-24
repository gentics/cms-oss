import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_NEW_SORT_ORDER,
    BO_ORIGINAL_SORT_ORDER,
    BO_PERMISSIONS,
    DataSourceEntryBO,
    EntityPageResponse,
    TableLoadOptions,
} from '@admin-ui/common';
import { EntityManagerService } from '@admin-ui/core';
import { BaseTableLoaderService, DataSourceEntryHandlerService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { DataSourceEntry, Raw } from '@gentics/cms-models';
import { TableRow } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface DataSourceEntryTableLoaderOptions {
    dataSourceId: string | number;
}

@Injectable()
export class DataSourceEntryTableLoaderService
    extends BaseTableLoaderService<DataSourceEntry<Raw>, DataSourceEntryBO, DataSourceEntryTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected handler: DataSourceEntryHandlerService,
    ) {
        super('dataSourceEntry', entityManager, appState);
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions: DataSourceEntryTableLoaderOptions,
    ): Observable<EntityPageResponse<DataSourceEntryBO>> {
        // No pagination available

        return this.handler.listMapped(additionalOptions.dataSourceId).pipe(
            map(response => {
                const entities = response.items.map((entry, index) => this.mapToBusinessObject(entry, index));

                return  {
                    entities,
                    totalCount: response.totalItems,
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

    public deleteEntry(dsId: string | number, entryId: string | number): Promise<void> {
        return this.handler.delete(dsId, entryId).toPromise();
    }

    public mapToBusinessObject(entry: DataSourceEntry<Raw>, index: number): DataSourceEntryBO {
        return {
            ...entry,
            [BO_ID]: String(entry.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: this.handler.displayName(entry),
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

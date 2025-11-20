/* eslint-disable @typescript-eslint/no-unused-vars */
import { EntityList } from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import {
    DataSourceEntry,
    DataSourceEntryCreateRequest,
    DataSourceEntryCreateResponse,
    DataSourceEntryListResponse,
    DataSourceEntryListUpdateRequest,
    DataSourceEntryListUpdateResponse,
    DataSourceEntryLoadResponse,
    Raw,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';

@Injectable()
export class DataSourceEntryHandlerService extends BaseEntityHandlerService {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: DataSourceEntry<Raw>): string {
        return entity.key;
    }

    create(
        datasourceId: string | number,
        body: DataSourceEntryCreateRequest,
        params?: never,
    ): Observable<DataSourceEntryCreateResponse> {
        return this.api.dataSource.createEntry(datasourceId, body).pipe(
            tap((res) => {
                const name = this.displayName(res.entry);
                this.nameMap[res.entry.id] = name;

                this.notification.show({
                    type: 'success',
                    message: 'shared.item_created',
                    translationParams: {
                        name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    createMapped(
        datasourceId: string | number,
        body: DataSourceEntryCreateRequest,
        params?: never,
    ): Observable<DataSourceEntry<Raw>> {
        return this.create(datasourceId, body, params).pipe(
            map((res) => res.entry),
        );
    }

    get(
        datasourceId: string | number,
        entryId: string | number,
        params?: never,
    ): Observable<DataSourceEntryLoadResponse> {
        return this.api.dataSource.getEntry(datasourceId, entryId).pipe(
            tap((res) => {
                const name = this.displayName(res.entry);
                this.nameMap[res.entry.id] = name;
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(
        datasourceId: string | number,
        entryId: string | number,
        params?: never,
    ): Observable<DataSourceEntry<Raw>> {
        return this.get(datasourceId, entryId, params).pipe(
            map((res) => res.entry),
        );
    }

    updateAll(
        datasourceId: string | number,
        body: DataSourceEntryListUpdateRequest,
        params?: never,
    ): Observable<DataSourceEntryListUpdateResponse> {
        return this.api.dataSource.updateEntries(datasourceId, body).pipe(
            tap(() => {
                // TODO: Show update message for DS instead?
                this.notification.show({
                    type: 'success',
                    message: 'data_source_entry.rearranged',
                    translationParams: { },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    updateAllMapped(
        datasourceId: string | number,
        body: DataSourceEntryListUpdateRequest,
        params?: never,
    ): Observable<EntityList<DataSourceEntry<Raw>>> {
        return this.updateAll(datasourceId, body, params).pipe(
            map((res) => ({
                items: res.items,
                totalItems: res.numItems,
            })),
        );
    }

    delete(
        datasourceId: string | number,
        entryId: string | number,
        params?: never,
    ): Observable<void> {
        return this.api.dataSource.deleteEntry(datasourceId, entryId).pipe(
            tap(() => {
                const name = this.nameMap[entryId];

                if (!name) {
                    return;
                }

                delete this.nameMap[entryId];
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_singular_deleted',
                    translationParams: {
                        name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    list(
        datasourceId: string | number,
        body?: never,
        params?: never,
    ): Observable<DataSourceEntryListResponse> {
        return this.api.dataSource.getEntries(datasourceId).pipe(
            tap((res) => {
                res.items.forEach((entry) => {
                    const name = this.displayName(entry);
                    this.nameMap[entry.id] = name;
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listMapped(
        datasourceId: string | number,
        body?: never,
        params?: never,
    ): Observable<EntityList<DataSourceEntry<Raw>>> {
        return this.list(datasourceId, body, params).pipe(
            map((res) => ({
                items: res.items,
                totalItems: res.numItems,
            })),
        );
    }
}

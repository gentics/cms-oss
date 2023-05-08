import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import {
    DataSourceEntry,
    DataSourceEntryBO,
    DataSourceEntryCreateRequest, DataSourceEntryListUpdateRequest, ModelType,
    Raw,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class DataSourceEntryOperations extends ExtendedEntityOperationsBase<'dataSourceEntry'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private appState: AppStateService,
        private notification: I18nNotificationService,
    ) {
        super(injector, 'dataSourceEntry');
    }

    /**
     * Get a list of all entries of a dataSource.
     * @param options - Set to null.
     * @param dataSourceId - Although ExtendedEntityOperationsBase suggest optionality, this argument must be supplied
     */
    getAll(options?: null, dataSourceId?: string): Observable<DataSourceEntryBO<Raw>[]> {
        if (!dataSourceId) {
            throw new Error('Invalid Argument: DataSource ID has to be provided');
        }

        return this.api.dataSource.getEntries(dataSourceId).pipe(
            map(res => res.items.map(entry => this.mapToBusinessObject(entry))),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single entry of a dataSource.
     * @param dataSourceEntryId - Entry ID.
     * @param options - Set to null.
     * @param dataSourceId - Although EntityOperationsBase suggest optionality, this argument must be supplied
     */
    get(dataSourceEntryId: string, options?: null, dataSourceId?: string): Observable<DataSourceEntryBO<Raw>> {
        if (!dataSourceId) {
            throw new Error('Invalid Argument: DataSource ID has to be provided');
        }

        return this.api.dataSource.getEntry(dataSourceId, dataSourceEntryId).pipe(
            map(res => this.mapToBusinessObject(res.entry)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Create a dataSource entry.
     */
    create(dataSourceEntry: DataSourceEntryCreateRequest, dataSourceId: string, notification: boolean = true): Observable<DataSourceEntryBO<Raw>> {
        return this.api.dataSource.createEntry(dataSourceId, dataSourceEntry).pipe(
            map(res => this.mapToBusinessObject(res.entry)),
            tap(dataSource => {
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_created',
                        translationParams: { name: dataSource.key },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
     }

    /**
     * Change dataSource entries in the given order
     */
    updateAll(
        dataSourceEntries: DataSourceEntryListUpdateRequest,
        dataSourceId: string,
        notification: boolean = true,
    ): Observable<DataSourceEntryBO<Raw>[]> {
        return this.api.dataSource.updateEntries(dataSourceId, dataSourceEntries).pipe(
            map(res => res.items.map(entry => this.mapToBusinessObject(entry))),
            // display toast notification
            tap(() => {
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'dataSourceEntry.rearranged',
                        translationParams: { },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Delete a single dataSource entry
     */
    delete(dataSourceEntryId: string, dataSourceId: string, notification: boolean = true): Observable<Response | void> {
        const dataSourceEntryToBeDeleted: DataSourceEntryBO = this.appState.now.entity.dataSourceEntry[dataSourceEntryId];
        return this.api.dataSource.deleteEntry(dataSourceId, dataSourceEntryId).pipe(
            // display toast notification
            tap(() => {
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_singular_deleted',
                        translationParams: {
                            name: dataSourceEntryToBeDeleted?.key,
                        },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    private mapToBusinessObject<T extends ModelType>(entry: DataSourceEntry<T>): DataSourceEntryBO<T> {
        return {
            ...entry,
            id: entry.globalId,
        };
    }
}

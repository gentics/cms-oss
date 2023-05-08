import { PackageEntityOperations } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import {
    DataSource,
    DataSourceBO,
    DataSourceCreateRequest,
    DataSourceListOptions,
    DataSourceUpdateRequest,
    ModelType,
    Raw,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class DataSourceOperations
    extends ExtendedEntityOperationsBase<'dataSource'>
    implements PackageEntityOperations<DataSourceBO<Raw>>
{

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entities: EntityManagerService,
        private appState: AppStateService,
        private notification: I18nNotificationService,
    ) {
        super(injector, 'dataSource');
    }

    /**
     * Get a list of all dataSources and adds them to the AppState.
     */
    getAll(options?: DataSourceListOptions): Observable<DataSourceBO<Raw>[]> {
        return this.api.dataSource.getDataSources(options).pipe(
            map(res => res.items.map(item => this.mapToBusinessObject(item))),
            tap(dataSources => {
                this.entities.addEntities(this.entityIdentifier, dataSources);
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single dataSource and add it to the AppState.
     */
    get(dataSourceId: string): Observable<DataSourceBO<Raw>> {
        return this.api.dataSource.getDataSource(dataSourceId).pipe(
            map(res => this.mapToBusinessObject(res.datasource)),
            tap(dataSource => {
                this.entities.addEntity(this.entityIdentifier, dataSource);
            }),
            this.catchAndRethrowError(),
        );
    }

    getAllFromPackage(packageId: string, options?: any): Observable<DataSourceBO<ModelType.Raw>[]> {
        return this.api.devTools.getDataSources(packageId, options).pipe(
            map(res => res.items.map(item => this.mapToBusinessObject(item))),
            this.catchAndRethrowError(),
        );
    }

    getFromPackage(packageId: string, entityId: string): Observable<DataSourceBO<ModelType.Raw>> {
        return this.api.devTools.getDataSource(packageId, entityId).pipe(
            map(res => this.mapToBusinessObject(res.datasource)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Create a dataSource.
     */
    create(dataSource: DataSourceCreateRequest, notify: boolean = true): Observable<DataSourceBO<Raw>> {
        return this.api.dataSource.createDataSource(dataSource).pipe(
            map(response => this.mapToBusinessObject(response.datasource)),
            tap(dataSource => {
                this.entities.addEntity(this.entityIdentifier, dataSource);

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_created',
                        translationParams: {
                            name: dataSource.name,
                        },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change a single dataSource
     */
    update(dataSourceId: string, payload: DataSourceUpdateRequest, notify: boolean = true): Observable<DataSourceBO<Raw>> {
        return this.api.dataSource.updateDataSource(dataSourceId, payload).pipe(
            map(res => this.mapToBusinessObject(res.datasource)),
            tap(dataSource => {
                this.entities.addEntity(this.entityIdentifier, dataSource);

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_updated',
                        translationParams: {
                            name: dataSource.name,
                        },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Delete a single dataSource
     */
    delete(dataSourceId: number, notify: boolean = true): Observable<Response | void> {
        const dataSourceToBeDeleted = this.appState.now.entity.dataSource[dataSourceId];

        return this.api.dataSource.deleteDataSource(dataSourceId).pipe(
            tap(() => {
                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_singular_deleted',
                        translationParams: {
                            name: dataSourceToBeDeleted.name,
                        },
                    });
                }

                this.entities.deleteEntities(this.entityIdentifier, [dataSourceId])
            }),
            this.catchAndRethrowError(),
        );
    }

    public mapToBusinessObject<T extends ModelType>(dataSource: DataSource<T>): DataSourceBO<T> {
        return {
            ...dataSource,
            id: dataSource.id.toString(),
        };
    }

}

import { Injectable, Injector } from '@angular/core';
import {
    ElasticSearchIndex,
    ElasticSearchIndexListResponse,
    Raw,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class ElasticSearchIndexOperations extends ExtendedEntityOperationsBase<'elasticSearchIndex'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entities: EntityManagerService,
        private notification: I18nNotificationService,
    ) {
        super(injector, 'elasticSearchIndex');
    }

    /**
     * Gets a list of all Elastic Search indices and adds them to the AppState.
     */
    getAll(): Observable<ElasticSearchIndex<Raw>[]> {
        return this.api.elasticSearchIndex.getItems().pipe(
            map((res: ElasticSearchIndexListResponse) => res.items),
            // equip entity with ID to enforce conformity
            map(items => items.map(item => {
                item.id = item.name;
                return item;
            })),
            tap((items: ElasticSearchIndex<Raw>[]) => this.entities.addEntities('elasticSearchIndex', items)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Schedule rebuilding of index
     * @param indexName identifier of the index
     * @param drop if true, the index shall be dropped first
     */
    rebuild(indexName: string, drop?: boolean): Observable<ElasticSearchIndex<Raw>[]> {
        return this.api.elasticSearchIndex.rebuild(indexName, drop).pipe(
            switchMap(() => this.getAll()),
            // update state with server response
            tap((items: ElasticSearchIndex<Raw>[]) => {
                this.entities.addEntities('elasticSearchIndex', items);
                // display toast notification
                const i18nMessage = drop ? 'elasticSearchIndex.index_delete_and_rebuild_success' : 'elasticSearchIndex.index_rebuild_success';
                this.notification.show({
                    type: 'success',
                    message: i18nMessage,
                    translationParams: { name: indexName },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /** NOT IMPLEMENTED! This method does not exist by REST API definition but is defined as an abstract class. */
    get(): Observable<ElasticSearchIndex<Raw>> {
        throw new Error('Not implemented. This method does not exist by REST API definition but is defined as an abstract class.');
    }

}

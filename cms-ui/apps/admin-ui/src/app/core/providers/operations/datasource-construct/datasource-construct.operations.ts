import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import {
    DataSource,
    DataSourceBO,
    DataSourceConstructListOptions,
    DataSourceConstructListResponse,
    Raw,
    TagTypeBO,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class DataSourceConstructOperations extends ExtendedEntityOperationsBase<'construct'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entities: EntityManagerService,
        private appState: AppStateService,
        private notification: I18nNotificationService,
    ) {
        super(injector, 'construct');
    }


    /**
     * Get a list of all constructs a dataSource is used in.
     * @param options - Options for construct lists.
     * @param dataSourceId - Although ExtendedEntityOperationsBase suggest optionality, this argument must be supplied
     */
    getAll(options?: DataSourceConstructListOptions, dataSourceId?: string): Observable<TagTypeBO<Raw>[]> {
        if (!dataSourceId) {
            throw new Error('Invalid Argument: DataSource ID has to be provided');
        }
        return this.api.dataSource.getConstructs(dataSourceId, options).pipe(
            map((res: DataSourceConstructListResponse) => {
                // fake entity's `id` property to enforce internal application entity uniformity
                return res.items.map(item => Object.assign(item, { id: item.globalId }) as TagTypeBO<Raw>);
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single entry of a construct used in a dataSource is not implemented.
     */
    get(dataSourceConstructId: string, options: null, dataSourceId: string): Observable<TagTypeBO<Raw>> {
        throw new Error('Not implemented');
    }



}

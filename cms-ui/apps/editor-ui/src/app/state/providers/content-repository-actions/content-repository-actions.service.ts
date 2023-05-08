import { Injectable } from '@angular/core';
import {
    ContentRepository,
    ContentRepositoryListOptions,
    ContentRepositoryListResponse,
} from '@gentics/cms-models';
import { normalize, schema } from 'normalizr';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { contentRepositorySchema } from '../../../common/models';
import { Api } from '../../../core/providers/api';
import { ApplicationStateService } from '../application-state/application-state.service';
import { AddEntitiesAction } from '../../modules';

@Injectable()
export class ContentRepositoryActionsService {

    constructor(
        private appState: ApplicationStateService,
        private api: Api,
    ) {}

    async fetchAllContentrepositories(): Promise<boolean> {

        // option '-1' to get infinite pagesize and all existing items
        return this.getContentrepositories({ pageSize: -1 })
            .toPromise()
            .then(response => {
                const normalizedContentrepositories = normalize(response, new schema.Array(contentRepositorySchema));
                this.appState.dispatch(new AddEntitiesAction(normalizedContentrepositories));
                return true;
            })
            .catch(() => false);
    }

    /**
     * Fetches contentrepositories
     */
    private getContentrepositories(options?: ContentRepositoryListOptions): Observable<ContentRepository[]> {
        return this.api.contentrepositories.getContentrepositories(options).pipe(
            map((response: ContentRepositoryListResponse) => response.items),
        );
    }

}

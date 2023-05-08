import {
    ElasticSearchIndexListOptions,
    ElasticSearchIndexListResponse,
    Response,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * API methods related to the Elastic Search index resource.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/guides/restapi/resource_SearchIndexResource.html
 *
 */
export class ElasticSearchIndexApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    /**
     * List all required search indices
     */
    getItems(options?: ElasticSearchIndexListOptions): Observable<ElasticSearchIndexListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get('index', options);
    }

    /**
     * Schedule rebuilding of index
     */
    rebuild(indexName: string, drop?: boolean): Observable<Response> {
        const queryParams = { drop: drop || false };
        return this.apiBase.put(`index/${indexName}/rebuild`, {}, queryParams);
    }

}

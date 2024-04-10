import {
    DataSourceConstructListOptions,
    DataSourceConstructListResponse,
    DataSourceCreateRequest,
    DataSourceCreateResponse,
    DataSourceEntryCreateRequest,
    DataSourceEntryCreateResponse,
    DataSourceEntryListResponse,
    DataSourceEntryListUpdateRequest,
    DataSourceEntryListUpdateResponse,
    DataSourceEntryLoadResponse,
    DataSourceListOptions,
    DataSourceListResponse,
    DataSourceLoadResponse,
    DataSourceUpdateRequest,
    DataSourceUpdateResponse,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * API methods related to the content repository resource.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/resource_ContentRepositoryFragmentResource.html
 */
export class DataSourceApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    /**
     * Get a list of datasources.
     */
    getDataSources(options?: DataSourceListOptions): Observable<DataSourceListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get('datasource', options);
    }

    /**
     * Get a single datasource by id.
     */
    getDataSource(dsId: string | number): Observable<DataSourceLoadResponse> {
        return this.apiBase.get(`datasource/${dsId}`);
    }

    /**
     * Create a new datasource.
     */
    createDataSource(request: DataSourceCreateRequest): Observable<DataSourceCreateResponse> {
        return this.apiBase.post('datasource', request);
    }

    /**
     * Update a single datasource by id.
     */
    updateDataSource(datasourceId: string | number, request: DataSourceUpdateRequest): Observable<DataSourceUpdateResponse> {
        return this.apiBase.put(`datasource/${datasourceId}`, request);
    }

    /**
     * Delete a single datasource by id.
     */
    deleteDataSource(datasourceId: string | number): Observable<void> {
        return this.apiBase.delete(`datasource/${datasourceId}`);
    }

    /**
     * Get a list of entries.
     */
    getEntries(datasourceId: string | number): Observable<DataSourceEntryListResponse> {
        return this.apiBase.get(`datasource/${datasourceId}/entries`);
    }

    /**
     * Get a single entry by id.
     */
    getEntry(datasourceId: string | number, datasourceEntryId: string | number): Observable<DataSourceEntryLoadResponse> {
        return this.apiBase.get(`datasource/${datasourceId}/entries/${datasourceEntryId}`);
    }

    /**
     * Create a new entry.
     */
    createEntry(datasourceId: string | number, request: DataSourceEntryCreateRequest): Observable<DataSourceEntryCreateResponse> {
        return this.apiBase.post(`datasource/${datasourceId}/entries`, request);
    }

    /**
     * Update a single entry by id.
     */
    updateEntries(datasourceId: string | number, request: DataSourceEntryListUpdateRequest): Observable<DataSourceEntryListUpdateResponse> {
        return this.apiBase.put(`datasource/${datasourceId}/entries`, request);
    }

    /**
     * Delete a single entry by id.
     */
    deleteEntry(datasourceId: string | number, datasourceEntryId: string | number): Observable<void> {
        return this.apiBase.delete(`datasource/${datasourceId}/entries/${datasourceEntryId}`);
    }

    /**
     * Get a list of constructs.
     */
    getConstructs(datasourceId: string | number, options?: DataSourceConstructListOptions): Observable<DataSourceConstructListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get(`datasource/${datasourceId}/constructs`, options);
    }

}

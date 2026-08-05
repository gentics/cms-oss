import { ApiTokenCreateRequest, ApiTokenListResponse, ApiTokenResponse, EmbeddedToolsResponse, Feature, FeatureResponse, UsersnapSettingsResponse, VersionResponse } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods for Api Tokens.
 */
export class ApiTokenApi {

    constructor(private apiBase: ApiBase) {}

    getAll(): Observable<ApiTokenListResponse> {
        return this.apiBase.get('admin/token');
    }

     /**
     * Create a new entry.
     */
    createEntry(request: ApiTokenCreateRequest): Observable<ApiTokenResponse> {
        return this.apiBase.post(`admin/token`, request);
    }

    /**
     * Delete a single entry by id.
     */
    deleteEntry(id: string | number): Observable<void> {
        return this.apiBase.delete(`admin/token/${id}`);
    }
}
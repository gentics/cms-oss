import { EmbeddedToolsResponse, Feature, FeatureResponse, UsersnapSettingsResponse, VersionResponse } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods related to admin.
 *
 * Docs for the endpoints used here can be found at:
 * http://www.gentics.com/Content.Node/cmp8/guides/restapi/resource_AdminResource.html
 */
export class AdminApi {

    constructor(private apiBase: ApiBase) {}

    /** Get a list of embedded tools available to the current user */
    getAvailableEmbeddedTools(): Observable<EmbeddedToolsResponse> {
        return this.apiBase.get('admin/tools');
    }

    /** Get the status of a feature */
    getFeature(key: Feature): Observable<FeatureResponse> {
        return this.apiBase.get(`admin/features/${key}`);
    }

    /** Get the backend cms version */
    getVersion(): Observable<VersionResponse> {
        return this.apiBase.get('admin/version');
    }

    getUsersnapSettings(): Observable<UsersnapSettingsResponse> {
        return this.apiBase.get('usersnap');
    }

}

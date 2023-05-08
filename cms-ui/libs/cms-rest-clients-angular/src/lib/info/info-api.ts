import { MaintenanceModeResponse } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods related to info.
 *
 * Docs for the endpoints used here can be found at:
 * http://www.gentics.com/Content.Node/guides/restapi/resource_InfoResource.html
 */
export class InfoApi {

    constructor(private apiBase: ApiBase) {}

    /**
     * Get the status of the maintenance mode.
     * Does not need the user to be logged in.
     */
    getMaintenanceModeStatus(): Observable<MaintenanceModeResponse> {
        return this.apiBase.get('info/maintenance');
    }
}

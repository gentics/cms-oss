import {
    EntityIdType,
    PortalImplementationListOptions,
    PortalImplementationListResponse,
    PortalImplementationResponse,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods related to the portal implementation resource (`/portalimplementations`).
 */
export class PortalImplementationApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    /**
     * Get a list of portal implementations.
     */
    getPortalImplementations(options?: PortalImplementationListOptions): Observable<PortalImplementationListResponse> {
        return this.apiBase.get('portalimplementations', options);
    }

    /**
     * Get a single portal implementation by id.
     */
    getPortalImplementation(portalImplementationId: EntityIdType): Observable<PortalImplementationResponse> {
        return this.apiBase.get(`portalimplementations/${portalImplementationId}`);
    }
}

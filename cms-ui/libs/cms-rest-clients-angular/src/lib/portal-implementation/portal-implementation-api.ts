import {
    EntityIdType,
    NodeListResponse,
    PortalImplementationCreateRequest,
    PortalImplementationFileListOptions,
    PortalImplementationFileListResponse,
    PortalImplementationListOptions,
    PortalImplementationListResponse,
    PortalImplementationResponse,
    PortalImplementationUpdateRequest,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

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
        return this.apiBase.get('portalimplementations', this.stringifySort(options));
    }

    /**
     * Get a single portal implementation by id.
     */
    getPortalImplementation(portalImplementationId: EntityIdType): Observable<PortalImplementationResponse> {
        return this.apiBase.get(`portalimplementations/${portalImplementationId}`);
    }

    /**
     * Create a new portal implementation.
     */
    createPortalImplementation(payload: PortalImplementationCreateRequest): Observable<PortalImplementationResponse> {
        return this.apiBase.post('portalimplementations', payload);
    }

    /**
     * Update the portal implementation.
     */
    updatePortalImplementation(portalImplementationId: EntityIdType, payload: PortalImplementationUpdateRequest): Observable<PortalImplementationResponse> {
        return this.apiBase.put(`portalimplementations/${portalImplementationId}`, payload);
    }

    /**
     * Delete the portal implementation (including all its files).
     */
    deletePortalImplementation(portalImplementationId: EntityIdType): Observable<void> {
        return this.apiBase.delete(`portalimplementations/${portalImplementationId}`);
    }

    /**
     * Get the nodes, the portal implementation is assigned to.
     * The assignment is changed via the node API (`node/{nodeId}/portalimplementations/{id}`).
     */
    getPortalImplementationNodes(portalImplementationId: EntityIdType): Observable<NodeListResponse> {
        return this.apiBase.get(`portalimplementations/${portalImplementationId}/nodes`);
    }

    /**
     * Get the files contained in the portal implementation.
     */
    getPortalImplementationFiles(
        portalImplementationId: EntityIdType,
        options?: PortalImplementationFileListOptions,
    ): Observable<PortalImplementationFileListResponse> {
        return this.apiBase.get(`portalimplementations/${portalImplementationId}/files`, this.stringifySort(options));
    }

    /**
     * Transform the sort options into their string representation (if set)
     */
    private stringifySort<T extends { sort?: any }>(options?: T): T {
        if (options?.sort) {
            const copy: any = { ...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            return copy;
        }
        return options;
    }
}

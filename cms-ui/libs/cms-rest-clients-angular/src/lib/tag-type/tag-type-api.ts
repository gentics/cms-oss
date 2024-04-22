import {
    ConstructCreateOptions,
    ConstructCreateRequest,
    ConstructCreateResponse,
    ConstructLinkedNodesResponse,
    ConstructLoadResponse,
    ConstructNodeLinkRequest,
    ConstructNodeLinkResponse,
    ConstructUpdateRequest,
    ConstructUpdateResponse,
    EntityIdType,
    PagedConstructListRequestOptions,
    PermissionListResponse,
    SinglePermissionResponse,
    TagType,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * API methods related to TagTypes.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/resource_ConstructResource.html
 */
export class TagTypeApi {

    constructor(
        private apiBase: ApiBase,
    ) {}

    /**
     * Get all constructs.
     * **Important:** This method only works for Admin users.
     */
    getTagTypes(options?: PagedConstructListRequestOptions): Observable<PermissionListResponse<TagType>> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get('construct', options);
    }

    /**
     * Get the construct with the specified id.
     * **Important:** This method only works for Admin users.
     */
    getTagType(id: EntityIdType): Observable<ConstructLoadResponse> {
        return this.apiBase.get(`construct/${id}`);
    }

    getTagPermission(id: EntityIdType, permission: 'view' | 'edit' | 'delete'): Observable<SinglePermissionResponse> {
        return this.apiBase.get(`perm/${permission}/construct/${id}`);
    }

    /**
     * Create a new construct.
     */
    createTagType(
        payload: ConstructCreateRequest,
        params: ConstructCreateOptions,
    ): Observable<ConstructCreateResponse> {
        return this.apiBase.post('construct', payload, params);
    }

    /**
     * Update a single construct by id.
     */
    updateTagType(
        constructId: EntityIdType,
        payload: ConstructUpdateRequest,
    ): Observable<ConstructUpdateResponse> {
        return this.apiBase.put(`construct/${constructId}`, payload);
    }

    /**
     * Delete a single construct by id.
     */
    deleteTagType(
        constructId: EntityIdType,
    ): Observable<void> {
        return this.apiBase.delete(`construct/${constructId}`);
    }

    /**
     * Loads the nodes which are linked to the construct
     */
    getLinkedNodes(constructId: EntityIdType): Observable<ConstructLinkedNodesResponse> {
        return this.apiBase.get(`construct/${constructId}/nodes`);
    }

    linkTagToNode(payload: ConstructNodeLinkRequest): Observable<ConstructNodeLinkResponse> {
        return this.apiBase.post('construct/link/nodes', payload);
    }

    unlinkTagFromNode(payload: ConstructNodeLinkRequest): Observable<ConstructNodeLinkResponse> {
        return this.apiBase.post('construct/unlink/nodes', payload);
    }

}

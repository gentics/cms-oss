import {
    EntityIdType, NodeListResponse,
    ObjectPropertyCreateRequest,
    ObjectPropertyCreateResponse,
    ObjectPropertyListOptions,
    ObjectPropertyListResponse,
    ObjectPropertyLoadResponse,
    ObjectPropertyUpdateRequest,
    ObjectPropertyUpdateResponse,
    Response,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyEmbedOptions, stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * API methods related to the content repository resource.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/guides/restapi/resource_ObjectPropertyResource.html
 */
export class ObjectPropertyApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    /**
     * Get a list of objectproperties.
     */
    getObjectProperties(options?: ObjectPropertyListOptions): Observable<ObjectPropertyListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }
        stringifyEmbedOptions(options);

        return this.apiBase.get('objectproperty', options);
    }

    /**
     * Get a single contentrepository by id.
     */
    getObjectProperty(crId: string): Observable<ObjectPropertyLoadResponse> {
        return this.apiBase.get(`objectproperty/${crId}`, {});
    }

    /**
     * Create a new objectproperty.
     */
    createObjectProperty(
        payload: ObjectPropertyCreateRequest,
    ): Observable<ObjectPropertyCreateResponse> {
        return this.apiBase.post('objectproperty', payload);
    }

    /**
     * Update a single objectproperty by id.
     */
    updateObjectProperty(
        objectpropertyId: EntityIdType,
        payload: ObjectPropertyUpdateRequest,
    ): Observable<ObjectPropertyUpdateResponse> {
        return this.apiBase.put(`objectproperty/${objectpropertyId}`, payload);
    }

    /**
     * Delete a single objectproperty by id.
     */
    deleteObjectProperty(
        contentrepositoryId: EntityIdType,
    ): Observable<void> {
        return this.apiBase.delete(`objectproperty/${contentrepositoryId}`);
    }

    /**
     * Get nodes linked to an objectproperty.
     */
    getObjectPropertyLinkedNodes(objectpropertyId: EntityIdType): Observable<NodeListResponse> {
        return this.apiBase.get(`objectproperty/${objectpropertyId}/nodes`);
    }

    /**
     * Link objectproperties to nodes.
     */
    linkObjectPropertiesToNodes(objectpropertyIds: EntityIdType[], nodeIds: EntityIdType[]): Observable<Response> {
        return this.apiBase.post('objectproperty/link/nodes', {
            ids: nodeIds,
            targetIds: objectpropertyIds,
        });
    }

    /**
     * Unlink objectproperties from nodes.
     */
    unlinkObjectPropertiesFromNodes(objectpropertyIds: EntityIdType[], nodeIds: EntityIdType[]): Observable<Response> {
        return this.apiBase.post('objectproperty/unlink/nodes', {
            ids: nodeIds,
            targetIds: objectpropertyIds,
        });
    }

}

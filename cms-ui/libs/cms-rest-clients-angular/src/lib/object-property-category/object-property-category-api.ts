import {
    EntityIdType,
    ObjectPropertyCategoryCreateRequest,
    ObjectPropertyCategoryCreateResponse,
    ObjectPropertyCategoryListOptions,
    ObjectPropertyCategoryListResponse,
    ObjectPropertyCategoryLoadResponse,
    ObjectPropertyCategoryUpdateRequest,
    ObjectPropertyCategoryUpdateResponse,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods related to the content repository resource.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/resource_ObjectPropertyCategoryResource.html
 */
export class ObjectPropertyCategoryApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    /**
     * Get a list of objectproperties.
     */
    getObjectPropertyCategories(options?: ObjectPropertyCategoryListOptions): Observable<ObjectPropertyCategoryListResponse> {
        return this.apiBase.get('objectproperty/category', options);
    }

    /**
     * Get a single contentrepository by id.
     */
    getObjectPropertyCategory(itemId: number | string): Observable<ObjectPropertyCategoryLoadResponse> {
        return this.apiBase.get(`objectproperty/category/${itemId}`);
    }

    /**
     * Create a new objectpropertycategory.
     */
    createObjectPropertyCategory(
        payload: ObjectPropertyCategoryCreateRequest,
    ): Observable<ObjectPropertyCategoryCreateResponse> {
        return this.apiBase.post('objectproperty/category', payload);
    }

    /**
     * Update a single objectpropertycategory by id.
     */
    updateObjectPropertyCategory(
        objectpropertycategoryId: EntityIdType,
        payload: ObjectPropertyCategoryUpdateRequest,
    ): Observable<ObjectPropertyCategoryUpdateResponse> {
        return this.apiBase.put(`objectproperty/category/${objectpropertycategoryId}`, payload);
    }

    /**
     * Delete a single objectpropertycategory by id.
     */
    deleteObjectPropertyCategory(
        contentrepositoryId: EntityIdType,
    ): Observable<void> {
        return this.apiBase.delete(`objectproperty/category/${contentrepositoryId}`);
    }

}

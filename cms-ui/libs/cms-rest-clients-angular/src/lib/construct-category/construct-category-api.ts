import {
    ConstructCategoryCreateRequest,
    ConstructCategoryCreateResponse,
    ConstructCategoryListOptions,
    ConstructCategoryListResponse,
    ConstructCategoryLoadResponse,
    ConstructCategorySortRequest,
    ConstructCategoryUpdateRequest,
    ConstructCategoryUpdateResponse,
    EntityIdType,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * API methods related to ConstructCategoryCategories.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/guides/restapi/resource_ConstructCategoryResource.html
 */
export class ConstructCategoryApi {

    constructor(
        private apiBase: ApiBase,
    ) {}

    /**
     * Get all construct categories.
     * **Important:** This method only works for Admin users.
     */
    getConstructCategoryCategories(options: ConstructCategoryListOptions): Observable<ConstructCategoryListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get('construct/category', options);
    }

    /**
     * Get the construct with the specified id.
     * **Important:** This method only works for Admin users.
     */
    getConstructCategoryCategory(id: number | string): Observable<ConstructCategoryLoadResponse> {
        return this.apiBase.get(`construct/category/${id}`);
    }

    /**
     * Create a new construct.
     */
    createConstructCategoryCategory(
        payload: ConstructCategoryCreateRequest,
    ): Observable<ConstructCategoryCreateResponse> {
        return this.apiBase.post('construct/category', payload);
    }

    /**
     * Update a single construct by id.
     */
    updateConstructCategoryCategory(
        constructId: EntityIdType,
        payload: ConstructCategoryUpdateRequest,
    ): Observable<ConstructCategoryUpdateResponse> {
        return this.apiBase.put(`construct/category/${constructId}`, payload);
    }

    /**
     * Delete a single construct by id.
     */
    deleteConstructCategoryCategory(
        constructId: EntityIdType,
    ): Observable<void> {
        return this.apiBase.delete(`construct/category/${constructId}`);
    }

    /**
     * Sorts the Categories by the provided ids order (Updates the `sortOrder` property for all categories).
     *
     * @param payload The ids of the categories how they should be sorted
     * @returns A new list which is sorted like the ids are sent.
     */
    sortConstructCategories(
        payload: ConstructCategorySortRequest,
    ): Observable<ConstructCategoryListResponse> {
        return this.apiBase.post('construct/category/sortorder', payload);
    }
}

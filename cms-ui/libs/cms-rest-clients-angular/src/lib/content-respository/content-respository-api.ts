import {
    ContentRepositoryCreateRequest,
    ContentRepositoryCreateResponse,
    ContentRepositoryFragmentListOptions,
    ContentRepositoryFragmentListResponse,
    ContentRepositoryListOptions,
    ContentRepositoryListResponse,
    ContentRepositoryListRolesResponse,
    ContentRepositoryResponse,
    ContentRepositoryUpdateRequest,
    ContentRepositoryUpdateResponse,
    EntityIdType,
    TagmapEntryCreateRequest,
    TagmapEntryCreateResponse,
    TagmapEntryListOptions,
    TagmapEntryListResponse,
    TagmapEntryResponse,
    TagmapEntryUpdateRequest,
    TagmapEntryUpdateResponse,
} from '@gentics/cms-models';
import { LoginResponse } from '@gentics/mesh-models';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiBase } from '../base/api-base.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * API methods related to the content repository resource.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/guides/restapi/resource_ContentRepositoryResource.html#resource_ContentRepositoryResource_get_GET
 */
export class ContentrespositoryApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    /**
     * Get a list of contentrepositories.
     */
    getContentrepositories(
        options?: ContentRepositoryListOptions,
    ): Observable<ContentRepositoryListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get('contentrepositories', options);
    }

    /**
     * Get a single contentRepository by id.
     */
    getContentRepository(
        contentRepositoryId: EntityIdType,
    ): Observable<ContentRepositoryResponse> {
        return this.apiBase.get(`contentrepositories/${contentRepositoryId}`);
    }

    /**
     * Create a new contentRepository.
     */
    createContentRepository(
        payload: ContentRepositoryCreateRequest,
    ): Observable<ContentRepositoryCreateResponse> {
        return this.apiBase.post('contentrepositories', payload);
    }

    /**
     * Update a single contentRepository by id.
     */
    updateContentRepository(
        contentRepositoryId: EntityIdType,
        payload: ContentRepositoryUpdateRequest,
    ): Observable<ContentRepositoryUpdateResponse> {
        return this.apiBase.put(`contentrepositories/${contentRepositoryId}`, payload);
    }

    /**
     * Delete a single contentRepository by id.
     */
    deleteContentRepository(
        contentRepositoryId: EntityIdType,
    ): Observable<void> {
        return this.apiBase.delete(`contentrepositories/${contentRepositoryId}`);
    }

    // ACTIONS

    /**
     * Check the data in the given contentRepository.
     */
    checkContentRepositoryData(
        contentRepositoryId: EntityIdType,
    ): Observable<ContentRepositoryResponse> {
        return this.apiBase.put(`contentrepositories/${contentRepositoryId}/data/check`, null);
    }

    /**
     * Check and repair the data in the given contentRepository.
     */
    repairContentRepositoryData(
        contentRepositoryId: EntityIdType,
    ): Observable<ContentRepositoryResponse> {
        return this.apiBase.put(`contentrepositories/${contentRepositoryId}/data/repair`, null);
    }

    /**
     * Check the connectivity and structure of the given contentRepository
     */
    checkContentRepositoryStructure(
        contentRepositoryId: EntityIdType,
    ): Observable<ContentRepositoryResponse> {
        return this.apiBase.put(`contentrepositories/${contentRepositoryId}/structure/check`, null);
    }

    /**
     * Check and repair the connectivity and structure of the given contentRepository.
     */
    repairContentRepositoryStructure(
        contentRepositoryId: EntityIdType,
    ): Observable<ContentRepositoryResponse> {
        return this.apiBase.put(`contentrepositories/${contentRepositoryId}/structure/repair`, null);
    }

    /** TAGMAP ENTRIES ************************************************************************************ */

    /**
     * Check consistency of tagmap entries and return inconsistencies.
     */
    checkContentRepositoryTagmapEntries(
        contentRepositoryId: EntityIdType,
    ): Observable<TagmapEntryListResponse> {
        return this.apiBase.get(`contentrepositories/${contentRepositoryId}/entries/check`);
    }

    /**
     * Get a list of tagmap entries of contentRepository.
     */
    getContentRepositoryTagmapEntries(
        contentRepositoryId: EntityIdType,
        options?: TagmapEntryListOptions,
    ): Observable<TagmapEntryListResponse> {
        return this.apiBase.get(`contentrepositories/${contentRepositoryId}/entries`, options);
    }

    /**
     * Get a single tagmap entry of contentRepository.
     */
    getContentRepositoryTagmapEntry(
        contentRepositoryId: EntityIdType,
        tagmapId: string,
    ): Observable<TagmapEntryResponse> {
        return this.apiBase.get(`contentrepositories/${contentRepositoryId}/entries/${tagmapId}`);
    }

    /**
     * Create a new tagmap entry at contentRepository.
     */
    createContentRepositoryTagmapEntry(
        contentRepositoryId: EntityIdType,
        payload: TagmapEntryCreateRequest,
    ): Observable<TagmapEntryCreateResponse> {
        return this.apiBase.post(`contentrepositories/${contentRepositoryId}/entries`, payload);
    }

    /**
     * Update a tagmap entry of contentRepository.
     */
    updateContentRepositoryTagmapEntry(
        contentRepositoryId: EntityIdType,
        tagmapId: string | number,
        payload: TagmapEntryUpdateRequest,
    ): Observable<TagmapEntryUpdateResponse> {
        return this.apiBase.put(`contentrepositories/${contentRepositoryId}/entries/${tagmapId}`, payload);
    }

    /**
     * Delete a tagmap entry of contentRepository.
     */
    deleteContentRepositoryTagmapEntry(
        contentRepositoryId: EntityIdType,
        tagmapId: string | number,
    ): Observable<void> {
        return this.apiBase.delete(`contentrepositories/${contentRepositoryId}/entries/${tagmapId}`);
    }

    /** CR FRAGMENTS ************************************************************************************ */

    /**
     * Get which CR Fragments are currently assigned to contentRepository of id.
     */
    getContentRepositoryFragments(
        contentRepositoryId: EntityIdType,
        options?: ContentRepositoryFragmentListOptions,
    ): Observable<ContentRepositoryFragmentListResponse> {
        return this.apiBase.get(`contentrepositories/${contentRepositoryId}/cr_fragments`, options);
    }

    /**
     * Assign a ContentRepository Fragment to the ContentRepository.
     */
    addContentRepositoryToFragment(
        contentRepositoryId: EntityIdType,
        crFragmentId: EntityIdType,
    ): Observable<void> {
        return this.apiBase.put(`contentrepositories/${contentRepositoryId}/cr_fragments/${crFragmentId}`, {}).pipe(
            map(() => {}),
        );
    }

    /**
     * Remove the ContentRepository Fragment from the ContentRepository.
     */
    removeContentRepositoryFromFragment(
        contentRepositoryId: EntityIdType,
        crFragmentId: EntityIdType,
    ): Observable<void> {
        return this.apiBase.delete(`contentrepositories/${contentRepositoryId}/cr_fragments/${crFragmentId}`);
    }

    loginToMeshInstance(
        contentRepositoryId: EntityIdType,
    ): Observable<LoginResponse> {
        return this.apiBase.post(`contentrepositories/${contentRepositoryId}/proxylogin`, null) as any;
    }

    /** MESH ROLES ************************************************************************************ */

    getAvailableContentRepositoryRoles(contentRepositoryId: EntityIdType): Observable<ContentRepositoryListRolesResponse> {
        return this.apiBase.get(`contentrepositories/${contentRepositoryId}/availableroles`);
    }

    getAssignedContentRepositoryRoles(contentRepositoryId: EntityIdType): Observable<ContentRepositoryListRolesResponse> {
        return this.apiBase.get(`contentrepositories/${contentRepositoryId}/roles`);
    }

    updateAssignedContentRepositoryRoles(
        contentRepositoryId: EntityIdType,
        roles: string[],
    ): Observable<ContentRepositoryListRolesResponse> {
        return this.apiBase.post(`contentrepositories/${contentRepositoryId}/roles`, {
            roles,
        });
    }
}

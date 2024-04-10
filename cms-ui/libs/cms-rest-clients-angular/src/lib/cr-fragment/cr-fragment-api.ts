import {
    ContentRepositoryFragmentCreateRequest,
    ContentRepositoryFragmentListOptions,
    ContentRepositoryFragmentListResponse,
    ContentRepositoryFragmentResponse,
    ContentRepositoryFragmentUpdateRequest,
    EntityIdType,
    TagmapEntryCreateRequest,
    TagmapEntryCreateResponse,
    TagmapEntryListOptions,
    TagmapEntryListResponse,
    TagmapEntryResponse,
    TagmapEntryUpdateRequest,
    TagmapEntryUpdateResponse,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods related to the content repository resource.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/resource_ContentRepositoryFragmentResource.html
 */
export class ContentrespositoryFragmentApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    createContentRepositoryFragment(payload: ContentRepositoryFragmentCreateRequest): Observable<ContentRepositoryFragmentResponse> {
        return this.apiBase.post('cr_fragments', payload);
    }

    /**
     * Get a list of contentrespositoryfragments.
     */
    getContentRepositoryFragments(options?: ContentRepositoryFragmentListOptions): Observable<ContentRepositoryFragmentListResponse> {
        return this.apiBase.get('cr_fragments', options);
    }

    /**
     * Get a single contentrespositoryfragment by id.
     */
    getContentRepositoryFragment(fragmentId: EntityIdType): Observable<ContentRepositoryFragmentResponse> {
        return this.apiBase.get(`cr_fragments/${fragmentId}`);
    }

    /**
     * Updates the fragment in the CMS
     *
     * @param fragmentId The ID of the CR-Fragment
     * @param payload The Data to update on the fragment
     * @returns The updated fragment
     */
    updateContentRepositoryFragment(
        fragmentId: EntityIdType,
        payload: ContentRepositoryFragmentUpdateRequest,
    ): Observable<ContentRepositoryFragmentResponse> {
        return this.apiBase.put(`cr_fragments/${fragmentId}`, payload);
    }

    deleteContentRepositoryFragment(fragmentId: number | string): Observable<void> {
        return this.apiBase.delete(`cr_fragments/${fragmentId}`);
    }

    /** TAGMAP ENTRIES ************************************************************************************ */

    /**
     * Get a list of tagmap entries of contentRepository.
     */
    getContentRepositoryFragmentTagmapEntries(
        fragmentId: EntityIdType,
        options?: TagmapEntryListOptions,
    ): Observable<TagmapEntryListResponse> {
        return this.apiBase.get(`cr_fragments/${fragmentId}/entries`, options);
    }

    /**
     * Get a single tagmap entry of contentRepository.
     */
    getContentRepositoryFragmentTagmapEntry(
        fragmentId: EntityIdType,
        tagmapId: string,
    ): Observable<TagmapEntryResponse> {
        return this.apiBase.get(`cr_fragments/${fragmentId}/entries/${tagmapId}`);
    }

    /**
     * Create a new tagmap entry at contentRepository.
     */
    createContentRepositoryFragmentTagmapEntry(
        fragmentId: EntityIdType,
        payload: TagmapEntryCreateRequest,
    ): Observable<TagmapEntryCreateResponse> {
        return this.apiBase.post(`cr_fragments/${fragmentId}/entries`, payload);
    }

    /**
     * Update a tagmap entry of contentRepository.
     */
    updateContentRepositoryFragmentTagmapEntry(
        fragmentId: EntityIdType,
        tagmapId: string | number,
        payload: TagmapEntryUpdateRequest,
    ): Observable<TagmapEntryUpdateResponse> {
        return this.apiBase.put(`cr_fragments/${fragmentId}/entries/${tagmapId}`, payload);
    }

    /**
     * Delete a tagmap entry of contentRepository.
     */
    deleteContentRepositoryFragmentTagmapEntry(
        fragmentId: EntityIdType,
        tagmapId: string | number,
    ): Observable<void> {
        return this.apiBase.delete(`cr_fragments/${fragmentId}/entries/${tagmapId}`);
    }
}

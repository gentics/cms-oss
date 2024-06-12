import {
    ExternalLinkStatistics,
    LinkCheckerCheckResponse,
    LinkCheckerExternalLinkList,
    LinkCheckerOptions,
    LinkCheckerPageList,
    LinkCheckerReplaceRequest,
    LinkCheckerUpdateResponse,
    LinkResponse,
    Response,
    UpdateExternalLinkRequestOptions,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * API methods related to Link Checker.
 *
 * Docs for the endpoints used here can be found at:
 * https://jira.gentics.com/browse/GTXPE-440
 */
export class LinkCheckerApi {

    constructor(private apiBase: ApiBase) {}

    /**
     * Get list of pages containing external links.
     */
    getPages(options: LinkCheckerOptions): Observable<LinkCheckerPageList> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }
        return this.apiBase.get('linkChecker/pages', options);
    }

    /**
     * Get the external links contained in a single page.
     */
    getPage(id: number): Observable<LinkCheckerExternalLinkList> {
        return this.apiBase.get(`linkChecker/pages/${id}`);
    }

    /**
     * Update the validity status for external links contained in the page.
     * Update permissions on the page are required.
     */
    updateStatus(id: number): Observable<LinkCheckerUpdateResponse> {
        return this.apiBase.post(`linkChecker/pages/${id}`, '');
    }

    /**
     * Get link checker statistics
     */
    getStats(nodeId?: number): Observable<ExternalLinkStatistics> {
        const options = {};

        if (nodeId) {
            options['nodeId'] = nodeId;
        }

        return this.apiBase.get('linkChecker/stats', options);
    }

    /**
     * Check an external link for validity.
     */
    checkLink(linkUrl: string): Observable<LinkCheckerCheckResponse> {
        return this.apiBase.post('linkChecker/check', {
            url: linkUrl,
        });
    }

    /**
     * Load a single link. Response contains additional information about occurrence of URL in the system
     *
     * @param id link ID
     * @param pageId page ID
     */
    getLink(id: number, pageId: number): Observable<LinkResponse> {
        return this.apiBase.get(`linkChecker/pages/${pageId}/links/${id}`);
    }

    /**
     * Update a link (optionally together with other occurrences) by replacing the URL with the given URL.
     *
     * @param id link ID
     * @param pageId page ID
     * @param payload Request Body
     * @param requestOptions Request params
     */
    updateLink(
        id: number,
        pageId: number,
        payload: LinkCheckerReplaceRequest,
        requestOptions?: UpdateExternalLinkRequestOptions,
    ): Observable<Response> {
        return this.apiBase.post(`linkChecker/pages/${pageId}/links/${id}`, payload, requestOptions);
    }
}

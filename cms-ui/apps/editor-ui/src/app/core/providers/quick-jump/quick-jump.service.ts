import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map } from 'rxjs/operators';
import { ResourceUrlBuilder } from '../resource-url-builder/resource-url-builder';

/** The target item of a quick jump operation */
export interface QuickJumpTarget {
    id: number;
    nodeId: number;
}

/**
 * Enables searching across nodes by using the `page/autocomplete` API endpoint.
 */
@Injectable()
export class QuickJumpService {

    constructor(
        private http: HttpClient,
        private resourceUrlBuilder: ResourceUrlBuilder,
    ) { }

    /**
     * Searches across nodes using a page ID.
     *
     * @param pageId The page ID to search for.
     * @param currNodeId The current node, which will be used if the node is a channel and the page is inherited from the master node.
     * @returns A Promise that resolves to a `QuickJumpTarget` representing the page with the specified `pageId` in the
     * node with `currNodeId` or, if there is no such node, the page in the master node or
     * `undefined` if no page with `pageId` could be found.
     */
    searchPageById(pageId: number, currNodeId: number): Promise<QuickJumpTarget | undefined> {
        const url = this.resourceUrlBuilder.autocomplete(pageId);
        return this.http.get(url, { responseType: 'text'}).pipe(
            map(text => {
                const html = this.toHtml(text);
                const pages = this.extractPages(html);
                return this.findJumpTarget(pageId, currNodeId, pages);
            }),
        ).toPromise();
    }

    /**
     * Takes a string of HTML markup and returns an HTML div containg that markup parsed into HTML objects.
     */
    private toHtml(source: string): HTMLElement {
        const htmlElement = document.createElement('div');
        htmlElement.innerHTML = source;
        return htmlElement;
    }

    /**
     * The autocomplete endpoint returns some HTML containing the data we need. We need to
     * parse out the pages and inspect the ids.
     */
    private extractPages(html: HTMLElement): QuickJumpTarget[] {
        const extractNumber = (el: Element, attr: string): number => Number.parseInt(el.getAttribute(attr), 10);
        const pages = Array.from(html.querySelectorAll('.ac_page'));
        return pages.map(pageDiv => {
            return {
                id: extractNumber(pageDiv, 'page_id'),
                nodeId: extractNumber(pageDiv, 'node_id'),
            };
        });
    }

    /**
     * Finds the quick jump target in the array of pages.
     *
     * @returns A `QuickJumpTarget` representing the page with the specified `pageId` in the
     * node with `currNodeId` or, if there is no such node, the page in the master node or
     * `undefined` if no page with `pageId` could be found.
     */
    private findJumpTarget(pageId: number, currNodeId: number, pages: QuickJumpTarget[]): QuickJumpTarget | undefined {
        pages = pages.filter(page => pageId && page.id === pageId);

        if (pages.length > 0) {
            const pageInCurrNode = pages.find(page => page.nodeId === currNodeId);
            if (pageInCurrNode) {
                return pageInCurrNode;
            } else {
                pages.sort((pageA, pageB) => pageA.nodeId - pageB.nodeId);
                return pages[0];
            }
        }
        return undefined;
    }
}

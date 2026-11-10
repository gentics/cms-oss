import { vi } from 'vitest';

/**
 * Stubs `fetch` with the CMS a published page's address comes from (`usePublishedUrls`): `GET /rest/node` lists
 * node `nodeId` on `host`, `GET /rest/page/load/<id>` gives each page of `publishPaths` with that publish path;
 * any other page, and any other request, is `404`. Returns the mock.
 */
export function stubCmsPublishPaths(publishPaths: Record<number, string>, { host = 'https://www.gentics.com', nodeId = 3 } = {}) {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const path = new URL(String(input), 'http://localhost').pathname;
        const page = /^\/rest\/page\/load\/(\d+)$/.exec(path);

        if (path === '/rest/node') {
            return Response.json({ items: [{ id: nodeId, name: 'Website', folderId: 2, host }] });
        }

        const publishPath = page ? publishPaths[Number(page[1])] : undefined;

        return publishPath === undefined
            ? new Response(null, { status: 404 })
            : Response.json({ responseInfo: { responseCode: 'OK' }, page: { id: Number(page![1]), name: 'x', folderId: 2, cdate: 0, edate: 0, publishPath } });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

import { afterEach, describe, expect, it, vi } from 'vitest';

import { HttpError } from '@/services/httpService/httpService';

import { approvePubqueuePages, assignCmsPages, claimCmsTask, cmsPageUrl, cmsVersionCompareUrl, completeCmsTask, listCmsTasks } from './cmsTaskApi';

function stubFetch(...responses: Response[]) {
    const fetchMock = vi.fn<typeof fetch>();

    for (const response of responses) {
        fetchMock.mockResolvedValueOnce(response);
    }

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

function sent(fetchMock: ReturnType<typeof stubFetch>, call = 0) {
    const [url, init] = fetchMock.mock.calls[call]!;

    return { url: String(url), method: init?.method ?? 'GET', body: init?.body ? JSON.parse(init.body as string) as unknown : undefined };
}

function urlOptions(url: string): unknown {
    const encoded = /;options=([^)]*)\)$/.exec(url)?.[1] ?? '';

    return JSON.parse(atob(decodeURIComponent(encoded)));
}

describe('cmsTaskApi', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('lists the tasks with the CMS session cookie', async () => {
        const fetchMock = stubFetch(Response.json({ items: [{ id: '1', source: 'manual', type: 'custom', status: 'open', actions: [] }], responseInfo: { responseCode: 'OK' } }));

        const tasks = await listCmsTasks();

        expect(tasks.map((task) => task.id)).toEqual(['1']);
        expect(fetchMock).toHaveBeenCalledWith('/rest/task', expect.objectContaining({ credentials: 'same-origin' }));
    });

    it('sets a task done, claims it, and encodes virtual task ids in the path', async () => {
        const fetchMock = stubFetch(
            Response.json({ task: { id: '5' }, responseInfo: { responseCode: 'OK' } }),
            Response.json({ task: { id: '5' }, responseInfo: { responseCode: 'OK' } }),
        );

        await completeCmsTask('5');
        await claimCmsTask('5');

        expect(sent(fetchMock, 0)).toEqual({ url: '/rest/task/5', method: 'PUT', body: { status: 'done' } });
        expect(sent(fetchMock, 1)).toMatchObject({ url: '/rest/task/5/claim', method: 'POST' });
    });

    it('approves a page in the queue by its id', async () => {
        const fetchMock = stubFetch(Response.json({ responseInfo: { responseCode: 'OK' } }));

        await approvePubqueuePages([9142]);

        expect(sent(fetchMock)).toEqual({ url: '/rest/page/pubqueue/approve', method: 'POST', body: { ids: [9142] } });
    });

    it('throws a refusal the CMS answers with 200, so it is not taken for success', async () => {
        stubFetch(Response.json({ responseInfo: { responseCode: 'INVALIDDATA' } }));

        const error = await assignCmsPages({ pageIds: [9142], userIds: [], message: '' }).catch((caught: unknown) => caught);

        expect(error).toBeInstanceOf(HttpError);
        expect((error as HttpError).body).toEqual({ responseInfo: { responseCode: 'INVALIDDATA' } });
    });

    it('sends the page ids as strings and the users and message when assigning', async () => {
        const fetchMock = stubFetch(Response.json({ responseInfo: { responseCode: 'OK' } }));

        await assignCmsPages({ pageIds: [9142], userIds: [42], message: 'Please fix the date' });

        expect(sent(fetchMock).body).toEqual({ pageIds: ['9142'], userIds: [42], message: 'Please fix the date' });
    });

    it('builds the CMS UI version comparison of the current and the last published version', async () => {
        const currentVersion = { number: '1.2', timestamp: 200 };
        const publishedVersion = { number: '1.0', timestamp: 100 };
        const fetchMock = stubFetch(Response.json({ page: { currentVersion, publishedVersion } }));

        const url = await cmsVersionCompareUrl(9142, 3);

        expect(sent(fetchMock).url).toBe('/rest/page/load/9142?nodeId=3&versioninfo=true');
        expect(url.startsWith('/editor/#/editor/(detail:node/3/page/9142/compareVersionContents;options=')).toBe(true);
        expect(urlOptions(url)).toEqual({ version: currentVersion, oldVersion: publishedVersion });
    });

    it('opens the preview when the version info cannot be read', async () => {
        stubFetch(new Response(null, { status: 500 }));

        expect(await cmsVersionCompareUrl(9142, 3)).toBe(cmsPageUrl(9142, 3));
        expect(urlOptions(cmsPageUrl(9142, 3))).toEqual({});
    });
});

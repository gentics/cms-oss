import { afterEach, describe, expect, it, vi } from 'vitest';

import { HttpError } from '@/services/httpService/httpService';

import { CMS_SEARCH_MAX_ITEMS, type CmsTokenInfo, createCmsToken, listCmsNodes, searchCmsItems } from './cmsApiService';

const NOW_S = 1_790_800_000;

function tokenInfo(overrides: Partial<CmsTokenInfo> = {}): Omit<CmsTokenInfo, 'token'> {
    return { id: 1, userId: 3, name: 'node', cdate: NOW_S - 3600, expires: NOW_S + 3600, lastUsed: 0, valid: true, ...overrides };
}

function stubFetchSequence(...responses: Response[]) {
    const fetchMock = vi.fn<typeof fetch>();

    for (const response of responses) {
        fetchMock.mockResolvedValueOnce(response);
    }

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

function createdResponse(overrides: Record<string, unknown> = {}) {
    return Response.json({ token: 'cmstok_secret', ...tokenInfo({ id: 4, name: 'genaix-workspace-1' }), ...overrides });
}

function postedBody(fetchMock: ReturnType<typeof stubFetchSequence>, call = 0): unknown {
    return JSON.parse(fetchMock.mock.calls[call]![1]?.body as string);
}

describe('createCmsToken', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
        vi.useRealTimers();
    });

    it('posts the name, an expiry 24 hours ahead and pruneOnExpiry with the CMS session cookie and returns the created token with that expiry', async () => {
        vi.useFakeTimers({ toFake: ['Date'] });
        vi.setSystemTime(NOW_S * 1000);

        const fetchMock = stubFetchSequence(createdResponse());

        await expect(createCmsToken('genaix-workspace-1'))
            .resolves.toEqual({ token: 'cmstok_secret', ...tokenInfo({ id: 4, name: 'genaix-workspace-1', expires: NOW_S + 86_400 }) });

        const [url, init] = fetchMock.mock.calls[0]!;

        expect(url).toBe('/rest/admin/token');
        expect(init?.method).toBe('POST');
        expect(init?.credentials).toBe('same-origin');
        expect(postedBody(fetchMock)).toEqual({ name: 'genaix-workspace-1', expires: NOW_S + 86_400, pruneOnExpiry: true });
    });

    it('throws an HttpError with the status when the CMS refuses', async () => {
        stubFetchSequence(new Response('Forbidden', { status: 403 }));

        const error = await createCmsToken('genaix-workspace-1').catch((caught: unknown) => caught);

        expect(error).toBeInstanceOf(HttpError);
        expect((error as HttpError).status).toBe(403);
    });
});

describe('listCmsNodes', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('lists the nodes with the CMS session cookie', async () => {
        const fetchMock = stubFetchSequence(Response.json({ items: [{ id: 3, name: 'Corporate Website', folderId: 7 }] }));

        await expect(listCmsNodes()).resolves.toEqual([{ id: 3, name: 'Corporate Website', folderId: 7 }]);
        expect(fetchMock).toHaveBeenCalledWith('/rest/node', expect.objectContaining({ credentials: 'same-origin' }));
    });
});

describe('searchCmsItems', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('searches pages, folders and images below the node root folder, recursively and capped', async () => {
        const fetchMock = stubFetchSequence(Response.json({ items: [{ id: 42, name: 'Campaigns', type: 'folder' }] }));

        await expect(searchCmsItems({ id: 3, name: 'Corporate Website', folderId: 7 }, 'Camp')).resolves.toEqual([{ id: 42, name: 'Campaigns', type: 'folder' }]);

        const url = new URL(fetchMock.mock.calls[0]![0] as string, 'http://cms.test');

        expect(url.pathname).toBe('/rest/folder/getItems/7');
        expect(url.searchParams.get('nodeId')).toBe('3');
        expect(url.searchParams.get('search')).toBe('Camp');
        expect(url.searchParams.get('recursive')).toBe('true');
        expect(url.searchParams.get('maxItems')).toBe(String(CMS_SEARCH_MAX_ITEMS));
        expect(url.searchParams.getAll('type')).toEqual(['page', 'folder', 'image']);
        expect(fetchMock.mock.calls[0]![1]).toEqual(expect.objectContaining({ credentials: 'same-origin' }));
    });
});

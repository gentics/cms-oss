import { afterEach, describe, expect, it, vi } from 'vitest';

import { HttpError } from '@/services/httpService/httpService';

import {
    CMS_SEARCH_MAX_ITEMS,
    CmsLoginError,
    type CmsTokenInfo,
    createCmsToken,
    getCmsKeycloakConfig,
    getCmsUser,
    listCmsNodes,
    loginToCms,
    logoutFromCms,
    searchCmsItems,
    ssoLoginToCms,
} from './cmsApiService';

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

describe('CMS login', () => {
    const user = { id: 3, login: 'editor', firstName: 'Eddie', lastName: 'Tor' };

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('getCmsUser returns the user of the session cookie, and null for 401', async () => {
        const fetchMock = stubFetchSequence(Response.json({ user }), new Response('', { status: 401 }));

        await expect(getCmsUser()).resolves.toEqual(user);
        await expect(getCmsUser()).resolves.toBeNull();
        expect(fetchMock).toHaveBeenCalledWith('/rest/user/me', expect.objectContaining({ credentials: 'same-origin' }));
    });

    it('getCmsUser throws other errors', async () => {
        stubFetchSequence(new Response('', { status: 500 }));

        await expect(getCmsUser()).rejects.toBeInstanceOf(HttpError);
    });

    it('loginToCms posts the credentials and returns the user', async () => {
        const fetchMock = stubFetchSequence(Response.json({ responseInfo: { responseCode: 'OK' }, user }));

        await expect(loginToCms('editor', 'secret')).resolves.toEqual(user);

        const [url, init] = fetchMock.mock.calls[0]!;

        expect(url).toBe('/rest/auth/login');
        expect(init).toMatchObject({ method: 'POST', credentials: 'same-origin' });
        expect(postedBody(fetchMock)).toEqual({ login: 'editor', password: 'secret' });
    });

    it('loginToCms throws the response code of a refused login, which the CMS answers with 200', async () => {
        stubFetchSequence(Response.json({ responseInfo: { responseCode: 'NOTFOUND' } }));

        const error = await loginToCms('editor', 'wrong').catch((caught: unknown) => caught);

        expect(error).toBeInstanceOf(CmsLoginError);
        expect(error).toMatchObject({ responseCode: 'NOTFOUND' });
    });

    it('logoutFromCms posts the logout', async () => {
        const fetchMock = stubFetchSequence(Response.json({ responseInfo: { responseCode: 'OK' } }));

        await logoutFromCms();

        expect(fetchMock).toHaveBeenCalledWith('/rest/auth/logout', expect.objectContaining({ method: 'POST', credentials: 'same-origin' }));
    });

    it('getCmsKeycloakConfig returns the settings, and null for 404', async () => {
        const config = { 'auth-server-url': 'https://sso.example.com', realm: 'cms', resource: 'cms-ui', showSSOButton: true };

        stubFetchSequence(Response.json(config), new Response('', { status: 404 }));

        await expect(getCmsKeycloakConfig()).resolves.toEqual(config);
        await expect(getCmsKeycloakConfig()).resolves.toBeNull();
    });

    it('ssoLoginToCms sends the access token and accepts the session id it gets back', async () => {
        const fetchMock = stubFetchSequence(new Response('1234'));

        await expect(ssoLoginToCms('kc-token')).resolves.toBeUndefined();

        const [url, init] = fetchMock.mock.calls[0]!;

        expect(url).toBe('/rest/auth/ssologin');
        expect(init?.headers).toMatchObject({ Authorization: 'Bearer kc-token' });
    });

    it('ssoLoginToCms throws the response code it gets instead of a session id', async () => {
        stubFetchSequence(new Response('MAINTENANCEMODE'));

        await expect(ssoLoginToCms('kc-token')).rejects.toMatchObject({ name: 'CmsLoginError', responseCode: 'MAINTENANCEMODE' });
    });
});

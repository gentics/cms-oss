import { afterEach, describe, expect, it, vi } from 'vitest';

import { HttpError } from '@/services/httpService/httpService';

import {
    canPublishCmsPage,
    CMS_SEARCH_MAX_ITEMS,
    cmsImageUrl,
    CmsLoginError,
    type CmsTokenInfo,
    createCmsToken,
    deleteCmsPackageInternalFile,
    getCmsKeycloakConfig,
    getCmsUser,
    isCmsFeatureActive,
    listCmsFolders,
    listCmsNodePackages,
    listCmsNodes,
    listCmsPackageInternalFiles,
    listCmsTemplates,
    loadCmsPackageInternalFile,
    loadCmsPage,
    loginToCms,
    logoutFromCms,
    saveCmsPackageInternalFile,
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
    const NODE = { id: 3, name: 'Corporate Website', folderId: 7 };

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.restoreAllMocks();
    });

    // The URL of the one request made.
    function requestUrl(fetchMock: ReturnType<typeof stubFetchSequence>): URL {
        return new URL(fetchMock.mock.calls[0]![0] as string, 'http://cms.test');
    }

    it('searches pages below the node root folder, recursively and capped', async () => {
        const fetchMock = stubFetchSequence(Response.json({ items: [{ id: 8871, name: 'Campaign launch', type: 'page' }] }));

        await expect(searchCmsItems(NODE, 'page', 'Camp')).resolves.toEqual([{ id: 8871, name: 'Campaign launch', type: 'page' }]);

        const url = requestUrl(fetchMock);

        expect(url.pathname).toBe('/rest/folder/getItems/7');
        expect(url.searchParams.get('nodeId')).toBe('3');
        expect(url.searchParams.get('search')).toBe('Camp');
        expect(url.searchParams.get('recursive')).toBe('true');
        expect(url.searchParams.get('maxItems')).toBe(String(CMS_SEARCH_MAX_ITEMS));
        expect(url.searchParams.getAll('type')).toEqual(['page']);
        expect(fetchMock.mock.calls[0]![1]).toEqual(expect.objectContaining({ credentials: 'same-origin' }));
    });

    it('lists the objects below the root folder too when asked to, without a query', async () => {
        const fetchMock = stubFetchSequence(Response.json({ items: [] }));

        await searchCmsItems(NODE, 'image', '', { recursive: true });

        const url = requestUrl(fetchMock);

        expect(url.pathname).toBe('/rest/folder/getItems/7');
        expect(url.searchParams.has('search')).toBe(false);
        expect(url.searchParams.get('recursive')).toBe('true');
        expect(url.searchParams.getAll('type')).toEqual(['image']);
    });

    it('searches folders with getFolders, which getItems leaves out', async () => {
        const fetchMock = stubFetchSequence(Response.json({ folders: [{ id: 42, name: 'Campaigns', type: 'folder', path: '/Corporate Website/' }] }));

        await expect(searchCmsItems(NODE, 'folder', 'Camp')).resolves.toEqual([{ id: 42, name: 'Campaigns', type: 'folder', path: '/Corporate Website/' }]);

        const url = requestUrl(fetchMock);

        expect(url.pathname).toBe('/rest/folder/getFolders/7');
        expect(url.searchParams.get('search')).toBe('Camp');
        expect(url.searchParams.get('recursive')).toBe('true');
        expect(url.searchParams.has('type')).toBe(false);
    });

    it('lists the root level without a query', async () => {
        const fetchMock = stubFetchSequence(Response.json({ items: [] }));

        await expect(searchCmsItems(NODE, 'image', '')).resolves.toEqual([]);

        const url = requestUrl(fetchMock);

        expect(url.pathname).toBe('/rest/folder/getItems/7');
        expect(url.searchParams.has('search')).toBe(false);
        expect(url.searchParams.has('recursive')).toBe(false);
        expect(url.searchParams.get('maxItems')).toBe(String(CMS_SEARCH_MAX_ITEMS));
        expect(url.searchParams.getAll('type')).toEqual(['image']);
    });

    it('lists another folder of the node in the root folder\'s place when given one', async () => {
        const fetchMock = stubFetchSequence(Response.json({ items: [{ id: 9150, name: 'AGB', type: 'page' }] }));

        await expect(searchCmsItems(NODE, 'page', '', { folderId: 8 })).resolves.toEqual([{ id: 9150, name: 'AGB', type: 'page' }]);

        const url = requestUrl(fetchMock);

        expect(url.pathname).toBe('/rest/folder/getItems/8');
        expect(url.searchParams.get('nodeId')).toBe('3');
        expect(url.searchParams.has('search')).toBe(false);
        expect(url.searchParams.has('recursive')).toBe(false);
        expect(url.searchParams.getAll('type')).toEqual(['page']);
    });

    it('searches with Elasticsearch when asked to, fuzzy and below the node root folder', async () => {
        const fetchMock = stubFetchSequence(Response.json({ hits: { total: 1, hits: [{ _id: 8871, _object: { id: 8871, name: 'Campaign launch', type: 'page' } }] } }));

        await expect(searchCmsItems(NODE, 'page', 'Campain', { elastic: true })).resolves.toEqual([{ id: 8871, name: 'Campaign launch', type: 'page' }]);

        const url = requestUrl(fetchMock);
        const body = postedBody(fetchMock) as { query: { bool: { must: [{ bool: { should: Record<string, unknown>[] } }] } }; size: number };
        const should = body.query.bool.must[0].bool.should;

        expect(fetchMock.mock.calls[0]![1]?.method).toBe('POST');
        expect(url.pathname).toBe('/rest/elastic/page/_search');
        expect(url.searchParams.get('nodeId')).toBe('3');
        expect(url.searchParams.get('folderId')).toBe('7');
        expect(url.searchParams.get('recursive')).toBe('true');
        expect(body.size).toBe(CMS_SEARCH_MAX_ITEMS);
        expect(should).toContainEqual({ multi_match: { fields: ['name^2', 'path', 'description', 'content'], query: 'Campain', fuzziness: 'AUTO' } });
        expect(should).toContainEqual({ wildcard: { filename: { value: '*campain*', boost: 2 } } });
        expect(should).not.toContainEqual({ term: { id: expect.anything() } });
    });

    it('also finds the id of a number with Elasticsearch', async () => {
        const fetchMock = stubFetchSequence(Response.json({ hits: { hits: [] } }));

        await searchCmsItems(NODE, 'folder', '42', { elastic: true });

        const body = postedBody(fetchMock) as { query: { bool: { must: [{ bool: { should: unknown[] } }] } } };

        expect(body.query.bool.must[0].bool.should).toContainEqual({ term: { id: 42 } });
    });

    it('falls back to the search without Elasticsearch when that fails', async () => {
        vi.spyOn(console, 'warn').mockImplementation(() => {});

        const fetchMock = stubFetchSequence(new Response('', { status: 500 }), Response.json({ folders: [{ id: 42, name: 'Campaigns', type: 'folder' }] }));

        await expect(searchCmsItems(NODE, 'folder', 'Camp', { elastic: true })).resolves.toEqual([{ id: 42, name: 'Campaigns', type: 'folder' }]);
        expect(new URL(fetchMock.mock.calls[1]![0] as string, 'http://cms.test').pathname).toBe('/rest/folder/getFolders/7');
    });

    it('lists the root level without Elasticsearch', async () => {
        const fetchMock = stubFetchSequence(Response.json({ items: [] }));

        await searchCmsItems(NODE, 'page', '', { elastic: true });

        expect(requestUrl(fetchMock).pathname).toBe('/rest/folder/getItems/7');
    });
});

describe('isCmsFeatureActive', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('asks the CMS whether the feature is on', async () => {
        const fetchMock = stubFetchSequence(Response.json({ name: 'elasticsearch', activated: true }), Response.json({ name: 'elasticsearch', activated: false }));

        await expect(isCmsFeatureActive('elasticsearch')).resolves.toBe(true);
        await expect(isCmsFeatureActive('elasticsearch')).resolves.toBe(false);
        expect(fetchMock).toHaveBeenCalledWith('/rest/admin/features/elasticsearch', expect.objectContaining({ credentials: 'same-origin' }));
    });
});

describe('loadCmsPage', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('loads the page of the node read only, with the CMS session cookie', async () => {
        const page = { id: 9142, name: 'Terms', fileName: 'terms.html', folderId: 42, cdate: 1, edate: 2 };
        const fetchMock = stubFetchSequence(Response.json({ page }));

        await expect(loadCmsPage(9142, 3)).resolves.toEqual(page);
        expect(fetchMock).toHaveBeenCalledWith('/rest/page/load/9142?nodeId=3', expect.objectContaining({ credentials: 'same-origin' }));
    });

    it('loads the page without a node when none is known', async () => {
        const fetchMock = stubFetchSequence(Response.json({ page: { id: 9142 } }));

        await loadCmsPage(9142);
        expect(fetchMock.mock.calls[0]![0]).toBe('/rest/page/load/9142');
    });
});

describe('canPublishCmsPage', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it.each([true, false])('answers whether the CMS grants publishing the page of the node: %s', async (granted) => {
        const fetchMock = stubFetchSequence(Response.json({ granted }));

        await expect(canPublishCmsPage(9142, 3)).resolves.toBe(granted);
        expect(fetchMock).toHaveBeenCalledWith('/rest/perm/publish/page/9142?nodeId=3', expect.objectContaining({ credentials: 'same-origin' }));
    });

    it('asks without a node when none is known, and takes a missing answer as not granted', async () => {
        const fetchMock = stubFetchSequence(Response.json({}));

        await expect(canPublishCmsPage(9142)).resolves.toBe(false);
        expect(fetchMock.mock.calls[0]![0]).toBe('/rest/perm/publish/page/9142');
    });
});

describe('listCmsTemplates', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('lists the templates linked to the folder of the node', async () => {
        const fetchMock = stubFetchSequence(Response.json({ templates: [{ id: 17, name: 'Kampagnen-Landingpage' }] }));

        await expect(listCmsTemplates(42, 3)).resolves.toEqual([{ id: 17, name: 'Kampagnen-Landingpage' }]);
        expect(fetchMock).toHaveBeenCalledWith('/rest/folder/getTemplates/42?folderNodeId=3', expect.objectContaining({ credentials: 'same-origin' }));
    });
});

describe('cmsImageUrl', () => {
    it('is where the CMS serves the image of the node', () => {
        expect(cmsImageUrl(3, 4411)).toBe('/rest/file/content/load/4411?nodeId=3');
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

describe('listCmsFolders', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    const FOLDER = { id: 42, name: 'Campaigns', path: '/Corporate Website/Campaigns/', hasSubfolders: true, inherited: false };

    it('lists one page of a folder\'s subfolders in the node, with how many there are', async () => {
        const fetchMock = stubFetchSequence(Response.json({ folders: [FOLDER], numItems: 61, hasMoreItems: true }));

        await expect(listCmsFolders(3, 7, { skipCount: 60, maxItems: 60 })).resolves.toEqual({ folders: [FOLDER], total: 61, hasMore: true });

        const url = new URL(fetchMock.mock.calls[0]![0] as string, 'http://cms.test');

        expect(url.pathname).toBe('/rest/folder/getFolders/7');
        expect(url.searchParams.get('nodeId')).toBe('3');
        expect(url.searchParams.get('skipCount')).toBe('60');
        expect(url.searchParams.get('maxItems')).toBe('60');
        expect(url.searchParams.has('search')).toBe(false);
        expect(url.searchParams.has('recursive')).toBe(false);
        expect(fetchMock.mock.calls[0]![1]).toEqual(expect.objectContaining({ credentials: 'same-origin' }));
    });

    it('searches below the folder, recursively', async () => {
        const fetchMock = stubFetchSequence(Response.json({ folders: [FOLDER] }));

        await expect(listCmsFolders(3, 7, { search: 'Camp', maxItems: 50 })).resolves.toEqual({ folders: [FOLDER], total: 1, hasMore: false });

        const url = new URL(fetchMock.mock.calls[0]![0] as string, 'http://cms.test');

        expect(url.searchParams.get('search')).toBe('Camp');
        expect(url.searchParams.get('recursive')).toBe('true');
        expect(url.searchParams.get('maxItems')).toBe('50');
    });
});

describe('devtools package files', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    const file = { path: 'guidelines/10_tone-of-voice.default.md', name: '10_tone-of-voice.default.md', directory: false, size: 120, lastModified: NOW_S };

    it('listCmsNodePackages lists the packages of the node', async () => {
        const fetchMock = stubFetchSequence(Response.json({ items: [{ name: 'corporate' }], numItems: 1 }));

        await expect(listCmsNodePackages(3)).resolves.toEqual([{ name: 'corporate' }]);
        expect(fetchMock).toHaveBeenCalledWith('/rest/devtools/nodes/3/packages', expect.objectContaining({ credentials: 'same-origin' }));
    });

    it('listCmsPackageInternalFiles lists one directory of files-internal', async () => {
        const fetchMock = stubFetchSequence(Response.json({ items: [file], numItems: 1 }));

        await expect(listCmsPackageInternalFiles('corporate', 'guidelines')).resolves.toEqual([file]);
        expect(fetchMock.mock.calls[0]![0]).toBe('/rest/devtools/packages/corporate/files-internal?path=guidelines');
    });

    it('listCmsPackageInternalFiles answers a directory that does not exist yet with no files, and throws other errors', async () => {
        stubFetchSequence(Response.json({}, { status: 404 }), Response.json({}, { status: 403 }));

        await expect(listCmsPackageInternalFiles('corporate', 'guidelines')).resolves.toEqual([]);
        await expect(listCmsPackageInternalFiles('corporate', 'guidelines')).rejects.toMatchObject({ status: 403 });
    });

    it('loadCmsPackageInternalFile returns the file as text', async () => {
        const fetchMock = stubFetchSequence(new Response('# Tone of voice\n', { headers: { 'Content-Type': 'text/markdown' } }));

        await expect(loadCmsPackageInternalFile('corporate', file.path)).resolves.toBe('# Tone of voice\n');
        expect(fetchMock.mock.calls[0]![0]).toBe('/rest/devtools/packages/corporate/files-internal/content?path=guidelines%2F10_tone-of-voice.default.md');
    });

    it('loadCmsPackageInternalFile throws an HttpError for a file that does not exist', async () => {
        stubFetchSequence(Response.json({ messages: [] }, { status: 404 }));

        await expect(loadCmsPackageInternalFile('corporate', file.path)).rejects.toBeInstanceOf(HttpError);
    });

    it('saveCmsPackageInternalFile posts the text as the multipart part file, named as the file', async () => {
        const fetchMock = stubFetchSequence(new Response(null, { status: 201 }));

        await expect(saveCmsPackageInternalFile('corporate', file.path, '# Tone of voice\n')).resolves.toBeUndefined();

        const [url, init] = fetchMock.mock.calls[0]!;
        const part = (init?.body as FormData).get('file') as File;

        expect(url).toBe('/rest/devtools/packages/corporate/files-internal/content?path=guidelines%2F10_tone-of-voice.default.md');
        expect(init?.method).toBe('POST');
        expect(new Headers(init?.headers).has('Content-Type')).toBe(false);
        expect(part.name).toBe('10_tone-of-voice.default.md');
        await expect(part.text()).resolves.toBe('# Tone of voice\n');
    });

    it('saveCmsPackageInternalFile throws an HttpError for a refused save', async () => {
        stubFetchSequence(Response.json({ messages: [] }, { status: 400 }));

        await expect(saveCmsPackageInternalFile('corporate', file.path, '')).rejects.toMatchObject({ status: 400 });
    });

    it('deleteCmsPackageInternalFile sends DELETE and resolves on 204', async () => {
        const fetchMock = stubFetchSequence(new Response(null, { status: 204 }));

        await expect(deleteCmsPackageInternalFile('corporate', file.path)).resolves.toBeUndefined();
        expect(fetchMock.mock.calls[0]![0]).toBe('/rest/devtools/packages/corporate/files-internal/content?path=guidelines%2F10_tone-of-voice.default.md');
        expect(fetchMock.mock.calls[0]![1]?.method).toBe('DELETE');
    });
});

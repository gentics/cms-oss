import { QueryClient } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { createWrapper } from '@/test/renderWithProviders';

import { useCanPublishCmsPages, useCmsGuidelines, useCmsSearch } from './useCmsQueries';

const NODE = { id: 3, name: 'Corporate Website', folderId: 7 };

// Answers the feature `elasticsearch` with `activated` (an error without), and every search with one page.
function stubCms(activated?: boolean) {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const url = new URL(String(input), 'http://app.test');

        if (url.pathname === '/rest/admin/features/elasticsearch') {
            return activated === undefined ? new Response('', { status: 500 }) : Response.json({ name: 'elasticsearch', activated });
        }

        if (url.pathname === '/rest/elastic/page/_search') {
            return Response.json({ hits: { hits: [{ _object: { id: 8871, name: 'Campaign launch', type: 'page' } }] } });
        }

        return Response.json({ items: [{ id: 8871, name: 'Campaign launch', type: 'page' }] });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

// The paths of the searches made.
function searchPaths(fetchMock: ReturnType<typeof stubCms>): string[] {
    return fetchMock.mock.calls
        .map(([input]) => new URL(String(input), 'http://app.test').pathname)
        .filter((path) => !path.startsWith('/rest/admin/'));
}

function renderSearch(query: string) {
    const wrapper = createWrapper(new QueryClient({ defaultOptions: { queries: { retry: false } } }));

    return renderHook(() => useCmsSearch([NODE], query, ['page']), { wrapper });
}

describe('useCmsSearch', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('searches with Elasticsearch while the CMS feature is on', async () => {
        const fetchMock = stubCms(true);
        const { result } = renderSearch('Campain');

        await waitFor(() => expect(result.current.results).toEqual([{ node: NODE, items: [{ id: 8871, name: 'Campaign launch', type: 'page' }] }]));
        expect(searchPaths(fetchMock)).toEqual(['/rest/elastic/page/_search']);
    });

    it.each([
        ['off', false],
        ['unknown', undefined],
    ])('searches without Elasticsearch while the CMS feature is %s', async (_, activated) => {
        const fetchMock = stubCms(activated);
        const { result } = renderSearch('Camp');

        await waitFor(() => expect(result.current.results).toHaveLength(1));
        expect(searchPaths(fetchMock)).toEqual(['/rest/folder/getItems/7']);
    });
});

describe('useCanPublishCmsPages', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    // Answers the publish permission of page `id` with `granted[id]`, an error for a page without an entry.
    function renderPermission(pages: { id: number; node_id?: number }[], granted: Record<number, boolean>) {
        const fetchMock = vi.fn<typeof fetch>(async (input) => {
            const id = Number(/\/rest\/perm\/publish\/page\/(\d+)/.exec(String(input))?.[1]);

            return id in granted ? Response.json({ granted: granted[id] }) : new Response('', { status: 500 });
        });

        vi.stubGlobal('fetch', fetchMock);
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

        return { fetchMock, queryClient, ...renderHook(() => useCanPublishCmsPages(pages), { wrapper: createWrapper(queryClient) }) };
    }

    it('is false while the permissions load, and true once every page is granted', async () => {
        const { result, fetchMock } = renderPermission([{ id: 9142, node_id: 3 }, { id: 9143, node_id: 3 }], { 9142: true, 9143: true });

        expect(result.current).toBe(false);
        await waitFor(() => expect(result.current).toBe(true));
        expect(fetchMock.mock.calls.map(([input]) => String(input))).toEqual(['/rest/perm/publish/page/9142?nodeId=3', '/rest/perm/publish/page/9143?nodeId=3']);
    });

    it('is false when one page is not granted', async () => {
        const { result, fetchMock, queryClient } = renderPermission([{ id: 9142, node_id: 3 }, { id: 9143, node_id: 3 }], { 9142: true, 9143: false });

        await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
        await waitFor(() => expect(queryClient.isFetching()).toBe(0));
        expect(result.current).toBe(false);
    });

    it('is false when a permission cannot be read', async () => {
        const { result, fetchMock, queryClient } = renderPermission([{ id: 9142, node_id: 3 }], {});

        await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
        await waitFor(() => expect(queryClient.isFetching()).toBe(0));
        expect(result.current).toBe(false);
    });

    it('is true for no pages, without asking the CMS', () => {
        const { result, fetchMock } = renderPermission([], {});

        expect(result.current).toBe(true);
        expect(fetchMock).not.toHaveBeenCalled();
    });
});

describe('useCmsGuidelines', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    const entry = (name: string, directory = false) => ({ path: `guidelines/${name}`, name, directory, size: 0, lastModified: 0 });

    // Two packages: `corporate` with guidelines, `legal` without a guidelines folder (404).
    function stubPackages() {
        const fetchMock = vi.fn<typeof fetch>(async (input) => {
            const url = new URL(String(input), 'http://app.test');

            if (url.pathname === '/rest/devtools/nodes/3/packages') {
                return Response.json({ items: [{ name: 'corporate' }, { name: 'legal' }] });
            }

            if (url.pathname === '/rest/devtools/packages/corporate/files-internal') {
                return Response.json({ items: [entry('archive', true), entry('20_seo-basics.md'), entry('10_tone-of-voice.default.md'), entry('README.md')] });
            }

            return Response.json({ messages: [] }, { status: 404 });
        });

        vi.stubGlobal('fetch', fetchMock);

        return fetchMock;
    }

    it('lists the guidelines of every package of the node by priority, leaving out directories and other files', async () => {
        const fetchMock = stubPackages();
        const { result } = renderHook(() => useCmsGuidelines(3), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data?.map(({ slug }) => slug)).toEqual(['corporate/tone-of-voice.default', 'corporate/seo-basics']);
        expect(fetchMock.mock.calls.map(([input]) => String(input))).toEqual([
            '/rest/devtools/nodes/3/packages',
            '/rest/devtools/packages/corporate/files-internal?path=guidelines',
            '/rest/devtools/packages/legal/files-internal?path=guidelines',
        ]);
    });

    it('asks nothing without a node', () => {
        const fetchMock = stubPackages();
        const { result } = renderHook(() => useCmsGuidelines(undefined), { wrapper: createWrapper() });

        expect(result.current.isPending).toBe(true);
        expect(fetchMock).not.toHaveBeenCalled();
    });
});

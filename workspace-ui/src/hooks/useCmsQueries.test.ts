import { QueryClient } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { createWrapper } from '@/test/renderWithProviders';

import { useCmsSearch } from './useCmsQueries';

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

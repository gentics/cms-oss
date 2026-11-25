import { QueryClient } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import type { Publication } from '@/services/apiService/genaix/types';
import { createWrapper } from '@/test/renderWithProviders';
import { stubCmsPublishPaths } from '@/test/stubCmsPublishPaths';

import { onlinePublicationOf, publishedUrlFrom, usePublishedUrls } from './usePublishedUrls';

// As GenAIx sends them: the page recorded with its connection, without an address.
function publication(id: number, status: Publication['status']): Publication {
    return { cms_ref: { type: 'page', id, node_id: 1, name: `Page ${id}`, connection_id: 'c-1' }, status, approval_chain: [] };
}

function renderUrls(publications: Publication[]) {
    const wrapper = createWrapper(new QueryClient({ defaultOptions: { queries: { retry: false } } }));

    return renderHook(() => usePublishedUrls(publications, publications.map(({ cms_ref: page }) => page)), { wrapper });
}

describe('usePublishedUrls', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('puts each published page\'s publish path on its node\'s host, read from the CMS', async () => {
        stubCmsPublishPaths({ 72: '/Aufgaben/72.html', 73: '/Aufgaben/73.html' }, { host: 'https://www.example.com', nodeId: 1 });

        const { result } = renderUrls([publication(72, 'published'), publication(73, 'review_requested')]);

        await waitFor(() => expect(result.current[0]?.href).toBe('https://www.example.com/Aufgaben/72.html'));
        expect(result.current[1]).toBeUndefined();
    });

    it('loads only the pages that are online', async () => {
        const fetchMock = stubCmsPublishPaths({ 72: '/Aufgaben/72.html', 73: '/Aufgaben/73.html', 74: '/Aufgaben/74.html' }, { host: 'https://www.example.com', nodeId: 1 });
        const { result } = renderUrls([publication(72, 'published'), publication(73, 'review_requested'), publication(74, 'failed')]);

        await waitFor(() => expect(result.current[0]).toBeDefined());

        const pageLoads = fetchMock.mock.calls.map(([input]) => String(input)).filter((url) => url.includes('/rest/page/load/'));

        expect(pageLoads).toEqual(['/rest/page/load/72?nodeId=1']);
    });

    it('gives no address when the page cannot be read', async () => {
        // The CMS gives no page 72.
        const fetchMock = stubCmsPublishPaths({}, { host: 'https://www.example.com', nodeId: 1 });

        const { result } = renderUrls([publication(72, 'published')]);

        await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes('/rest/page/load/72'))).toBe(true));
        expect(result.current[0]).toBeUndefined();
    });
});

const NOW = Date.parse('2026-11-03T10:00:00Z');

// A publication as GenAIx sends it: the page recorded with its connection, without an address.
function recordedPublication(id: number, status: Publication['status'], extra: Partial<Publication> = {}): Publication {
    return { cms_ref: { type: 'page', id, node_id: 1, name: `Page ${id}`, connection_id: 'c-1' }, status, approval_chain: [], ...extra };
}

describe('onlinePublicationOf', () => {
    const page = { type: 'page' as const, id: 72, node_id: 1 };

    it('finds the publication of a published page, also by a reference without the connection, as a preview has it', () => {
        const published = recordedPublication(72, 'published');

        expect(onlinePublicationOf([recordedPublication(73, 'published'), published], page, NOW)).toBe(published);
    });

    it('finds none for a page in review, failed, pending, scheduled for later, of another connection, or not among the publications', () => {
        expect(onlinePublicationOf([recordedPublication(72, 'review_requested')], page, NOW)).toBeUndefined();
        expect(onlinePublicationOf([recordedPublication(72, 'failed')], page, NOW)).toBeUndefined();
        expect(onlinePublicationOf([recordedPublication(72, 'pending')], page, NOW)).toBeUndefined();
        expect(onlinePublicationOf([recordedPublication(72, 'published', { publish_at: '2026-11-04T08:00:00Z' })], page, NOW)).toBeUndefined();
        expect(onlinePublicationOf([recordedPublication(72, 'published')], { ...page, connection_id: 'c-2' }, NOW)).toBeUndefined();
        expect(onlinePublicationOf([recordedPublication(73, 'published')], page, NOW)).toBeUndefined();
    });

    it('finds a page scheduled for a time already past', () => {
        const scheduled = recordedPublication(72, 'published', { publish_at: '2026-11-03T08:00:00Z' });

        expect(onlinePublicationOf([scheduled], page, NOW)).toBe(scheduled);
    });
});

describe('publishedUrlFrom', () => {
    it('puts the publish path on the node\'s host, with the protocol the host names', () => {
        expect(publishedUrlFrom('https://www.example.com', '/Aufgaben/test-publish.html')?.href).toBe('https://www.example.com/Aufgaben/test-publish.html');
        expect(publishedUrlFrom('http://intranet.example.com', '/news/a.html')?.href).toBe('http://intranet.example.com/news/a.html');
    });

    it('gives none without a host or a path, or for anything but http(s)', () => {
        expect(publishedUrlFrom(undefined, '/a.html')).toBeUndefined();
        expect(publishedUrlFrom('https://www.example.com', undefined)).toBeUndefined();
        expect(publishedUrlFrom('https://www.example.com', '')).toBeUndefined();
        expect(publishedUrlFrom('https://www.example.com', 'javascript:alert(1)')).toBeUndefined();
        expect(publishedUrlFrom('www.example.com', '/a.html')).toBeUndefined();
    });
});

import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { createWrapper } from '@/test/renderWithProviders';

import { useMissingGuidelines } from './useMissingGuidelines';

const entry = (name: string) => ({ path: `guidelines/${name}`, name, directory: false, size: 0, lastModified: 0 });

// The session `s-1` on node 3 (none with `nodeId` undefined), whose package `corporate` has
// `tone-of-voice` (default) and `seo-basics`.
function stubSession(nodeId: number | undefined = 3) {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const url = new URL(String(input), 'http://app.test');

        if (url.pathname === '/rest/proxy/genaix/sessions/s-1') {
            return Response.json({ id: 's-1', context: { ...(nodeId !== undefined && { node_id: nodeId }), references: [], guidelines: [], connection_ids: [] } });
        }

        if (url.pathname === '/rest/devtools/nodes/3/packages') {
            return Response.json({ items: [{ name: 'corporate' }] });
        }

        if (url.pathname === '/rest/devtools/packages/corporate/files-internal') {
            return Response.json({ items: [entry('10_tone-of-voice.default.md'), entry('20_seo-basics.md')] });
        }

        return Response.json({}, { status: 404 });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

describe('useMissingGuidelines', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('names the slugs that are no guideline of the session\'s node', async () => {
        stubSession();
        const slugs = ['corporate/tone-of-voice.default', 'corporate/tone-of-voice', 'corporate/seo-basics', 'other/faq'];
        const { result } = renderHook(() => useMissingGuidelines('s-1', slugs), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current).toEqual(['corporate/tone-of-voice', 'other/faq']));
    });

    it('asks nothing without slugs', () => {
        const fetchMock = stubSession();
        const { result } = renderHook(() => useMissingGuidelines('s-1', []), { wrapper: createWrapper() });

        expect(result.current).toEqual([]);
        expect(fetchMock).not.toHaveBeenCalled();
    });

    it('names none for a session without a node, and asks the CMS nothing', async () => {
        const fetchMock = stubSession(undefined);
        const { result } = renderHook(() => useMissingGuidelines('s-1', ['other/faq']), { wrapper: createWrapper() });

        await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
        expect(result.current).toEqual([]);
    });
});

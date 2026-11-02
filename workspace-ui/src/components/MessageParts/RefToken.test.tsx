import { render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { createWrapper } from '@/test/renderWithProviders';

import { RefToken } from './RefToken';

// The session `s-1` on node 3, whose package `corporate` has the one guideline `seo-basics`.
function stubSession() {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const url = new URL(String(input), 'http://app.test');

        if (url.pathname === '/rest/proxy/genaix/sessions/s-1') {
            return Response.json({ id: 's-1', context: { node_id: 3, references: [], guidelines: [], connection_ids: [] } });
        }

        if (url.pathname === '/rest/devtools/nodes/3/packages') {
            return Response.json({ items: [{ name: 'corporate' }] });
        }

        if (url.pathname === '/rest/devtools/packages/corporate/files-internal') {
            return Response.json({ items: [{ path: 'guidelines/20_seo-basics.md', name: '20_seo-basics.md', directory: false, size: 0, lastModified: 0 }] });
        }

        return Response.json({}, { status: 404 });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

describe('RefToken', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows a guideline the session\'s node has as a reference', async () => {
        const fetchMock = stubSession();

        render(<RefToken value={{ type: 'guideline', id: 'corporate/seo-basics', label: 'SEO basics' }} sessionId="s-1" />, { wrapper: createWrapper() });

        await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3));
        expect(screen.getByText('SEO basics')).toBeInTheDocument();
        expect(screen.queryByText(/was removed or renamed/)).not.toBeInTheDocument();
    });

    it('says a guideline was removed or renamed once the node no longer has it', async () => {
        stubSession();
        render(<RefToken value={{ type: 'guideline', id: 'corporate/tone-of-voice' }} sessionId="s-1" />, { wrapper: createWrapper() });

        expect(await screen.findByText('tone-of-voice was removed or renamed')).toBeInTheDocument();
    });

    it('asks nothing for a reference that is no guideline, or without a session', () => {
        const fetchMock = stubSession();

        render(
            <>
                <RefToken value={{ type: 'page', id: '8871', label: 'Campaign launch' }} sessionId="s-1" />
                <RefToken value={{ type: 'guideline', id: 'corporate/tone-of-voice', label: 'Tone of voice' }} />
            </>,
            { wrapper: createWrapper() },
        );

        expect(screen.getByText('Campaign launch')).toBeInTheDocument();
        expect(screen.getByText('Tone of voice')).toBeInTheDocument();
        expect(fetchMock).not.toHaveBeenCalled();
    });
});

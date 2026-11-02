import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import type { CitationPart } from '@/services/apiService/genaix/types';
import { createWrapper } from '@/test/renderWithProviders';

import { CitationPartView } from './CitationPartView';

// The session `s-1` on node 3, whose package `corporate` has the one guideline `seo-basics`.
function stubSession() {
    vi.stubGlobal('fetch', vi.fn<typeof fetch>(async (input) => {
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
    }));
}

describe('CitationPartView', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('names a cited guideline the node no longer has as removed or renamed, and the others as they are', async () => {
        stubSession();
        const part: CitationPart = {
            type: 'citation',
            refs: [
                { marker: '[1]', ref: { type: 'guideline', id: 'corporate/seo-basics', label: 'SEO basics' } },
                { marker: '[2]', ref: { type: 'guideline', id: 'corporate/tone-of-voice.default', label: 'Tone of voice' } },
                { marker: '[3]', ref: { type: 'url', id: 'https://www.gentics.com' } },
            ],
        };

        render(<CitationPartView part={part} sessionId="s-1" />, { wrapper: createWrapper() });

        expect(await screen.findByText('Tone of voice was removed or renamed')).toBeInTheDocument();
        expect(screen.getByText('SEO basics')).toBeInTheDocument();
        expect(screen.getByText('url https://www.gentics.com')).toBeInTheDocument();
    });
});

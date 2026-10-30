import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import type { Plan, PlanStatus, SessionContext } from '@/services/apiService/genaix/types';
import { createWrapper } from '@/test/renderWithProviders';

import { PlanGuidelines } from './PlanGuidelines';

function plan(status: PlanStatus): Plan {
    return { id: 'pl-1', workflows: [{ id: 'content_create', version: '1.0.0' }], status, version: 1, summary: 'New terms page.', items: [] };
}

const entry = (name: string) => ({ path: `guidelines/${name}`, name, directory: false, size: 0, lastModified: 0 });

// The node's guidelines: `tone-of-voice` (default), `seo-basics` and `legal-notes` (available).
const FILES = [entry('10_tone-of-voice.default.md'), entry('20_seo-basics.md'), entry('30_legal-notes.md')];

// Answers the session with `guidelines` in its context (on node 3 unless `nodeId` is undefined), a patch
// with the session as patched, and the CMS with the package `corporate` and its `FILES`.
function stubSession(guidelines: string[], nodeId: number | undefined = 3) {
    let context: SessionContext = { ...(nodeId !== undefined && { node_id: nodeId }), references: [], guidelines, connection_ids: [] };
    const fetchMock = vi.fn<typeof fetch>(async (input, init) => {
        const url = new URL(String(input), 'http://app.test');

        if (url.pathname === '/rest/proxy/genaix/sessions/s-1') {
            if (init?.method === 'POST') {
                ({ context } = JSON.parse(init.body as string) as { context: SessionContext });
            }

            return Response.json({ id: 's-1', context });
        }

        if (url.pathname === '/rest/devtools/nodes/3/packages') {
            return Response.json({ items: [{ name: 'corporate' }] });
        }

        if (url.pathname === '/rest/devtools/packages/corporate/files-internal') {
            return Response.json({ items: FILES });
        }

        return Response.json({}, { status: 404 });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

// The contexts patched, in order.
function patchedContexts(fetchMock: ReturnType<typeof stubSession>): SessionContext[] {
    return fetchMock.mock.calls
        .filter(([, init]) => init?.method === 'POST')
        .map(([, init]) => (JSON.parse(init?.body as string) as { context: SessionContext }).context);
}

describe('PlanGuidelines', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('lists the guidelines the user added, and none of the default ones', async () => {
        stubSession(['corporate/tone-of-voice.default', 'corporate/seo-basics']);
        render(<PlanGuidelines plan={plan('proposed')} sessionId="s-1" />, { wrapper: createWrapper() });

        expect(await screen.findByText('seo-basics')).toBeInTheDocument();
        expect(screen.queryByText('tone-of-voice')).not.toBeInTheDocument();
    });

    it('offers the available guidelines while the plan is proposed, and adds one to the session context', async () => {
        const user = userEvent.setup();
        const fetchMock = stubSession(['corporate/tone-of-voice.default', 'corporate/seo-basics']);

        render(<PlanGuidelines plan={plan('proposed')} sessionId="s-1" />, { wrapper: createWrapper() });
        await user.click(await screen.findByRole('button', { name: 'Add guideline' }));

        expect(screen.getByRole('menuitemcheckbox', { name: 'seo-basics' })).toHaveAttribute('aria-checked', 'true');
        expect(screen.queryByRole('menuitemcheckbox', { name: 'tone-of-voice' })).not.toBeInTheDocument();

        await user.click(screen.getByRole('menuitemcheckbox', { name: 'legal-notes' }));

        // The whole context goes back, the default guideline in it kept.
        await waitFor(() => expect(patchedContexts(fetchMock)).toEqual([{
            node_id: 3,
            references: [],
            guidelines: ['corporate/tone-of-voice.default', 'corporate/seo-basics', 'corporate/legal-notes'],
            connection_ids: [],
        }]));
        await waitFor(() => expect(within(screen.getByRole('list')).getByText('legal-notes')).toBeInTheDocument());
    });

    it('takes a guideline out of the session context again', async () => {
        const user = userEvent.setup();
        const fetchMock = stubSession(['corporate/tone-of-voice.default', 'corporate/seo-basics']);

        render(<PlanGuidelines plan={plan('proposed')} sessionId="s-1" />, { wrapper: createWrapper() });
        await user.click(await screen.findByRole('button', { name: 'Remove seo-basics' }));

        await waitFor(() => expect(patchedContexts(fetchMock).map(({ guidelines }) => guidelines)).toEqual([['corporate/tone-of-voice.default']]));
        await waitFor(() => expect(screen.queryByText('seo-basics')).not.toBeInTheDocument());
    });

    it('warns of a guideline that was removed or renamed, and lets the user take it out', async () => {
        const user = userEvent.setup();
        // `tone-of-voice` was made a default guideline since, so its old slug is gone; `other/faq` never existed here.
        const fetchMock = stubSession(['corporate/tone-of-voice', 'other/faq']);

        render(<PlanGuidelines plan={plan('proposed')} sessionId="s-1" />, { wrapper: createWrapper() });

        expect(await screen.findByText('tone-of-voice was removed or renamed')).toBeInTheDocument();
        expect(screen.getByText('faq was removed or renamed')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Remove tone-of-voice' }));

        await waitFor(() => expect(patchedContexts(fetchMock).map(({ guidelines }) => guidelines)).toEqual([['other/faq']]));
    });

    it('lists the added guidelines and warns once the plan is accepted, but changes nothing', async () => {
        stubSession(['corporate/seo-basics', 'corporate/tone-of-voice']);
        render(<PlanGuidelines plan={plan('accepted')} sessionId="s-1" />, { wrapper: createWrapper() });

        expect(await screen.findByText('seo-basics')).toBeInTheDocument();
        expect(screen.getByText('tone-of-voice was removed or renamed')).toBeInTheDocument();
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('shows nothing once the plan is accepted when the user added none', async () => {
        const fetchMock = stubSession(['corporate/tone-of-voice.default']);

        render(<PlanGuidelines plan={plan('accepted')} sessionId="s-1" />, { wrapper: createWrapper() });

        await waitFor(() => expect(fetchMock.mock.calls.map(([input]) => String(input))).toContain('/rest/devtools/packages/corporate/files-internal?path=guidelines'));
        expect(screen.queryByRole('region', { name: 'Guidelines' })).not.toBeInTheDocument();
    });

    it('asks the CMS nothing and shows nothing while the session has no node', async () => {
        const fetchMock = stubSession([], undefined);

        render(<PlanGuidelines plan={plan('proposed')} sessionId="s-1" />, { wrapper: createWrapper() });

        await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
        expect(screen.queryByRole('region', { name: 'Guidelines' })).not.toBeInTheDocument();
    });
});

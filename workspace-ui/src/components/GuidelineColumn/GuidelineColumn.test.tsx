import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { Plan, PlanStatus } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { useGuidelineDraftStore } from '@/store/useGuidelineDraftStore';
import { createWrapper } from '@/test/renderWithProviders';

import { GuidelineColumn } from './GuidelineColumn';

const FILE_URL = '/rest/devtools/packages/corporate/files-internal/guidelines/20_seo-basics.md';
// Saving posts to the file's directory, the file named in the multipart part.
const SAVE_URL = '/rest/devtools/packages/corporate/files-internal/guidelines/';

function plan(status: PlanStatus): Plan {
    return {
        id: 'pl-1',
        workflows: [{ id: 'content_edit', version: '1.0.0' }],
        status,
        version: 1,
        summary: 'Sharpen the SEO guideline.',
        items: [{ id: 'p1', label: 'Edit the guideline', kind: 'update', workflow: 'content_edit', status: 'pending', target: { type: 'guideline', id: 'corporate/seo-basics' } }],
    };
}

/**
 * The session on node 3, whose package `corporate` has `20_seo-basics.md` with `# SEO basics`; a
 * save answers `201`. Returns the fetch mock.
 */
function stubRoutes() {
    const fetchMock = vi.fn<typeof fetch>(async (input, init) => {
        const url = new URL(String(input), 'http://localhost');

        if (url.pathname === '/rest/proxy/genaix/sessions/s-1') {
            return Response.json({ id: 's-1', context: { node_id: 3, references: [], guidelines: [], connection_ids: [] } });
        }

        if (url.pathname === '/rest/devtools/nodes/3/packages') {
            return Response.json({ items: [{ name: 'corporate' }] });
        }

        if (url.pathname === '/rest/devtools/packages/corporate/files-internal') {
            return Response.json({ items: [{ path: 'guidelines/20_seo-basics.md', name: '20_seo-basics.md', directory: false, size: 0, lastModified: 0 }] });
        }

        if (init?.method === 'POST' && url.pathname === SAVE_URL) {
            return new Response(null, { status: 201 });
        }

        if (url.pathname === FILE_URL) {
            return new Response('# SEO basics', { headers: { 'Content-Type': 'text/markdown' } });
        }

        return Response.json({}, { status: 404 });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

// The text sent by every save, in order.
async function savedTexts(fetchMock: ReturnType<typeof stubRoutes>): Promise<string[]> {
    const saves = fetchMock.mock.calls.filter(([input, init]) => String(input) === SAVE_URL && init?.method === 'POST');

    return Promise.all(saves.map(([, init]) => {
        const part = (init?.body as FormData).get('file') as File;

        expect(part.name).toBe('20_seo-basics.md');

        return part.text();
    }));
}

function renderColumn(status: PlanStatus, slug = 'corporate/seo-basics') {
    return render(<GuidelineColumn sessionId="s-1" plan={plan(status)} slug={slug} />, { wrapper: createWrapper() });
}

// Whether the page would ask before it goes (`beforeunload` cancelled).
function asksBeforeLeaving(): boolean {
    const event = new Event('beforeunload', { cancelable: true });

    window.dispatchEvent(event);

    return event.defaultPrevented;
}

describe('GuidelineColumn', () => {
    beforeEach(() => {
        useErrorNotificationStore.setState({ errors: [] });
        useGuidelineDraftStore.setState({ drafts: {} });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows the guideline\'s text to edit, and saves the edited text directly through the CMS', async () => {
        const user = userEvent.setup();
        const fetchMock = stubRoutes();

        renderColumn('done');

        expect(screen.getByRole('heading', { name: 'seo-basics' })).toBeInTheDocument();

        const field = await screen.findByRole('textbox', { name: 'Guideline 20_seo-basics.md' });

        expect(field).toHaveValue('# SEO basics');
        expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();

        await user.type(field, '{Enter}{Enter}- Every page has a meta description.');
        await user.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(async () => expect(await savedTexts(fetchMock)).toEqual(['# SEO basics\n\n- Every page has a meta description.']));
        await waitFor(() => expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled());
        expect(field).toHaveValue('# SEO basics\n\n- Every page has a meta description.');
    });

    it('undoes changes not saved yet', async () => {
        const user = userEvent.setup();

        stubRoutes();
        renderColumn('proposed');

        const field = await screen.findByRole('textbox', { name: 'Guideline 20_seo-basics.md' });

        await user.type(field, ' draft');
        await user.click(screen.getByRole('button', { name: 'Undo changes' }));

        expect(field).toHaveValue('# SEO basics');
        expect(screen.getByRole('button', { name: 'Undo changes' })).toBeDisabled();
    });

    it('is read-only while the plan\'s run may write the file', async () => {
        stubRoutes();
        renderColumn('running');

        expect(await screen.findByRole('textbox', { name: 'Guideline 20_seo-basics.md' })).toBeDisabled();
    });

    it('names a guideline the node no longer has as removed or renamed', async () => {
        stubRoutes();
        renderColumn('done', 'corporate/tone-of-voice');

        expect(await screen.findByText('tone-of-voice was removed or renamed')).toBeInTheDocument();
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    });

    it('keeps changes not saved yet when the column goes and comes back', async () => {
        const user = userEvent.setup();

        stubRoutes();
        const { unmount } = renderColumn('done');

        await user.type(await screen.findByRole('textbox', { name: 'Guideline 20_seo-basics.md' }), ' draft');
        unmount();
        renderColumn('done');

        expect(await screen.findByRole('textbox', { name: 'Guideline 20_seo-basics.md' })).toHaveValue('# SEO basics draft');
        expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled();
    });

    it('asks before the page goes only while there are changes not saved yet', async () => {
        const user = userEvent.setup();

        stubRoutes();
        renderColumn('done');

        const field = await screen.findByRole('textbox', { name: 'Guideline 20_seo-basics.md' });

        expect(asksBeforeLeaving()).toBe(false);

        await user.type(field, ' draft');

        expect(asksBeforeLeaving()).toBe(true);

        await user.click(screen.getByRole('button', { name: 'Undo changes' }));

        expect(asksBeforeLeaving()).toBe(false);
    });
});

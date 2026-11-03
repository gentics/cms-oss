import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ensureSessionCmsAuthorization } from '@/helper/sessionAuthorization/sessionAuthorization';
import type { Plan } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';
import { renderWithProviders } from '@/test/renderWithProviders';

import { PlanHead } from './PlanHead';

// The CMS credential has tests of its own (`sessionAuthorization.test.ts`).
vi.mock('@/helper/sessionAuthorization/sessionAuthorization', () => ({
    ensureSessionCmsAuthorization: vi.fn(),
}));

const SESSION = 's-1';
const TRANSITIONS = `/rest/proxy/genaix/sessions/${SESSION}/plans/pl-1/transitions`;
const FILE_URL = '/rest/devtools/packages/corporate/files-internal/content?path=guidelines%2F20_seo-basics.md';

// A done plan whose one item created the guideline `corporate/seo-basics`.
const createdGuideline: Plan = {
    id: 'pl-1',
    workflows: [{ id: 'content_create', version: '1.0.0' }],
    status: 'done',
    version: 1,
    summary: 'New SEO guideline.',
    items: [{ id: 'p1', label: 'Create the guideline', kind: 'create', workflow: 'content_create', status: 'done', created: { type: 'guideline', id: 'corporate/seo-basics' } }],
};

/**
 * The session on node 3, whose package `corporate` has `20_seo-basics.md`; the transition answers with
 * `transition` (default `200` with the plan discarded), deleting the file with `deleted` (default `204`).
 * Returns the method and URL of every call, in order.
 */
function stubRoutes({ transition, deleted = 204 }: { transition?: Response; deleted?: number } = {}) {
    const calls: string[] = [];

    vi.stubGlobal('fetch', vi.fn<typeof fetch>(async (input, init) => {
        const url = new URL(String(input), 'http://localhost');
        const method = init?.method ?? 'GET';

        calls.push(`${method} ${url.pathname}${url.search}`);

        if (url.pathname === `/rest/proxy/genaix/sessions/${SESSION}`) {
            return Response.json({ id: SESSION, context: { node_id: 3, references: [], guidelines: [], connection_ids: [] } });
        }

        if (url.pathname === '/rest/devtools/nodes/3/packages') {
            return Response.json({ items: [{ name: 'corporate' }] });
        }

        if (url.pathname === '/rest/devtools/packages/corporate/files-internal') {
            return Response.json({ items: [{ path: 'guidelines/20_seo-basics.md', name: '20_seo-basics.md', directory: false, size: 0, lastModified: 0 }] });
        }

        if (url.pathname === TRANSITIONS) {
            return transition ?? Response.json({ ...createdGuideline, status: 'discarded' });
        }

        if (method === 'DELETE') {
            return new Response(null, { status: deleted });
        }

        return Response.json({ items: [], next_cursor: null });
    }));

    return calls;
}

async function discard() {
    const user = userEvent.setup();

    renderWithProviders(<PlanHead sessionId={SESSION} plan={createdGuideline} />, { path: `/sessions/${SESSION}` });
    await user.click(await screen.findByRole('button', { name: 'Discard' }));

    const dialog = await screen.findByRole('dialog', { name: 'Discard this plan?' });

    expect(await within(dialog).findByText('The plan is closed and the guideline seo-basics it created is deleted.')).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'Discard' }));
}

describe('PlanHead', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
        useErrorNotificationStore.setState({ errors: [] });
        vi.mocked(ensureSessionCmsAuthorization).mockResolvedValue();
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.clearAllMocks();
    });

    it('deletes the guideline a discarded plan created, once the plan is discarded', async () => {
        const calls = stubRoutes();

        await discard();

        await waitFor(() => expect(calls).toContain(`DELETE ${FILE_URL}`));
        expect(calls.indexOf(`POST ${TRANSITIONS}`)).toBeLessThan(calls.indexOf(`DELETE ${FILE_URL}`));
        expect(useErrorNotificationStore.getState().errors).toEqual([]);
    });

    it('takes a guideline that is already gone as deleted', async () => {
        const calls = stubRoutes({ deleted: 404 });

        await discard();

        await waitFor(() => expect(calls).toContain(`DELETE ${FILE_URL}`));
        expect(useErrorNotificationStore.getState().errors).toEqual([]);
    });

    it('deletes nothing when the discard fails', async () => {
        const calls = stubRoutes({ transition: Response.json({ type: 't', title: 't', status: 409, genaix_code: 'plan_closed' }, { status: 409 }) });

        await discard();

        await waitFor(() => expect(useErrorNotificationStore.getState().errors).toMatchObject([{ messageKey: 'planBar.transitionFailed' }]));
        expect(calls.filter((call) => call.startsWith('DELETE'))).toEqual([]);
    });
});

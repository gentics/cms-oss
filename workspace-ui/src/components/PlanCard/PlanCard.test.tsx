import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ensureSessionCmsAuthorization } from '@/helper/sessionAuthorization/sessionAuthorization';
import type { Plan, PlanItem } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';
import { createWrapper } from '@/test/renderWithProviders';

import { PlanCard } from './PlanCard';

// The CMS credential has tests of its own (`sessionAuthorization.test.ts`).
vi.mock('@/helper/sessionAuthorization/sessionAuthorization', () => ({
    ensureSessionCmsAuthorization: vi.fn(),
}));

const PAGE = { type: 'page' as const, id: '9142', node_id: 3, label: 'Neue Nutzungsbedingungen ab Q3' };

const PLAN: Plan = {
    id: '5b0e7c2a-91d4-4f6b-8a3e-2c7d9f1e4a60',
    workflows: [{ id: 'content_create', version: '1.0.0' }],
    status: 'proposed',
    version: 2,
    summary: 'Create the page in Richtlinien.',
    items: [
        { id: 'p1', label: 'Create the page', kind: 'create', workflow: 'content_create', target: PAGE, status: 'pending' },
        { id: 'p2', label: 'Write the intro', kind: 'create', workflow: 'content_create', status: 'pending' },
    ],
};

// The PNDemo plan of S8 while its third version is proposed (rc.5): p1 and p2 done earlier, p3 new, p4
// new and depending on p1 and p3, and a step of the accepted version the revision removed.
const REVISION: Plan = {
    id: 'd4a7e1c9-6b52-4f08-9e3d-2a7c5b8f1e60',
    workflows: [{ id: 'content_create', version: '1.0.0' }, { id: 'content_edit', version: '1.0.0' }],
    status: 'proposed',
    version: 3,
    accepted_version: 2,
    summary: 'Invitation page PNDemo.',
    items: [
        { id: 'p1', label: 'Create page PNDemo', kind: 'create', workflow: 'content_create', status: 'done', change: 'unchanged' },
        { id: 'p2', label: 'Colour title and teaser', kind: 'update', workflow: 'content_edit', status: 'done', change: 'unchanged', depends_on: ['p1'] },
        { id: 'p3', label: 'Upload the PDF', kind: 'create', workflow: 'content_create', status: 'pending', change: 'added' },
        { id: 'p4', label: 'Link the PDF in the text', kind: 'link', workflow: 'content_edit', status: 'pending', change: 'added', depends_on: ['p1', 'p3'] },
        { id: 'p5', label: 'Add a teaser image', kind: 'update', workflow: 'content_edit', status: 'removed', change: 'removed' },
    ],
};

// Answers every request with `response` (an accepted message by default) and returns the mock, to read what was posted.
function stubFetch(response = () => Response.json({ message_id: 'm-1', run_id: 'r-1', session_id: 's-1' }, { status: 202 })) {
    const fetchMock = vi.fn<typeof fetch>(async () => response());

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

async function postedBody(fetchMock: ReturnType<typeof stubFetch>): Promise<unknown> {
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());

    const [input, init] = fetchMock.mock.calls[0]!;

    expect(String(input)).toBe('/rest/proxy/genaix/sessions/s-1/messages');

    return JSON.parse(String(init?.body));
}

describe('PlanCard', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
        useErrorNotificationStore.setState({ errors: [] });
        vi.mocked(ensureSessionCmsAuthorization).mockResolvedValue();
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.clearAllMocks();
    });

    it('accepts the plan as proposed with a plan part naming the plan and its version', async () => {
        const user = userEvent.setup();
        const fetchMock = stubFetch();

        render(<PlanCard plan={PLAN} sessionId="s-1" canAnswer />, { wrapper: createWrapper() });
        await user.click(screen.getByRole('button', { name: 'Accept plan' }));

        expect(await postedBody(fetchMock)).toEqual({
            parts: [{ type: 'plan', plan: { plan_id: PLAN.id, based_on_version: 2 } }],
            options: { mode: 'execute' },
        });
    });

    it('accepts the adjusted plan with its steps in order, kept steps by their id, a new one without', async () => {
        const user = userEvent.setup();
        const fetchMock = stubFetch();

        render(<PlanCard plan={PLAN} sessionId="s-1" canAnswer />, { wrapper: createWrapper() });
        await user.click(screen.getByRole('button', { name: 'Adjust' }));
        await user.clear(screen.getByRole('textbox', { name: 'Step 2' }));
        await user.type(screen.getByRole('textbox', { name: 'Step 2' }), 'Write a short intro');
        await user.click(screen.getByRole('button', { name: 'Add a step' }));
        await user.type(screen.getByRole('textbox', { name: 'Step 3' }), 'Add an FAQ block');
        await user.click(screen.getByRole('button', { name: 'Done' }));
        await user.click(screen.getByRole('button', { name: 'Accept plan' }));

        expect(await postedBody(fetchMock)).toEqual({
            parts: [{
                type: 'plan',
                plan: {
                    plan_id: PLAN.id,
                    based_on_version: 2,
                    items: [
                        { id: 'p1', label: 'Create the page', target: PAGE },
                        { id: 'p2', label: 'Write a short intro' },
                        { label: 'Add an FAQ block' },
                    ],
                },
            }],
            options: { mode: 'execute' },
        });
    });
    it('marks what a proposed revision added, and strikes a removed step through (rc.5)', () => {
        render(<PlanCard plan={REVISION} sessionId="s-1" canAnswer />, { wrapper: createWrapper() });

        const [p1, , p3, , p5] = screen.getAllByRole('listitem');

        expect(p1).toHaveTextContent('Create page PNDemo');
        expect(p1).not.toHaveTextContent('New');
        expect(p3).toHaveTextContent('Upload the PDFNew');
        expect(p5).toHaveTextContent('Removed');
    });

    it('marks nothing on a plan that was never accepted, where every step is added', () => {
        const items: PlanItem[] = PLAN.items.map((item) => ({ ...item, change: 'added' }));

        render(<PlanCard plan={{ ...PLAN, items }} sessionId="s-1" canAnswer />, { wrapper: createWrapper() });

        expect(screen.queryByText('New')).not.toBeInTheDocument();
    });

    it('accepts a revision as proposed without sending the removed step', async () => {
        const user = userEvent.setup();
        const fetchMock = stubFetch();

        render(<PlanCard plan={REVISION} sessionId="s-1" canAnswer />, { wrapper: createWrapper() });
        await user.click(screen.getByRole('button', { name: 'Accept plan' }));

        expect(await postedBody(fetchMock)).toEqual({
            parts: [{ type: 'plan', plan: { plan_id: REVISION.id, based_on_version: 3 } }],
            options: { mode: 'execute' },
        });
    });

    it('keeps done steps read-only while adjusting, and blocks what breaks an order of dependencies', async () => {
        const user = userEvent.setup();

        render(<PlanCard plan={REVISION} sessionId="s-1" canAnswer />, { wrapper: createWrapper() });
        await user.click(screen.getByRole('button', { name: 'Adjust' }));

        // Only the pending steps are fields; p1 and p2 are done.
        expect(screen.getAllByRole('textbox')).toHaveLength(2);
        expect(screen.queryByRole('textbox', { name: 'Step 1' })).not.toBeInTheDocument();

        const [upP3, upP4] = screen.getAllByRole('button', { name: 'Move step up' });
        const [downP3, downP4] = screen.getAllByRole('button', { name: 'Move step down' });
        const [removeP3, removeP4] = screen.getAllByRole('button', { name: 'Remove step' });

        // p3 cannot pass the done p2, nor move below p4, which needs it; p4 cannot move above p3.
        expect(upP3).toBeDisabled();
        expect(downP3).toBeDisabled();
        expect(upP4).toBeDisabled();
        expect(downP4).toBeDisabled();
        // p4 needs p3, so p3 cannot be removed; p4 can.
        expect(removeP3).toBeDisabled();
        expect(removeP4).toBeEnabled();
    });

    it('sends a removed step again only once the user restored it', async () => {
        const user = userEvent.setup();
        const fetchMock = stubFetch();

        render(<PlanCard plan={REVISION} sessionId="s-1" canAnswer />, { wrapper: createWrapper() });
        await user.click(screen.getByRole('button', { name: 'Adjust' }));
        await user.click(screen.getByRole('button', { name: 'Restore' }));
        await user.click(screen.getByRole('button', { name: 'Done' }));
        await user.click(screen.getByRole('button', { name: 'Accept plan' }));

        expect(await postedBody(fetchMock)).toEqual({
            parts: [{
                type: 'plan',
                plan: {
                    plan_id: REVISION.id,
                    based_on_version: 3,
                    items: [
                        { id: 'p1', label: 'Create page PNDemo' },
                        { id: 'p2', label: 'Colour title and teaser' },
                        { id: 'p3', label: 'Upload the PDF' },
                        { id: 'p4', label: 'Link the PDF in the text' },
                        { id: 'p5', label: 'Add a teaser image' },
                    ],
                },
            }],
            options: { mode: 'execute' },
        });
    });

    it('highlights the step the server refused with 422 instead of an error notification', async () => {
        const user = userEvent.setup();

        stubFetch(() => Response.json({
            type: 't',
            title: 'Validation failed',
            status: 422,
            genaix_code: 'validation_failed',
            errors: [{ pointer: '/parts/0/plan/items/1', detail: 'p2 depends on p1', code: 'dependency_order' }],
        }, { status: 422 }));

        render(<PlanCard plan={PLAN} sessionId="s-1" canAnswer />, { wrapper: createWrapper() });
        await user.click(screen.getByRole('button', { name: 'Adjust' }));
        await user.clear(screen.getByRole('textbox', { name: 'Step 2' }));
        await user.type(screen.getByRole('textbox', { name: 'Step 2' }), 'Write a short intro');
        await user.click(screen.getByRole('button', { name: 'Done' }));
        await user.click(screen.getByRole('button', { name: 'Accept plan' }));

        expect(await screen.findByText('Comes before a step it needs')).toBeInTheDocument();
        expect(screen.getAllByRole('listitem')[1]).toHaveTextContent('Write a short intro');
        expect(screen.getAllByRole('listitem')[1]).toHaveTextContent('Comes before a step it needs');
        expect(screen.getByText('Adjust the highlighted steps')).toBeInTheDocument();
        expect(useErrorNotificationStore.getState().errors).toEqual([]);
    });
});

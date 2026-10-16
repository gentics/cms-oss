import { act, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { GenaixEvent, Plan, PlanStatus } from '@/services/apiService/genaix/types';
import { useRevisePlanStore } from '@/store/useRevisePlanStore';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';
import { renderWithProviders } from '@/test/renderWithProviders';

import { PlanBar } from './PlanBar';

const SESSION = 's-1';

// jsdom has no ResizeObserver; the plan bar reports its height with one.
class ResizeObserverStub {
    observe() {}

    disconnect() {}
}

function plan(status: PlanStatus, overrides: Partial<Plan> = {}): Plan {
    return { id: 'pl-1', workflows: [{ id: 'content_create', version: '1.0.0' }], status, version: 1, summary: 'New terms page.', items: [], ...overrides };
}

async function renderBar(status: PlanStatus, overrides: Partial<Plan> = {}) {
    renderWithProviders(<PlanBar sessionId={SESSION} plan={plan(status, overrides)} />, { path: `/sessions/${SESSION}` });

    return screen.findByRole('region', { name: 'Open plan' });
}

describe('PlanBar', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
        useRevisePlanStore.setState({ planIds: {} });
        vi.stubGlobal('ResizeObserver', ResizeObserverStub);
        vi.stubGlobal('fetch', vi.fn<typeof fetch>(async () => Response.json({ items: [], next_cursor: null })));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('offers no release or publish, only the way to the review page, which opens this plan', async () => {
        const bar = await renderBar('done');

        expect(within(bar).getByRole('button', { name: 'Discard' })).toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Release' })).not.toBeInTheDocument();
        expect(within(bar).getByRole('link', { name: 'Review & release' })).toHaveAttribute('href', `/sessions/${SESSION}/review?plan=pl-1`);
    });

    it('keeps a released plan\'s publish and request review for the review page', async () => {
        const bar = await renderBar('released');

        expect(within(bar).getByRole('button', { name: 'Ask for a change' })).toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Request review' })).not.toBeInTheDocument();
        expect(within(bar).getByRole('link', { name: 'Review & release' })).toBeInTheDocument();
    });

    it('leads a running plan to the review page too, where it can be released', async () => {
        const bar = await renderBar('running');

        expect(within(bar).queryByRole('button', { name: 'Release' })).not.toBeInTheDocument();
        expect(within(bar).getByRole('link', { name: 'Review & release' })).toBeInTheDocument();
    });

    it('puts the composer into revising a finished plan with Ask for a change (rc.5)', async () => {
        const user = userEvent.setup();
        const bar = await renderBar('done');

        await user.click(within(bar).getByRole('button', { name: 'Ask for a change' }));

        expect(useRevisePlanStore.getState().planIds).toEqual({ [SESSION]: 'pl-1' });
        expect(within(bar).getByRole('button', { name: 'Ask for a change' })).toHaveAttribute('aria-pressed', 'true');
    });

    it('offers no Stop and change while the plan executes (GenAIx\'s router redirects it on a message)', async () => {
        const bar = await renderBar('running');

        expect(within(bar).queryByRole('button', { name: 'Stop and change' })).not.toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Stop' })).not.toBeInTheDocument();
    });

    it('stops the plan\'s run with Stop', async () => {
        const user = userEvent.setup();
        const fetchMock = vi.fn<typeof fetch>(async () => Response.json({ id: 'r-1', status: 'cancelling' }, { status: 202 }));

        useWorkspaceEventStore.getState().applyEvent(SESSION, {
            seq: 1,
            ts: '2026-10-14T14:00:01Z',
            session_id: SESSION,
            run_id: 'r-1',
            plan_id: 'pl-1',
            type: 'run.started',
            run: { id: 'r-1', status: 'running', plan_id: 'pl-1', started_at: '2026-10-14T14:00:01Z', step_count: 0 },
        });

        const bar = await renderBar('running');

        vi.stubGlobal('fetch', fetchMock);
        await user.click(within(bar).getByRole('button', { name: 'Stop' }));

        expect(fetchMock.mock.calls.map(([input, init]) => [String(input), init?.method])).toContainEqual(['/rest/proxy/genaix/sessions/s-1/runs/r-1/cancel', 'POST']);
    });

    it('says only "Done" and the pages to publish in the head of a done plan, without the glance', async () => {
        const items: Plan['items'] = [
            { id: 'p1', label: 'Edit Versand und Lieferung', kind: 'update', workflow: 'content_edit', status: 'done', target: { type: 'page', id: '8644', node_id: 3 } },
        ];

        useWorkspaceEventStore.getState().applyEvent(SESSION, {
            seq: 1,
            ts: '2026-10-15T09:00:00Z',
            session_id: SESSION,
            plan_id: 'pl-1',
            type: 'publications.updated',
            publications: [{ cms_ref: { type: 'page', id: 8644, node_id: 3 }, status: 'pending', approval_chain: [] }],
        } as GenaixEvent);

        const bar = await renderBar('done', { items });
        const head = within(bar).getByText('Done').parentElement!;

        expect(head).toHaveTextContent(/^Done · 1 page to publish$/);
    });

    it('shows the plan at a glance in the head while collapsed', async () => {
        const user = userEvent.setup();
        const items: Plan['items'] = [
            { id: 'p1', label: 'Create folder Events', kind: 'create', workflow: 'content_create', status: 'done', creates: { type: 'folder', label: 'Events' } },
            { id: 'p2', label: 'Create page Fair', kind: 'create', workflow: 'content_create', status: 'pending', creates: { type: 'page', label: 'Fair' } },
            { id: 'p3', label: 'Fill the hero', kind: 'construct', workflow: 'content_create', status: 'pending', parent: 'p2' },
        ];
        const bar = await renderBar('running', { items });

        expect(within(bar).getByText(/1\/1 folder · 0\/1 new page/)).toBeInTheDocument();

        await user.click(within(bar).getByRole('button', { name: 'Show plan' }));

        expect(within(bar).queryByText(/1\/1 folder · 0\/1 new page/)).not.toBeInTheDocument();
        expect(within(bar).getByRole('group', { name: 'At a glance' })).toHaveTextContent('1/1 folder');
    });

    it('expands by itself for a proposal, but not for a question of its run, which is answered in the chat', async () => {
        useWorkspaceEventStore.getState().applyEvent(SESSION, {
            seq: 1,
            ts: '2026-10-15T09:00:00Z',
            session_id: SESSION,
            run_id: 'r-1',
            plan_id: 'pl-1',
            type: 'interaction.requested',
            interaction: { id: 'i-1', kind: 'settings_review', prompt: 'Check the settings', multi: false, other_allowed: false, blocking: true, status: 'pending', requested_at: '2026-10-15T09:00:00Z', expires_at: '2099-01-01T00:00:00Z', run_id: 'r-1' },
        });

        const items: Plan['items'] = [{ id: 'p1', label: 'Create page PNDemo', kind: 'create', workflow: 'content_create', status: 'done' }];
        const bar = await renderBar('drafting', { items });

        expect(within(bar).getByRole('button', { name: 'Show plan' })).toHaveAttribute('aria-expanded', 'false');
    });

    // The module catalogue (`GET /workflows`) with `plan_approval` per module; every other read gets an empty list.
    function stubWorkflows(planApproval: Record<string, boolean>) {
        vi.stubGlobal('fetch', vi.fn<typeof fetch>(async (input) => (String(input).endsWith('/workflows')
            ? Response.json({ items: Object.entries(planApproval).map(([id, approval]) => ({ id, version: '1.0.0', title: id, description: '', roles: [], optional: false, steps_template: [], plan_approval: approval })) })
            : Response.json({ items: [], next_cursor: null }))));
    }

    it('expands by itself while a plan accepted on its card is proposed', async () => {
        stubWorkflows({ content_create: true });

        const bar = await renderBar('proposed');

        expect(await within(bar).findByRole('button', { name: 'Hide plan' })).toHaveAttribute('aria-expanded', 'true');
    });

    describe('a plan confirmed in the chat (plan_approval: false): its head alone', () => {
        const quickEdit = { workflows: [{ id: 'content_quick_edit', version: '1.0.0' }] };

        it('offers Stop while it runs, with no steps to expand and no review yet', async () => {
            stubWorkflows({ content_quick_edit: false });
            useWorkspaceEventStore.getState().applyEvent(SESSION, {
                seq: 1,
                ts: '2026-10-16T10:39:39Z',
                session_id: SESSION,
                run_id: 'r-1',
                plan_id: 'pl-1',
                type: 'run.started',
                run: { id: 'r-1', status: 'running', plan_id: 'pl-1', started_at: '2026-10-16T10:39:39Z', step_count: 0 },
            });

            const bar = await renderBar('running', quickEdit);

            await vi.waitFor(() => expect(within(bar).queryByRole('button', { name: 'Show plan' })).not.toBeInTheDocument());
            expect(within(bar).getByRole('button', { name: 'Stop' })).toBeInTheDocument();
            expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
        });

        it('offers "Review & release" once it is done with a page to publish', async () => {
            stubWorkflows({ content_quick_edit: false });
            useWorkspaceEventStore.getState().applyEvent(SESSION, {
                seq: 1,
                ts: '2026-10-16T10:39:45Z',
                session_id: SESSION,
                plan_id: 'pl-1',
                type: 'publications.updated',
                publications: [{ cms_ref: { type: 'page', id: 41, node_id: 1 }, status: 'pending', approval_chain: [] }],
            } as GenaixEvent);

            const bar = await renderBar('done', quickEdit);

            await vi.waitFor(() => expect(within(bar).queryByRole('button', { name: 'Show plan' })).not.toBeInTheDocument());
            expect(within(bar).getByRole('link', { name: 'Review & release' })).toBeInTheDocument();
            expect(within(bar).getByText('Done').parentElement).toHaveTextContent(/^Done · 1 page to publish$/);
        });
    });

    it('stays collapsed for a plan its run confirms in the chat (plan_approval: false)', async () => {
        stubWorkflows({ content_quick_edit: false });

        const bar = await renderBar('proposed', { workflows: [{ id: 'content_quick_edit', version: '1.0.0' }] });

        // Collapsed also once the catalogue was read, not only before it came.
        await vi.waitFor(() => expect(vi.mocked(fetch).mock.calls.some(([input]) => String(input).endsWith('/workflows'))).toBe(true));
        await act(() => new Promise((resolve) => setTimeout(resolve, 0)));
        expect(within(bar).getByRole('button', { name: 'Show plan' })).toHaveAttribute('aria-expanded', 'false');
    });

    it('shows a revision being drafted with its items, offering discard only', async () => {
        const items: Plan['items'] = [{ id: 'p1', label: 'Create page PNDemo', kind: 'create', workflow: 'content_create', status: 'done' }];
        const bar = await renderBar('drafting', { items });

        expect(within(bar).getByText('Being revised')).toBeInTheDocument();
        expect(within(bar).getByRole('button', { name: 'Discard' })).toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Ask for a change' })).not.toBeInTheDocument();
    });

    it('has a review link unless every module of the plan writes nothing (rc.5: several modules)', async () => {
        const research = { id: 'content_research', version: '1.0.0' };
        const bar = await renderBar('done', { workflows: [research, { id: 'content_create', version: '1.0.0' }] });

        expect(within(bar).getByRole('link', { name: 'Review & release' })).toBeInTheDocument();
    });

    it('has no review link for a plan that only reads', async () => {
        const bar = await renderBar('done', { workflows: [{ id: 'content_research', version: '1.0.0' }] });

        expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
    });

    it('has no review link while the plan is only proposed', async () => {
        const bar = await renderBar('proposed');

        expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
    });
});

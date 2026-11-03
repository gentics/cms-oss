import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { CmsObjectRef, GenaixEvent, Plan, PlanStatus } from '@/services/apiService/genaix/types';
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

// A page the plan wrote, as `PlanState.cms_objects` lists it.
const WRITTEN_PAGE: CmsObjectRef = { type: 'page', id: 9142, node_id: 3, operation: 'created' };

// Answers the plan's state (`GET …/plans/pl-1`) with `cmsObjects`, every list with an empty page.
function stubPlanState(cmsObjects: CmsObjectRef[]) {
    vi.stubGlobal('fetch', vi.fn<typeof fetch>(async (input) => (
        String(input).endsWith(`/sessions/${SESSION}/plans/pl-1`)
            ? Response.json({ plan: plan('done'), cms_objects: cmsObjects })
            : Response.json({ items: [], next_cursor: null })
    )));
}

// The head's line of text (a `span`), not the head row around it, which reads the same while its buttons
// are icons only (Discard, the toggle).
function headLine(text: string) {
    return (_: string, element: Element | null) => element?.tagName === 'SPAN' && element.textContent === text;
}

async function renderBar(status: PlanStatus, overrides: Partial<Plan> = {}) {
    renderWithProviders(<PlanBar sessionId={SESSION} plan={plan(status, overrides)} />, { path: `/sessions/${SESSION}` });

    return screen.findByRole('region', { name: 'Open plan' });
}

describe('PlanBar', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
        vi.stubGlobal('ResizeObserver', ResizeObserverStub);
        stubPlanState([WRITTEN_PAGE]);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('offers no release or publish, only the way to the review page, which opens this plan', async () => {
        const bar = await renderBar('done');

        expect(within(bar).queryByRole('button', { name: 'Release' })).not.toBeInTheDocument();
        expect(await within(bar).findByRole('link', { name: 'Review & release' })).toHaveAttribute('href', `/sessions/${SESSION}/review?plan=pl-1`);
    });

    it('offers discard with the other buttons, asking first', async () => {
        const user = userEvent.setup();
        const bar = await renderBar('done');

        await user.click(within(bar).getByRole('button', { name: 'Discard' }));

        expect(await screen.findByRole('dialog', { name: 'Discard this plan?' })).toBeInTheDocument();
    });

    it('keeps a released plan\'s publish and request review for the review page', async () => {
        const bar = await renderBar('released');

        // A change is asked for with a plain message, which GenAIx's session agent tells apart (contract §4.4).
        expect(within(bar).queryByRole('button', { name: 'Ask for a change' })).not.toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Request review' })).not.toBeInTheDocument();
        expect(await within(bar).findByRole('link', { name: 'Review & release' })).toBeInTheDocument();
    });

    it('has no review link while the plan is still running, even with a page written', async () => {
        const bar = await renderBar('running');

        // The plan's state, which lists a page, is not even asked for while the plan runs.
        await waitFor(() => expect(vi.mocked(fetch).mock.calls.length).toBeGreaterThan(0));
        expect(vi.mocked(fetch).mock.calls.map(([input]) => String(input))).not.toContainEqual(expect.stringMatching(/\/plans\/pl-1$/));
        expect(within(bar).queryByRole('button', { name: 'Release' })).not.toBeInTheDocument();
        expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
    });

    it('has no review link for a done plan that wrote no page', async () => {
        const fetchMock = vi.fn<typeof fetch>(async (input) => (
            String(input).endsWith(`/sessions/${SESSION}/plans/pl-1`)
                ? Response.json({ plan: plan('done'), cms_objects: [{ ...WRITTEN_PAGE, operation: 'read' }] })
                : Response.json({ items: [], next_cursor: null })
        ));

        vi.stubGlobal('fetch', fetchMock);
        const bar = await renderBar('done');

        await waitFor(() => expect(fetchMock.mock.calls.map(([input]) => String(input))).toContainEqual(expect.stringMatching(/\/plans\/pl-1$/)));
        expect(within(bar).getByRole('button', { name: 'Discard' })).toBeInTheDocument();
        expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
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

        // The head's line alone (the folded plan card has a step "Done" of its own).
        expect(within(bar).getByText(headLine('Done · 1 page to publish'))).toBeInTheDocument();
    });

    it('says only the status in the head while collapsed, the plan at a glance only in the plan itself', async () => {
        const user = userEvent.setup();
        const items: Plan['items'] = [
            { id: 'p1', label: 'Create folder Events', kind: 'create', workflow: 'content_create', status: 'done', creates: { type: 'folder', label: 'Events' } },
            { id: 'p2', label: 'Create page Fair', kind: 'create', workflow: 'content_create', status: 'pending', creates: { type: 'page', label: 'Fair' } },
            { id: 'p3', label: 'Fill the hero', kind: 'construct', workflow: 'content_create', status: 'pending', parent: 'p2' },
        ];
        const bar = await renderBar('running', { items });

        expect(within(bar).queryByText(/1\/1 folder · 0\/1 new page/)).not.toBeInTheDocument();

        await user.click(within(bar).getByRole('button', { name: 'Show plan' }));

        expect(within(bar).getByRole('group', { name: 'At a glance' })).toHaveTextContent('1/1 folder');
    });

    it('offers Adjust and Accept plan in the head, also collapsed; Adjust expands it, Done says so after the status', async () => {
        const user = userEvent.setup();
        const items: Plan['items'] = [
            { id: 'p1', label: 'Create the page', kind: 'create', workflow: 'content_create', status: 'pending' },
            { id: 'p2', label: 'Write the intro', kind: 'create', workflow: 'content_create', status: 'pending' },
        ];
        const bar = await renderBar('proposed', { items });

        expect(within(bar).getByRole('button', { name: 'Show plan' })).toHaveAttribute('aria-expanded', 'false');
        expect(within(bar).getByRole('button', { name: 'Accept plan' })).toBeInTheDocument();

        await user.click(within(bar).getByRole('button', { name: 'Adjust' }));

        expect(within(bar).getByRole('button', { name: 'Hide plan' })).toHaveAttribute('aria-expanded', 'true');

        await user.clear(within(bar).getByRole('textbox', { name: 'Step 2' }));
        await user.type(within(bar).getByRole('textbox', { name: 'Step 2' }), 'Write a short intro');
        await user.click(within(bar).getByRole('button', { name: 'Done' }));

        expect(within(bar).getByText(headLine('Proposed · Adjusted by you'))).toBeInTheDocument();
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

    // The module catalogue (`GET /workflows`) with `plan_approval` per module, the plan's state with a page it
    // wrote; every other read gets an empty list.
    function stubWorkflows(planApproval: Record<string, boolean>) {
        vi.stubGlobal('fetch', vi.fn<typeof fetch>(async (input) => {
            const url = String(input);

            if (url.endsWith('/workflows')) {
                return Response.json({ items: Object.entries(planApproval).map(([id, approval]) => ({ id, version: '1.0.0', title: id, description: '', roles: [], optional: false, steps_template: [], plan_approval: approval })) });
            }

            return url.endsWith(`/sessions/${SESSION}/plans/pl-1`)
                ? Response.json({ plan: plan('done'), cms_objects: [WRITTEN_PAGE] })
                : Response.json({ items: [], next_cursor: null });
        }));
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
            expect(await within(bar).findByRole('link', { name: 'Review & release' })).toBeInTheDocument();
            expect(within(bar).getByText('Done').parentElement).toHaveTextContent(/^Done · 1 page to publish$/);
        });
    });

    it('has nothing to expand for a plan its run confirms in the chat (plan_approval: false): its head alone', async () => {
        stubWorkflows({ content_quick_edit: false });

        const bar = await renderBar('proposed', { workflows: [{ id: 'content_quick_edit', version: '1.0.0' }] });

        // Once the catalogue was read: no toggle, and no steps to show.
        await vi.waitFor(() => expect(within(bar).queryByRole('button', { name: 'Show plan' })).not.toBeInTheDocument());
        expect(within(bar).queryByRole('button', { name: 'Hide plan' })).not.toBeInTheDocument();
        expect(within(bar).queryByRole('list')).not.toBeInTheDocument();
    });

    it('shows a revision being drafted as such, offering discard only', async () => {
        const items: Plan['items'] = [{ id: 'p1', label: 'Create page PNDemo', kind: 'create', workflow: 'content_create', status: 'done' }];
        const bar = await renderBar('drafting', { items });

        expect(within(bar).getByText('Being revised')).toBeInTheDocument();
        expect(within(bar).getByRole('button', { name: 'Discard' })).toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Ask for a change' })).not.toBeInTheDocument();
    });

    it('says to tell GenAIx how to go on with a plan its run left drafting (failed or stopped)', async () => {
        const items: Plan['items'] = [{ id: 'p1', label: 'Create page PNDemo', kind: 'create', workflow: 'content_create', status: 'pending' }];
        const bar = await renderBar('drafting', { items });

        expect(within(bar).getByText('Tell GenAIx how to continue with this plan')).toBeInTheDocument();
        expect(within(bar).getByRole('button', { name: 'Discard' })).toBeInTheDocument();
    });

    it('says the same for a plan left accepted, whose release is made on the review page', async () => {
        const bar = await renderBar('accepted');

        expect(within(bar).getByText('Tell GenAIx how to continue with this plan')).toBeInTheDocument();
    });

    it('says nothing of the kind while the plan\'s run works', async () => {
        useWorkspaceEventStore.getState().applyEvent(SESSION, {
            seq: 1,
            ts: '2026-10-14T14:00:01Z',
            session_id: SESSION,
            run_id: 'r-1',
            plan_id: 'pl-1',
            type: 'run.started',
            run: { id: 'r-1', status: 'running', plan_id: 'pl-1', started_at: '2026-10-14T14:00:01Z', step_count: 0 },
        });

        const bar = await renderBar('drafting');

        expect(within(bar).getByRole('button', { name: 'Stop' })).toBeInTheDocument();
        expect(within(bar).queryByText('Tell GenAIx how to continue with this plan')).not.toBeInTheDocument();
    });

    it('has a review link unless every module of the plan writes nothing (rc.5: several modules)', async () => {
        const research = { id: 'content_research', version: '1.0.0' };
        const bar = await renderBar('done', { workflows: [research, { id: 'content_create', version: '1.0.0' }] });

        expect(await within(bar).findByRole('link', { name: 'Review & release' })).toBeInTheDocument();
    });

    it('has no review link for a plan that only reads', async () => {
        const bar = await renderBar('done', { workflows: [{ id: 'content_research', version: '1.0.0' }] });

        expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
    });

    it('offers the other open plans only while more than one is open', async () => {
        const onPickPlan = vi.fn();
        const other = plan('released', { id: 'pl-0', summary: 'Older terms page.' });
        const { unmount } = renderWithProviders(<PlanBar sessionId={SESSION} plan={plan('done')} openPlans={[plan('done')]} onPickPlan={onPickPlan} />, { path: `/sessions/${SESSION}` });

        // One open plan: the bar as it always was.
        expect(within(await screen.findByRole('region', { name: 'Open plan' })).queryByRole('button', { name: /open plans?$/ })).not.toBeInTheDocument();
        unmount();

        renderWithProviders(<PlanBar sessionId={SESSION} plan={plan('done')} openPlans={[other, plan('done')]} onPickPlan={onPickPlan} />, { path: `/sessions/${SESSION}` });

        expect(await within(await screen.findByRole('region', { name: 'Open plan' })).findByRole('button', { name: '2 open plans' })).toBeInTheDocument();
    });

    it('has no review link once the plan went out, published or sent to review', async () => {
        for (const status of ['published', 'review_requested'] as const) {
            const { unmount } = renderWithProviders(<PlanBar sessionId={SESSION} plan={plan(status)} />, { path: `/sessions/${SESSION}` });
            const bar = await screen.findByRole('region', { name: 'Open plan' });

            expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
            unmount();
        }
    });

    it('has no review link while the plan is only proposed', async () => {
        const bar = await renderBar('proposed');

        expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
    });

    it('has no review link for a proposed revision either: it is accepted in the bar, and reviewed once done', async () => {
        const bar = await renderBar('proposed', { version: 3, accepted_version: 2 });

        expect(within(bar).getByRole('button', { name: 'Accept plan' })).toBeInTheDocument();
        expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
    });
});

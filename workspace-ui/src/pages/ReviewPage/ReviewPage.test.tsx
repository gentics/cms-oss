import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import { render, screen, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import { routeTree } from '@/router';
import type { GenaixEvent, PlanState, PlanStatus } from '@/services/apiService/genaix/types';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import '@/i18n';

// The stream has tests of its own; here the page reads the plan.
vi.mock('@/hooks/useSessionEvents', () => ({ useSessionEvents: () => {} }));

const SESSION = 's-1';

const state: PlanState = {
    workflows: [{ id: 'content_create', version: '1.0.0' }],
    state: 'done',
    plan: { id: 'pl-2', workflows: [{ id: 'content_create', version: '1.0.0' }], status: 'done', version: 3, summary: 'New terms page in Richtlinien.', items: [] },
    steps: [],
    checks: [{ id: 'chk-1', label: 'Formal address', severity: 'pass', blocking: false, message: 'Fine.', source: { type: 'guideline', label: 'Tone & Address' } }],
    gates: [],
    approval_chain: [],
    cms_objects: [],
    publications: [],
    preview: { cms_ref: { type: 'page', id: 9142, node_id: 3, name: 'Neue Nutzungsbedingungen' }, preview_url: '/alohapage?realid=9142&nodeid=3&mode=view', mode: 'view' },
};

// The plans as the stream reported them, oldest first (the stream itself is mocked here).
function planUpdated(...plans: [id: string, status: PlanStatus][]) {
    plans.forEach(([id, status], index) => useWorkspaceEventStore.getState().applyEvent(SESSION, {
        seq: index + 1,
        ts: '2026-10-13T09:00:00Z',
        session_id: SESSION,
        plan_id: id,
        type: 'plan.updated',
        plan: { id, workflows: [{ id: 'content_create', version: '1.0.0' }], status, version: 1, summary: '', items: [] },
    } as GenaixEvent));
}

// Answers the state of plan pl-2, and of the older plan pl-1.
function stubRoutes() {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const path = new URL(String(input), 'http://localhost').pathname;

        if (path.endsWith(`/sessions/${SESSION}/plans/pl-2`)) {
            return Response.json(state);
        }

        if (path.endsWith(`/sessions/${SESSION}/plans/pl-1`)) {
            return Response.json({ ...state, plan: { ...state.plan, id: 'pl-1', status: 'released', summary: 'Older terms page.' } });
        }

        return Response.json({ items: [], next_cursor: null });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

function renderReview(path = `/sessions/${SESSION}/review`) {
    const router = createRouter({ routeTree, history: createMemoryHistory({ initialEntries: [path] }) });

    render(
        <QueryClientProvider client={new QueryClient()}>
            <UiProvider>
                <RouterProvider router={router} />
            </UiProvider>
        </QueryClientProvider>,
    );
}

describe('ReviewPage', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows the newest plan\'s preview and checklist side by side', async () => {
        const fetchMock = stubRoutes();

        planUpdated(['pl-1', 'published'], ['pl-2', 'done']);

        renderReview();

        const panel = await screen.findByRole('region', { name: 'Review' });

        expect(within(panel).getByRole('region', { name: 'Checklist' })).toHaveTextContent('Formal addressPassed');
        expect(within(panel).getByText('New terms page in Richtlinien.')).toBeInTheDocument();
        expect(screen.getByTitle('Preview of the page in the CMS')).toHaveAttribute('src', expect.stringContaining('/alohapage?realid=9142'));
        // The newest plan, not the earlier one.
        expect(fetchMock.mock.calls.map(([input]) => String(input))).toContain(`/rest/proxy/genaix/sessions/${SESSION}/plans/pl-2`);
    });

    it('opens the plan ?plan= names, an older one, instead of the newest', async () => {
        const fetchMock = stubRoutes();

        planUpdated(['pl-1', 'released'], ['pl-2', 'done']);

        renderReview(`/sessions/${SESSION}/review?plan=pl-1`);

        const panel = await screen.findByRole('region', { name: 'Review' });

        expect(await within(panel).findByText('Older terms page.')).toBeInTheDocument();
        expect(within(panel).getByRole('button', { name: 'Publish' })).toBeInTheDocument();
        expect(fetchMock.mock.calls.map(([input]) => String(input))).not.toContain(`/rest/proxy/genaix/sessions/${SESSION}/plans/pl-2`);
    });

    it('shows the newest plan for an empty ?plan=', async () => {
        const fetchMock = stubRoutes();

        planUpdated(['pl-1', 'released'], ['pl-2', 'done']);

        renderReview(`/sessions/${SESSION}/review?plan=`);

        expect(await screen.findByText('New terms page in Richtlinien.')).toBeInTheDocument();
        expect(fetchMock.mock.calls.map(([input]) => String(input))).toContain(`/rest/proxy/genaix/sessions/${SESSION}/plans/pl-2`);
    });

    it('only shows the page in the preview: no page settings, no part to select', async () => {
        stubRoutes();
        planUpdated(['pl-2', 'done']);

        renderReview();

        await screen.findByTitle('Preview of the page in the CMS');
        expect(screen.queryByRole('button', { name: 'Page settings' })).not.toBeInTheDocument();
    });

    it('links back to the chat', async () => {
        stubRoutes();

        renderReview();

        expect(await screen.findByRole('link', { name: 'Back to the chat' })).toHaveAttribute('href', `/sessions/${SESSION}`);
    });

    it('says so when the session has no plan yet', async () => {
        stubRoutes();

        renderReview();

        expect(await screen.findByText('This session has no plan to review yet.')).toBeInTheDocument();
        expect(screen.getByText('No preview yet.')).toBeInTheDocument();
    });
});

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import { routeTree } from '@/router';
import type { Check, GenaixEvent, PlanState, PlanStatus, Preview, Publication } from '@/services/apiService/genaix/types';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import '@/i18n';

// The stream has tests of its own; here the page reads the plan.
vi.mock('@/hooks/useSessionEvents', () => ({ useSessionEvents: () => {} }));

const SESSION = 's-1';

const state: PlanState = {
    workflow: 'content_create',
    state: 'done',
    plan: { id: 'pl-2', workflow: 'content_create', status: 'done', version: 3, summary: 'New terms page in Richtlinien.', items: [] },
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
        plan: { id, workflow: 'content_create', status, version: 1, summary: '', items: [] },
    } as GenaixEvent));
}

// A `preview.updated` of `page` for the plan `planId`, as the stream sends it.
function previewUpdated(seq: number, planId: string, { cms_ref, preview_url }: Preview) {
    useWorkspaceEventStore.getState().applyEvent(SESSION, {
        seq,
        ts: '2026-10-13T09:00:00Z',
        session_id: SESSION,
        plan_id: planId,
        type: 'preview.updated',
        cms_ref,
        preview_url,
        mode: 'view',
    } as GenaixEvent);
}

// Answers the state of plan pl-2 (`newest`), and of the older plan pl-1. The CMS lets the user
// publish every page but 9011.
function stubRoutes(newest: PlanState = state) {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const path = new URL(String(input), 'http://localhost').pathname;
        const permission = /\/rest\/perm\/publish\/page\/(\d+)$/.exec(path);

        if (permission) {
            return Response.json({ granted: permission[1] !== '9011' });
        }

        if (path.endsWith(`/sessions/${SESSION}/plans/pl-2`)) {
            return Response.json(newest);
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

    it('offers only the plan\'s pages in the preview, the one the stream sent last first', async () => {
        const user = userEvent.setup();
        const terms = state.preview!;
        const shipping: Preview = { cms_ref: { type: 'page', id: 8644, node_id: 3, name: 'Versand und Lieferung' }, preview_url: '/alohapage?realid=8644&nodeid=3&mode=view', mode: 'view' };
        const imprint: Preview = { cms_ref: { type: 'page', id: 9500, node_id: 3, name: 'Impressum' }, preview_url: '/alohapage?realid=9500&nodeid=3&mode=view', mode: 'view' };

        stubRoutes({ ...state, previews: [terms, shipping] });
        planUpdated(['pl-1', 'published'], ['pl-2', 'done']);
        // A page of the older plan, then the plan's own second page again.
        previewUpdated(3, 'pl-1', imprint);
        previewUpdated(4, 'pl-2', shipping);

        renderReview();

        const pages = await screen.findByRole('combobox', { name: 'Pages' });

        expect(pages).toHaveTextContent('Versand und Lieferung');
        expect(screen.getByTitle('Preview of the page in the CMS')).toHaveAttribute('src', expect.stringContaining('realid=8644'));

        await user.click(pages);

        expect((await screen.findAllByRole('option')).map((option) => option.textContent)).toEqual(['Neue Nutzungsbedingungen', 'Versand und Lieferung']);
    });

    describe('with several pages', () => {
        function page(id: number, name: string): { preview: Preview; publication: Publication } {
            const cmsRef = { type: 'page' as const, id, node_id: 3, name };

            return {
                preview: { cms_ref: cmsRef, preview_url: `/alohapage?realid=${id}&nodeid=3&mode=view`, mode: 'view' },
                publication: { cms_ref: cmsRef, status: 'pending', approval_chain: [] },
            };
        }

        function check(id: string, label: string, severity: Check['severity'], pageId?: number): Check {
            return { id, label, severity, blocking: false, message: `${label}.`, source: { type: 'gate', label: 'Gates' }, ...(pageId && { target: { type: 'page', id: String(pageId) } }) };
        }

        const shipping = page(8644, 'Versand und Lieferung');
        const service = page(9011, 'Serviceinformationen');

        it('lists them, and shows the one picked: its preview, its checks and whether it can be published', async () => {
            const user = userEvent.setup();

            stubRoutes({
                ...state,
                publications: [shipping.publication, service.publication],
                previews: [shipping.preview, service.preview],
                preview: service.preview,
                checks: [
                    check('chk-plan', 'Formal address', 'pass'),
                    check('chk-8644', 'Alt text missing', 'warn', 8644),
                    check('chk-9011', 'Changed meanwhile', 'fail', 9011),
                ],
            });
            planUpdated(['pl-2', 'done']);

            renderReview();

            const pages = await screen.findByRole('navigation', { name: 'Pages' });

            // The page previewed last, with its checks and those about the whole plan.
            expect(within(pages).getByRole('link', { name: /Serviceinformationen/ })).toHaveAttribute('aria-current', 'page');
            expect(screen.getByTitle('Preview of the page in the CMS')).toHaveAttribute('src', expect.stringContaining('realid=9011'));
            expect(screen.getByRole('region', { name: 'Checklist' })).toHaveTextContent('Formal address');
            expect(screen.getByRole('region', { name: 'Checklist' })).toHaveTextContent('Changed meanwhile');
            expect(screen.getByRole('region', { name: 'Checklist' })).not.toHaveTextContent('Alt text missing');
            expect(await screen.findByText('Needs a review: you may not publish this page.')).toBeInTheDocument();
            // The list picks the page; the preview's head has no select of its own.
            expect(screen.queryByRole('combobox', { name: 'Pages' })).not.toBeInTheDocument();

            await user.click(within(pages).getByRole('link', { name: /Versand und Lieferung/ }));

            expect(await screen.findByText('You may publish this page.')).toBeInTheDocument();
            expect(screen.getByTitle('Preview of the page in the CMS')).toHaveAttribute('src', expect.stringContaining('realid=8644'));
            expect(screen.getByRole('region', { name: 'Checklist' })).toHaveTextContent('Alt text missing');
            expect(screen.getByRole('region', { name: 'Checklist' })).not.toHaveTextContent('Changed meanwhile');
        });

        it('opens the page ?page= names', async () => {
            stubRoutes({ ...state, publications: [shipping.publication, service.publication], previews: [shipping.preview, service.preview], preview: service.preview });
            planUpdated(['pl-2', 'done']);

            renderReview(`/sessions/${SESSION}/review?page=8644`);

            const pages = await screen.findByRole('navigation', { name: 'Pages' });

            expect(within(pages).getByRole('link', { name: /Versand und Lieferung/ })).toHaveAttribute('aria-current', 'page');
            expect(screen.getByTitle('Preview of the page in the CMS')).toHaveAttribute('src', expect.stringContaining('realid=8644'));
        });

        it('lists no pages for a plan that wrote one', async () => {
            stubRoutes({ ...state, publications: [service.publication] });
            planUpdated(['pl-2', 'done']);

            renderReview();

            await screen.findByTitle('Preview of the page in the CMS');
            expect(screen.queryByRole('navigation', { name: 'Pages' })).not.toBeInTheDocument();
        });
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

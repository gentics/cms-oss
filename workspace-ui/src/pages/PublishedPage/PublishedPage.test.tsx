import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import { routeTree } from '@/router';
import type { PlanState, PlanStatus, Publication } from '@/services/apiService/genaix/types';
import { sessionFixture } from '@/test/genaixSessions';

import '@/i18n';

const SESSION = 's-1';
const PLAN = 'pl-1';

function publication(id: number, name: string, status: Publication['status'], url?: string): Publication {
    return { cms_ref: { type: 'page', id, node_id: 3, name, ...(url && { url }) }, status, approval_chain: [] };
}

function planState(status: PlanStatus, publications: Publication[]): PlanState {
    return {
        workflows: [{ id: 'content_create', version: '1.0.0' }],
        state: 'done',
        plan: { id: PLAN, workflows: [{ id: 'content_create', version: '1.0.0' }], status, version: 2, summary: '', items: [] },
        steps: [],
        checks: [],
        gates: [],
        approval_chain: [],
        cms_objects: [],
        publications,
    };
}

// Answers the plan with `state`; the to-dos' lists (filtered by `plan_status` or `status`, `SessionTodos`)
// with a session to release, the others empty; the unfiltered list with the sessions to continue. The CMS: node 3
// on `host`, each page of `publishPaths` with that publish path, any other page not readable.
function stubRoutes(state: PlanState, publishPaths: Record<number, string> = {}, host = 'https://www.gentics.com') {
    vi.stubGlobal('fetch', vi.fn<typeof fetch>(async (input) => {
        const url = new URL(String(input), 'http://localhost');
        const path = url.pathname;
        const page = /^\/rest\/page\/load\/(\d+)$/.exec(path);

        if (path === '/rest/node') {
            return Response.json({ items: [{ id: 3, name: 'Website', folderId: 2, host }] });
        }

        if (page) {
            const publishPath = publishPaths[Number(page[1])];

            return publishPath === undefined
                ? new Response(null, { status: 404 })
                : Response.json({ responseInfo: { responseCode: 'OK' }, page: { id: Number(page[1]), name: 'x', folderId: 2, cdate: 0, edate: 0, publishPath } });
        }

        if (path.endsWith(`/sessions/${SESSION}/plans/${PLAN}`)) {
            return Response.json(state);
        }

        if (path.endsWith('/sessions') && (url.searchParams.has('plan_status') || url.searchParams.has('status'))) {
            const toRelease = url.searchParams.get('plan_status') === 'released';

            return Response.json({ items: toRelease ? [sessionFixture({ id: 's-3', title: 'Prices 2026' })] : [], next_cursor: null });
        }

        if (path.endsWith('/sessions')) {
            return Response.json({ items: [sessionFixture({ id: 's-2', title: 'Rework the product page' })], next_cursor: null });
        }

        return Response.json({ items: [], next_cursor: null });
    }));
}

function renderPublished(path = `/sessions/${SESSION}/published?plan=${PLAN}`, queryClient = new QueryClient()) {
    const router = createRouter({ routeTree, history: createMemoryHistory({ initialEntries: [path] }) });

    render(
        <QueryClientProvider client={queryClient}>
            <UiProvider>
                <RouterProvider router={router} />
            </UiProvider>
        </QueryClientProvider>,
    );
}

describe('PublishedPage', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('says one page was published, with its address and a link to open it', async () => {
        stubRoutes(planState('published', [publication(9142, 'Products – Overview', 'published')]), { 9142: '/products' });
        renderPublished();

        expect(await screen.findByRole('heading', { level: 1, name: 'Published' })).toBeInTheDocument();
        // The address comes from the CMS, after the plan.
        expect(await screen.findByText('Products – Overview · www.gentics.com/products')).toBeInTheDocument();

        const open = screen.getByRole('link', { name: 'Open the page' });

        expect(open).toHaveAttribute('href', 'https://www.gentics.com/products');
        expect(open).toHaveAttribute('target', '_blank');
    });

    it('opens the page at its publish path on its node\'s host, as the CMS has them, whatever GenAIx names as its address', async () => {
        stubRoutes(
            planState('published', [publication(9142, 'Products – Overview', 'published', '/alohapage?realid=9142&nodeid=3&mode=view')]),
            { 9142: '/Produkte/uebersicht.html' },
            'http://intranet.example.com',
        );
        renderPublished();

        expect(await screen.findByRole('heading', { level: 1, name: 'Published' })).toBeInTheDocument();
        expect(await screen.findByText('Products – Overview · intranet.example.com/Produkte/uebersicht.html')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Open the page' })).toHaveAttribute('href', 'http://intranet.example.com/Produkte/uebersicht.html');
    });

    it('offers no link for an address that is not http(s)', async () => {
        const queryClient = new QueryClient();

        stubRoutes(planState('published', [publication(9142, 'Products – Overview', 'published')]), { 9142: 'javascript:alert(1)' });
        renderPublished(undefined, queryClient);

        expect(await screen.findByRole('heading', { level: 1, name: 'Published' })).toBeInTheDocument();
        // Only once the CMS has given the page: before, there is no link anyway.
        await waitFor(() => expect(queryClient.getQueryState(['cms', 'pages', 9142, 3])?.status).toBe('success'));
        expect(screen.getByText('Products – Overview')).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Open the page' })).not.toBeInTheDocument();
    });

    it('counts several pages and names them, each published one with an address a link to it on the website', async () => {
        // The blog's page the CMS does not give: no address.
        stubRoutes(planState('review_requested', [
            publication(1, 'Events – archive', 'published'),
            publication(2, 'Blog: Migration without downtime', 'published'),
            publication(3, 'Reference Norhaus', 'review_requested'),
        ]), { 1: '/events', 3: '/norhaus' });
        renderPublished();

        expect(await screen.findByRole('heading', { level: 1, name: '2 published, 1 in review' })).toBeInTheDocument();
        expect(screen.getByText((_, element) => element?.localName === 'p' && element.textContent === 'Events – archive · Blog: Migration without downtime · Reference Norhaus')).toBeInTheDocument();

        const events = await screen.findByRole('link', { name: 'Events – archive' });

        expect(events).toHaveAttribute('href', 'https://www.gentics.com/events');
        expect(events).toHaveAttribute('target', '_blank');
        // No address, and only in review: no link.
        expect(screen.queryByRole('link', { name: 'Blog: Migration without downtime' })).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Reference Norhaus' })).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Open the page' })).not.toBeInTheDocument();
    });

    it('says when the pages went to review, and how many went which way', async () => {
        stubRoutes(planState('review_requested', [publication(1, 'Versand', 'published'), publication(2, 'Service', 'review_requested')]));
        renderPublished();

        expect(await screen.findByRole('heading', { level: 1, name: '1 published, 1 in review' })).toBeInTheDocument();
    });

    it('says a page was sent to review and offers its CMS preview, read-only, not the page: it is not online yet', async () => {
        // As GenAIx sends them: the publication's page recorded with its connection, the preview's without.
        const service = { ...publication(2, 'Service', 'review_requested', 'https://www.gentics.com/service'), cms_ref: { type: 'page' as const, id: 2, node_id: 3, name: 'Service', url: 'https://www.gentics.com/service', connection_id: 'c-1' } };
        const preview = { cms_ref: { type: 'page' as const, id: 2, node_id: 3, name: 'Service' }, preview_url: 'https://cms.example.com/alohapage?realid=2&nodeid=3&mode=view', mode: 'view' as const };

        stubRoutes({ ...planState('review_requested', [service]), previews: [preview] });
        renderPublished();

        expect(await screen.findByRole('heading', { level: 1, name: 'Sent to review' })).toBeInTheDocument();
        expect(screen.getByText('Service')).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Open the page' })).not.toBeInTheDocument();

        const open = screen.getByRole('link', { name: 'Open the preview' });

        expect(open).toHaveAttribute('href', `${preview.preview_url}&real=view&tagmarkers=true`);
        expect(open).toHaveAttribute('target', '_blank');
    });

    it('offers no preview for a page in review that has none', async () => {
        stubRoutes(planState('review_requested', [publication(2, 'Service', 'review_requested', 'https://www.gentics.com/service')]));
        renderPublished();

        expect(await screen.findByRole('heading', { level: 1, name: 'Sent to review' })).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Open the preview' })).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Open the page' })).not.toBeInTheDocument();
    });

    it('says why the plan could not be read, with a way to try again, instead of that it was not published', async () => {
        const fetchMock = vi.fn<typeof fetch>(async (input) => {
            const path = new URL(String(input), 'http://localhost').pathname;

            if (path.endsWith(`/sessions/${SESSION}/plans/${PLAN}`)) {
                return Response.json(
                    { type: 'about:blank', title: 'Plan not found', status: 404, genaix_code: 'plan_not_found' },
                    { status: 404, headers: { 'Content-Type': 'application/problem+json' } },
                );
            }

            return Response.json({ items: [], next_cursor: null });
        });

        vi.stubGlobal('fetch', fetchMock);
        renderPublished();

        const alert = await screen.findByRole('alert');

        expect(screen.queryByText('This plan has not been published.')).not.toBeInTheDocument();
        expect(alert).toHaveTextContent('The plan was not found.');

        const planCalls = () => fetchMock.mock.calls.filter(([input]) => String(input).includes(`/plans/${PLAN}`)).length;
        const before = planCalls();

        await userEvent.setup().click(screen.getByRole('button', { name: 'Try again' }));

        await waitFor(() => expect(planCalls()).toBe(before + 1));
    });

    it('leads back to the session, and on to a new one or one to continue', async () => {
        stubRoutes(planState('published', [publication(9142, 'Products – Overview', 'published')]));
        renderPublished();

        await screen.findByRole('heading', { level: 1, name: 'Published' });
        expect(screen.getByRole('region', { name: 'Start a new session' })).toBeInTheDocument();
        // The sessions to continue are folded out.
        expect(await screen.findByRole('link', { name: /Rework the product page/ })).toBeInTheDocument();
        expect(within(screen.getByRole('main')).getByRole('button', { name: /Continue a session/ })).toHaveAttribute('aria-expanded', 'true');
        expect(screen.getByRole('link', { name: 'Open this session' })).toHaveAttribute('href', `/sessions/${SESSION}`);
    });

    it('shows the open to-dos, as the dashboard does', async () => {
        stubRoutes(planState('published', [publication(9142, 'Products – Overview', 'published')]));
        renderPublished();

        const todos = await screen.findByRole('region', { name: 'Open to-dos' });

        expect(await within(todos).findByRole('link', { name: /Prices 2026/ })).toBeInTheDocument();
    });

    it('links back to the review of a plan that has not been published', async () => {
        stubRoutes(planState('released', [publication(9142, 'Products – Overview', 'pending')]));
        renderPublished();

        expect(await screen.findByText('This plan has not been published.')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Go to its review' })).toBeInTheDocument();
        expect(screen.queryByRole('heading', { level: 1 })).not.toBeInTheDocument();
    });
});

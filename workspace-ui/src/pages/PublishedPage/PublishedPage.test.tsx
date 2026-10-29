import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import { render, screen, within } from '@testing-library/react';
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
// with a session to release, the others empty; the unfiltered list with the sessions to continue.
function stubRoutes(state: PlanState) {
    vi.stubGlobal('fetch', vi.fn<typeof fetch>(async (input) => {
        const url = new URL(String(input), 'http://localhost');
        const path = url.pathname;

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

function renderPublished(path = `/sessions/${SESSION}/published?plan=${PLAN}`) {
    const router = createRouter({ routeTree, history: createMemoryHistory({ initialEntries: [path] }) });

    render(
        <QueryClientProvider client={new QueryClient()}>
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
        stubRoutes(planState('published', [publication(9142, 'Products – Overview', 'published', 'https://www.gentics.com/products')]));
        renderPublished();

        expect(await screen.findByRole('heading', { level: 1, name: 'Published' })).toBeInTheDocument();
        expect(screen.getByText('Products – Overview · www.gentics.com/products')).toBeInTheDocument();

        const open = screen.getByRole('link', { name: 'Open the page' });

        expect(open).toHaveAttribute('href', 'https://www.gentics.com/products');
        expect(open).toHaveAttribute('target', '_blank');
    });

    it('opens a page whose address is a path, such as its CMS preview, on the host that serves this app', async () => {
        const preview = '/alohapage?realid=9142&nodeid=3&mode=view';
        const expected = new URL(preview, document.baseURI);

        stubRoutes(planState('published', [publication(9142, 'Products – Overview', 'published', preview)]));
        renderPublished();

        expect(await screen.findByRole('heading', { level: 1, name: 'Published' })).toBeInTheDocument();
        expect(screen.getByText(`Products – Overview · ${expected.host}/alohapage`)).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Open the page' })).toHaveAttribute('href', expected.href);
    });

    it('offers no link for an address that is not http(s)', async () => {
        stubRoutes(planState('published', [publication(9142, 'Products – Overview', 'published', 'javascript:alert(1)')]));
        renderPublished();

        expect(await screen.findByRole('heading', { level: 1, name: 'Published' })).toBeInTheDocument();
        expect(screen.getByText('Products – Overview')).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Open the page' })).not.toBeInTheDocument();
    });

    it('counts several pages and names them, without a link to any one of them', async () => {
        stubRoutes(planState('published', [
            publication(1, 'Events – archive', 'published', 'https://www.gentics.com/events'),
            publication(2, 'Blog: Migration without downtime', 'published'),
            publication(3, 'Reference Norhaus', 'published'),
        ]));
        renderPublished();

        expect(await screen.findByRole('heading', { level: 1, name: '3 pages published' })).toBeInTheDocument();
        expect(screen.getByText('Events – archive · Blog: Migration without downtime · Reference Norhaus')).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Open the page' })).not.toBeInTheDocument();
    });

    it('says when the pages went to review, and how many went which way', async () => {
        stubRoutes(planState('review_requested', [publication(1, 'Versand', 'published'), publication(2, 'Service', 'review_requested')]));
        renderPublished();

        expect(await screen.findByRole('heading', { level: 1, name: '1 published, 1 in review' })).toBeInTheDocument();
    });

    it('says a page was sent to review, and still offers its address', async () => {
        stubRoutes(planState('review_requested', [publication(2, 'Service', 'review_requested', 'https://www.gentics.com/service')]));
        renderPublished();

        expect(await screen.findByRole('heading', { level: 1, name: 'Sent to review' })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Open the page' })).toHaveAttribute('href', 'https://www.gentics.com/service');
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

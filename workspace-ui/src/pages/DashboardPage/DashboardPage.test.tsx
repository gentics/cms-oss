import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ErrorNotifications } from '@/components/ErrorNotifications/ErrorNotifications';
import { UiProvider } from '@/components/ui/provider';
import { routeTree } from '@/router';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { sessionFixture, stubSessionRoutes, withEmptySessionList } from '@/test/genaixSessions';

import '@/i18n';

// The CMS credential of a new session has tests of its own (`sessionAuthorization.test.ts`); here it
// would take the stubbed fetch responses meant for starting the session.
const { AUTHORIZATION } = vi.hoisted(() => ({
    AUTHORIZATION: { connection_id: 'c-1', auth_type: 'bearer', token: 'cmstok_dev_1', token_name: 'genaix-pending-1', expires_at: '2026-10-08T08:00:00.000Z' },
}));

vi.mock('@/helper/sessionAuthorization/sessionAuthorization', () => ({
    newSessionCmsAuthorization: () => Promise.resolve(AUTHORIZATION),
}));

// jsdom has no ResizeObserver; the session page's AppShell measures its columns with one.
class ResizeObserverStub {
    observe() {}

    disconnect() {}
}

function renderDashboard() {
    const router = createRouter({ routeTree, history: createMemoryHistory({ initialEntries: ['/'] }) });
    const queryClient = new QueryClient();

    render(
        <QueryClientProvider client={queryClient}>
            <UiProvider>
                <RouterProvider router={router} />
                <ErrorNotifications />
            </UiProvider>
        </QueryClientProvider>,
    );

    return { router, queryClient };
}

describe('DashboardPage', () => {
    beforeEach(() => {
        vi.stubGlobal('ResizeObserver', ResizeObserverStub);
        useErrorNotificationStore.setState({ errors: [] });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.useRealTimers();
    });

    it('greets by the time of day', async () => {
        vi.useFakeTimers({ toFake: ['Date'] });
        vi.setSystemTime(new Date(2026, 9, 5, 9, 0));

        renderDashboard();

        expect(await screen.findByRole('heading', { name: 'Good morning, Dominik' })).toBeInTheDocument();
    });

    it('shows the open to-dos and folds out the recent sessions', async () => {
        const user = userEvent.setup();
        const todo = sessionFixture({ id: 's-1', title: 'Prices 2026', status: 'review_requested' });
        const recent = sessionFixture({ id: 's-2', title: 'Careers page' });

        stubSessionRoutes((url) => ({ items: url.searchParams.has('status') ? [todo] : [recent, todo], next_cursor: null }));
        renderDashboard();

        const todos = await screen.findByRole('region', { name: 'Open to-dos' });

        expect(await within(todos).findByRole('link', { name: /Prices 2026/ })).toBeInTheDocument();
        expect(within(todos).getByRole('heading', { name: 'Waiting for review' })).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Recent sessions' }));

        expect(await screen.findByRole('link', { name: /Careers page/ })).toBeInTheDocument();
    });

    it('creates the session with the prompt, caches it and opens its page', async () => {
        const user = userEvent.setup();
        const session = { id: 's-1', status: 'active', workflow: 'content_research', run_id: 'r-1', message_id: 'm-1' };
        const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(Response.json(session, { status: 201 }));

        // The to-dos and recent sessions list sessions; `fetchMock` sees only the start.
        vi.stubGlobal('fetch', withEmptySessionList(fetchMock));

        const { router, queryClient } = renderDashboard();

        await user.click(await screen.findByRole('textbox'));
        await user.keyboard('Which pages are offline?{Enter}');

        await waitFor(() => expect(router.state.location.pathname).toBe('/sessions/s-1'));

        const [url, init] = fetchMock.mock.calls[0]!;

        expect(url).toBe('/genaix/api/v1/sessions');
        expect(JSON.parse(init?.body as string)).toEqual({
            workflow: 'content_research',
            message: { parts: [{ type: 'text', text: 'Which pages are offline?' }] },
            authorizations: [AUTHORIZATION],
        });
        expect(queryClient.getQueryData(['genaix', 'sessions', 's-1'])).toEqual(session);
        // The session page is, for now, the workspace: its left column has a toggle, even without a preview.
        expect(await screen.findByRole('separator', { name: 'Width of the left column' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Hide the left column' })).toBeInTheDocument();
    });

    it('shows an error and stays on the dashboard with the input kept when starting fails', async () => {
        const user = userEvent.setup();

        vi.stubGlobal('fetch', withEmptySessionList(vi.fn<typeof fetch>().mockResolvedValue(Response.json(
            { type: 't', title: 't', status: 503, genaix_code: 'service_unavailable' },
            { status: 503 },
        ))));

        const { router } = renderDashboard();

        await user.click(await screen.findByRole('textbox'));
        await user.keyboard('Which pages are offline?{Enter}');

        await waitFor(() => expect(useErrorNotificationStore.getState().errors).toMatchObject([
            { messageKey: 'dashboard.startFailed', detailKey: 'errors.genaix.service_unavailable' },
        ]));
        expect(await screen.findByText('The session could not be started')).toBeInTheDocument();
        expect(router.state.location.pathname).toBe('/');
        expect(screen.getByRole('textbox')).toHaveTextContent('Which pages are offline?');
    });
});

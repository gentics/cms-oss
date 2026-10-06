import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ErrorNotifications } from '@/components/ErrorNotifications/ErrorNotifications';
import { UiProvider } from '@/components/ui/provider';
import { routeTree } from '@/router';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

import '@/i18n';

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

    it('fills the field with a starter suggestion', async () => {
        const user = userEvent.setup();

        renderDashboard();

        await user.click(await screen.findByRole('button', { name: 'Create a new landing page for the partner programme' }));

        expect(screen.getByRole('textbox')).toHaveTextContent('Create a new landing page for the partner programme');
    });

    it('creates the session with the prompt, caches it and opens its page', async () => {
        const user = userEvent.setup();
        const session = { id: 's-1', status: 'active', workflow: 'content_research', run_id: 'r-1', message_id: 'm-1' };
        const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(Response.json(session, { status: 201 }));

        vi.stubGlobal('fetch', fetchMock);

        const { router, queryClient } = renderDashboard();

        await user.click(await screen.findByRole('textbox'));
        await user.keyboard('Which pages are offline?{Enter}');

        await waitFor(() => expect(router.state.location.pathname).toBe('/sessions/s-1'));

        const [url, init] = fetchMock.mock.calls[0]!;

        expect(url).toBe('/genaix/api/v1/sessions');
        expect(JSON.parse(init?.body as string)).toEqual({
            workflow: 'content_research',
            message: { parts: [{ type: 'text', text: 'Which pages are offline?' }] },
        });
        expect(queryClient.getQueryData(['genaix', 'sessions', 's-1'])).toEqual(session);
        // The session page is, for now, the workspace; without a preview yet, it has no
        // left-column toggle.
        expect(await screen.findByRole('separator', { name: 'Width of the left column' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /left column/ })).not.toBeInTheDocument();
    });

    it('shows an error and stays on the dashboard with the input kept when starting fails', async () => {
        const user = userEvent.setup();

        vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockResolvedValue(Response.json(
            { type: 't', title: 't', status: 503, genaix_code: 'service_unavailable' },
            { status: 503 },
        )));

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

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ErrorNotifications } from '@/components/ErrorNotifications/ErrorNotifications';
import { UiProvider } from '@/components/ui/provider';
import { CMS_USER_QUERY_KEY } from '@/hooks/useCmsAuth';
import { routeTree } from '@/router';
import type { CmsUser } from '@/services/cmsApiService/cmsApiService';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { taskFixture, withTasks } from '@/test/cmsTasks';
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

const CMS_USER: CmsUser = { id: 3, login: 'emu', firstName: 'Erika', lastName: 'Mustermann' };

// `cmsUser` is the user `LoginGate` has loaded before it shows the page.
function renderDashboard(cmsUser: CmsUser = CMS_USER) {
    const router = createRouter({ routeTree, history: createMemoryHistory({ initialEntries: ['/'] }) });
    const queryClient = new QueryClient();

    queryClient.setQueryData(CMS_USER_QUERY_KEY, cmsUser);

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

    it('greets the CMS user by first name and the time of day', async () => {
        vi.useFakeTimers({ toFake: ['Date'] });
        vi.setSystemTime(new Date(2026, 9, 5, 9, 0));

        renderDashboard();

        expect(await screen.findByRole('heading', { name: 'Good morning, Erika' })).toBeInTheDocument();
    });

    it('greets the CMS user by login when the user has no name', async () => {
        vi.useFakeTimers({ toFake: ['Date'] });
        vi.setSystemTime(new Date(2026, 9, 5, 20, 0));

        renderDashboard({ ...CMS_USER, firstName: '', lastName: '' });

        expect(await screen.findByRole('heading', { name: 'Good evening, emu' })).toBeInTheDocument();
    });

    it('takes the CMS user LoginGate has loaded, without asking the CMS again', async () => {
        const fetchMock = vi.fn<typeof fetch>(async () => new Response(null, { status: 404 }));

        vi.stubGlobal('fetch', fetchMock);

        renderDashboard();

        expect(await screen.findByRole('heading', { name: /Erika/ })).toBeInTheDocument();
        expect(fetchMock.mock.calls.map(([input]) => String(input))).not.toContain('/rest/user/me');
    });

    it('shows the open tasks and folds out the recent sessions', async () => {
        const user = userEvent.setup();
        const recent = sessionFixture({ id: 's-2', title: 'Careers page' });
        const task = taskFixture({ id: '7', source: 'genaix', type: 'genaixReadyToPublish', title: 'Prices 2026', genaix: { sessionId: 's-1' }, actions: ['open'] });

        vi.stubGlobal('fetch', withTasks(stubSessionRoutes(() => ({ items: [recent], next_cursor: null })), [task]));
        renderDashboard();

        await user.click(await screen.findByRole('button', { name: '1 open task' }));

        const tasks = await screen.findByRole('dialog', { name: 'Open tasks' });

        expect(await within(tasks).findByRole('link', { name: /Prices 2026/ })).toHaveAttribute('href', '/sessions/s-1');
        expect(within(tasks).getByRole('heading', { name: 'Ready to publish' })).toBeInTheDocument();

        await user.keyboard('{Escape}');
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        await user.click(screen.getByRole('button', { name: 'Recent sessions' }));

        expect(await screen.findByRole('link', { name: /Careers page/ })).toBeInTheDocument();
    });

    it('puts the page of a publish queue task into the field, and sending starts the session with it', async () => {
        const user = userEvent.setup();
        const session = { id: 's-1', status: 'active', workflow: 'content_research', run_id: 'r-1', message_id: 'm-1' };
        const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(Response.json(session, { status: 201 }));
        const page = { type: 'page', id: '9142', node_id: 3, label: 'Terms of use' };
        const task = taskFixture({
            id: 'publishQueue:page:9142',
            source: 'publishQueue',
            type: 'approvePublish',
            title: undefined,
            objects: [{ type: 'page', id: 9142, nodeId: 3, name: 'Terms of use' }],
            actions: ['open', 'approve', 'startSession'],
        });

        vi.stubGlobal('fetch', withTasks(withEmptySessionList(fetchMock), [task]));

        const { router } = renderDashboard();

        await user.click(await screen.findByRole('button', { name: '1 open task' }));
        await user.click(await screen.findByRole('button', { name: 'Edit with GenAIx' }));

        // The drawer closes and leaves the focus in the field, not on its button.
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

        const field = screen.getByRole('textbox');

        await waitFor(() => expect(field).toHaveTextContent('Terms of use'));
        await waitFor(() => expect(field).toHaveFocus());
        expect(fetchMock).not.toHaveBeenCalled();

        await user.keyboard('Add the new cancellation period{Enter}');

        await waitFor(() => expect(router.state.location.pathname).toBe('/sessions/s-1'));

        const [url, init] = fetchMock.mock.calls[0]!;

        expect(url).toBe('/rest/proxy/genaix/sessions');
        expect(JSON.parse(init?.body as string)).toMatchObject({
            context: { references: [page] },
            message: { parts: [{ type: 'reference', ref: page }, { type: 'text', text: ' Add the new cancellation period' }] },
        });
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

        expect(url).toBe('/rest/proxy/genaix/sessions');
        expect(JSON.parse(init?.body as string)).toEqual({
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

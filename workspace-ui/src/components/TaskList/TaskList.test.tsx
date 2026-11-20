import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { taskFixture, withTasks } from '@/test/cmsTasks';
import { renderWithProviders } from '@/test/renderWithProviders';

import { TaskList } from './TaskList';

// The CMS credential of a new session has tests of its own; here it would take the stubbed responses.
vi.mock('@/helper/sessionAuthorization/sessionAuthorization', () => ({
    newSessionCmsAuthorization: () => Promise.resolve({ connection_id: 'c-1', auth_type: 'bearer', token: 'cmstok', token_name: 'n', expires_at: '2026-12-01T00:00:00.000Z' }),
}));

const genaixTask = taskFixture({
    id: '7',
    source: 'genaix',
    type: 'genaixReadyToPublish',
    title: 'Prices 2026',
    genaix: { sessionId: 's-1' },
    actions: ['open'],
});

const queueTask = taskFixture({
    id: 'publishQueue:page:9142',
    source: 'publishQueue',
    type: 'approvePublish',
    title: undefined,
    objects: [{ type: 'page', id: 9142, nodeId: 3, name: 'Terms of use' }],
    details: { requestedBy: { id: 42, name: 'Example Erika' } },
    actions: ['open', 'approve', 'assignBack', 'startSession'],
});

const linkTask = taskFixture({
    id: 'linkChecker:page:7710',
    source: 'linkChecker',
    type: 'fixBrokenLinks',
    title: undefined,
    objects: [{ type: 'page', id: 7710, nodeId: 3, name: 'Directions' }],
    details: { brokenCount: 1, links: [{ id: 1, url: 'https://example.com/a.pdf' }], online: true, liveUrl: 'https://www.example.com/directions' },
    actions: ['startSession', 'view'],
});

const groupTask = taskFixture({
    id: '8',
    type: 'requestConstruct',
    title: 'New tag type for price tables',
    assignee: { type: 'group', id: 9, name: 'Chief CC' },
    assigneeGroup: { type: 'group', id: 9, name: 'Chief CC' },
    actions: ['startSession', 'complete', 'dismiss', 'claim'],
});

function stubCms(tasks = [genaixTask, queueTask, linkTask, groupTask]) {
    const fetchMock = vi.fn<typeof fetch>(async () => Response.json({ responseInfo: { responseCode: 'OK' } }));

    vi.stubGlobal('fetch', vi.fn(withTasks(fetchMock, tasks)));

    return fetchMock;
}

function writes(fetchMock: ReturnType<typeof stubCms>) {
    return fetchMock.mock.calls.map(([url, init]) => ({ url: String(url), method: init?.method, body: init?.body ? JSON.parse(init.body as string) as unknown : undefined }));
}

describe('TaskList', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('groups the tasks by source and names those without a title', async () => {
        stubCms();
        renderWithProviders(<TaskList />);

        const card = await screen.findByRole('region', { name: 'Open tasks' });

        expect(within(card).getAllByRole('heading', { level: 3 }).map((heading) => heading.textContent))
            .toEqual(['Ready to publish', 'Waiting for your approval', 'Broken links', 'Assigned to you']);
        expect(within(card).getByText('Publish Terms of use')).toBeInTheDocument();
        expect(within(card).getByText('Asked by Example Erika')).toBeInTheDocument();
        expect(within(card).getByText('1 broken link')).toBeInTheDocument();
        expect(within(card).getByText('For Chief CC')).toBeInTheDocument();
    });

    it('renders nothing once it knows there are no tasks', async () => {
        stubCms([]);
        const { container, queryClient } = renderWithProviders(<TaskList />);

        // While loading it renders nothing either, so wait until the list has arrived.
        await waitFor(() => expect(queryClient.getQueryState(['cms', 'tasks'])?.status).toBe('success'));
        expect(container.querySelector('section')).toBeNull();
    });

    it('links a GenAIx task to its session and offers nothing that would change it', async () => {
        stubCms([genaixTask]);
        renderWithProviders(<TaskList />);

        const region = within(await screen.findByRole('region', { name: 'Ready to publish' }));

        expect(region.getByRole('link', { name: 'Prices 2026' })).toHaveAttribute('href', '/sessions/s-1');
        expect(region.queryAllByRole('button')).toEqual([]);
    });

    it('approves a page in the queue and reads the tasks again', async () => {
        const user = userEvent.setup();
        const fetchMock = stubCms([queueTask]);

        renderWithProviders(<TaskList />);
        await user.click(await screen.findByRole('button', { name: 'Approve' }));

        await waitFor(() => expect(writes(fetchMock)).toEqual([{ url: '/rest/page/pubqueue/approve', method: 'POST', body: { ids: [9142] } }]));
        await waitFor(() => expect(vi.mocked(fetch).mock.calls.filter(([url]) => String(url) === '/rest/task')).toHaveLength(2));
    });

    it('lets a group member take over a group task', async () => {
        const user = userEvent.setup();
        const fetchMock = stubCms([groupTask]);

        renderWithProviders(<TaskList />);
        await user.click(await screen.findByRole('button', { name: 'Assign to me' }));

        await waitFor(() => expect(writes(fetchMock)).toEqual([{ url: '/rest/task/8/claim', method: 'POST', body: undefined }]));
    });

    it('links the live page of a link checker task and starts a session to fix its links', async () => {
        const user = userEvent.setup();
        const fetchMock = stubCms([linkTask]);

        renderWithProviders(<TaskList />);

        expect(await screen.findByRole('link', { name: 'View live' })).toHaveAttribute('href', 'https://www.example.com/directions');

        await user.click(screen.getByRole('button', { name: 'Fix with GenAIx' }));

        await waitFor(() => expect(writes(fetchMock)[0]).toMatchObject({ url: '/rest/proxy/genaix/sessions', method: 'POST' }));
        expect(writes(fetchMock)[0]!.body).toMatchObject({
            context: { references: [{ type: 'page', id: '7710', node_id: 3, label: 'Directions' }] },
            message: { parts: [
                { type: 'reference', ref: { type: 'page', id: '7710', node_id: 3, label: 'Directions' } },
                { type: 'text', text: 'Fix the broken links on this page:\nhttps://example.com/a.pdf' },
            ] },
        });
    });
});

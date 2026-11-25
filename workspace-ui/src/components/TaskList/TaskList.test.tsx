import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { TASK_USER, taskFixture, withTasks } from '@/test/cmsTasks';
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

function stubCms(tasks = [genaixTask, queueTask, linkTask, groupTask], options: { brokenLinkPages?: number } = {}) {
    const fetchMock = vi.fn<typeof fetch>(async () => Response.json({ responseInfo: { responseCode: 'OK' } }));

    vi.stubGlobal('fetch', vi.fn(withTasks(fetchMock, tasks, options)));

    return fetchMock;
}

// Every path the page asked for, including those `withTasks` answers.
function requestedPaths(): string[] {
    return vi.mocked(fetch).mock.calls.map(([input]) => new URL(String(input), 'http://localhost').pathname);
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
        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);

        // The tasks drawer gives the title; the list has only its groups.
        expect((await screen.findAllByRole('heading', { level: 3 })).map((heading) => heading.textContent))
            .toEqual(['Ready to publish', 'Waiting for your approval', 'Broken links', 'Open to take']);
        expect(screen.getByText('Publish Terms of use')).toBeInTheDocument();
        expect(screen.getByText('Asked by Example Erika')).toBeInTheDocument();
        expect(screen.getByText('1 broken link')).toBeInTheDocument();
        expect(screen.getByText('For Chief CC')).toBeInTheDocument();
    });

    it('says so once it knows there are no tasks', async () => {
        stubCms([]);
        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);

        expect(await screen.findByText('No open tasks.')).toBeInTheDocument();
        expect(screen.queryByRole('heading')).not.toBeInTheDocument();
    });

    it('links a GenAIx task to its session and offers nothing that would change it', async () => {
        stubCms([genaixTask]);
        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);

        const region = within(await screen.findByRole('region', { name: 'Ready to publish' }));

        expect(region.getByRole('link', { name: 'Prices 2026' })).toHaveAttribute('href', '/sessions/s-1');
        expect(region.queryAllByRole('button')).toEqual([]);
    });

    it('approves a page in the queue and reads the tasks again', async () => {
        const user = userEvent.setup();
        const fetchMock = stubCms([queueTask]);

        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);
        await user.click(await screen.findByRole('button', { name: 'Publish' }));

        await waitFor(() => expect(writes(fetchMock)).toEqual([{ url: '/rest/page/pubqueue/approve', method: 'POST', body: { ids: [9142] } }]));
        await waitFor(() => expect(vi.mocked(fetch).mock.calls.filter(([url]) => String(url) === '/rest/task')).toHaveLength(2));
    });

    it('names the approval of a request to take a page offline after what it does', async () => {
        const user = userEvent.setup();
        const fetchMock = stubCms([{ ...queueTask, type: 'approveOffline' }]);

        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);

        expect(await screen.findByText('Take Terms of use offline')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Take offline' }));

        await waitFor(() => expect(writes(fetchMock)).toEqual([{ url: '/rest/page/pubqueue/approve', method: 'POST', body: { ids: [9142] } }]));
    });

    it('folds a group in and out by its heading, keeping its count, and leaves the others open', async () => {
        const user = userEvent.setup();

        stubCms([queueTask, groupTask]);
        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);

        const toggle = await screen.findByRole('button', { name: 'Waiting for your approval' });
        const tasks = screen.getByRole('region', { name: 'Waiting for your approval' });

        expect(toggle).toHaveAttribute('aria-expanded', 'true');
        expect(toggle).toHaveAttribute('aria-controls', tasks.id);

        await user.click(toggle);

        expect(toggle).toHaveAttribute('aria-expanded', 'false');
        expect(screen.getByText('Publish Terms of use')).not.toBeVisible();
        expect(screen.getByRole('heading', { name: 'Waiting for your approval' })).toBeVisible();
        expect(screen.getAllByText('1')[0]).toBeVisible();
        expect(screen.getByText('New tag type for price tables')).toBeVisible();

        await user.click(toggle);

        expect(toggle).toHaveAttribute('aria-expanded', 'true');
        expect(screen.getByText('Publish Terms of use')).toBeVisible();
    });

    it('compares a queue task\'s page from the corner icon or the top of its card, not from the other actions', async () => {
        const user = userEvent.setup();
        // The new tab `openTaskInCms` opens and then sends to the page.
        const tab = { opener: {}, location: { href: '' } };
        const open = vi.fn(() => tab);

        stubCms([queueTask]);
        vi.stubGlobal('open', open);
        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);

        const compare = await screen.findByRole('button', { name: 'Compare' });

        expect(compare).toHaveTextContent('');
        expect(screen.getAllByRole('button', { name: 'Compare' })).toHaveLength(1);

        await user.click(screen.getByRole('button', { name: 'Publish' }));
        expect(open).not.toHaveBeenCalled();

        await user.click(screen.getByText('Publish Terms of use'));

        expect(open).toHaveBeenCalledExactlyOnceWith('', '_blank');
        await waitFor(() => expect(tab.location.href).toContain('detail:node/3/page/9142/'));
    });

    it('opens the live page of a link checker task from the top of its card', async () => {
        const user = userEvent.setup();
        const open = vi.fn();

        stubCms([linkTask]);
        vi.stubGlobal('open', open);
        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);

        await user.click(await screen.findByText('1 broken link'));

        expect(open).toHaveBeenCalledExactlyOnceWith('https://www.example.com/directions', '_blank', 'noopener,noreferrer');
        expect(screen.getByRole('link', { name: 'View live' })).toHaveTextContent('');
    });

    it('hands the page of a queue task to the start composer instead of starting a session', async () => {
        const user = userEvent.setup();
        const fetchMock = stubCms([queueTask]);
        const onEditWithGenaix = vi.fn();

        renderWithProviders(<TaskList onEditWithGenaix={onEditWithGenaix} />);

        await user.click(await screen.findByRole('button', { name: 'Edit with GenAIx' }));

        expect(onEditWithGenaix).toHaveBeenCalledExactlyOnceWith([{ type: 'page', id: '9142', node_id: 3, label: 'Terms of use' }]);
        expect(screen.queryByRole('button', { name: 'Start session' })).not.toBeInTheDocument();
        expect(writes(fetchMock)).toEqual([]);
    });

    it('lets a group member take over a group task', async () => {
        const user = userEvent.setup();
        const fetchMock = stubCms([groupTask]);

        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);
        await user.click(await screen.findByRole('button', { name: 'Assign to me' }));

        await waitFor(() => expect(writes(fetchMock)).toEqual([{ url: '/rest/task/8/claim', method: 'POST', body: undefined }]));
    });

    it('takes a group task without opening its page', async () => {
        const user = userEvent.setup();
        const open = vi.fn();
        const withPage = taskFixture({
            ...groupTask,
            id: '9',
            objects: [{ type: 'page', id: 9142, nodeId: 3, name: 'Terms of use' }],
            actions: ['open', 'claim'],
        });
        const fetchMock = stubCms([withPage]);

        vi.stubGlobal('open', open);
        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);
        await user.click(await screen.findByRole('button', { name: 'Assign to me' }));

        await waitFor(() => expect(writes(fetchMock)).toEqual([{ url: '/rest/task/9/claim', method: 'POST', body: undefined }]));
        expect(open).not.toHaveBeenCalled();
    });

    it('keeps the tasks for the user apart from those for the group, and leaves out what another member took', async () => {
        const group = { type: 'group' as const, id: 9, name: 'Chief CC' };
        const direct = taskFixture({ id: '11', title: 'Check the imprint', assignee: { type: 'user', id: TASK_USER.id } });
        const takenByUser = taskFixture({ id: '12', title: 'New tag type for banners', assignee: { type: 'user', id: TASK_USER.id }, assigneeGroup: group });
        const takenByOther = taskFixture({ id: '13', title: 'New tag type for quotes', assignee: { type: 'user', id: 77, name: 'Other One' }, assigneeGroup: group });

        stubCms([direct, takenByUser, groupTask, takenByOther]);
        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);

        const own = within(await screen.findByRole('region', { name: 'Assigned to you' }));

        await waitFor(() => expect(screen.queryByText('New tag type for quotes')).not.toBeInTheDocument());

        const forGroup = within(screen.getByRole('region', { name: 'Open to take' }));

        expect(own.getAllByRole('listitem')).toHaveLength(2);
        // Each group says how many tasks it has, beside its heading.
        expect(screen.getByText('2')).toBeInTheDocument();
        expect(screen.getByText('1')).toBeInTheDocument();
        expect(own.getByText('Check the imprint')).toBeInTheDocument();
        expect(own.getByText('New tag type for banners')).toBeInTheDocument();
        expect(forGroup.getAllByRole('listitem')).toHaveLength(1);
        expect(forGroup.getByText('New tag type for price tables')).toBeInTheDocument();
        expect(forGroup.getByRole('button', { name: 'Assign to me' })).toBeInTheDocument();
    });

    it('counts the other pages with broken links the user may edit below the own, and links the link checker', async () => {
        stubCms([linkTask], { brokenLinkPages: 3 });
        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);

        const group = await screen.findByRole('region', { name: 'Broken links' });

        expect(await within(group).findByText('2 more pages with broken links you may edit')).toBeInTheDocument();
        expect(within(group).getByRole('link', { name: 'View in link checker' })).toHaveAttribute('href', '/editor/#/tools/linkchecker');
        expect(within(group).getByRole('link', { name: 'View in link checker' })).toHaveAttribute('target', '_blank');
        expect(vi.mocked(fetch).mock.calls.map(([input]) => String(input))).toContain('/rest/linkChecker/pages?editable=true&status=invalid&pageSize=0');
    });

    it('shows the pages with broken links without own link checker tasks, also without any task', async () => {
        stubCms([], { brokenLinkPages: 1 });
        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);

        const group = await screen.findByRole('region', { name: 'Broken links' });

        expect(within(group).getByText('1 page with broken links you may edit')).toBeInTheDocument();
        expect(screen.getByText('No open tasks.')).toBeInTheDocument();
    });

    it('shows no count when every page with broken links is an own task', async () => {
        stubCms([linkTask], { brokenLinkPages: 1 });
        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);

        await screen.findByRole('region', { name: 'Broken links' });
        await waitFor(() => expect(requestedPaths()).toContain('/rest/linkChecker/pages'));

        expect(screen.queryByText(/broken links you may edit/)).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'View in link checker' })).not.toBeInTheDocument();
    });

    it('does not count without the link checker', async () => {
        stubCms([linkTask]);
        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);

        await screen.findByRole('region', { name: 'Broken links' });
        await waitFor(() => expect(requestedPaths()).toContain('/rest/admin/features/link_checker'));

        expect(requestedPaths()).not.toContain('/rest/linkChecker/pages');
        expect(screen.queryByRole('link', { name: 'View in link checker' })).not.toBeInTheDocument();
    });

    it('folds out the instruction a task has for GenAIx, and offers nothing to fold without one', async () => {
        const user = userEvent.setup();
        const request = taskFixture({
            id: '31',
            type: 'requestConstruct',
            title: 'Create construct: Terms change table',
            genaix: { instruction: 'Create the construct "Terms change table" (terms_change_table) in the CMS.\nNodes: 3' },
            actions: ['startSession'],
        });

        stubCms([request, groupTask]);
        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);

        const details = await screen.findByRole('button', { name: 'See details' });
        const instruction = screen.getByText(/Create the construct "Terms change table"/);

        expect(details).toHaveAttribute('aria-expanded', 'false');
        expect(instruction).not.toBeVisible();

        await user.click(details);

        expect(screen.getByRole('button', { name: 'Hide details' })).toHaveAttribute('aria-expanded', 'true');
        expect(instruction).toBeVisible();
        expect(instruction).toHaveTextContent('Nodes: 3');
        // Only the task with an instruction has the button.
        expect(screen.getAllByRole('button', { name: /details/ })).toHaveLength(1);
    });

    it('links the live page of a link checker task and starts a session to fix its links', async () => {
        const user = userEvent.setup();
        const fetchMock = stubCms([linkTask]);

        renderWithProviders(<TaskList onEditWithGenaix={vi.fn()} />);

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

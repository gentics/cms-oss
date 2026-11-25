import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import type { CmsTask } from '@/services/cmsApiService/cmsTaskApi';
import { TASK_USER, taskFixture, withTasks } from '@/test/cmsTasks';
import { renderWithProviders } from '@/test/renderWithProviders';

import { TaskDrawer } from './TaskDrawer';

const queueTask = taskFixture({
    id: 'publishQueue:page:9142',
    source: 'publishQueue',
    type: 'approvePublish',
    title: undefined,
    objects: [{ type: 'page', id: 9142, nodeId: 3, name: 'Terms of use' }],
    actions: ['open', 'approve', 'startSession'],
});

const groupTask = taskFixture({ id: '8', title: 'New tag type for price tables', actions: ['complete'] });

const linkTask = taskFixture({
    id: 'linkChecker:page:7710',
    source: 'linkChecker',
    type: 'fixBrokenLinks',
    title: undefined,
    objects: [{ type: 'page', id: 7710, nodeId: 3, name: 'Directions' }],
    actions: ['startSession'],
});

function stubTasks(tasks: CmsTask[]) {
    const fetchMock = vi.fn<typeof fetch>(async () => Response.json({ responseInfo: { responseCode: 'OK' } }));

    vi.stubGlobal('fetch', vi.fn(withTasks(fetchMock, tasks)));

    return fetchMock;
}

describe('TaskDrawer', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows the number of open tasks on its button', async () => {
        stubTasks([queueTask, groupTask, linkTask]);
        renderWithProviders(<TaskDrawer onEditWithGenaix={vi.fn()} />);

        const button = await screen.findByRole('button', { name: '3 open tasks' });

        expect(button).toHaveTextContent('Tasks3');
    });

    it('does not count a group task another member has taken', async () => {
        const takenByOther = taskFixture({
            id: '13',
            assignee: { type: 'user', id: TASK_USER.id + 1, name: 'Other One' },
            assigneeGroup: { type: 'group', id: 9, name: 'Chief CC' },
            actions: ['claim'],
        });

        stubTasks([queueTask, takenByOther]);
        renderWithProviders(<TaskDrawer onEditWithGenaix={vi.fn()} />);

        expect(await screen.findByRole('button', { name: '1 open task' })).toHaveTextContent('Tasks1');
    });

    it('shows no number without open tasks, and says so inside', async () => {
        const user = userEvent.setup();

        stubTasks([]);
        renderWithProviders(<TaskDrawer onEditWithGenaix={vi.fn()} />);

        const button = await screen.findByRole('button', { name: 'No open tasks' });

        expect(button).toHaveTextContent(/^Tasks$/);

        await user.click(button);

        expect(within(await screen.findByRole('dialog', { name: 'Open tasks' })).getByText('No open tasks.')).toBeInTheDocument();
    });

    it('opens the tasks as a drawer from the right', async () => {
        const user = userEvent.setup();

        stubTasks([queueTask, groupTask]);
        renderWithProviders(<TaskDrawer onEditWithGenaix={vi.fn()} />);

        await user.click(await screen.findByRole('button', { name: '2 open tasks' }));

        const drawer = await screen.findByRole('dialog', { name: 'Open tasks' });

        expect(drawer).toHaveAttribute('data-swipe-direction', 'right');
        expect(within(drawer).getAllByRole('heading', { level: 3 }).map((heading) => heading.textContent))
            .toEqual(['Waiting for your approval', 'Assigned to you']);
    });

    it('closes on "Edit with GenAIx" and only then hands the page over', async () => {
        const user = userEvent.setup();
        const fetchMock = stubTasks([queueTask]);
        const onEditWithGenaix = vi.fn();

        renderWithProviders(<TaskDrawer onEditWithGenaix={onEditWithGenaix} />);

        await user.click(await screen.findByRole('button', { name: '1 open task' }));
        await user.click(await screen.findByRole('button', { name: 'Edit with GenAIx' }));

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        await waitFor(() => expect(onEditWithGenaix).toHaveBeenCalledExactlyOnceWith([{ type: 'page', id: '9142', node_id: 3, label: 'Terms of use' }]));
        expect(fetchMock).not.toHaveBeenCalled();
    });
});

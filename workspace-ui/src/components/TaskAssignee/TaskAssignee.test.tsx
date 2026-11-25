import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import { taskFixture } from '@/test/cmsTasks';

import { TaskAssignee } from './TaskAssignee';

import '@/i18n';

const group = { type: 'group' as const, id: 9, name: 'Chief CC' };

describe('TaskAssignee', () => {
    it('offers to take a group task nobody has taken', async () => {
        const user = userEvent.setup();
        const onClaim = vi.fn();
        const task = taskFixture({ id: '8', assignee: group, assigneeGroup: group, actions: ['claim'] });

        render(<TaskAssignee task={task} onClaim={onClaim} onUnclaim={vi.fn()} />, { wrapper: UiProvider });
        await user.click(screen.getByRole('button', { name: 'Assign to me' }));

        expect(onClaim).toHaveBeenCalledOnce();
    });

    it('shows who took the task with initials and name, and gives it back from its menu', async () => {
        const user = userEvent.setup();
        const onUnclaim = vi.fn();
        const task = taskFixture({ id: '8', assignee: { type: 'user', id: 5, name: 'Max Mustermann' }, assigneeGroup: group, actions: ['unclaim'] });

        render(<TaskAssignee task={task} onClaim={vi.fn()} onUnclaim={onUnclaim} />, { wrapper: UiProvider });

        const trigger = screen.getByRole('button', { name: 'Assigned to Max Mustermann' });

        expect(trigger).toHaveTextContent('MMMax Mustermann');
        expect(screen.queryByRole('button', { name: 'Assign to me' })).not.toBeInTheDocument();

        await user.click(trigger);
        await user.click(await screen.findByRole('menuitem', { name: 'Give back' }));

        expect(onUnclaim).toHaveBeenCalledOnce();
        await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
    });

    it('shows nothing for a task that is not up for taking or giving back', () => {
        render(<TaskAssignee task={taskFixture({ id: '11', actions: ['complete'] })} onClaim={vi.fn()} onUnclaim={vi.fn()} />, { wrapper: UiProvider });

        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });
});

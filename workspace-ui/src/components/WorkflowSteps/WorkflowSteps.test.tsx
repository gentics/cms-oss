import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import type { StepView } from '@/helper/workflowSteps/workflowSteps';

import { WorkflowSteps } from './WorkflowSteps';

import '@/i18n';

const STEPS: StepView[] = [
    { id: 'st-1', label: 'Read source material', status: 'done' },
    { id: 'st-2', label: 'Derive folder, template and language', status: 'running' },
    { id: 'st-3', label: 'Search existing content', status: 'pending' },
];

describe('WorkflowSteps', () => {
    it('shows the current step as one line, the list closed', () => {
        render(<WorkflowSteps steps={STEPS} />, { wrapper: UiProvider });

        expect(screen.getByRole('group', { name: 'Steps' })).toHaveTextContent('Step 2 of 3: Derive folder, template and language');
        expect(screen.queryByRole('list')).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Show the steps' })).toHaveAttribute('aria-expanded', 'false');
    });

    it('says when the current step waits for the user', () => {
        render(<WorkflowSteps steps={[STEPS[0]!, { ...STEPS[1]!, status: 'waiting' }]} />, { wrapper: UiProvider });

        expect(screen.getByRole('group', { name: 'Steps' })).toHaveTextContent('Step 2 of 2: Derive folder, template and languageWaiting for you');
    });

    it('shows how many steps are done while none is current', () => {
        render(<WorkflowSteps steps={[STEPS[0]!, STEPS[2]!]} />, { wrapper: UiProvider });

        expect(screen.getByRole('group', { name: 'Steps' })).toHaveTextContent('1 of 2 steps done');
    });

    it('opens the list of the main steps with their status, and closes it again', async () => {
        const user = userEvent.setup();

        render(<WorkflowSteps steps={STEPS} />, { wrapper: UiProvider });

        await user.click(screen.getByRole('button', { name: 'Show the steps' }));

        expect(screen.getAllByRole('listitem').map((item) => item.textContent)).toEqual([
            'Read source materialDone',
            'Derive folder, template and languageIn progress',
            'Search existing contentPending',
        ]);

        await user.click(screen.getByRole('button', { name: 'Hide the steps' }));

        expect(screen.queryByRole('list')).not.toBeInTheDocument();
    });
});

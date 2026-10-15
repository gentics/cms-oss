import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import type { Plan } from '@/services/apiService/genaix/types';

import { RevisePlanBanner } from './RevisePlanBanner';

import '@/i18n';

const plan: Plan = {
    id: 'plan-1',
    workflows: [{ id: 'content_create', version: '1.0.0' }],
    status: 'done',
    version: 1,
    summary: 'Invitation page PNDemo',
    items: [],
};

describe('RevisePlanBanner', () => {
    it('names the plan the next message changes, and leaves that mode with ×', async () => {
        const onCancel = vi.fn();

        render(<RevisePlanBanner plan={plan} revision="ask" onCancel={onCancel} />, { wrapper: UiProvider });

        expect(screen.getByRole('status')).toHaveTextContent('Change this plan');
        expect(screen.getByText('Invitation page PNDemo')).toBeInTheDocument();
        expect(screen.getByText('Your next message changes this plan.')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'Don\'t change the plan' }));
        expect(onCancel).toHaveBeenCalledOnce();
    });

    it('says that sending stops the work of an executing plan', () => {
        render(<RevisePlanBanner plan={{ ...plan, status: 'running' }} revision="stop" onCancel={vi.fn()} />, { wrapper: UiProvider });

        expect(screen.getByText('Sending stops the current work and changes the plan.')).toBeInTheDocument();
    });
});

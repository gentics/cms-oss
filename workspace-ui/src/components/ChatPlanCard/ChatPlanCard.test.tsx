import { screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { Plan, PlanStatus } from '@/services/apiService/genaix/types';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';
import { renderWithProviders } from '@/test/renderWithProviders';

import { ChatPlanCard } from './ChatPlanCard';

const SESSION = 's-1';

function plan(status: PlanStatus): Plan {
    return { id: 'pl-old', workflow: 'content_create', status, version: 1, summary: 'Older terms page.', items: [] };
}

describe('ChatPlanCard', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
        vi.stubGlobal('fetch', vi.fn<typeof fetch>(async () => Response.json({ items: [], next_cursor: null })));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('sends an older released plan to the review page to publish it, offering only discard and reopen here', async () => {
        renderWithProviders(<ChatPlanCard sessionId={SESSION} plan={plan('released')} />, { path: `/sessions/${SESSION}` });

        expect(await screen.findByRole('link', { name: 'Review & release' })).toHaveAttribute('href', `/sessions/${SESSION}/review?plan=pl-old`);
        expect(screen.getByRole('button', { name: 'Discard' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Reopen' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
    });

    it('has no review link for a published plan', async () => {
        renderWithProviders(<ChatPlanCard sessionId={SESSION} plan={plan('published')} />, { path: `/sessions/${SESSION}` });

        expect(await screen.findByRole('button', { name: 'Reopen' })).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
    });
});

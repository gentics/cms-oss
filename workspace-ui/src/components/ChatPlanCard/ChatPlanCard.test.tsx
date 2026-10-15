import { screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { Plan, PlanStatus } from '@/services/apiService/genaix/types';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';
import { renderWithProviders } from '@/test/renderWithProviders';

import { ChatPlanCard } from './ChatPlanCard';

const SESSION = 's-1';

function plan(status: PlanStatus): Plan {
    return { id: 'pl-old', workflows: [{ id: 'content_create', version: '1.0.0' }], status, version: 1, summary: 'Older terms page.', items: [] };
}

describe('ChatPlanCard', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
        vi.stubGlobal('fetch', vi.fn<typeof fetch>(async () => Response.json({ items: [], next_cursor: null })));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('sends an older released plan to the review page to publish it, offering only discard and a change here', async () => {
        renderWithProviders(<ChatPlanCard sessionId={SESSION} plan={plan('released')} />, { path: `/sessions/${SESSION}` });

        expect(await screen.findByRole('link', { name: 'Review & release' })).toHaveAttribute('href', `/sessions/${SESSION}/review?plan=pl-old`);
        expect(screen.getByRole('button', { name: 'Discard' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Ask for a change' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
    });

    it('has no review link for a published plan, and no reopen (rc.5: a revision replaces it)', async () => {
        renderWithProviders(<ChatPlanCard sessionId={SESSION} plan={plan('published')} />, { path: `/sessions/${SESSION}` });

        expect(await screen.findByRole('button', { name: 'Ask for a change' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Reopen' })).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
    });
});

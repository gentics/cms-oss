import { screen, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { Plan, PlanStatus } from '@/services/apiService/genaix/types';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';
import { renderWithProviders } from '@/test/renderWithProviders';

import { PlanBar } from './PlanBar';

const SESSION = 's-1';

// jsdom has no ResizeObserver; the plan bar reports its height with one.
class ResizeObserverStub {
    observe() {}

    disconnect() {}
}

function plan(status: PlanStatus): Plan {
    return { id: 'pl-1', workflow: 'content_create', status, version: 1, summary: 'New terms page.', items: [] };
}

async function renderBar(status: PlanStatus) {
    renderWithProviders(<PlanBar sessionId={SESSION} plan={plan(status)} />, { path: `/sessions/${SESSION}` });

    return screen.findByRole('region', { name: 'Open plan' });
}

describe('PlanBar', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
        vi.stubGlobal('ResizeObserver', ResizeObserverStub);
        vi.stubGlobal('fetch', vi.fn<typeof fetch>(async () => Response.json({ items: [], next_cursor: null })));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('offers no release or publish, only the way to the review page, which opens this plan', async () => {
        const bar = await renderBar('done');

        expect(within(bar).getByRole('button', { name: 'Discard' })).toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Release' })).not.toBeInTheDocument();
        expect(within(bar).getByRole('link', { name: 'Review & release' })).toHaveAttribute('href', `/sessions/${SESSION}/review?plan=pl-1`);
    });

    it('keeps a released plan\'s publish and request review for the review page', async () => {
        const bar = await renderBar('released');

        expect(within(bar).getByRole('button', { name: 'Reopen' })).toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Request review' })).not.toBeInTheDocument();
        expect(within(bar).getByRole('link', { name: 'Review & release' })).toBeInTheDocument();
    });

    it('leads a running plan to the review page too, where it can be released', async () => {
        const bar = await renderBar('running');

        expect(within(bar).queryByRole('button', { name: 'Release' })).not.toBeInTheDocument();
        expect(within(bar).getByRole('link', { name: 'Review & release' })).toBeInTheDocument();
    });

    it('has no review link while the plan is only proposed', async () => {
        const bar = await renderBar('proposed');

        expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
    });
});

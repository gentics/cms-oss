import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { Plan, PlanStatus } from '@/services/apiService/genaix/types';
import { useRevisePlanStore } from '@/store/useRevisePlanStore';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';
import { renderWithProviders } from '@/test/renderWithProviders';

import { PlanBar } from './PlanBar';

const SESSION = 's-1';

// jsdom has no ResizeObserver; the plan bar reports its height with one.
class ResizeObserverStub {
    observe() {}

    disconnect() {}
}

function plan(status: PlanStatus, overrides: Partial<Plan> = {}): Plan {
    return { id: 'pl-1', workflows: [{ id: 'content_create', version: '1.0.0' }], status, version: 1, summary: 'New terms page.', items: [], ...overrides };
}

async function renderBar(status: PlanStatus, overrides: Partial<Plan> = {}) {
    renderWithProviders(<PlanBar sessionId={SESSION} plan={plan(status, overrides)} />, { path: `/sessions/${SESSION}` });

    return screen.findByRole('region', { name: 'Open plan' });
}

describe('PlanBar', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
        useRevisePlanStore.setState({ planIds: {} });
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

        expect(within(bar).getByRole('button', { name: 'Ask for a change' })).toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Request review' })).not.toBeInTheDocument();
        expect(within(bar).getByRole('link', { name: 'Review & release' })).toBeInTheDocument();
    });

    it('leads a running plan to the review page too, where it can be released', async () => {
        const bar = await renderBar('running');

        expect(within(bar).queryByRole('button', { name: 'Release' })).not.toBeInTheDocument();
        expect(within(bar).getByRole('link', { name: 'Review & release' })).toBeInTheDocument();
    });

    it('puts the composer into revising a finished plan with Ask for a change (rc.5)', async () => {
        const user = userEvent.setup();
        const bar = await renderBar('done');

        await user.click(within(bar).getByRole('button', { name: 'Ask for a change' }));

        expect(useRevisePlanStore.getState().planIds).toEqual({ [SESSION]: 'pl-1' });
        expect(within(bar).getByRole('button', { name: 'Ask for a change' })).toHaveAttribute('aria-pressed', 'true');
    });

    it('offers Stop and change while the plan executes', async () => {
        const user = userEvent.setup();
        const bar = await renderBar('running');

        await user.click(within(bar).getByRole('button', { name: 'Stop and change' }));

        expect(useRevisePlanStore.getState().planIds).toEqual({ [SESSION]: 'pl-1' });
    });

    it('shows a revision being drafted with its items, offering discard only', async () => {
        const items: Plan['items'] = [{ id: 'p1', label: 'Create page PNDemo', kind: 'create', workflow: 'content_create', status: 'done' }];
        const bar = await renderBar('drafting', { items });

        expect(within(bar).getByText('Being revised')).toBeInTheDocument();
        expect(within(bar).getByRole('button', { name: 'Discard' })).toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Ask for a change' })).not.toBeInTheDocument();
    });

    it('has a review link unless every module of the plan writes nothing (rc.5: several modules)', async () => {
        const research = { id: 'content_research', version: '1.0.0' };
        const bar = await renderBar('done', { workflows: [research, { id: 'content_create', version: '1.0.0' }] });

        expect(within(bar).getByRole('link', { name: 'Review & release' })).toBeInTheDocument();
    });

    it('has no review link for a plan that only reads', async () => {
        const bar = await renderBar('done', { workflows: [{ id: 'content_research', version: '1.0.0' }] });

        expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
    });

    it('has no review link while the plan is only proposed', async () => {
        const bar = await renderBar('proposed');

        expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
    });
});

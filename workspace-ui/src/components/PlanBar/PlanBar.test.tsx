import { screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { CmsObjectRef, Plan, PlanStatus } from '@/services/apiService/genaix/types';
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

// A page the plan wrote, as `PlanState.cms_objects` lists it.
const WRITTEN_PAGE: CmsObjectRef = { type: 'page', id: 9142, node_id: 3, operation: 'created' };

// Answers the plan's state (`GET …/plans/pl-1`) with `cmsObjects`, every list with an empty page.
function stubPlanState(cmsObjects: CmsObjectRef[]) {
    vi.stubGlobal('fetch', vi.fn<typeof fetch>(async (input) => (
        String(input).endsWith(`/sessions/${SESSION}/plans/pl-1`)
            ? Response.json({ plan: plan('done'), cms_objects: cmsObjects })
            : Response.json({ items: [], next_cursor: null })
    )));
}

async function renderBar(status: PlanStatus) {
    renderWithProviders(<PlanBar sessionId={SESSION} plan={plan(status)} />, { path: `/sessions/${SESSION}` });

    return screen.findByRole('region', { name: 'Open plan' });
}

describe('PlanBar', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
        vi.stubGlobal('ResizeObserver', ResizeObserverStub);
        stubPlanState([WRITTEN_PAGE]);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('offers no release or publish, only the way to the review page, which opens this plan', async () => {
        const bar = await renderBar('done');

        expect(within(bar).getByRole('button', { name: 'Discard' })).toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Release' })).not.toBeInTheDocument();
        expect(await within(bar).findByRole('link', { name: 'Review & release' })).toHaveAttribute('href', `/sessions/${SESSION}/review?plan=pl-1`);
    });

    it('keeps a released plan\'s publish and request review for the review page', async () => {
        const bar = await renderBar('released');

        expect(within(bar).getByRole('button', { name: 'Reopen' })).toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
        expect(within(bar).queryByRole('button', { name: 'Request review' })).not.toBeInTheDocument();
        expect(await within(bar).findByRole('link', { name: 'Review & release' })).toBeInTheDocument();
    });

    it('has no review link while the plan is still running, even with a page written', async () => {
        const bar = await renderBar('running');

        // The plan's state, which lists a page, is not even asked for while the plan runs.
        await waitFor(() => expect(vi.mocked(fetch).mock.calls.length).toBeGreaterThan(0));
        expect(vi.mocked(fetch).mock.calls.map(([input]) => String(input))).not.toContainEqual(expect.stringMatching(/\/plans\/pl-1$/));
        expect(within(bar).queryByRole('button', { name: 'Release' })).not.toBeInTheDocument();
        expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
    });

    it('has no review link for a done plan that wrote no page', async () => {
        const fetchMock = vi.fn<typeof fetch>(async (input) => (
            String(input).endsWith(`/sessions/${SESSION}/plans/pl-1`)
                ? Response.json({ plan: plan('done'), cms_objects: [{ ...WRITTEN_PAGE, operation: 'read' }] })
                : Response.json({ items: [], next_cursor: null })
        ));

        vi.stubGlobal('fetch', fetchMock);
        const bar = await renderBar('done');

        await waitFor(() => expect(fetchMock.mock.calls.map(([input]) => String(input))).toContainEqual(expect.stringMatching(/\/plans\/pl-1$/)));
        expect(within(bar).getByRole('button', { name: 'Discard' })).toBeInTheDocument();
        expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
    });

    it('has no review link while the plan is only proposed', async () => {
        const bar = await renderBar('proposed');

        expect(within(bar).queryByRole('link', { name: 'Review & release' })).not.toBeInTheDocument();
    });
});

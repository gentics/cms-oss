import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';

import type { GenaixEvent, PlanStatus } from '@/services/apiService/genaix/types';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import { type PickedPlan, useOpenPlan, useOpenPlans } from './useOpenPlan';

const SESSION = 's-1';

let seq = 0;

function apply(event: Record<string, unknown>) {
    seq += 1;
    act(() => useWorkspaceEventStore.getState().applyEvent(SESSION, { seq, ts: '2026-10-19T09:00:00Z', session_id: SESSION, ...event } as GenaixEvent));
}

// A plan as `plan.updated` reports it, with one item so that a draft is more than a first one.
function planUpdated(id: string, status: PlanStatus) {
    apply({
        type: 'plan.updated',
        plan_id: id,
        plan: { id, workflows: [{ id: 'content_create', version: '1.0.0' }], status, version: 1, summary: `Plan ${id}`, items: [{ id: 'p1', label: 'Item', kind: 'update', workflow: 'content_create', status: 'pending' }] },
    });
}

// `session.updated` naming the open plans, the one the conversation works on and the executing one.
function sessionUpdated(openPlanIds: string[], openPlanId: string | null, executingPlanId: string | null = null) {
    apply({ type: 'session.updated', changed: ['open_plan_ids'], session: { id: SESSION, open_plan_id: openPlanId, open_plan_ids: openPlanIds, executing_plan_id: executingPlanId } });
}

function ids(plans: { id: string }[]) {
    return plans.map(({ id }) => id);
}

describe('useOpenPlan', () => {
    beforeEach(() => {
        seq = 0;
        useWorkspaceEventStore.setState({ sessions: {} });
    });

    it('has no choice with one open plan, and shows it', () => {
        planUpdated('pl-1', 'done');
        sessionUpdated(['pl-1'], 'pl-1');

        expect(ids(renderHook(() => useOpenPlans(SESSION)).result.current)).toEqual(['pl-1']);
        expect(renderHook(() => useOpenPlan(SESSION)).result.current?.id).toBe('pl-1');
    });

    it('lists several open plans, oldest first, and shows the one the conversation works on', () => {
        planUpdated('pl-1', 'done');
        planUpdated('pl-2', 'proposed');
        sessionUpdated(['pl-1', 'pl-2'], 'pl-2');

        expect(ids(renderHook(() => useOpenPlans(SESSION)).result.current)).toEqual(['pl-1', 'pl-2']);
        expect(renderHook(() => useOpenPlan(SESSION)).result.current?.id).toBe('pl-2');
    });

    it('shows the plan picked while GenAIx names the same open and executing plan, then the open one again', () => {
        planUpdated('pl-1', 'done');
        planUpdated('pl-2', 'proposed');
        sessionUpdated(['pl-1', 'pl-2'], 'pl-2');

        const picked: PickedPlan = { planId: 'pl-1', openPlanId: 'pl-2', executingPlanId: null };
        const { result } = renderHook(() => useOpenPlan(SESSION, picked));

        expect(result.current?.id).toBe('pl-1');

        // A new draft: the plan the conversation works on comes back in view.
        planUpdated('pl-3', 'proposed');
        sessionUpdated(['pl-1', 'pl-2', 'pl-3'], 'pl-3');

        expect(result.current?.id).toBe('pl-3');
    });

    it('ignores a pick that is no open plan any more', () => {
        planUpdated('pl-1', 'published');
        planUpdated('pl-2', 'proposed');
        sessionUpdated(['pl-2'], 'pl-2');

        const { result } = renderHook(() => useOpenPlan(SESSION, { planId: 'pl-1', openPlanId: 'pl-2', executingPlanId: null }));

        expect(result.current?.id).toBe('pl-2');
    });

    it('leaves out an open plan with nothing to show yet, a first draft', () => {
        planUpdated('pl-1', 'done');
        apply({ type: 'plan.updated', plan_id: 'pl-2', plan: { id: 'pl-2', workflows: [], status: 'drafting', version: 0, summary: '', items: [] } });
        sessionUpdated(['pl-1', 'pl-2'], 'pl-2');

        expect(ids(renderHook(() => useOpenPlans(SESSION)).result.current)).toEqual(['pl-1']);
    });
});

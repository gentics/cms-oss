import { isFirstDraft } from '@/helper/planActions/planActions';
import type { Plan, PlanStatus } from '@/services/apiService/genaix/types';
import { type LivePlan, selectSession, type SessionLiveState, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

// The statuses in which the plan is being revised, waits for the user, or has something to release,
// publish or change (09-integration-guide.md, "The plan card"). A revision is `drafting` with the items
// of its current version (rc.5) and stays in view; a first draft has nothing to show yet
// (`isFirstDraft`). A discarded or superseded plan is over and is not shown (plans are not in the chat).
const BAR_STATUSES: ReadonlySet<PlanStatus> = new Set(['drafting', 'proposed', 'accepted', 'running', 'done', 'released', 'published', 'review_requested']);

// The plan the bar is for: the executing one, else the open one, as `session.updated` names them (rc.4);
// with neither, the latest plan that went out (`published`, `review_requested`), so it can be revised.
// Before any `session.updated` the newest plan, as the session's latest `plan.updated` reported it.
function barPlan({ plans = [], openPlanId, executingPlanId }: SessionLiveState): LivePlan | undefined {
    if (openPlanId === undefined && executingPlanId === undefined) {
        return plans.at(-1);
    }

    const id = executingPlanId ?? openPlanId;

    if (id) {
        return plans.find((live) => live.plan.id === id);
    }

    return plans.findLast((live) => live.plan.status === 'published' || live.plan.status === 'review_requested');
}

/**
 * The plan the session's plan bar is for (`barPlan`), while its status is one of `BAR_STATUSES` and it
 * is more than a first draft. `undefined` otherwise.
 */
export function useOpenPlan(sessionId: string): Plan | undefined {
    const plan = useWorkspaceEventStore((state) => barPlan(selectSession(sessionId)(state))?.plan);

    return plan && BAR_STATUSES.has(plan.status) && !isFirstDraft(plan) ? plan : undefined;
}

import { useShallow } from 'zustand/react/shallow';

import { isFirstDraft } from '@/helper/planActions/planActions';
import type { Plan, PlanStatus } from '@/services/apiService/genaix/types';
import { type LivePlan, selectSession, type SessionLiveState, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

// The statuses in which the plan is being revised, waits for the user, or has something to release,
// publish or change (09-integration-guide.md, "The plan card"). A revision is `drafting` with the items
// of its current version (rc.5) and stays in view; a first draft has nothing to show yet
// (`isFirstDraft`). A discarded or superseded plan is over and is not shown (plans are not in the chat).
const BAR_STATUSES: ReadonlySet<PlanStatus> = new Set(['drafting', 'proposed', 'accepted', 'running', 'done', 'released', 'published', 'review_requested']);

/**
 * A plan the user picked from the open plans (`PlanSwitcher`), with the open and executing plan as
 * GenAIx named them when it was picked: it holds until GenAIx names others (a new draft, a revision,
 * a plan that starts executing), so the plan the conversation works on comes back in view (09 §9).
 */
export interface PickedPlan {
    planId: string;
    openPlanId: string | null | undefined;
    executingPlanId: string | null | undefined;
}

function isShown(plan: Plan): boolean {
    return BAR_STATUSES.has(plan.status) && !isFirstDraft(plan);
}

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

// The open plans the bar can show (`Session.open_plan_ids`, oldest first, rc.5), as the stream last reported each.
function openPlansOf({ plans = [], openPlanIds = [] }: SessionLiveState): Plan[] {
    return openPlanIds.flatMap((id) => {
        const plan = plans.find((live) => live.plan.id === id)?.plan;

        return plan && isShown(plan) ? [plan] : [];
    });
}

/**
 * The session's open plans the bar can show, oldest first (rc.5: several may be open, at most one
 * executes). More than one means there is a choice (`PlanSwitcher`); one or none, there is not.
 */
export function useOpenPlans(sessionId: string): Plan[] {
    return useWorkspaceEventStore(useShallow((state) => openPlansOf(selectSession(sessionId)(state))));
}

/**
 * The plan the session's plan bar is for: `picked`, while it still holds (`PickedPlan`) and is one of
 * the open plans; otherwise `barPlan`, while its status is one of `BAR_STATUSES` and it is more than a
 * first draft. `undefined` otherwise.
 */
export function useOpenPlan(sessionId: string, picked?: PickedPlan): Plan | undefined {
    return useWorkspaceEventStore((state) => {
        const session = selectSession(sessionId)(state);
        const holds = picked !== undefined && picked.openPlanId === session.openPlanId && picked.executingPlanId === session.executingPlanId;
        const pickedPlan = holds ? openPlansOf(session).find(({ id }) => id === picked.planId) : undefined;

        if (pickedPlan) {
            return pickedPlan;
        }

        const plan = barPlan(session)?.plan;

        return plan && isShown(plan) ? plan : undefined;
    });
}

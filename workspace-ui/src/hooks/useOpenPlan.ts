import type { Plan, PlanStatus } from '@/services/apiService/genaix/types';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

// The statuses in which the plan waits for the user or has something to release, publish or reopen
// (09-integration-guide.md, "The plan card"). A plan still being drafted has nothing to show yet; a
// discarded or superseded one is over and stays in the chat (`ChatPlanCard`).
const BAR_STATUSES: ReadonlySet<PlanStatus> = new Set(['proposed', 'accepted', 'running', 'done', 'released', 'published', 'review_requested']);

/**
 * The plan the session's plan bar is for: its newest plan, the one `plan.updated` reported last (rc.4:
 * a session holds a sequence of plans), while its status is one of `BAR_STATUSES`. `undefined` otherwise.
 */
export function useOpenPlan(sessionId: string): Plan | undefined {
    const plan = useWorkspaceEventStore((state) => selectSession(sessionId)(state).plans?.at(-1)?.plan);

    return plan && BAR_STATUSES.has(plan.status) ? plan : undefined;
}

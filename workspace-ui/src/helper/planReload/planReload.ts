import { getPlanState, listPlans } from '@/services/apiService/apiService';
import type { PlanSummary } from '@/services/apiService/genaix/types';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

// The codes of a `plan` part that named a plan version no longer current, a closed plan, or one not
// waiting for acceptance: the client re-reads the plans (09-integration-guide.md, "The Workspace UI").
const STALE_PLAN_CODES: ReadonlySet<string> = new Set(['plan_outdated', 'plan_closed', 'plan_not_proposed']);

/** Whether a `genaix_code` says the plans this client shows are out of date. */
export function isStalePlanCode(code: string | undefined): boolean {
    return code !== undefined && STALE_PLAN_CODES.has(code);
}

/**
 * Reads the session's plans again (`GET …/plans`, then `GET …/plans/{plan_id}` for each one this client
 * does not hold as listed) and puts them into `useWorkspaceEventStore` (`hydratePlans`). After
 * `410 events_pruned`, and after a `plan` part was refused as out of date (`isStalePlanCode`). What
 * the event stream reported for a plan meanwhile wins over its snapshot.
 */
export async function reloadPlans(sessionId: string): Promise<void> {
    const store = useWorkspaceEventStore.getState;
    const sinceSeq = selectSession(sessionId)(store()).lastSeq;
    const { items } = await listPlans(sessionId);
    const known = new Map((selectSession(sessionId)(store()).plans ?? []).map(({ plan }) => [plan.id, plan]));
    const isCurrent = ({ id, status, version }: PlanSummary) => known.get(id)?.status === status && known.get(id)?.version === version;
    const states = await Promise.all(items.filter((summary) => !isCurrent(summary)).map(({ id }) => getPlanState(sessionId, id)));

    store().hydratePlans(sessionId, states, sinceSeq);
}

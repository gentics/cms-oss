import type { PlanStatus, TransitionRequest } from '@/services/apiService/genaix/types';

export type PlanAction = TransitionRequest['action'];

// What the user can do with the plan in each status (09-integration-guide.md, "The plan card"), the
// main action last. A proposed plan is accepted from the plan itself, with a message (`planMessage`).
const ACTIONS: Partial<Record<PlanStatus, PlanAction[]>> = {
    proposed: ['discard'],
    accepted: ['release'],
    running: ['release'],
    done: ['discard', 'release'],
    released: ['discard', 'reopen', 'request_review', 'publish'],
    published: ['reopen'],
    review_requested: ['reopen'],
};

/** The transitions the quality gates run for (contract, transitions route): offered only on the review page. */
export const GATED_ACTIONS: ReadonlySet<PlanAction> = new Set<PlanAction>(['release', 'publish', 'request_review']);

/** The transitions the plan's status allows; the gated ones only with `withGated` (on the review page). */
export function planActions(status: PlanStatus, withGated: boolean): PlanAction[] {
    return (ACTIONS[status] ?? []).filter((action) => withGated || !GATED_ACTIONS.has(action));
}

/** Whether the plan's status allows a gated transition, which is made on the review page. */
export function hasGatedActions(status: PlanStatus): boolean {
    return (ACTIONS[status] ?? []).some((action) => GATED_ACTIONS.has(action));
}

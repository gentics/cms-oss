import type { Plan, PlanStatus, TransitionRequest } from '@/services/apiService/genaix/types';

export type PlanAction = TransitionRequest['action'];

// What the user can do with the plan in each status (09-integration-guide.md, "The plan card"), the
// main action last. A proposed plan is accepted from the plan itself, with a message (`planMessage`);
// a plan is changed with a message too (`revisionOf`), never a transition (rc.5 removed `reopen`).
const ACTIONS: Partial<Record<PlanStatus, PlanAction[]>> = {
    drafting: ['discard'],
    proposed: ['discard'],
    accepted: ['release'],
    running: ['release'],
    done: ['discard', 'release'],
    released: ['discard', 'request_review', 'publish'],
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

/** How the user changes a plan with a message (`options.mode: revise`, rc.5). */
export type Revision = 'ask' | 'stop';

// "Ask for a change" on a plan that finished its work, "Stop and change" on one that executes, which
// the message redirects (contract §4.4, §4.5). A proposed plan is changed in the card (Adjust) or by
// writing; a drafting one is being revised already; a final one never changes again.
const REVISIONS: Partial<Record<PlanStatus, Revision>> = {
    accepted: 'stop',
    running: 'stop',
    done: 'ask',
    released: 'ask',
    published: 'ask',
    review_requested: 'ask',
};

/** How the plan can be changed with a message in its status, if at all. */
export function revisionOf(status: PlanStatus): Revision | undefined {
    return REVISIONS[status];
}

/** Whether the plan can still be revised: every status but the final ones (`409 plan_not_revisable`). */
export function isRevisable(status: PlanStatus): boolean {
    return status !== 'discarded' && status !== 'superseded';
}

/**
 * A plan the session agent has just drafted, with nothing to show yet: `drafting` without items. A
 * revision is `drafting` too, but keeps its items and version while it is drafted (rc.5), so it stays
 * in view, being revised.
 */
export function isFirstDraft(plan: Plan): boolean {
    return plan.status === 'drafting' && (plan.items ?? []).length === 0;
}

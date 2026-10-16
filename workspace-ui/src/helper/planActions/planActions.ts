import type { Plan, PlanStatus, Publication, TransitionRequest, Workflow } from '@/services/apiService/genaix/types';

export type PlanAction = TransitionRequest['action'];

// What the user can do with the plan in each status (09-integration-guide.md, "The plan card"), the
// main action last. A proposed plan is accepted from the plan itself, with a message (`planMessage`);
// a plan is changed with a message too, never a transition (rc.5 removed `reopen`).
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

/**
 * Whether the plan is accepted on its card: one of its modules has `plan_approval: true`
 * (08-workflow-modules.md §6.5). Otherwise its run asks the one confirmation in the chat (a module that
 * writes) or it needs none (one that only reads). `false` while the modules are not known.
 */
export function isAcceptedOnCard(plan: Plan, workflows: Workflow[] | undefined): boolean {
    return (plan.workflows ?? []).some(({ id }) => workflows?.find((workflow) => workflow.id === id)?.plan_approval === true);
}

/** How much of a plan the UI shows: its whole card, only its head line, or nothing. */
export type PlanPresence = 'card' | 'head' | 'none';

// The statuses of a plan whose work is under way, from its draft to its run.
const UNDER_WAY: ReadonlySet<PlanStatus> = new Set<PlanStatus>(['drafting', 'proposed', 'accepted', 'running']);

/**
 * How much of a plan to show (`PlanPresence`). A plan accepted on its card (`isAcceptedOnCard`) shows its
 * card. Every other one is confirmed in the chat or needs no acceptance, so its steps are left out: its
 * head alone, for what the user may still do (Stop while it works, "Review & release" for its pages,
 * the per-page outcome once published, a closed plan as closed, 09-integration-guide.md "The Workspace
 * UI"), and nothing once it is done without a page to publish. While the modules are not known, its
 * card.
 */
export function planPresence(plan: Plan, workflows: Workflow[] | undefined, publications: Publication[] | undefined): PlanPresence {
    const modules = (plan.workflows ?? []).map(({ id }) => workflows?.find((workflow) => workflow.id === id));

    if (!workflows || modules.length === 0 || modules.some((module) => !module || module.plan_approval === true)) {
        return 'card';
    }

    const isClosed = plan.status === 'discarded' || plan.status === 'superseded';

    return UNDER_WAY.has(plan.status) || isClosed || (publications?.length ?? 0) > 0 ? 'head' : 'none';
}

/**
 * A plan the session agent has just drafted, with nothing to show yet: `drafting` without items. A
 * revision is `drafting` too, but keeps its items and version while it is drafted (rc.5), so it stays
 * in view, being revised.
 */
export function isFirstDraft(plan: Plan): boolean {
    return plan.status === 'drafting' && (plan.items ?? []).length === 0;
}

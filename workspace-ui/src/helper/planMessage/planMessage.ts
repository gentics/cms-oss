import type { ContextReference, MessageCreateBody, Plan, UserPlanItem } from '@/services/apiService/genaix/types';

/** One step of a plan the user accepts: its label, the plan item it keeps (`id`) and its target. */
export interface PlanStepInput {
    /** The id of a step of the proposed plan; a step without one is new. */
    id?: string;
    label: string;
    target?: ContextReference;
}

/**
 * The message that accepts the agent's plan: a `plan` part naming the plan and the version the user
 * saw, in `execute` mode. Since rc.4 only this part accepts a plan; a plain message goes to the session
 * agent (contract, `UserPlanPart`). Accepted as proposed, it carries no `items`. Adjusted, it carries
 * the steps in their new order: one with the `id` of a proposed step keeps that step, one without an
 * `id` is new, a proposed step left out is removed.
 */
export function planMessage(plan: Plan, steps?: PlanStepInput[]): MessageCreateBody {
    const items = steps?.map(({ id, label, target }): UserPlanItem => ({
        ...(id !== undefined && { id }),
        label,
        ...(target && { target }),
    }));

    return {
        parts: [{ type: 'plan', plan: { plan_id: plan.id, based_on_version: plan.version, ...(items && { items }) } }],
        options: { mode: 'execute' },
    };
}

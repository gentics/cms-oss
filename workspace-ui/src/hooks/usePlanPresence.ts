import { createdUsersOf, isFinished, type PlanPresence, planPresence } from '@/helper/planActions/planActions';
import { usePlanState, useWorkflows } from '@/hooks/useGenaixQueries';
import type { Plan } from '@/services/apiService/genaix/types';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

/**
 * How much of `plan` to show (`planPresence`), from the module catalogue and its pages' publications; `none` without a plan.
 * A finished plan that would show nothing keeps its head when it created a CMS user, for the user's password link
 * (`PasswordLinkButton`); only then is its state asked for.
 */
export function usePlanPresence(sessionId: string, plan: Plan | undefined): PlanPresence {
    // The module catalogue only for a plan: without one there is nothing to show either way.
    const workflows = useWorkflows(plan !== undefined).data;
    const publications = useWorkspaceEventStore((state) => (plan ? selectSession(sessionId)(state).publications?.[plan.id] : undefined));
    const presence = plan ? planPresence(plan, workflows, publications) : 'none';
    const asksState = presence === 'none' && plan !== undefined && isFinished(plan.status);
    const state = usePlanState(sessionId, asksState ? plan.id : undefined);

    return asksState && createdUsersOf(state.data?.cms_objects ?? []).length > 0 ? 'head' : presence;
}

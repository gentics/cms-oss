import { type PlanPresence, planPresence } from '@/helper/planActions/planActions';
import { useWorkflows } from '@/hooks/useGenaixQueries';
import type { Plan } from '@/services/apiService/genaix/types';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

/** How much of `plan` to show (`planPresence`), from the module catalogue and its pages' publications; `none` without a plan. */
export function usePlanPresence(sessionId: string, plan: Plan | undefined): PlanPresence {
    // The module catalogue only for a plan: without one there is nothing to show either way.
    const workflows = useWorkflows(plan !== undefined).data;
    const publications = useWorkspaceEventStore((state) => (plan ? selectSession(sessionId)(state).publications?.[plan.id] : undefined));

    return plan ? planPresence(plan, workflows, publications) : 'none';
}

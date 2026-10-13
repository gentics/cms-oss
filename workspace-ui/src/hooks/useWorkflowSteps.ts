import { mergeSteps, type StepView } from '@/helper/workflowSteps/workflowSteps';
import { useWorkflows } from '@/hooks/useGenaixQueries';
import type { PlanStatus } from '@/services/apiService/genaix/types';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

// A closed plan never changes again (contract rc.4, `PlanStatus`): its steps are no longer where the agent is.
const CLOSED: ReadonlySet<PlanStatus> = new Set(['published', 'review_requested', 'discarded', 'superseded']);

/**
 * The steps of the session's current plan, the newest one `plan.updated` reported (rc.4: each plan has
 * its own workflow): its module's `steps_template` (`GET /workflows`, picked by `Plan.workflow`), each
 * with what `step.updated` reported for that plan. Empty without a plan, once the plan is closed, while
 * the modules are loading, and for a module without steps.
 */
export function useWorkflowSteps(sessionId: string): StepView[] {
    const plan = useWorkspaceEventStore((state) => selectSession(sessionId)(state).plans?.at(-1)?.plan);
    const live = useWorkspaceEventStore((state) => (plan ? selectSession(sessionId)(state).steps?.[plan.id] : undefined));
    const workflow = useWorkflows().data?.find(({ id }) => id === plan?.workflow);

    return plan && workflow && !CLOSED.has(plan.status) ? mergeSteps(workflow.steps_template, live) : [];
}

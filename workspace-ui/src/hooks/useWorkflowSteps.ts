import { mergeSteps, type ModuleSteps, type StepView } from '@/helper/workflowSteps/workflowSteps';
import { useWorkflows } from '@/hooks/useGenaixQueries';
import type { PlanStatus } from '@/services/apiService/genaix/types';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

// A settled or final plan is not where the agent is any more (contract rc.5, `PlanStatus`); a settled
// one that is revised is `drafting` again and shows its steps again.
const CLOSED: ReadonlySet<PlanStatus> = new Set(['published', 'review_requested', 'discarded', 'superseded']);

/**
 * The steps of the session's current plan, the newest one `plan.updated` reported: the `steps_template`
 * of each of its modules (`GET /workflows`, in the order of `Plan.workflows`, rc.5), each with what
 * `step.updated` reported for that plan. Empty without a plan, once the plan is closed, while the
 * modules are loading, and for modules without steps.
 */
export function useWorkflowSteps(sessionId: string): StepView[] {
    const plan = useWorkspaceEventStore((state) => selectSession(sessionId)(state).plans?.at(-1)?.plan);
    const live = useWorkspaceEventStore((state) => (plan ? selectSession(sessionId)(state).steps?.[plan.id] : undefined));
    const workflows = useWorkflows().data;

    if (!plan || !workflows || CLOSED.has(plan.status)) {
        return [];
    }

    const modules = (plan.workflows ?? []).flatMap(({ id }): ModuleSteps[] => {
        const workflow = workflows.find((candidate) => candidate.id === id);

        return workflow ? [{ workflow: id, steps: workflow.steps_template }] : [];
    });

    return modules.length > 0 ? mergeSteps(modules, live) : [];
}

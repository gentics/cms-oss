import { mergeSteps, type StepView } from '@/helper/workflowSteps/workflowSteps';
import { useSession, useWorkflows } from '@/hooks/useGenaixQueries';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

/**
 * The steps of the session's workflow: the module's `steps_template` (`GET /workflows`, picked by
 * `Session.workflow`), each with what `step.updated` reported. Empty while either is loading, and for a
 * module without steps (`free_chat`).
 */
export function useWorkflowSteps(sessionId: string): StepView[] {
    const workflowId = useSession(sessionId).data?.workflow;
    const workflow = useWorkflows().data?.find(({ id }) => id === workflowId);
    const live = useWorkspaceEventStore((state) => selectSession(sessionId)(state).steps);

    return workflow ? mergeSteps(workflow.steps_template, live) : [];
}

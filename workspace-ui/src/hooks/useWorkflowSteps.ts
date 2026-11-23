import { useShallow } from 'zustand/react/shallow';

import { type StepLine, stepLines } from '@/helper/workflowSteps/workflowSteps';
import { useWorkflows } from '@/hooks/useGenaixQueries';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

/** The session's steps lines; templates from the catalogue, as runs report only the steps they reached. */
export function useWorkflowSteps(sessionId: string): StepLine[] {
    const { stepRuns = [], plans = [] } = useWorkspaceEventStore(useShallow((state) => {
        const session = selectSession(sessionId)(state);

        return { stepRuns: session.stepRuns, plans: session.plans };
    }));
    const workflows = useWorkflows().data ?? [];

    return stepLines(
        stepRuns,
        (id) => workflows.find((workflow) => workflow.id === id)?.steps_template,
        (planId) => (plans.find(({ plan }) => plan.id === planId)?.plan.workflows ?? []).map(({ id }) => id),
    );
}

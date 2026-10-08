import type { Step, StepTemplate } from '@/services/apiService/genaix/types';

/** One main step as the chat shows it: the module's template step with what the agent reported. */
export interface StepView {
    id: string;
    label: string;
    status: Step['status'];
}

/**
 * The workflow's main steps for the chat: `steps_template` of the session's module (`GET /workflows`)
 * in its order, every step `pending` until `step.updated` reports it. Reported steps the template does
 * not name follow at the end (the session can run an older module version than `GET /workflows` lists).
 */
export function mergeSteps(template: StepTemplate[], live: Record<string, Step> = {}): StepView[] {
    const known = new Set(template.map(({ id }) => id));
    const view = ({ id, label }: { id: string; label: string }): StepView => ({
        id,
        label: live[id]?.label ?? label,
        status: live[id]?.status ?? 'pending',
    });

    return [...template.map(view), ...Object.values(live).filter(({ id }) => !known.has(id)).map(view)];
}

/** The step the agent is at: one waiting for the user, else the last one running. */
export function currentStep(steps: StepView[]): StepView | undefined {
    return steps.find(({ status }) => status === 'waiting') ?? steps.findLast(({ status }) => status === 'running');
}

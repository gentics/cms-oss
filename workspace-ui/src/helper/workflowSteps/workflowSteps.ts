import type { Step, StepTemplate } from '@/services/apiService/genaix/types';

/** One main step as the chat shows it: the module's template step with what the agent reported. */
export interface StepView {
    id: string;
    label: string;
    status: Step['status'];
}

/** One module of a plan with its `steps_template` (`GET /workflows`). */
export interface ModuleSteps {
    workflow: string;
    steps: StepTemplate[];
}

/**
 * The plan's main steps for the chat: the `steps_template` of each of its modules (`GET /workflows`),
 * in the plan's module order, every step `pending` until `step.updated` reports it. A reported step's
 * id is always `<workflow>/<id>` (rc.5), the template's is the module's own, so the template ids are
 * prefixed before they are matched. Reported steps no template names follow at the end: a step of no
 * module (`plan/<id>`), or one of a module version older than `GET /workflows` lists.
 */
export function mergeSteps(modules: ModuleSteps[], live: Record<string, Step> = {}): StepView[] {
    const template = modules.flatMap(({ workflow, steps }) => steps.map(({ id, label }) => ({ id: `${workflow}/${id}`, label })));
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

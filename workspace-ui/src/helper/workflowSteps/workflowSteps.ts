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

/** One run of a plan and its steps (`LiveStepRun`); own type so the helper needs no store. */
export interface ReportedRun {
    runId: string;
    planId: string;
    workflow?: string;
    steps: Record<string, Step>;
    closesPlan?: true;
}

/** One steps line; `closingRunId` is set once it is through, so the chat knows where it stays. */
export interface StepLine {
    /** From the first run, so the line keeps its open state while it moves. */
    key: string;
    runIds: string[];
    closingRunId?: string;
    steps: StepView[];
}

// Not `failed`: the agent may report it again.
const THROUGH: ReadonlySet<Step['status']> = new Set(['done', 'skipped']);

// A proposing run names no module, so only its step ids (`<workflow>/<id>`) tell.
function moduleOf(stepId: string): string {
    return stepId.slice(0, Math.max(stepId.indexOf('/'), 0));
}

/**
 * One steps line per pass through a plan's work. `closesPlan` also ends a line, or a discarded plan's
 * line would never stop moving.
 */
export function stepLines(
    runs: ReportedRun[],
    templateOf: (workflow: string) => StepTemplate[] | undefined,
    moduleOrder: (planId: string) => string[] = () => [],
): StepLine[] {
    const plans = new Map<string, { modules: string[]; steps: Record<string, Step>; open?: StepLine; hasReported: boolean }>();
    const lines: StepLine[] = [];
    // Else a transition run that reports nothing adds a copy of the line before.
    const ownWork = new Set<StepLine>();

    runs.forEach((run) => {
        const plan = plans.get(run.planId) ?? { modules: [], steps: {}, hasReported: false };
        const line = plan.open ?? { key: `workflow-steps-${run.runId}`, runIds: [], steps: [] };

        if (!plan.open) {
            plan.open = line;
            lines.push(line);
        }

        plans.set(run.planId, plan);
        line.runIds.push(run.runId);
        // Once per module, or its template repeats per reported step.
        [...(run.workflow ? [run.workflow] : []), ...Object.keys(run.steps).map(moduleOf)].forEach((module) => {
            if (!plan.modules.includes(module)) {
                plan.modules.push(module);
            }
        });
        Object.assign(plan.steps, run.steps);
        plan.hasReported ||= Object.keys(run.steps).length > 0;

        if (run.workflow || Object.keys(run.steps).length > 0) {
            ownWork.add(line);
        }

        const order = moduleOrder(run.planId);
        const rank = (module: string) => (order.includes(module) ? order.indexOf(module) : order.length);
        const modules = [...plan.modules]
            .sort((a, b) => rank(a) - rank(b))
            .flatMap((workflow): ModuleSteps[] => {
                const steps = templateOf(workflow);

                return steps ? [{ workflow, steps }] : [];
            });

        line.steps = mergeSteps(modules, plan.steps);

        // While the catalogue loads, the reported steps alone could look all through.
        const isKnown = plan.modules.every((module) => module === 'plan' || modules.some(({ workflow }) => workflow === module));

        if (run.closesPlan || (isKnown && plan.hasReported && line.steps.every(({ status }) => THROUGH.has(status)))) {
            line.closingRunId = run.runId;
            plan.open = undefined;
            plan.hasReported = false;
        }
    });

    return lines.filter((line) => ownWork.has(line) && line.steps.length > 0);
}

/** The step the agent is at: one waiting for the user, else the last one running. */
export function currentStep(steps: StepView[]): StepView | undefined {
    return steps.find(({ status }) => status === 'waiting') ?? steps.findLast(({ status }) => status === 'running');
}

import { describe, expect, it } from 'vitest';

import type { Step, StepTemplate } from '@/services/apiService/genaix/types';

import { currentStep, mergeSteps, type ModuleSteps, type ReportedRun, type StepLine, stepLines } from './workflowSteps';

const TEMPLATE: StepTemplate[] = [
    { id: 'st-1', label: 'Read source material' },
    { id: 'st-2', label: 'Derive folder, template and language', substeps: [{ id: 'st-2-1', label: 'Check templates' }] },
    { id: 'st-3', label: 'Search existing content' },
];

const CREATE: ModuleSteps[] = [{ workflow: 'content_create', steps: TEMPLATE }];

function step(id: string, label: string, status: Step['status']): Step {
    return { id, label, status, substeps: [] };
}

describe('mergeSteps', () => {
    it('lists the main template steps as pending before anything was reported, with the module prefix (rc.5)', () => {
        expect(mergeSteps(CREATE)).toEqual([
            { id: 'content_create/st-1', label: 'Read source material', status: 'pending' },
            { id: 'content_create/st-2', label: 'Derive folder, template and language', status: 'pending' },
            { id: 'content_create/st-3', label: 'Search existing content', status: 'pending' },
        ]);
    });

    it('takes status and label from the reported step, matched by its prefixed id', () => {
        expect(mergeSteps(CREATE, {
            'content_create/st-1': step('content_create/st-1', 'Read the source', 'done'),
            'content_create/st-2': step('content_create/st-2', 'Derive folder, template and language', 'waiting'),
        })).toEqual([
            { id: 'content_create/st-1', label: 'Read the source', status: 'done' },
            { id: 'content_create/st-2', label: 'Derive folder, template and language', status: 'waiting' },
            { id: 'content_create/st-3', label: 'Search existing content', status: 'pending' },
        ]);
    });

    it('lists the steps of every module in the plan\'s order, each once', () => {
        const edit: ModuleSteps = { workflow: 'content_edit', steps: [{ id: 'st-1', label: 'Read the pages' }, { id: 'st-2', label: 'Edit the pages' }] };
        const steps = mergeSteps([...CREATE, edit], {
            'content_create/st-3': step('content_create/st-3', 'Search existing content', 'done'),
            'content_edit/st-1': step('content_edit/st-1', 'Read the pages', 'running'),
        });

        expect(steps.map(({ id, status }) => `${id} ${status}`)).toEqual([
            'content_create/st-1 pending',
            'content_create/st-2 pending',
            'content_create/st-3 done',
            'content_edit/st-1 running',
            'content_edit/st-2 pending',
        ]);
    });

    it('appends reported steps no template names, such as a step of no module', () => {
        expect(mergeSteps(CREATE, { 'plan/st-publish': step('plan/st-publish', 'Publish', 'running') }).map(({ id }) => id))
            .toEqual(['content_create/st-1', 'content_create/st-2', 'content_create/st-3', 'plan/st-publish']);
    });
});

describe('stepLines', () => {
    const EDIT_TEMPLATE: StepTemplate[] = [{ id: 'st-1', label: 'Read the pages' }, { id: 'st-2', label: 'Edit the pages' }];
    const templateOf = (workflow: string) => ({ content_create: TEMPLATE, content_edit: EDIT_TEMPLATE } as Record<string, StepTemplate[]>)[workflow];
    const run = (runId: string, steps: Record<string, Step['status']>, extra: Partial<ReportedRun> = {}): ReportedRun => ({
        runId,
        planId: 'p-1',
        steps: Object.fromEntries(Object.entries(steps).map(([id, status]) => [id, step(id, id, status)])),
        ...extra,
    });
    const statuses = (line?: StepLine) => line?.steps.map(({ id, status }) => `${id} ${status}`);

    it('takes a plan\'s proposing and execute runs into one line with the whole template, through once every step is done', () => {
        const proposing = run('r-1', { 'content_create/st-1': 'done', 'content_create/st-2': 'done' });
        const executing = run('r-2', { 'content_create/st-3': 'running' }, { workflow: 'content_create' });

        expect(stepLines([proposing], templateOf)).toEqual([{
            key: 'workflow-steps-r-1',
            runIds: ['r-1'],
            steps: [
                { id: 'content_create/st-1', label: 'content_create/st-1', status: 'done' },
                { id: 'content_create/st-2', label: 'content_create/st-2', status: 'done' },
                { id: 'content_create/st-3', label: 'Search existing content', status: 'pending' },
            ],
        }]);

        const [running] = stepLines([proposing, executing], templateOf);

        expect(running?.runIds).toEqual(['r-1', 'r-2']);
        expect(running?.closingRunId).toBeUndefined();

        const [through] = stepLines([proposing, { ...executing, steps: { 'content_create/st-3': step('content_create/st-3', 'Search', 'done') } }], templateOf);

        expect(through?.closingRunId).toBe('r-2');
    });

    it('starts the next line with the plan\'s next run once a line is through, each step as last reported', () => {
        const lines = stepLines([
            run('r-1', { 'content_create/st-1': 'done', 'content_create/st-2': 'done', 'content_create/st-3': 'done' }),
            run('r-2', { 'content_create/st-2': 'running' }),
        ], templateOf);

        expect(lines.map(({ key, closingRunId }) => [key, closingRunId])).toEqual([['workflow-steps-r-1', 'r-1'], ['workflow-steps-r-2', undefined]]);
        expect(statuses(lines[0])).toEqual(['content_create/st-1 done', 'content_create/st-2 done', 'content_create/st-3 done']);
        expect(statuses(lines[1])).toEqual(['content_create/st-1 done', 'content_create/st-2 running', 'content_create/st-3 done']);
    });

    it('keeps a line open while the template of one of its modules is not known', () => {
        const [line] = stepLines([run('r-1', { 'content_create/st-1': 'done' })], () => undefined);

        expect(statuses(line)).toEqual(['content_create/st-1 done']);
        expect(line?.closingRunId).toBeUndefined();
    });

    it('keeps a line with a failed step open', () => {
        const [line] = stepLines([run('r-1', { 'content_create/st-1': 'done', 'content_create/st-2': 'failed', 'content_create/st-3': 'skipped' })], templateOf);

        expect(line?.closingRunId).toBeUndefined();
    });

    it('takes a line through once GenAIx said the plan\'s work is over, whatever its steps say', () => {
        const [line] = stepLines([run('r-1', { 'content_create/st-1': 'done' }, { closesPlan: true })], templateOf);

        expect(line?.closingRunId).toBe('r-1');
        expect(statuses(line)).toEqual(['content_create/st-1 done', 'content_create/st-2 pending', 'content_create/st-3 pending']);
    });

    it('shows the modules in the plan\'s order, a step of no module last, and no module a later revision adds in an earlier line', () => {
        const order = () => ['content_create', 'content_edit'];
        const lines = stepLines([
            run('r-1', { 'content_create/st-1': 'done', 'content_create/st-2': 'done', 'content_create/st-3': 'done' }),
            run('r-2', { 'plan/st-publish': 'running', 'content_edit/st-1': 'done' }),
        ], templateOf, order);

        expect(statuses(lines[0])).toEqual(['content_create/st-1 done', 'content_create/st-2 done', 'content_create/st-3 done']);
        expect(statuses(lines[1])).toEqual([
            'content_create/st-1 done',
            'content_create/st-2 done',
            'content_create/st-3 done',
            'content_edit/st-1 done',
            'content_edit/st-2 pending',
            'plan/st-publish running',
        ]);
    });

    it('lists each module\'s template once, however many of its steps a run reported', () => {
        const [line] = stepLines([run('r-1', { 'content_create/st-1': 'done', 'content_create/st-2': 'done', 'content_create/st-3': 'running' })], templateOf);

        expect(line?.steps.map(({ id }) => id)).toEqual(['content_create/st-1', 'content_create/st-2', 'content_create/st-3']);
    });

    it('leaves out the line of a run that reported nothing and works in no module, such as a transition run', () => {
        const lines = stepLines([
            run('r-1', { 'content_create/st-1': 'done', 'content_create/st-2': 'done', 'content_create/st-3': 'done' }),
            run('r-2', {}, { closesPlan: true }),
        ], templateOf);

        expect(lines.map(({ key }) => key)).toEqual(['workflow-steps-r-1']);
    });

    it('keeps the lines of two plans apart, and leaves out a line with nothing to show', () => {
        const lines = stepLines([
            run('r-1', { 'content_create/st-1': 'running' }),
            { runId: 'r-2', planId: 'p-2', steps: {} },
        ], templateOf);

        expect(lines.map(({ key }) => key)).toEqual(['workflow-steps-r-1']);
    });
});

describe('currentStep', () => {
    it('names a step waiting for the user before one that runs', () => {
        const steps = mergeSteps(CREATE, {
            'content_create/st-2': step('content_create/st-2', 'Derive', 'running'),
            'content_create/st-3': step('content_create/st-3', 'Search', 'waiting'),
        });

        expect(currentStep(steps)?.id).toBe('content_create/st-3');
    });

    it('names the last running step, and none when nothing runs', () => {
        const running = mergeSteps(CREATE, {
            'content_create/st-2': step('content_create/st-2', 'Derive', 'running'),
            'content_create/st-3': step('content_create/st-3', 'Search', 'running'),
        });

        expect(currentStep(running)?.id).toBe('content_create/st-3');
        expect(currentStep(mergeSteps(CREATE))).toBeUndefined();
    });
});

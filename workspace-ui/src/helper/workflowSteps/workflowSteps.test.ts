import { describe, expect, it } from 'vitest';

import type { Step, StepTemplate } from '@/services/apiService/genaix/types';

import { currentStep, mergeSteps, type ModuleSteps } from './workflowSteps';

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

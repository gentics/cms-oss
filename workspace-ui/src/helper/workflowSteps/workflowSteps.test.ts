import { describe, expect, it } from 'vitest';

import type { Step, StepTemplate } from '@/services/apiService/genaix/types';

import { currentStep, mergeSteps } from './workflowSteps';

const TEMPLATE: StepTemplate[] = [
    { id: 'st-1', label: 'Read source material' },
    { id: 'st-2', label: 'Derive folder, template and language', substeps: [{ id: 'st-2-1', label: 'Check templates' }] },
    { id: 'st-3', label: 'Search existing content' },
];

function step(id: string, label: string, status: Step['status']): Step {
    return { id, label, status, substeps: [] };
}

describe('mergeSteps', () => {
    it('lists the main template steps as pending before anything was reported', () => {
        expect(mergeSteps(TEMPLATE)).toEqual([
            { id: 'st-1', label: 'Read source material', status: 'pending' },
            { id: 'st-2', label: 'Derive folder, template and language', status: 'pending' },
            { id: 'st-3', label: 'Search existing content', status: 'pending' },
        ]);
    });

    it('takes status and label from the reported step', () => {
        expect(mergeSteps(TEMPLATE, { 'st-1': step('st-1', 'Read the source', 'done'), 'st-2': step('st-2', 'Derive folder, template and language', 'waiting') })).toEqual([
            { id: 'st-1', label: 'Read the source', status: 'done' },
            { id: 'st-2', label: 'Derive folder, template and language', status: 'waiting' },
            { id: 'st-3', label: 'Search existing content', status: 'pending' },
        ]);
    });

    it('appends reported steps the template does not name', () => {
        expect(mergeSteps(TEMPLATE, { 'st-9': step('st-9', 'Extra step', 'running') }).map(({ id }) => id)).toEqual(['st-1', 'st-2', 'st-3', 'st-9']);
    });
});

describe('currentStep', () => {
    it('names a step waiting for the user before one that runs', () => {
        const steps = mergeSteps(TEMPLATE, { 'st-2': step('st-2', 'Derive', 'running'), 'st-3': step('st-3', 'Search', 'waiting') });

        expect(currentStep(steps)?.id).toBe('st-3');
    });

    it('names the last running step, and none when nothing runs', () => {
        const running = mergeSteps(TEMPLATE, { 'st-2': step('st-2', 'Derive', 'running'), 'st-3': step('st-3', 'Search', 'running') });

        expect(currentStep(running)?.id).toBe('st-3');
        expect(currentStep(mergeSteps(TEMPLATE))).toBeUndefined();
    });
});

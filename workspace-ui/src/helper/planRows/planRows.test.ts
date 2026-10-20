import { describe, expect, it } from 'vitest';

import type { PlanItem, Problem } from '@/services/apiService/genaix/types';

import { invalidRowsOf, isChanged, rowsOf, stepsOf } from './planRows';

const ITEMS: PlanItem[] = [
    { id: 'p1', label: 'Create page PNDemo', kind: 'create', workflow: 'content_create', status: 'done' },
    { id: 'p2', label: 'Upload the PDF', kind: 'create', workflow: 'content_create', status: 'pending' },
    { id: 'p3', label: 'Add a teaser image', kind: 'update', workflow: 'content_edit', status: 'removed' },
];

describe('planRows', () => {
    it('sends the steps trimmed, without empty or removed ones', () => {
        const [p1, p2, p3] = rowsOf(ITEMS);

        expect(stepsOf([p1!, { ...p2!, label: '  Upload the PDF ' }, { key: 'new', label: ' ' }, p3!]).map(({ id, label }) => ({ id, label }))).toEqual([
            { id: 'p1', label: 'Create page PNDemo' },
            { id: 'p2', label: 'Upload the PDF' },
        ]);
    });

    it('tells an adjustment from the plan as proposed, which leaves out the removed items', () => {
        const rows = rowsOf(ITEMS);

        expect(isChanged(stepsOf(rows), ITEMS)).toBe(false);
        expect(isChanged(stepsOf([...rows].reverse()), ITEMS)).toBe(true);
        expect(isChanged(stepsOf(rows.map((row) => ({ ...row, isRemoved: false }))), ITEMS)).toBe(true);
    });

    it('finds the rows a 422 points at, by the order they were sent in', () => {
        const sent = stepsOf(rowsOf(ITEMS));
        const problem: Problem = {
            type: 't',
            title: 'Validation failed',
            status: 422,
            genaix_code: 'validation_failed',
            errors: [
                { pointer: '/parts/0/plan/items/1', detail: 'p2 depends on p1', code: 'dependency_order' },
                { pointer: '/parts/0/plan/items/0/label', detail: 'done', code: 'done_item_changed' },
                { pointer: '/options/mode', detail: 'unknown', code: 'other' },
            ],
        };

        expect(invalidRowsOf(problem, sent)).toEqual({
            p1: 'parts.plan.invalid.other',
            p2: 'parts.plan.invalid.dependency_order',
        });
    });
});

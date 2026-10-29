import { describe, expect, it } from 'vitest';

import type { Plan, Workflow } from '@/services/apiService/genaix/types';

import { hasGatedActions, isAcceptedOnCard, isFirstDraft, planActions, planPresence, writtenPagesOf } from './planActions';

describe('planActions', () => {
    it('leaves the gated transitions out unless asked for them (the review page)', () => {
        expect(planActions('done', false)).toEqual(['discard']);
        expect(planActions('done', true)).toEqual(['discard', 'release']);
        expect(planActions('released', false)).toEqual(['discard']);
        expect(planActions('released', true)).toEqual(['discard', 'request_review', 'publish']);
        expect(planActions('running', false)).toEqual([]);
        expect(planActions('drafting', false)).toEqual(['discard']);
        expect(planActions('discarded', true)).toEqual([]);
    });

    it('offers no reopen any more (rc.5: a revision replaces it)', () => {
        expect(planActions('published', true)).toEqual([]);
        expect(planActions('review_requested', true)).toEqual([]);
    });

    it('says which statuses allow a gated transition', () => {
        expect(['accepted', 'running', 'done', 'released'].every((status) => hasGatedActions(status as never))).toBe(true);
        expect(['proposed', 'published', 'review_requested', 'discarded', 'superseded', 'drafting'].some((status) => hasGatedActions(status as never))).toBe(false);
    });
});

describe('isFirstDraft', () => {
    const plan = (status: Plan['status'], itemCount: number): Plan => ({
        id: 'plan-1',
        workflows: [{ id: 'content_create', version: '1.0.0' }],
        status,
        version: 1,
        summary: '',
        items: Array.from({ length: itemCount }, (_, index) => ({ id: `p${index}`, label: 'Step', kind: 'create', workflow: 'content_create', status: 'done' })),
    });

    it('is a drafting plan without items, not a revision being drafted', () => {
        expect(isFirstDraft(plan('drafting', 0))).toBe(true);
        expect(isFirstDraft(plan('drafting', 2))).toBe(false);
        expect(isFirstDraft(plan('proposed', 0))).toBe(false);
    });
});

describe('isAcceptedOnCard', () => {
    function workflow(id: string, planApproval: boolean): Workflow {
        return { id, version: '1.0.0', title: id, description: '', roles: [], optional: false, steps_template: [], plan_approval: planApproval };
    }

    const quickEdit: Plan = { id: 'pl-1', workflows: [{ id: 'content_quick_edit', version: '1.0.0' }], status: 'proposed', version: 2, summary: '', items: [] };
    const catalogue = [workflow('content_edit', true), workflow('content_quick_edit', false)];

    it('is true when a module of the plan waits for the acceptance on its card (plan_approval)', () => {
        expect(isAcceptedOnCard({ ...quickEdit, workflows: [{ id: 'content_quick_edit', version: '1.0.0' }, { id: 'content_edit', version: '1.0.0' }] }, catalogue)).toBe(true);
    });

    it('is false for a plan its run confirms in the chat, and while the modules are not known', () => {
        expect(isAcceptedOnCard(quickEdit, catalogue)).toBe(false);
        expect(isAcceptedOnCard(quickEdit, undefined)).toBe(false);
    });
});

describe('planPresence', () => {
    const catalogue: Workflow[] = [
        { id: 'content_edit', version: '1.0.0', title: 'content_edit', description: '', roles: [], optional: false, steps_template: [], plan_approval: true },
        { id: 'content_quick_edit', version: '1.0.0', title: 'content_quick_edit', description: '', roles: [], optional: false, steps_template: [], plan_approval: false },
    ];
    const quick = (status: Plan['status']): Plan => ({ id: 'pl-1', workflows: [{ id: 'content_quick_edit', version: '1.0.0' }], status, version: 1, summary: '', items: [] });
    const page = { cms_ref: { type: 'page' as const, id: 41, node_id: 1 }, status: 'pending' as const, approval_chain: [] };

    it('shows the card of a plan accepted on it, and while the modules are not known', () => {
        expect(planPresence({ ...quick('proposed'), workflows: [{ id: 'content_edit', version: '1.0.0' }] }, catalogue, undefined)).toBe('card');
        expect(planPresence(quick('proposed'), undefined, undefined)).toBe('card');
        expect(planPresence({ ...quick('proposed'), workflows: [{ id: 'unknown', version: '1.0.0' }] }, catalogue, undefined)).toBe('card');
    });

    it('shows only the head of a plan confirmed in the chat while it is under way, closed, or has pages', () => {
        expect(['drafting', 'proposed', 'accepted', 'running', 'discarded', 'superseded'].map((status) => planPresence(quick(status as Plan['status']), catalogue, undefined))).toEqual(['head', 'head', 'head', 'head', 'head', 'head']);
        expect(planPresence(quick('done'), catalogue, [page])).toBe('head');
        expect(planPresence(quick('published'), catalogue, [{ ...page, status: 'published' }])).toBe('head');
    });

    it('shows nothing of such a plan once it is done without a page to publish', () => {
        expect(planPresence(quick('done'), catalogue, undefined)).toBe('none');
        expect(planPresence(quick('done'), catalogue, [])).toBe('none');
    });
});

describe('writtenPagesOf', () => {
    it('keeps the pages the plan created, changed or published, and nothing else', () => {
        expect(writtenPagesOf([
            { type: 'page', id: 1, operation: 'created' },
            { type: 'page', id: 2, operation: 'updated' },
            { type: 'page', id: 3, operation: 'published' },
            { type: 'page', id: 4, operation: 'read' },
            { type: 'image', id: 5, operation: 'created' },
            { type: 'page', id: 6 },
        ]).map(({ id }) => id)).toEqual([1, 2, 3]);
    });
});

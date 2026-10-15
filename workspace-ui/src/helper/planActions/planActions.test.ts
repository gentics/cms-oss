import { describe, expect, it } from 'vitest';

import type { Plan } from '@/services/apiService/genaix/types';

import { hasGatedActions, isFirstDraft, isRevisable, planActions, revisionOf } from './planActions';

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

describe('revisionOf', () => {
    it('asks for a change once the work is done, and stops and changes an executing plan', () => {
        expect(['done', 'released', 'published', 'review_requested'].map((status) => revisionOf(status as never))).toEqual(['ask', 'ask', 'ask', 'ask']);
        expect(revisionOf('accepted')).toBe('stop');
        expect(revisionOf('running')).toBe('stop');
        expect(['drafting', 'proposed', 'discarded', 'superseded'].map((status) => revisionOf(status as never))).toEqual([undefined, undefined, undefined, undefined]);
    });

    it('knows the final statuses cannot be revised', () => {
        expect(isRevisable('published')).toBe(true);
        expect(isRevisable('discarded')).toBe(false);
        expect(isRevisable('superseded')).toBe(false);
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

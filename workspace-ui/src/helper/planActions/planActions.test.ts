import { describe, expect, it } from 'vitest';

import { hasGatedActions, planActions } from './planActions';

describe('planActions', () => {
    it('leaves the gated transitions out unless asked for them (the review page)', () => {
        expect(planActions('done', false)).toEqual(['discard']);
        expect(planActions('done', true)).toEqual(['discard', 'release']);
        expect(planActions('released', false)).toEqual(['discard', 'reopen']);
        expect(planActions('released', true)).toEqual(['discard', 'reopen', 'request_review', 'publish']);
        expect(planActions('running', false)).toEqual([]);
        expect(planActions('published', false)).toEqual(['reopen']);
        expect(planActions('discarded', true)).toEqual([]);
    });

    it('says which statuses allow a gated transition', () => {
        expect(['accepted', 'running', 'done', 'released'].every((status) => hasGatedActions(status as never))).toBe(true);
        expect(['proposed', 'published', 'review_requested', 'discarded', 'superseded', 'drafting'].some((status) => hasGatedActions(status as never))).toBe(false);
    });
});

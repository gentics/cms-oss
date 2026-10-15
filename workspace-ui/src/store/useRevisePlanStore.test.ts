import { beforeEach, describe, expect, it } from 'vitest';

import { useRevisePlanStore } from './useRevisePlanStore';

describe('useRevisePlanStore', () => {
    beforeEach(() => {
        useRevisePlanStore.setState({ planIds: {} });
    });

    it('keeps the plan to revise per session until the mode ends', () => {
        useRevisePlanStore.getState().start('s-1', 'plan-1');

        expect(useRevisePlanStore.getState().planIds).toEqual({ 's-1': 'plan-1' });

        useRevisePlanStore.getState().end('s-1');

        expect(useRevisePlanStore.getState().planIds).toEqual({});
    });

    it('keeps only the latest plan of a session, and those of other sessions', () => {
        useRevisePlanStore.getState().start('s-1', 'plan-1');
        useRevisePlanStore.getState().start('s-2', 'plan-2');
        useRevisePlanStore.getState().start('s-1', 'plan-3');

        expect(useRevisePlanStore.getState().planIds).toEqual({ 's-1': 'plan-3', 's-2': 'plan-2' });
    });
});

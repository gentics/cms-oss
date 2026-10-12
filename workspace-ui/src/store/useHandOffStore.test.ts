import { beforeEach, describe, expect, it } from 'vitest';

import { useHandOffStore } from './useHandOffStore';

const first = { type: 'page' as const, id: '8871', node_id: 3 };
const second = { type: 'page' as const, id: '8903', node_id: 3 };

describe('useHandOffStore', () => {
    beforeEach(() => {
        useHandOffStore.setState({ handOffs: {} });
    });

    it('keeps a hand-off per session until it ends', () => {
        useHandOffStore.getState().request('s-1', { references: [first], nodeId: 3 });

        expect(useHandOffStore.getState().handOffs).toEqual({ 's-1': { references: [first], nodeId: 3, isInField: false, replaces: [] } });

        useHandOffStore.getState().markInField('s-1');

        expect(useHandOffStore.getState().handOffs['s-1']).toMatchObject({ isInField: true });

        useHandOffStore.getState().end('s-1');

        expect(useHandOffStore.getState().handOffs).toEqual({});
    });

    it('replaces the objects already in the field with those of a later request', () => {
        useHandOffStore.getState().request('s-1', { references: [first] });
        useHandOffStore.getState().markInField('s-1');
        useHandOffStore.getState().request('s-1', { references: [second] });

        expect(useHandOffStore.getState().handOffs['s-1']).toEqual({ references: [second], isInField: false, replaces: [first] });
    });

    it('replaces nothing new for a request that came before the field took the last one', () => {
        useHandOffStore.getState().request('s-1', { references: [first] });
        useHandOffStore.getState().request('s-1', { references: [second] });

        expect(useHandOffStore.getState().handOffs['s-1']).toMatchObject({ references: [second], replaces: [] });
    });
});

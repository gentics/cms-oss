import { beforeEach, describe, expect, it } from 'vitest';

import { useHandOffStore } from './useHandOffStore';

const first = { type: 'page' as const, id: '8871', node_id: 3 };
const second = { type: 'page' as const, id: '8903', node_id: 3 };

describe('useHandOffStore', () => {
    beforeEach(() => {
        useHandOffStore.setState({ handOffs: {} });
    });

    it('keeps the objects per session until the composer has taken them', () => {
        useHandOffStore.getState().request('s-1', [first]);

        expect(useHandOffStore.getState().handOffs).toEqual({ 's-1': [first] });

        useHandOffStore.getState().end('s-1');

        expect(useHandOffStore.getState().handOffs).toEqual({});
    });

    it('keeps only the latest request of a session, and those of other sessions', () => {
        useHandOffStore.getState().request('s-1', [first]);
        useHandOffStore.getState().request('s-2', [first]);
        useHandOffStore.getState().request('s-1', [second]);

        expect(useHandOffStore.getState().handOffs).toEqual({ 's-1': [second], 's-2': [first] });
    });
});

import { beforeEach, describe, expect, it } from 'vitest';

import { useSessionArchiveStore } from './useSessionArchiveStore';

describe('useSessionArchiveStore', () => {
    beforeEach(() => {
        useSessionArchiveStore.setState({ hiddenIds: [] });
    });

    it('hides a session once, however often it is hidden', () => {
        const { hide } = useSessionArchiveStore.getState();

        hide('s-1');
        hide('s-1');
        hide('s-2');

        expect(useSessionArchiveStore.getState().hiddenIds).toEqual(['s-1', 's-2']);
    });

    it('restores only the given session', () => {
        const { hide, restore } = useSessionArchiveStore.getState();

        hide('s-1');
        hide('s-2');
        restore('s-1');

        expect(useSessionArchiveStore.getState().hiddenIds).toEqual(['s-2']);
    });
});

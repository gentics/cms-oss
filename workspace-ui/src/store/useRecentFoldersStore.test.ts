import { beforeEach, describe, expect, it } from 'vitest';

import { useRecentFoldersStore } from './useRecentFoldersStore';

function folder(id: number) {
    return { id, name: `Folder ${id}`, hasSubfolders: false, inherited: false };
}

describe('useRecentFoldersStore', () => {
    beforeEach(() => {
        useRecentFoldersStore.setState({ recent: [] });
    });

    it('keeps the last three folders, newest first, each once per node', () => {
        const { remember } = useRecentFoldersStore.getState();

        remember(3, folder(1));
        remember(3, folder(2));
        remember(4, folder(2));
        remember(3, folder(1));
        remember(3, folder(5));

        expect(useRecentFoldersStore.getState().recent.map(({ nodeId, folder: { id } }) => `${nodeId}:${id}`)).toEqual(['3:5', '3:1', '4:2']);
    });
});

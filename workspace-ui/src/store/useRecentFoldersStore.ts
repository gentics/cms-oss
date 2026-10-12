import { create } from 'zustand';

import type { CmsFolder } from '@/services/cmsApiService/cmsApiService';

/** A folder chosen in the folder search, and the node or channel it was chosen in. */
export interface RecentFolder {
    nodeId: number;
    folder: CmsFolder;
}

// How many folders the list keeps (draft `RECENT_FD`).
const MAX_RECENT_FOLDERS = 3;

interface RecentFoldersState {
    /** The folders chosen last, newest first. */
    recent: RecentFolder[];
    /** Puts `folder` of the node `nodeId` first; the oldest one drops out. */
    remember: (nodeId: number, folder: CmsFolder) => void;
}

/** The folders chosen last in the folder search, for this tab only. */
export const useRecentFoldersStore = create<RecentFoldersState>((set) => ({
    recent: [],

    remember: (nodeId, folder) =>
        set((state) => ({
            recent: [
                { nodeId, folder },
                ...state.recent.filter((entry) => entry.nodeId !== nodeId || entry.folder.id !== folder.id),
            ].slice(0, MAX_RECENT_FOLDERS),
        })),
}));

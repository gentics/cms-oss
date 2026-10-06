import { create } from 'zustand';

interface SessionArchiveState {
    /**
     * Sessions the user deleted in this tab: waiting for the undo toast to close, or archived.
     * Every session list hides them, so a row disappears at once and does not come back from a
     * cached page. A failed archive or an undo shows the row again.
     */
    hiddenIds: string[];
    hide: (sessionId: string) => void;
    restore: (sessionId: string) => void;
}

export const useSessionArchiveStore = create<SessionArchiveState>((set) => ({
    hiddenIds: [],

    hide: (sessionId) => set((state) => (
        state.hiddenIds.includes(sessionId) ? state : { hiddenIds: [...state.hiddenIds, sessionId] }
    )),

    restore: (sessionId) => set((state) => ({
        hiddenIds: state.hiddenIds.filter((id) => id !== sessionId),
    })),
}));

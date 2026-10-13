import { create } from 'zustand';

import type { ContextReference } from '@/services/apiService/genaix/types';

/**
 * "Edit these", by session id: the objects selected in a result card, until the session's chat
 * composer has put them into its field (`end`). The card and the composer are in different parts of
 * the page. The next message goes to the same session, whose session agent drafts a plan for them.
 */
interface HandOffState {
    handOffs: Record<string, ContextReference[] | undefined>;
    request: (sessionId: string, references: ContextReference[]) => void;
    end: (sessionId: string) => void;
}

export const useHandOffStore = create<HandOffState>((set) => ({
    handOffs: {},

    request: (sessionId, references) => set((state) => ({ handOffs: { ...state.handOffs, [sessionId]: references } })),

    end: (sessionId) => set((state) => {
        const { [sessionId]: _ended, ...handOffs } = state.handOffs;

        return { handOffs };
    }),
}));

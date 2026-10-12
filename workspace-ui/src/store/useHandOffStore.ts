import { create } from 'zustand';

import type { ContextReference } from '@/services/apiService/genaix/types';

/** Objects selected in a result card of a read-only session, to act on in a new session. */
export interface HandOffRequest {
    references: ContextReference[];
    /** The node the read-only session works in, the new session's too. */
    nodeId?: number;
}

/** A hand-off in a session's chat composer: the next message starts a new session. */
export interface HandOff extends HandOffRequest {
    /** Whether the composer has put `references` into its field yet. */
    isInField: boolean;
    /** The objects of an earlier hand-off that are still in the field, to take out for these. */
    replaces: ContextReference[];
}

/**
 * "Edit these", by session id: pressed in a result card, taken over into the session's chat composer
 * (`markInField`), ended by sending, by the composer's notice, or with no object left (`end`). The
 * card and the composer are in different parts of the page.
 */
interface HandOffState {
    handOffs: Record<string, HandOff | undefined>;
    request: (sessionId: string, request: HandOffRequest) => void;
    markInField: (sessionId: string) => void;
    end: (sessionId: string) => void;
}

export const useHandOffStore = create<HandOffState>((set) => ({
    handOffs: {},

    request: (sessionId, request) => set((state) => {
        const current = state.handOffs[sessionId];
        const replaces = current?.isInField ? current.references : current?.replaces ?? [];

        return { handOffs: { ...state.handOffs, [sessionId]: { ...request, isInField: false, replaces } } };
    }),

    markInField: (sessionId) => set((state) => {
        const current = state.handOffs[sessionId];

        return current ? { handOffs: { ...state.handOffs, [sessionId]: { ...current, isInField: true, replaces: [] } } } : state;
    }),

    end: (sessionId) => set((state) => {
        const { [sessionId]: _ended, ...handOffs } = state.handOffs;

        return { handOffs };
    }),
}));

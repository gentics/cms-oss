import { create } from 'zustand';

import type { InteractionAnswer } from '@/services/apiService/genaix/types';

/**
 * Why a pending interaction can no longer be answered from this client: an answer is on its way
 * (`sending`), GenAIx accepted one or says it already has one (`answered`), or its `expires_at` has
 * passed (`expired`). Holds until `interaction.resolved` removes the interaction.
 */
export type InteractionLock = 'sending' | 'answered' | 'expired';

/**
 * What the user has filled into a pending interaction's card so far, by interaction id: a complete
 * answer, or none while it is incomplete. The chat composer sends it as `reply_to_interaction` when the
 * user answers by writing a message instead of through the card. `locks` keep the card and the
 * composer from answering the same interaction twice.
 */
interface InteractionDraftState {
    drafts: Record<string, InteractionAnswer | undefined>;
    locks: Record<string, InteractionLock | undefined>;
    setDraft: (interactionId: string, answer: InteractionAnswer | undefined) => void;
    lock: (interactionId: string, lock: InteractionLock) => void;
    unlock: (interactionId: string) => void;
    /** Drops the draft and the lock, once the interaction is gone. */
    clearDraft: (interactionId: string) => void;
}

export const useInteractionDraftStore = create<InteractionDraftState>((set) => ({
    drafts: {},
    locks: {},

    setDraft: (interactionId, answer) => set((state) => ({ drafts: { ...state.drafts, [interactionId]: answer } })),

    lock: (interactionId, lock) => set((state) => ({ locks: { ...state.locks, [interactionId]: lock } })),

    unlock: (interactionId) => set((state) => {
        const { [interactionId]: _removed, ...locks } = state.locks;

        return { locks };
    }),

    clearDraft: (interactionId) => set((state) => {
        const { [interactionId]: _draft, ...drafts } = state.drafts;
        const { [interactionId]: _lock, ...locks } = state.locks;

        return { drafts, locks };
    }),
}));

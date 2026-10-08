import { create } from 'zustand';

import type { InteractionAnswer } from '@/services/apiService/genaix/types';

/**
 * What the user has filled into a pending interaction's card so far, by interaction id: a complete
 * answer, or none while it is incomplete. The chat composer sends it as `reply_to_interaction` when the
 * user answers by writing a message instead of through the card.
 */
interface InteractionDraftState {
    drafts: Record<string, InteractionAnswer | undefined>;
    setDraft: (interactionId: string, answer: InteractionAnswer | undefined) => void;
    clearDraft: (interactionId: string) => void;
}

export const useInteractionDraftStore = create<InteractionDraftState>((set) => ({
    drafts: {},

    setDraft: (interactionId, answer) => set((state) => ({ drafts: { ...state.drafts, [interactionId]: answer } })),

    clearDraft: (interactionId) => set((state) => {
        const { [interactionId]: _removed, ...drafts } = state.drafts;

        return { drafts };
    }),
}));

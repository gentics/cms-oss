import { create } from 'zustand';

/**
 * The plan the chat composer revises, by session id: set by "Ask for a change" or "Stop and change" on a
 * plan (`PlanHead`), until the next message went out with `options: { mode: 'revise', plan_id }` or the
 * user left the mode (`end`). The plan's head and the composer are in different parts of the page.
 */
interface RevisePlanState {
    planIds: Record<string, string | undefined>;
    start: (sessionId: string, planId: string) => void;
    end: (sessionId: string) => void;
}

export const useRevisePlanStore = create<RevisePlanState>((set) => ({
    planIds: {},

    start: (sessionId, planId) => set((state) => ({ planIds: { ...state.planIds, [sessionId]: planId } })),

    end: (sessionId) => set((state) => {
        const { [sessionId]: _ended, ...planIds } = state.planIds;

        return { planIds };
    }),
}));

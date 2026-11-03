import { create } from 'zustand';

/**
 * The text the user has written into a guideline and not saved yet, by the guideline's file
 * (`guidelineDraftKey`). Kept here rather than in the editor, so that it outlives the editor: a
 * change of session or of the plan shown takes the editor away, and coming back finds the text as
 * it was left. Gone on a reload, before which the editor asks while it shows a draft (`GuidelineColumn`).
 */
interface GuidelineDraftState {
    drafts: Record<string, string | undefined>;
    /** Sets the draft of `key`, or drops it with `undefined` (saved, or undone). */
    setDraft: (key: string, text: string | undefined) => void;
}

/** The key of a guideline's draft: its package and file, e.g. `corporate/guidelines/20_seo-basics.md`. */
export function guidelineDraftKey(packageName: string, path: string): string {
    return `${packageName}/${path}`;
}

export const useGuidelineDraftStore = create<GuidelineDraftState>((set) => ({
    drafts: {},

    setDraft: (key, text) => set((state) => {
        const { [key]: _removed, ...drafts } = state.drafts;

        return { drafts: text === undefined ? drafts : { ...drafts, [key]: text } };
    }),
}));

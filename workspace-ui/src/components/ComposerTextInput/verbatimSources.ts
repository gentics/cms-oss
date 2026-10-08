import { createContext } from 'react';

/** One choice of where a verbatim passage was quoted from; `value` is the passage's `source`. */
export interface VerbatimSourceOption {
    value: string;
    label: string;
}

/** What the passages in a composer's field can name as their source (`VerbatimPassage`). */
export interface VerbatimSources {
    /** The files attached in the composer. */
    attachments: VerbatimSourceOption[];
    /** The session whose uploaded files are offered too, loaded when a passage's menu opens. */
    sessionId?: string;
}

/**
 * The sources a passage in the field can name besides being typed by the user. The `Composer`
 * provides them; without it there are none.
 */
export const VerbatimSourcesContext = createContext<VerbatimSources>({ attachments: [] });

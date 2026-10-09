import { createContext } from 'react';

/** The read-only session whose results can be handed off to a new session. */
export interface HandOffSource {
    /** The node the read-only session works in (`context.node_id`), the new session's too. */
    nodeId?: number;
}

/**
 * Set while the chat shows a read-only session (`READ_ONLY_WORKFLOW`): its result cards then let the
 * user select objects and hand them off ("Edit these", `EditTheseButton`). `null` everywhere else.
 */
export const HandOffContext = createContext<HandOffSource | null>(null);

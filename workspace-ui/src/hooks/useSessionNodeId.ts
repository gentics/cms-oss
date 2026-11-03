import { useCmsFolderNodeId } from '@/hooks/useCmsQueries';
import { useSession } from '@/hooks/useGenaixQueries';

/**
 * The CMS node the session works in: its context's `node_id`, else the node of its target folder
 * (`folder_id`, `useCmsFolderNodeId`), which GenAIx derives where the user named no node, e.g. in a
 * create flow. Undefined while either loads, or for a session with neither. With `isEnabled` false
 * nothing is asked; only what the cache holds counts.
 */
export function useSessionNodeId(sessionId: string, isEnabled = true): number | undefined {
    const context = useSession(sessionId, isEnabled).data?.context;
    const folderNodeId = useCmsFolderNodeId(isEnabled && context?.node_id === undefined ? context?.folder_id : undefined).data;

    return context?.node_id ?? folderNodeId;
}

import { unknownGuidelines } from '@/helper/guidelines/guidelines';
import { useCmsGuidelines } from '@/hooks/useCmsQueries';
import { useSession } from '@/hooks/useGenaixQueries';

/**
 * Which of `slugs` (guideline ids, `helper/guidelines`) name no guideline of the session's node any
 * more, because the file was removed or renamed (`unknownGuidelines`). Asks nothing without slugs.
 * Nothing is missing while the session or the node's guidelines load, or for a session without a node.
 */
export function useMissingGuidelines(sessionId: string, slugs: string[]): string[] {
    const isAsked = slugs.length > 0;
    const nodeId = useSession(sessionId, isAsked).data?.context.node_id;
    const guidelines = useCmsGuidelines(isAsked ? nodeId : undefined).data;

    return isAsked && guidelines ? unknownGuidelines(slugs, guidelines) : [];
}

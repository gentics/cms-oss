import type { UserMessagePart } from '@/services/apiService/genaix/types';

/** The workflow a session starts with when no keyword of `guessWorkflow` matches. */
export const DEFAULT_WORKFLOW = 'content_research';

// Keywords per workflow id (`GET /workflows`), English and German, checked in this order: the more
// specific workflows first, so "create a construct" is `construct_create`, not `content_create`.
// Matched at word starts, so "list" also matches "listing" and "erstelle" matches "erstellen".
const RULES: [workflow: string, keywords: string[]][] = [
    ['construct_create', ['construct', 'baustein', 'component', 'komponente', 'template for', 'vorlage für']],
    ['admin_user', ['user', 'benutzer', 'nutzer', 'account', 'konto', 'group', 'gruppe']],
    ['content_create', ['create', 'build', 'new page', 'landing page', 'landingpage', 'write', 'draft', 'erstell', 'bau', 'anlegen', 'lege', 'neue seite', 'schreib', 'entwurf']],
    ['content_edit', ['edit', 'change', 'update', 'rewrite', 'fix', 'änder', 'aktualisier', 'bearbeit', 'überarbeit', 'korrigier']],
    ['content_research', ['search', 'find', 'list', 'which', 'where', 'show', 'how many', 'report', 'suche', 'durchsuch', 'liste', 'welche', 'wo ', 'zeig', 'wie viele']],
];

function escapeRegExp(text: string): string {
    return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * TEMPORARY: picks the workflow of a new session from keywords in its first message, until the user
 * can choose one. The GenAIx mock replays a scenario per workflow (`content_create` is S1), so this is
 * what makes them reachable from the dashboard. Falls back to `DEFAULT_WORKFLOW`.
 */
export function guessWorkflow(parts: UserMessagePart[]): string {
    const text = parts
        .map((part) => (part.type === 'text' || part.type === 'verbatim' ? part.text : ''))
        .join(' ')
        .toLowerCase();

    const match = RULES.find(([, keywords]) => keywords.some((keyword) => (
        new RegExp(`(^|[^\\p{L}])${escapeRegExp(keyword)}`, 'u').test(text)
    )));

    return match?.[0] ?? DEFAULT_WORKFLOW;
}

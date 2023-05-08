/**
 * REST Model type of check link status for external Links
 *
 * https://www.gentics.com/Content.Node/guides/restapi/json_ExternalLinkStatus.html
 */
export type ExternalLinkStatus = 'valid' | 'invalid' | 'unchecked';

/**
 * Scope of link replacement
 *
 * https://www.gentics.com/Content.Node/guides/restapi/json_ReplaceScope.html
 */
export enum ReplaceScope {
    /** Replace the link only */
    Link = 'link',

    /** Replace all occurrences in the page */
    Page = 'page',

    /** Replace all occurrences in the node */
    Node = 'node',

    /** Replace all occurrences in the system */
    Global = 'global',
}

/**
 * Item in the history of validity checks for an external link
 *
 * https://www.gentics.com/Content.Node/guides/restapi/json_ExternalLinkCheckHistoryEntry.html
 */
export interface ExternalLinkCheckHistoryEntry {
    /** Timestamp of the check */
    timestamp: number;

    /** Check status */
    status: ExternalLinkStatus;

    /** Failure reason */
    reason: string;
}

/**
 * Represents an external link in the CMS.
 *
 * https://www.gentics.com/Content.Node/guides/restapi/json_ExternalLink.html
 */
export interface ExternalLink {
    /** ID */
    id: number;

    /** ID of the content containing the external link */
    contentId: number;

    /** ID of the contenttag containing the external link */
    contenttagId: number;

    /** Name of the contenttag */
    contenttagName: string;

    /** ID of the value containing the external link */
    valueId: number;

    /** Name of the part */
    partName: string;

    /** URL of the external link */
    url: string;

    /** Link text */
    text: string;

    /** Timestamp of the last validity check (0 if never checked) */
    lastCheckTimestamp: number;

    /** Last check status */
    lastStatus: ExternalLinkStatus;

    /** Last failure reason */
    lastReason: string;

    /** History of last 5 validity checks */
    history: ExternalLinkCheckHistoryEntry[];
}

/**
 * Request to replace an external Link with another URL
 * POST body for `linkChecker/pages/{pageId}/links/{id}`
 *
 * https://www.gentics.com/Content.Node/guides/restapi/json_ReplaceExternalLinkRequest.html
 */
export interface ReplaceExternalLinkRequest {
    url: string;
    scope: ReplaceScope;
}

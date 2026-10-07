import { httpRequest } from '@/services/httpService/httpService';

/**
 * `responseInfo.responseCode` of a CMS REST response, which CMS REST responses carry alongside the
 * HTTP status (`.claude/contracts/genaix-docs/04-mcp-scope.md`, §2.5).
 */
export type CmsResponseCode =
    | 'AUTHREQUIRED'
    | 'PERMISSION'
    | 'NOTFOUND'
    | 'INVALIDDATA'
    | 'LOCKED'
    | 'MAINTENANCEMODE'
    | 'NOTLICENSED'
    | 'FAILURE';

/** A CMS API token of the current CMS user, as returned by `POST /rest/admin/token`: the token plus its metadata. */
export interface CmsTokenInfo {
    token: string;
    id: number;
    userId: number;
    name: string;
    /** Creation time, Unix timestamp in seconds. */
    cdate: number;
    /** Unix timestamp in seconds; `0` means never. */
    expires: number;
    /** Unix timestamp in seconds; `0` means never used. */
    lastUsed: number;
    valid: boolean;
}

// How long a CMS API token is valid: 24 hours, the guide's default for a session token
// (09-integration-guide.md, "Creating the CMS API token for a session").
const CMS_TOKEN_LIFETIME_S = 24 * 60 * 60;

/**
 * Requests a CMS API token for the current CMS user, authenticated by the browser's own CMS session
 * cookie: `POST /rest/admin/token` with `{ name, expires, pruneOnExpiry: true }`, valid for 24 hours;
 * with `pruneOnExpiry` the CMS deletes the token itself once it has expired (09-integration-guide.md,
 * "Creating the CMS API token for a session"). Returns the token and its metadata, `expires` being
 * the value sent (a Unix timestamp in seconds), so a caller can register the token until exactly then.
 */
export async function createCmsToken(name: string): Promise<CmsTokenInfo> {
    // Relative, so it is same-origin on whichever CMS host serves the UI. In development the Vite dev
    // server forwards it (see `vite.config.ts`).
    const CMS_TOKEN_URL = '/rest/admin/token';
    // The CMS takes `expires` as a Unix timestamp in seconds, the clock gives milliseconds.
    const expires = Math.floor(Date.now() / 1000) + CMS_TOKEN_LIFETIME_S;
    const created = await httpRequest<CmsTokenInfo>(CMS_TOKEN_URL, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, expires, pruneOnExpiry: true }),
    });

    return { ...created, expires };
}

/** A CMS node (`GET /rest/node`), as far as the composer needs it. `folderId` is its root folder. */
export interface CmsNode {
    id: number;
    name: string;
    folderId: number;
}

/** The CMS object types the composer's @-menu searches. */
export type CmsItemType = 'page' | 'folder' | 'image';

/** A page, folder or image found by `searchCmsItems`, as far as the composer needs it. */
export interface CmsItem {
    id: number;
    name: string;
    type: CmsItemType;
    /** The folder path, e.g. `/Campaigns/`; not every type carries it. */
    path?: string;
}

// The same-origin request the CMS REST API answers with the browser's own CMS session cookie.
const CMS_GET: RequestInit = { credentials: 'same-origin', headers: { Accept: 'application/json' } };

/** The nodes the current CMS user can see: `GET /rest/node`. */
export async function listCmsNodes(): Promise<CmsNode[]> {
    const { items } = await httpRequest<{ items?: CmsNode[] }>('/rest/node', CMS_GET);

    return items ?? [];
}

/** How many objects one node returns for one search. */
export const CMS_SEARCH_MAX_ITEMS = 20;

/**
 * Pages, folders and images in `node` whose id, name or description matches `query`: `GET
 * /rest/folder/getItems/{root folder}` with `search`, recursive, at most `CMS_SEARCH_MAX_ITEMS`.
 */
export async function searchCmsItems(node: CmsNode, query: string, signal?: AbortSignal): Promise<CmsItem[]> {
    const params = new URLSearchParams({ nodeId: String(node.id), search: query, recursive: 'true', maxItems: String(CMS_SEARCH_MAX_ITEMS) });

    for (const type of ['page', 'folder', 'image'] satisfies CmsItemType[]) {
        params.append('type', type);
    }

    const { items } = await httpRequest<{ items?: CmsItem[] }>(`/rest/folder/getItems/${node.folderId}?${params}`, { ...CMS_GET, signal });

    return items ?? [];
}

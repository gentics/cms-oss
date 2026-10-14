import { HttpError, httpRequest, toHttpError } from '@/services/httpService/httpService';

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

// The same-origin request the CMS REST API answers with the browser's own CMS session cookie.
const CMS_GET: RequestInit = { credentials: 'same-origin', headers: { Accept: 'application/json' } };

/** The logged-in CMS user (`GET /rest/user/me`), as far as the workspace needs it. */
export interface CmsUser {
    id: number;
    login: string;
    firstName: string;
    lastName: string;
}

/**
 * A CMS login that did not succeed: `POST /rest/auth/login` answered with a `responseInfo.responseCode`
 * other than `OK` (`NOTFOUND` for wrong credentials, `MAINTENANCEMODE`, `FAILURE`), or
 * `GET /rest/auth/ssologin` answered with such a code instead of a session id.
 */
export class CmsLoginError extends Error {
    readonly responseCode: string;

    constructor(responseCode: string) {
        super(`CMS login failed: ${responseCode}`);
        this.name = 'CmsLoginError';
        this.responseCode = responseCode;
    }
}

/**
 * The CMS user of the browser's CMS session cookie: `GET /rest/user/me`. `null` while there is no
 * valid session: the CMS answers `401` for a missing or expired cookie.
 */
export async function getCmsUser(): Promise<CmsUser | null> {
    try {
        const { user } = await httpRequest<{ user?: CmsUser }>('/rest/user/me', CMS_GET);

        return user ?? null;
    } catch (error) {
        if (error instanceof HttpError && error.status === 401) {
            return null;
        }

        throw error;
    }
}

/**
 * Logs in with user name and password, as the editor and admin UI do: `POST /rest/auth/login`. The
 * CMS sets the session cookie itself. It answers `200` for a refused login too, with the reason in
 * `responseInfo.responseCode`, which is thrown as a `CmsLoginError`.
 */
export async function loginToCms(login: string, password: string): Promise<CmsUser> {
    const response = await httpRequest<{ responseInfo?: { responseCode?: string }; user?: CmsUser }>('/rest/auth/login', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
        body: JSON.stringify({ login, password }),
    });
    const responseCode = response.responseInfo?.responseCode ?? 'FAILURE';

    if (responseCode !== 'OK' || !response.user) {
        throw new CmsLoginError(responseCode);
    }

    return response.user;
}

/** Ends the CMS session: `POST /rest/auth/logout`, which also deletes the session cookie. */
export async function logoutFromCms(): Promise<void> {
    await httpRequest<unknown>('/rest/auth/logout', { method: 'POST', credentials: 'same-origin', headers: { Accept: 'application/json' } });
}

/** The Keycloak settings the CMS offers its UIs (`GET /rest/keycloak`, the `cms-keycloak` module). */
export interface CmsKeycloakConfig {
    'auth-server-url': string;
    realm: string;
    resource: string;
    /** `keycloak.show_sso_button`: offer SSO as a button next to the login form instead of redirecting at once. */
    showSSOButton?: boolean;
}

/** The CMS's Keycloak settings, or `null` when it has none: `404` while the feature is off or not installed. */
export async function getCmsKeycloakConfig(): Promise<CmsKeycloakConfig | null> {
    try {
        return await httpRequest<CmsKeycloakConfig>('/rest/keycloak', CMS_GET);
    } catch (error) {
        if (error instanceof HttpError && error.status === 404) {
            return null;
        }

        throw error;
    }
}

/**
 * Turns a Keycloak access token into a CMS session, as the editor and admin UI do:
 * `GET /rest/auth/ssologin` with `Authorization: Bearer`. The CMS sets the session cookie and answers
 * the session id as plain text; anything else is the response code of a refused login, thrown as a
 * `CmsLoginError`.
 */
export async function ssoLoginToCms(accessToken: string): Promise<void> {
    const url = '/rest/auth/ssologin';
    const response = await fetch(url, {
        credentials: 'same-origin',
        headers: { Accept: 'text/plain', Authorization: `Bearer ${accessToken}` },
    });

    if (!response.ok) {
        throw await toHttpError(url, response);
    }

    const result = (await response.text()).trim();

    if (!/^\d+$/.test(result)) {
        throw new CmsLoginError(result || 'FAILURE');
    }
}

/**
 * A CMS node (`GET /rest/node`), as far as the composer needs it. `folderId` is its root folder. A
 * channel (`type: 'channel'`) names the node or channel it inherits from in `inheritedFromName`.
 */
export interface CmsNode {
    id: number;
    name: string;
    folderId: number;
    type?: 'node' | 'channel';
    inheritedFromName?: string;
}

/** The CMS object types the composer's @-menu searches. */
export const CMS_ITEM_TYPES = ['page', 'folder', 'image'] as const;

export type CmsItemType = typeof CMS_ITEM_TYPES[number];

/** A page, folder or image listed by `searchCmsItems`, as far as the composer needs it. */
export interface CmsItem {
    id: number;
    name: string;
    type: CmsItemType;
    /** The folder path, e.g. `/Campaigns/`; not every type carries it. */
    path?: string;
}

/** The nodes the current CMS user can see: `GET /rest/node`. */
export async function listCmsNodes(): Promise<CmsNode[]> {
    const { items } = await httpRequest<{ items?: CmsNode[] }>('/rest/node', CMS_GET);

    return items ?? [];
}

/**
 * Whether the CMS feature `name` is on: `GET /rest/admin/features/{name}`, as the editor UI asks for
 * `elasticsearch`. The CMS answers every user, and `activated: false` for a feature it does not know.
 */
export async function isCmsFeatureActive(name: string): Promise<boolean> {
    const { activated } = await httpRequest<{ activated?: boolean }>(`/rest/admin/features/${encodeURIComponent(name)}`, CMS_GET);

    return activated === true;
}

/** How many objects one node returns for one search. */
export const CMS_SEARCH_MAX_ITEMS = 20;

// The fields `*query*` is looked for in, per type, as in the editor UI's search (`allFilterAdd` in
// `cms-ui/apps/editor-ui/.../query-assembler-elasticsearch.service.ts`).
const ELASTIC_WILDCARD_FIELDS: Record<CmsItemType, readonly string[]> = {
    page: ['niceUrl', 'filename', 'name.raw'],
    folder: ['name.raw'],
    image: ['filename', 'name.raw'],
};

// The Elasticsearch query of the editor UI's search: `query` in name (weighted twice), path,
// description and content, or as part of the names and file names. Unlike there, the words also
// match with a typo (`fuzziness`), and a number also finds the object of that id, as `getItems` does.
function elasticQuery(type: CmsItemType, query: string) {
    const part = `*${query.toLowerCase().replace(/[\\*?]/g, '\\$&')}*`;
    const should: unknown[] = [
        { multi_match: { fields: ['name^2', 'path', 'description', 'content'], query, fuzziness: 'AUTO' } },
        ...ELASTIC_WILDCARD_FIELDS[type].map((field) => ({ wildcard: { [field]: { value: part, boost: field === 'niceUrl' ? 1 : 2 } } })),
    ];

    if (/^\d+$/.test(query)) {
        should.push({ term: { id: Number(query) } });
    }

    return { query: { bool: { must: [{ bool: { should } }] } }, from: 0, size: CMS_SEARCH_MAX_ITEMS, _source: false };
}

/**
 * The objects of `type` below the root folder of `node` that Elasticsearch finds for `query`, best
 * first: `POST /rest/elastic/{type}/_search` with `nodeId`, `folderId` and `recursive`, as the editor
 * UI searches (feature `elasticsearch`). The CMS answers the objects in `hits.hits[]._object`.
 */
export async function elasticSearchCmsItems(node: CmsNode, type: CmsItemType, query: string, signal?: AbortSignal): Promise<CmsItem[]> {
    const params = new URLSearchParams({ nodeId: String(node.id), folderId: String(node.folderId), recursive: 'true' });
    const { hits } = await httpRequest<{ hits?: { hits?: { _object?: CmsItem }[] } }>(`/rest/elastic/${type}/_search?${params}`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
        body: JSON.stringify(elasticQuery(type, query)),
        signal,
    });

    return (hits?.hits ?? []).flatMap(({ _object: item }) => (item ? [item] : []));
}

/**
 * The objects of `type` in `node`: for an empty `query` those directly in its root folder, otherwise
 * those below it that match `query` (recursive). At most `CMS_SEARCH_MAX_ITEMS`. With `elastic`, a
 * query goes to Elasticsearch (`elasticSearchCmsItems`), and only if that fails to the search below,
 * which matches id, name or description: folders from `GET /rest/folder/getFolders/{root folder}`,
 * pages and images from `GET /rest/folder/getItems/{root folder}`, which leaves out folders. With
 * `recursive`, an empty `query` lists the objects below the root folder too, not only those in it.
 */
export async function searchCmsItems(
    node: CmsNode,
    type: CmsItemType,
    query: string,
    { elastic = false, recursive = false, signal }: { elastic?: boolean; recursive?: boolean; signal?: AbortSignal } = {},
): Promise<CmsItem[]> {
    if (elastic && query !== '') {
        try {
            return await elasticSearchCmsItems(node, type, query, signal);
        } catch (error) {
            if (signal?.aborted) {
                throw error;
            }

            console.warn('Elasticsearch search failed, searching without it.', error);
        }
    }

    const params = new URLSearchParams({ nodeId: String(node.id), maxItems: String(CMS_SEARCH_MAX_ITEMS) });

    if (query !== '') {
        params.set('search', query);
    }

    if (query !== '' || recursive) {
        params.set('recursive', 'true');
    }

    if (type === 'folder') {
        const { folders } = await httpRequest<{ folders?: CmsItem[] }>(`/rest/folder/getFolders/${node.folderId}?${params}`, { ...CMS_GET, signal });

        return folders ?? [];
    }

    params.set('type', type);

    const { items } = await httpRequest<{ items?: CmsItem[] }>(`/rest/folder/getItems/${node.folderId}?${params}`, { ...CMS_GET, signal });

    return items ?? [];
}

/**
 * A folder as `GET /rest/folder/getFolders` lists it, as far as the folder search needs it. `path`
 * runs from the node's root folder to the folder itself, e.g. `/Website/Products/Cloud/`; `inherited`
 * is `false` for a folder of the node itself, in a channel for one local to the channel.
 */
export interface CmsFolder {
    id: number;
    name: string;
    path?: string;
    hasSubfolders: boolean;
    inherited: boolean;
}

/** A page of folders and how many there are in all. */
export interface CmsFolderPage {
    folders: CmsFolder[];
    /** All folders listed or found, not only those on this page. */
    total: number;
    hasMore: boolean;
}

/**
 * The folders in the folder `folderId` of the node `nodeId`, sorted by name: `GET /rest/folder/getFolders/{folderId}`
 * with `nodeId` (multichannelling), `skipCount` and `maxItems`. With `search`, those below it whose id,
 * name or description contains `search` (`recursive`). The CMS leaves out folders the user may not
 * view, and answers how many there are in all (`numItems`) and whether more follow (`hasMoreItems`).
 */
export async function listCmsFolders(
    nodeId: number,
    folderId: number,
    { search = '', skipCount = 0, maxItems, signal }: { search?: string; skipCount?: number; maxItems: number; signal?: AbortSignal },
): Promise<CmsFolderPage> {
    const params = new URLSearchParams({ nodeId: String(nodeId), skipCount: String(skipCount), maxItems: String(maxItems) });

    if (search !== '') {
        params.set('search', search);
        params.set('recursive', 'true');
    }

    const response = await httpRequest<{ folders?: CmsFolder[]; numItems?: number; hasMoreItems?: boolean }>(
        `/rest/folder/getFolders/${folderId}?${params}`,
        { ...CMS_GET, signal },
    );
    const folders = response.folders ?? [];

    return { folders, total: response.numItems ?? folders.length, hasMore: response.hasMoreItems === true };
}

/**
 * A page's properties as `GET /rest/page/load/{id}` returns them (`rest/model/Page.java`), as far as the
 * page settings need them. `cdate` and `edate` are unix seconds.
 */
export interface CmsPage {
    id: number;
    name: string;
    fileName?: string;
    description?: string;
    niceUrl?: string;
    priority?: number;
    templateId?: number;
    folderId: number;
    language?: string;
    cdate: number;
    edate: number;
}

/** The page `id`, in `nodeId` if given: `GET /rest/page/load/{id}`, read only (no `update`, so it is not locked). */
export async function loadCmsPage(id: number, nodeId?: number, signal?: AbortSignal): Promise<CmsPage> {
    const params = nodeId === undefined ? '' : `?${new URLSearchParams({ nodeId: String(nodeId) })}`;
    const { page } = await httpRequest<{ page: CmsPage }>(`/rest/page/load/${id}${params}`, { ...CMS_GET, signal });

    return page;
}

/**
 * Whether the current CMS user may publish the page `id`: `GET /rest/perm/publish/page/{id}`, in
 * `nodeId` if given. The CMS checks `publishpages` on the page's folder and the role permission for its
 * language (`PermHandler.canPublish`), the permission that decides between publishing a page and
 * sending it to review.
 */
export async function canPublishCmsPage(id: number, nodeId?: number, signal?: AbortSignal): Promise<boolean> {
    const params = nodeId === undefined ? '' : `?${new URLSearchParams({ nodeId: String(nodeId) })}`;
    const { granted } = await httpRequest<{ granted?: boolean }>(`/rest/perm/publish/page/${id}${params}`, { ...CMS_GET, signal });

    return granted === true;
}

/** A template (`rest/model/Template.java`), as far as the page settings need it. */
export interface CmsTemplate {
    id: number;
    name: string;
}

/** The templates linked to the folder `folderId`, in `nodeId` if given: `GET /rest/folder/getTemplates/{folderId}`. */
export async function listCmsTemplates(folderId: number, nodeId?: number, signal?: AbortSignal): Promise<CmsTemplate[]> {
    const params = nodeId === undefined ? '' : `?${new URLSearchParams({ folderNodeId: String(nodeId) })}`;
    const { templates } = await httpRequest<{ templates?: CmsTemplate[] }>(`/rest/folder/getTemplates/${folderId}${params}`, { ...CMS_GET, signal });

    return templates ?? [];
}

/** A language of a node (`rest/model/ContentLanguage.java`): `code` is what a `language` setting carries. */
export interface CmsLanguage {
    id: number;
    code: string;
    name: string;
}

/** The languages of the node `nodeId`, in the node's order: `GET /rest/node/{id}/languages`. */
export async function listCmsLanguages(nodeId: number, signal?: AbortSignal): Promise<CmsLanguage[]> {
    const { items } = await httpRequest<{ items?: CmsLanguage[] }>(`/rest/node/${nodeId}/languages`, { ...CMS_GET, signal });

    return items ?? [];
}

/** Where the CMS serves the image `imageId` of `nodeId` itself: `GET /rest/file/content/load/{id}`, with the user's CMS session. */
export function cmsImageUrl(nodeId: number, imageId: number): string {
    return `/rest/file/content/load/${imageId}?${new URLSearchParams({ nodeId: String(nodeId) })}`;
}

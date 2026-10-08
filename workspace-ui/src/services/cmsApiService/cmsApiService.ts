import { httpRequest } from '@/services/httpService/httpService';
import { type CmsToken, useCmsTokenStore } from '@/store/useCmsTokenStore';

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

export interface CmsTokenOptions {
    /** How long the token is valid, in seconds. Default: 60 minutes. */
    lifetimeSeconds?: number;
    /** Lets the CMS delete the token itself once it has expired (`02-auth-and-context-flow.md`). */
    pruneOnExpiry?: boolean;
}

/**
 * Requests a CMS API token for the current CMS user, authenticated by the browser's own CMS session
 * cookie: `POST /rest/admin/token` with `{ name, expires }`, valid for 60 minutes unless
 * `lifetimeSeconds` says otherwise, and `pruneOnExpiry` when given. Returns the token and its metadata.
 */
export async function createCmsToken(name: string, { lifetimeSeconds = 60 * 60, pruneOnExpiry }: CmsTokenOptions = {}): Promise<CmsTokenInfo> {
    // Relative, so it is same-origin on whichever CMS host serves the UI. In development the Vite dev
    // server forwards it (see `vite.config.ts`).
    const CMS_TOKEN_URL = '/rest/admin/token';
    // The CMS takes `expires` as a Unix timestamp in seconds (integration guide,
    // `POST /rest/admin/token`), the clock gives milliseconds.
    const expires = Math.floor(new Date().getTime() / 1000) + lifetimeSeconds;

    return httpRequest<CmsTokenInfo>(CMS_TOKEN_URL, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, expires, pruneOnExpiry }),
    });
}

/**
 * Lifetime of the CMS token of one GenAIx session: 24 hours, as in the integration guide ("The whole
 * sequence with curl") and the GenAIx mock's default for a session authorization.
 */
export const SESSION_CMS_TOKEN_LIFETIME_SECONDS = 24 * 60 * 60;

/** A CMS API token for one GenAIx session: the secret, its name on the CMS and when it expires. */
export interface SessionCmsToken {
    token: string;
    name: string;
    /** ISO 8601, for `expires_at` of the session authorization. */
    expiresAt: string;
}

/**
 * Creates the CMS API token a GenAIx session acts on the CMS with (`02-auth-and-context-flow.md`,
 * "Session-scoped authorization"): `POST /rest/admin/token`, valid for
 * `SESSION_CMS_TOKEN_LIFETIME_SECONDS`, pruned by the CMS once it has expired.
 *
 * In development (`import.meta.env.DEV`) no request goes to the CMS: the token is a fake. The GenAIx
 * mock contacts no CMS and accepts any token without one of its test prefixes (its README, "MCP
 * connections"); a real GenAIx refuses it with `422` `mcp_authorization_rejected`.
 */
export async function createSessionCmsToken(name: string): Promise<SessionCmsToken> {
    if (import.meta.env.DEV) {
        const expires = Math.floor(Date.now() / 1000) + SESSION_CMS_TOKEN_LIFETIME_SECONDS;

        return { token: `cmstok_dev_${crypto.randomUUID()}`, name, expiresAt: new Date(expires * 1000).toISOString() };
    }

    const created = await createCmsToken(name, { lifetimeSeconds: SESSION_CMS_TOKEN_LIFETIME_SECONDS, pruneOnExpiry: true });

    return { token: created.token, name: created.name, expiresAt: new Date(created.expires * 1000).toISOString() };
}

/** Valid while `expires` is `0` (never) or in the future. */
function isUnexpired(expires: number, nowSeconds: number): boolean {
    return expires === 0 || expires > nowSeconds;
}

// Shared by concurrent callers while a token is being requested, so they send one POST, not several.
let pendingCmsToken: Promise<CmsToken> | null = null;

/**
 * The CMS API token for later API calls: the one in `useCmsTokenStore` while it has not expired,
 * otherwise a new one from `createCmsToken` (`POST /rest/admin/token`), which is then stored.
 */
export function getCmsToken(now: number = Date.now()): Promise<CmsToken> {
    const stored = useCmsTokenStore.getState().cmsToken;

    if (stored && isUnexpired(stored.expires, now / 1000)) {
        return Promise.resolve(stored);
    }

    pendingCmsToken ??= createCmsToken(`genaix-workspace-${crypto.randomUUID()}`).then((created) => {
        const cmsToken = { token: created.token, id: created.id, name: created.name, expires: created.expires };

        useCmsTokenStore.getState().setCmsToken(cmsToken);

        return cmsToken;
    }).finally(() => {
        pendingCmsToken = null;
    });

    return pendingCmsToken;
}

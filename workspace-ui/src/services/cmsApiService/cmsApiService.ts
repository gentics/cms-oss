import { httpRequest } from '@/services/httpService/httpService';
import { type CmsToken, useCmsTokenStore } from '@/store/useCmsTokenStore';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

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

/**
 * Requests a CMS API token for the current CMS user, authenticated by the browser's own CMS session
 * cookie: `POST /rest/admin/token` with `{ name }`. Returns the token and its metadata.
 */
export async function createCmsToken(name: string): Promise<CmsTokenInfo> {
    // Relative, so it is same-origin on whichever CMS host serves the UI. In development the Vite dev
    // server forwards it (see `vite.config.ts`).
    const CMS_TOKEN_URL = '/rest/admin/token';

    return httpRequest<CmsTokenInfo>(CMS_TOKEN_URL, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
        body: JSON.stringify({ name }),
    });
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
    }, (error: unknown) => {
        // Shown once here for every caller. The error carries no response body.
        useErrorNotificationStore.getState().addError({
            messageKey: 'errorNotifications.cmsTokenFailed',
            detail: error instanceof Error ? error.message : undefined,
        });
        throw error;
    }).finally(() => {
        pendingCmsToken = null;
    });

    return pendingCmsToken;
}

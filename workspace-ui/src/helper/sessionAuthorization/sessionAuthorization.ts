import { getSession, listMcpConnections, NoCmsConnectionError, putSessionAuthorization } from '@/services/apiService/apiService';
import type { SessionAuthorizationCreate, SessionAuthorizationRequest } from '@/services/apiService/genaix/types';
import { createCmsToken } from '@/services/cmsApiService/cmsApiService';

// A session's CMS credential is renewed once it expires within this margin, so a run that starts
// just before the expiry does not lose it midway. The contract names no value; this one is ours.
const RENEW_MARGIN_MS = 10 * 60 * 1000;

/**
 * The id of the user's default cms connection. `GET /mcp/connections` creates it on the first call
 * when the installation names a default URL (contract, `listMcpConnections`). Without one it throws
 * `NoCmsConnectionError`.
 */
async function cmsConnectionId(): Promise<string> {
    const connections = await listMcpConnections('cms');
    const connection = connections.find((item) => item.default) ?? connections[0];

    if (!connection) {
        throw new NoCmsConnectionError();
    }

    return connection.id;
}

/**
 * A new CMS API token for one session (`createCmsToken`: 24 hours, pruned by the CMS once it has
 * expired), as the `bearer` credential GenAIx registers. Never one reused from another session.
 */
async function newCmsCredential(name: string): Promise<SessionAuthorizationRequest> {
    const { token, name: tokenName, expires } = await createCmsToken(name);

    return { auth_type: 'bearer', token, token_name: tokenName, expires_at: new Date(expires * 1000).toISOString() };
}

/**
 * The CMS credential of a session that is about to be created, for `POST /sessions` `authorizations`
 * (integration guide, "The same thing in the browser"): a new CMS token for the default cms
 * connection. The connection first, so no token is created without one. The session has no id yet,
 * so the token is named with a correlation id.
 */
export async function newSessionCmsAuthorization(): Promise<SessionAuthorizationCreate> {
    const connectionId = await cmsConnectionId();

    return { connection_id: connectionId, ...await newCmsCredential(`genaix-pending-${crypto.randomUUID()}`) };
}

/**
 * Makes sure an existing session can act on the CMS before a turn is posted. Unless the session
 * holds an `authorized` credential for the default cms connection that is valid beyond
 * `RENEW_MARGIN_MS`, a new CMS token is created and registered with
 * `PUT /sessions/{session_id}/authorizations/{connection_id}`. That covers a session created without
 * one and a credential that expired or was refused (`02-auth-and-context-flow.md`, "Lifetime").
 */
export async function ensureSessionCmsAuthorization(sessionId: string, now: number = Date.now()): Promise<void> {
    const [connectionId, session] = await Promise.all([cmsConnectionId(), getSession(sessionId)]);
    const current = session.authorizations?.find((entry) => entry.connection_id === connectionId);
    const validLongEnough = !current?.expires_at || Date.parse(current.expires_at) - now > RENEW_MARGIN_MS;

    if (current?.status === 'authorized' && validLongEnough) {
        return;
    }

    // The same name only once the CMS has pruned the expired token, a new suffix otherwise
    // (`02-auth-and-context-flow.md`, "Lifetime"). Whether it has is not known here, so always a new one.
    await putSessionAuthorization(sessionId, connectionId, await newCmsCredential(`genaix-${sessionId}-${crypto.randomUUID().slice(0, 8)}`));
}

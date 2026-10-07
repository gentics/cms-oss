import { expect } from 'vitest';

import type { McpConnection } from '@/services/apiService/genaix/types';
import type { CmsTokenInfo } from '@/services/cmsApiService/cmsApiService';

const CONNECTION: McpConnection = {
    id: '0d6e2a9c-4f13-4b7a-8c51-9f1b6e4a3c2d',
    connector: 'cms',
    url: 'https://cms.example.com/mcp',
    label: 'cms.example.com',
    default: true,
    created_at: '2026-10-07T08:00:00Z',
    status: { reachable: true, checked_at: '2026-10-07T08:00:00Z' },
    authorization_status: 'unauthorized',
};

const TOKEN: CmsTokenInfo = { token: 'cmstok_session', id: 9, userId: 3, name: 'genaix-workspace-test', cdate: 1_790_800_000, expires: 1_790_886_400, lastUsed: 0, valid: true };

/** The `authorizations` a session start sends with the connection and token of `withSessionAuthorization`. */
export const sessionAuthorizations = [{
    connection_id: CONNECTION.id,
    auth_type: 'bearer',
    token: TOKEN.token,
    token_name: TOKEN.name,
    expires_at: expect.any(String),
}];

/**
 * Wraps `fetchMock` for a session start: `GET /mcp/connections` gets one default `cms` connection and
 * `POST /rest/admin/token` a new token; every other call goes to `fetchMock`, which so sees only the
 * calls a test is about.
 */
export function withSessionAuthorization(fetchMock: typeof fetch): typeof fetch {
    return async (input, init) => {
        const url = new URL(String(input), 'http://localhost');

        if ((init?.method ?? 'GET') === 'GET' && url.pathname === '/genaix/api/v1/mcp/connections') {
            return Response.json({ items: [CONNECTION] });
        }

        if (init?.method === 'POST' && url.pathname === '/rest/admin/token') {
            return Response.json(TOKEN);
        }

        return fetchMock(input, init);
    };
}

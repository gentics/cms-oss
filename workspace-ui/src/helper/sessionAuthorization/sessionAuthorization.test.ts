import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { NoCmsConnectionError } from '@/services/apiService/apiService';

import { ensureSessionCmsAuthorization, newSessionCmsAuthorization } from './sessionAuthorization';

const NOW = Date.parse('2026-10-07T08:00:00Z');

const connections = [
    { id: 'c-other', connector: 'cms', default: false },
    { id: 'c-default', connector: 'cms', default: true },
];

const TOKEN_ROUTE = 'POST /rest/admin/token';

// The CMS's answer to `POST /rest/admin/token`: a new token with the name it was asked for.
function createdToken(init?: RequestInit) {
    const { name } = JSON.parse(init?.body as string) as { name: string };

    return { token: `cmstok_${name}`, id: 9, userId: 3, name, cdate: 0, expires: 0, lastUsed: 0, valid: true };
}

// Answers each GenAIx and CMS route from `routes`, by method and path, and `POST /rest/admin/token`
// with a new token; anything else is a test error.
function stubRoutes(routes: Record<string, unknown>) {
    const fetchMock = vi.fn<typeof fetch>((url, init) => {
        const key = `${init?.method ?? 'GET'} ${String(url)}`;

        if (key === TOKEN_ROUTE) {
            return Promise.resolve(Response.json(createdToken(init)));
        }

        if (!(key in routes)) {
            throw new Error(`Unexpected request ${key}`);
        }

        return Promise.resolve(Response.json(routes[key]));
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

function requests(fetchMock: ReturnType<typeof stubRoutes>) {
    return fetchMock.mock.calls.map(([url, init]) => `${init?.method ?? 'GET'} ${String(url)}`);
}

describe('session CMS authorization', () => {
    beforeEach(() => {
        vi.useFakeTimers({ toFake: ['Date'] });
        vi.setSystemTime(NOW);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.useRealTimers();
    });

    describe('newSessionCmsAuthorization', () => {
        it('returns a new CMS token, valid for 24 hours, for the default cms connection', async () => {
            const fetchMock = stubRoutes({ 'GET /rest/proxy/genaix/mcp/connections?connector=cms': { items: connections } });

            await expect(newSessionCmsAuthorization()).resolves.toEqual({
                connection_id: 'c-default',
                auth_type: 'bearer',
                token: expect.stringMatching(/^cmstok_genaix-pending-/),
                token_name: expect.stringMatching(/^genaix-pending-/),
                expires_at: '2026-10-08T08:00:00.000Z',
            });
            expect(requests(fetchMock)).toEqual(['GET /rest/proxy/genaix/mcp/connections?connector=cms', TOKEN_ROUTE]);
        });

        it('creates a new token for every session, never reusing one', async () => {
            const fetchMock = stubRoutes({ 'GET /rest/proxy/genaix/mcp/connections?connector=cms': { items: connections } });

            const first = await newSessionCmsAuthorization();
            const second = await newSessionCmsAuthorization();

            expect(requests(fetchMock).filter((request) => request === TOKEN_ROUTE)).toHaveLength(2);
            expect(second.token).not.toBe(first.token);
        });

        it('throws NoCmsConnectionError when the user has no cms connection, before creating a token', async () => {
            const fetchMock = stubRoutes({ 'GET /rest/proxy/genaix/mcp/connections?connector=cms': { items: [] } });

            await expect(newSessionCmsAuthorization()).rejects.toBeInstanceOf(NoCmsConnectionError);
            expect(requests(fetchMock)).not.toContain(TOKEN_ROUTE);
        });
    });

    describe('ensureSessionCmsAuthorization', () => {
        const put = 'PUT /rest/proxy/genaix/sessions/s-1/authorizations/c-default';

        function routes(authorizations: unknown[]) {
            return {
                'GET /rest/proxy/genaix/mcp/connections?connector=cms': { items: connections },
                'GET /rest/proxy/genaix/sessions/s-1': { id: 's-1', authorizations },
                [put]: { connection_id: 'c-default', status: 'authorized' },
            };
        }

        it('leaves an authorized credential that is valid beyond the renewal margin', async () => {
            const fetchMock = stubRoutes(routes([{ connection_id: 'c-default', status: 'authorized', expires_at: '2026-10-07T09:00:00Z' }]));

            await ensureSessionCmsAuthorization('s-1');

            expect(requests(fetchMock)).not.toContain(put);
        });

        it.each([
            ['the session has no credential', []],
            ['the credential is for another connection', [{ connection_id: 'c-other', status: 'authorized' }]],
            ['the credential has expired', [{ connection_id: 'c-default', status: 'expired' }]],
            ['the credential was refused', [{ connection_id: 'c-default', status: 'invalid' }]],
            ['the credential expires within 10 minutes', [{ connection_id: 'c-default', status: 'authorized', expires_at: '2026-10-07T08:05:00Z' }]],
        ])('registers a new token when %s', async (_case, authorizations) => {
            const fetchMock = stubRoutes(routes(authorizations));

            await ensureSessionCmsAuthorization('s-1');

            expect(requests(fetchMock)).toContain(put);

            const [, init] = fetchMock.mock.calls.find(([url, init]) => `${init?.method} ${String(url)}` === put)!;

            expect(JSON.parse(init?.body as string)).toEqual({
                auth_type: 'bearer',
                token: expect.stringMatching(/^cmstok_genaix-s-1-/),
                token_name: expect.stringMatching(/^genaix-s-1-/),
                expires_at: '2026-10-08T08:00:00.000Z',
            });
        });
    });
});

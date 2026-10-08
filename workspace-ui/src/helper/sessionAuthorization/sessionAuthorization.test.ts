import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ensureSessionCmsAuthorization, newSessionCmsAuthorization, NoCmsConnectionError } from './sessionAuthorization';

const NOW = Date.parse('2026-10-07T08:00:00Z');

const connections = [
    { id: 'c-other', connector: 'cms', default: false },
    { id: 'c-default', connector: 'cms', default: true },
];

// Answers each GenAIx route from `routes`, by method and path; anything else is a test error.
function stubRoutes(routes: Record<string, unknown>) {
    const fetchMock = vi.fn<typeof fetch>((url, init) => {
        const key = `${init?.method ?? 'GET'} ${String(url)}`;

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

// The tests run with `import.meta.env.DEV`, so the CMS token is the development fake and no request
// goes to `/rest/admin/token` (`createSessionCmsToken`).
describe('session CMS authorization', () => {
    beforeEach(() => {
        vi.stubEnv('DEV', true);
        vi.useFakeTimers({ toFake: ['Date'] });
        vi.setSystemTime(NOW);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.unstubAllEnvs();
        vi.useRealTimers();
    });

    describe('newSessionCmsAuthorization', () => {
        it('returns a new token for the default cms connection', async () => {
            const fetchMock = stubRoutes({ 'GET /genaix/api/v1/mcp/connections?connector=cms': { items: connections } });

            await expect(newSessionCmsAuthorization()).resolves.toEqual({
                connection_id: 'c-default',
                auth_type: 'bearer',
                token: expect.stringMatching(/^cmstok_dev_/),
                token_name: expect.stringMatching(/^genaix-pending-/),
                expires_at: '2026-10-08T08:00:00.000Z',
            });
            expect(requests(fetchMock)).toEqual(['GET /genaix/api/v1/mcp/connections?connector=cms']);
        });

        it('throws NoCmsConnectionError when the user has no cms connection', async () => {
            stubRoutes({ 'GET /genaix/api/v1/mcp/connections?connector=cms': { items: [] } });

            await expect(newSessionCmsAuthorization()).rejects.toBeInstanceOf(NoCmsConnectionError);
        });
    });

    describe('ensureSessionCmsAuthorization', () => {
        const put = 'PUT /genaix/api/v1/sessions/s-1/authorizations/c-default';

        function routes(authorizations: unknown[]) {
            return {
                'GET /genaix/api/v1/mcp/connections?connector=cms': { items: connections },
                'GET /genaix/api/v1/sessions/s-1': { id: 's-1', authorizations },
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
                token: expect.stringMatching(/^cmstok_dev_/),
                token_name: expect.stringMatching(/^genaix-s-1-/),
                expires_at: '2026-10-08T08:00:00.000Z',
            });
        });
    });
});

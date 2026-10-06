import { afterEach, describe, expect, it, vi } from 'vitest';

import { HttpError } from '@/services/httpService/httpService';

import {
    archiveSession,
    GenaixApiError,
    genaixRetry,
    genaixRetryDelay,
    getMe,
    isRetryableGenaixError,
    listMessages,
    listSessions,
    listWorkflows,
    postMessage,
} from './apiService';

function stubFetch(response: Response) {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(response);

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

function problemResponse(status: number, problem: Record<string, unknown>, headers: Record<string, string> = {}) {
    return Response.json(problem, { status, headers: { 'Content-Type': 'application/problem+json', ...headers } });
}

function apiError(status: number, problem?: Record<string, unknown>, headers: Record<string, string> = {}) {
    return new GenaixApiError('/genaix/api/v1/x', status, problem as never, new Headers(headers));
}

describe('getMe', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('calls /me through the proxy path and returns the body', async () => {
        const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(Response.json({ user: { subject: 'sub_test' } }));

        vi.stubGlobal('fetch', fetchMock);

        await expect(getMe()).resolves.toEqual({ user: { subject: 'sub_test' } });
        expect(fetchMock.mock.calls[0]![0]).toBe('/genaix/api/v1/me');
    });
});

describe('GENAIX_API_BASE', () => {
    afterEach(() => {
        vi.unstubAllEnvs();
        vi.unstubAllGlobals();
        vi.resetModules();
    });

    it('comes from VITE_GENAIX_API_BASE when it is set', async () => {
        vi.stubEnv('VITE_GENAIX_API_BASE', '/proxy/genaix/v1');
        vi.resetModules();

        const fetchMock = stubFetch(Response.json({}));
        const apiService = await import('./apiService');

        await apiService.getMe();

        expect(apiService.GENAIX_API_BASE).toBe('/proxy/genaix/v1');
        expect(fetchMock.mock.calls[0]![0]).toBe('/proxy/genaix/v1/me');
    });
});

describe('genaixRequest errors', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('throws a GenaixApiError carrying the Problem, its code and the request id', async () => {
        stubFetch(problemResponse(404, {
            type: 'https://genaix.gentics.com/problems/session-not-found',
            title: 'Session not found',
            status: 404,
            genaix_code: 'session_not_found',
            request_id: 'req_body',
        }));

        const error = await listMessages('s-1').catch((e: unknown) => e);

        expect(error).toBeInstanceOf(GenaixApiError);
        expect(error).toBeInstanceOf(HttpError);
        expect(error).toMatchObject({ status: 404, genaixCode: 'session_not_found', requestId: 'req_body' });
        expect((error as GenaixApiError).problem?.title).toBe('Session not found');
    });

    it('takes the request id from X-GenAIx-Request-Id when the body is not a Problem', async () => {
        stubFetch(new Response('Bad Gateway', { status: 502, headers: { 'X-GenAIx-Request-Id': 'req_header' } }));

        const error = await getMe().catch((e: unknown) => e);

        expect(error).toBeInstanceOf(GenaixApiError);
        expect(error).toMatchObject({ status: 502, genaixCode: undefined, problem: undefined, requestId: 'req_header' });
    });

    it('reads Retry-After when the Problem has no retry_after_seconds', async () => {
        stubFetch(problemResponse(429, { type: 't', title: 't', status: 429, genaix_code: 'rate_limited' }, { 'Retry-After': '7' }));

        await expect(getMe()).rejects.toMatchObject({ retryAfterSeconds: 7 });
    });
});

describe('routes', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('sends Accept: application/json and the same-origin credentials', async () => {
        const fetchMock = stubFetch(Response.json({ items: [] }));

        await listWorkflows();

        const init = fetchMock.mock.calls[0]![1]!;

        expect(new Headers(init.headers).get('Accept')).toBe('application/json');
        expect(init.credentials).toBe('same-origin');
    });

    it('listWorkflows returns the items of GET /workflows', async () => {
        const fetchMock = stubFetch(Response.json({ items: [{ id: 'free_chat' }] }));

        await expect(listWorkflows()).resolves.toEqual([{ id: 'free_chat' }]);
        expect(fetchMock.mock.calls[0]![0]).toBe('/genaix/api/v1/workflows');
    });

    it('listSessions repeats array filters and passes the cursor', async () => {
        const fetchMock = stubFetch(Response.json({ items: [], next_cursor: null }));

        await listSessions({ status: ['active', 'waiting_for_input'], q: 'terms' }, { limit: 25, cursor: 'c2' });

        expect(fetchMock.mock.calls[0]![0]).toBe('/genaix/api/v1/sessions?status=active&status=waiting_for_input&q=terms&limit=25&cursor=c2');
    });

    it('listSessions without filters has no query string', async () => {
        const fetchMock = stubFetch(Response.json({ items: [], next_cursor: null }));

        await listSessions();

        expect(fetchMock.mock.calls[0]![0]).toBe('/genaix/api/v1/sessions');
    });

    it('archiveSession sends DELETE /sessions/{session_id} and resolves on 204', async () => {
        const fetchMock = stubFetch(new Response(null, { status: 204 }));

        await expect(archiveSession('s 1')).resolves.toBeUndefined();

        const [url, init] = fetchMock.mock.calls[0]!;

        expect(url).toBe('/genaix/api/v1/sessions/s%201');
        expect(init?.method).toBe('DELETE');
    });

    it('listMessages pages the messages of one session', async () => {
        const fetchMock = stubFetch(Response.json({ items: [], next_cursor: null }));

        await listMessages('s 1', { cursor: 'c2' });

        expect(fetchMock.mock.calls[0]![0]).toBe('/genaix/api/v1/sessions/s%201/messages?cursor=c2');
    });

    it('postMessage posts the turn as JSON and returns the accepted ids', async () => {
        const accepted = { message_id: 'm-1', run_id: 'r-1', session_id: 's-1' };
        const fetchMock = stubFetch(Response.json(accepted, { status: 202 }));

        await expect(postMessage('s-1', { content: 'Hello' })).resolves.toEqual(accepted);

        const [url, init] = fetchMock.mock.calls[0]!;

        expect(url).toBe('/genaix/api/v1/sessions/s-1/messages');
        expect(init?.method).toBe('POST');
        expect(new Headers(init?.headers).get('Content-Type')).toBe('application/json');
        expect(new Headers(init?.headers).get('Accept')).toBe('application/json');
        expect(JSON.parse(init?.body as string)).toEqual({ content: 'Hello' });
    });
});

describe('retries', () => {
    it.each([
        ['a network error', new TypeError('Failed to fetch'), true],
        ['429', apiError(429), true],
        ['503', apiError(503), true],
        ['400', apiError(400), false],
        ['404', apiError(404), false],
        ['409', apiError(409), false],
        ['500', apiError(500), false],
        ['another error', new Error('boom'), false],
    ])('isRetryableGenaixError: %s → %s', (_name, error, expected) => {
        expect(isRetryableGenaixError(error)).toBe(expected);
    });

    it('genaixRetry stops after three retries', () => {
        expect(genaixRetry(2, apiError(503))).toBe(true);
        expect(genaixRetry(3, apiError(503))).toBe(false);
        expect(genaixRetry(0, apiError(404))).toBe(false);
    });

    it('genaixRetryDelay waits retry_after_seconds when the server sent it', () => {
        expect(genaixRetryDelay(0, apiError(429, { genaix_code: 'rate_limited', retry_after_seconds: 12 }))).toBe(12_000);
    });

    it('genaixRetryDelay otherwise doubles from 1 s up to 30 s', () => {
        expect([0, 1, 2, 3, 4, 5, 6].map((n) => genaixRetryDelay(n, apiError(503)))).toEqual([1000, 2000, 4000, 8000, 16_000, 30_000, 30_000]);
    });
});

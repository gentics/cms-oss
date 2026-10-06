import { vi } from 'vitest';

import type { Session, SessionPage } from '@/services/apiService/genaix/types';

/** A complete `Session` as `GET /sessions` returns it; `overrides` sets what a test is about. */
export function sessionFixture(overrides: Partial<Session> & Pick<Session, 'id'>): Session {
    return {
        title: `Session ${overrides.id}`,
        workflow: 'free_chat',
        workflow_version: '1.0.0',
        inputs: {},
        status: 'active',
        context: { references: [], guidelines: [], connection_ids: [] },
        owner: { subject: 'sub_test' },
        created_at: '2026-09-23T10:00:00Z',
        updated_at: '2026-09-23T10:00:00Z',
        last_activity_at: '2026-09-23T10:00:00Z',
        message_count: 0,
        iteration: 0,
        cms_objects: [],
        labels: [],
        authorizations: [],
        ...overrides,
    };
}

/**
 * Stubs `fetch` as the GenAIx session routes: `GET /sessions` answers with `listSessions(url)`,
 * `DELETE /sessions/{id}` with `archive(id)` (default `204`). Returns the mock to inspect the calls.
 */
export function stubSessionRoutes(
    listSessions: (url: URL) => SessionPage,
    archive: (sessionId: string) => Response = () => new Response(null, { status: 204 }),
) {
    const fetchMock = vi.fn<typeof fetch>(async (input, init) => {
        const url = new URL(String(input), 'http://localhost');
        const sessionId = /^\/genaix\/api\/v1\/sessions\/([^/]+)$/.exec(url.pathname)?.[1];

        if (init?.method === 'DELETE' && sessionId) {
            return archive(decodeURIComponent(sessionId));
        }

        if (url.pathname === '/genaix/api/v1/sessions') {
            return Response.json(listSessions(url));
        }

        return new Response(null, { status: 404 });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

/**
 * Wraps `fetchMock` for a page that lists sessions: `GET /sessions` gets an empty list, every other
 * call goes to `fetchMock`, which so sees only the calls a test is about.
 */
export function withEmptySessionList(fetchMock: typeof fetch): typeof fetch {
    return async (input, init) => {
        const url = new URL(String(input), 'http://localhost');

        if ((init?.method ?? 'GET') === 'GET' && url.pathname === '/genaix/api/v1/sessions') {
            return Response.json({ items: [], next_cursor: null } satisfies SessionPage);
        }

        return fetchMock(input, init);
    };
}

/** The `GET /sessions` query strings `fetchMock` was called with, in order. */
export function listQueries(fetchMock: ReturnType<typeof stubSessionRoutes>): string[] {
    return fetchMock.mock.calls
        .filter(([, init]) => (init?.method ?? 'GET') === 'GET')
        .map(([input]) => new URL(String(input), 'http://localhost').search);
}

/** The ids `fetchMock` sent `DELETE /sessions/{id}` for. */
export function archivedIds(fetchMock: ReturnType<typeof stubSessionRoutes>): string[] {
    return fetchMock.mock.calls
        .filter(([, init]) => init?.method === 'DELETE')
        .map(([input]) => decodeURIComponent(String(input).split('/').pop()!));
}

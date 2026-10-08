import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { pendingFileSource } from '@/helper/composerParts/composerParts';
import { ensureSessionCmsAuthorization, newSessionCmsAuthorization } from '@/helper/sessionAuthorization/sessionAuthorization';
import { GenaixApiError } from '@/services/apiService/apiService';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';
import { stubUploads } from '@/test/stubUploads';

import { useAnswerInteraction,
    useArchiveSession,
    useCancelRun,
    useMe,
    useSendMessage,
    useSendTurn,
    useSession,
    useSessionMessages,
    useSessions,
    useStartSession,
    useWorkflows,
} from './useGenaixQueries';

// The CMS credential has tests of its own (`sessionAuthorization.test.ts`); here only where it goes.
vi.mock('@/helper/sessionAuthorization/sessionAuthorization', () => ({
    newSessionCmsAuthorization: vi.fn(),
    ensureSessionCmsAuthorization: vi.fn(),
}));

const AUTHORIZATION = {
    connection_id: 'c-1',
    auth_type: 'bearer' as const,
    token: 'cmstok_dev_1',
    token_name: 'genaix-pending-1',
    expires_at: '2026-10-08T08:00:00.000Z',
};

function wrapper({ children }: { children: ReactNode }) {
    // The hooks' own retry settings apply; the client adds none.
    const queryClient = new QueryClient();

    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function problem(status: number, genaixCode: string, extra: Record<string, unknown> = {}) {
    return Response.json({ type: 't', title: 't', status, genaix_code: genaixCode, ...extra }, { status });
}

function stubFetch(...responses: Response[]) {
    const fetchMock = vi.fn<typeof fetch>();

    responses.forEach((response) => fetchMock.mockResolvedValueOnce(response));
    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

describe('GenAIx query hooks', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
        vi.mocked(newSessionCmsAuthorization).mockResolvedValue(AUTHORIZATION);
        vi.mocked(ensureSessionCmsAuthorization).mockResolvedValue();
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.clearAllMocks();
    });

    it('useMe loads GET /me', async () => {
        stubFetch(Response.json({ user: { subject: 'sub_test' } }));

        const { result } = renderHook(() => useMe(), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual({ user: { subject: 'sub_test' } });
    });

    it('retries a 503 and succeeds', async () => {
        const fetchMock = stubFetch(problem(503, 'service_unavailable', { retry_after_seconds: 0 }), Response.json({ items: [{ id: 'free_chat' }] }));

        const { result } = renderHook(() => useWorkflows(), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual([{ id: 'free_chat' }]);
        expect(fetchMock).toHaveBeenCalledTimes(2);
    });

    it('does not retry a 404 and exposes the GenaixApiError', async () => {
        const fetchMock = stubFetch(problem(404, 'session_not_found'));

        const { result } = renderHook(() => useSessionMessages('s-1'), { wrapper });

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(result.current.error).toBeInstanceOf(GenaixApiError);
        expect((result.current.error as GenaixApiError).genaixCode).toBe('session_not_found');
    });

    it('useSessions follows next_cursor until it is null', async () => {
        const fetchMock = stubFetch(
            Response.json({ items: [{ id: 's-1' }], next_cursor: 'c2' }),
            Response.json({ items: [{ id: 's-2' }], next_cursor: null }),
        );

        const { result } = renderHook(() => useSessions({ status: ['active'] }), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.hasNextPage).toBe(true);

        await act(() => result.current.fetchNextPage());
        await waitFor(() => expect(result.current.data?.pages).toHaveLength(2));

        expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
            '/genaix/api/v1/sessions?status=active',
            '/genaix/api/v1/sessions?status=active&cursor=c2',
        ]);
        expect(result.current.data?.pages.flatMap((page) => page.items)).toEqual([{ id: 's-1' }, { id: 's-2' }]);
        expect(result.current.hasNextPage).toBe(false);
    });

    it('useArchiveSession sends DELETE and refetches the session lists, which no longer hold the session', async () => {
        const fetchMock = stubFetch(
            Response.json({ items: [{ id: 's-1' }, { id: 's-2' }], next_cursor: null }),
            new Response(null, { status: 204 }),
            Response.json({ items: [{ id: 's-2' }], next_cursor: null }),
        );

        const { result } = renderHook(() => ({ sessions: useSessions(), archive: useArchiveSession() }), { wrapper });

        await waitFor(() => expect(result.current.sessions.isSuccess).toBe(true));

        await act(() => result.current.archive.mutateAsync('s-1'));

        expect(fetchMock.mock.calls.map(([url, init]) => [init?.method ?? 'GET', url])).toEqual([
            ['GET', '/genaix/api/v1/sessions'],
            ['DELETE', '/genaix/api/v1/sessions/s-1'],
            ['GET', '/genaix/api/v1/sessions'],
        ]);
        await waitFor(() => expect(result.current.sessions.data?.pages.flatMap((page) => page.items)).toEqual([{ id: 's-2' }]));
    });

    describe('useSendMessage', () => {
        it('shows the turn as sending, then sent with the server message id', async () => {
            let accept!: (response: Response) => void;

            vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockReturnValue(new Promise((resolve) => {
                accept = resolve;
            })));

            const { result } = renderHook(() => useSendMessage('s-1'), { wrapper });

            act(() => result.current.mutate({ content: 'Hello' }));

            await waitFor(() => expect(selectSession('s-1')(useWorkspaceEventStore.getState()).messages).toMatchObject([
                { kind: 'sent', request: { content: 'Hello' }, status: 'sending' },
            ]));

            accept(Response.json({ message_id: 'm-1', run_id: 'r-1', session_id: 's-1' }, { status: 202 }));

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(selectSession('s-1')(useWorkspaceEventStore.getState()).messages).toMatchObject([
                { kind: 'sent', messageId: 'm-1', status: 'sent' },
            ]);
        });

        it('marks the turn send_failed and does not send it again', async () => {
            const fetchMock = stubFetch(problem(503, 'service_unavailable'));

            const { result } = renderHook(() => useSendMessage('s-1'), { wrapper });

            act(() => result.current.mutate({ content: 'Hello' }));

            await waitFor(() => expect(result.current.isError).toBe(true));
            expect(fetchMock).toHaveBeenCalledTimes(1);
            expect(selectSession('s-1')(useWorkspaceEventStore.getState()).messages).toMatchObject([
                { kind: 'sent', status: 'send_failed', error: expect.any(GenaixApiError) },
            ]);
        });

        it('makes sure the session holds a CMS credential before it posts the turn', async () => {
            const fetchMock = stubFetch(Response.json({ message_id: 'm-1', run_id: 'r-1', session_id: 's-1' }, { status: 202 }));

            vi.mocked(ensureSessionCmsAuthorization).mockImplementation(() => {
                expect(fetchMock).not.toHaveBeenCalled();

                return Promise.resolve();
            });

            const { result } = renderHook(() => useSendMessage('s-1'), { wrapper });

            act(() => result.current.mutate({ content: 'Hello' }));

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(ensureSessionCmsAuthorization).toHaveBeenCalledWith('s-1');
            expect(fetchMock).toHaveBeenCalledTimes(1);
        });

        it('marks the turn send_failed and posts nothing when no CMS credential can be registered', async () => {
            const fetchMock = stubFetch();
            const refused = new GenaixApiError('/genaix/api/v1/sessions/s-1/authorizations/c-1', 422, undefined, new Headers());

            vi.mocked(ensureSessionCmsAuthorization).mockRejectedValue(refused);

            const { result } = renderHook(() => useSendMessage('s-1'), { wrapper });

            act(() => result.current.mutate({ content: 'Hello' }));

            await waitFor(() => expect(result.current.isError).toBe(true));
            expect(fetchMock).not.toHaveBeenCalled();
            expect(selectSession('s-1')(useWorkspaceEventStore.getState()).messages).toMatchObject([
                { kind: 'sent', status: 'send_failed', error: refused },
            ]);
        });
    });

    describe('useStartSession', () => {
        const parts = [{ type: 'text' as const, text: 'Which pages are offline?' }];

        function requestOf(fetchMock: ReturnType<typeof stubFetch>, index: number) {
            const [url, init] = fetchMock.mock.calls[index]!;

            return { url, method: init?.method, body: init?.body };
        }

        it('without files creates the session with its first message in one call', async () => {
            const fetchMock = stubFetch(Response.json({ id: 's-1', status: 'active', run_id: 'r-1', message_id: 'm-1' }, { status: 201 }));

            vi.stubGlobal('fetch', fetchMock);

            const { result } = renderHook(() => useStartSession(), { wrapper });

            act(() => result.current.mutate({ parts, files: [] }));

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(fetchMock).toHaveBeenCalledTimes(1);
            expect(requestOf(fetchMock, 0).url).toBe('/genaix/api/v1/sessions');
            expect(JSON.parse(requestOf(fetchMock, 0).body as string)).toEqual({ workflow: 'content_research', message: { parts }, authorizations: [AUTHORIZATION] });
            expect(result.current.data).toMatchObject({ id: 's-1' });
        });

        it('starts the workflow the first message suggests (TEMPORARY keyword guess)', async () => {
            const fetchMock = stubFetch(Response.json({ id: 's-1', status: 'active' }, { status: 201 }));
            const createParts = [{ type: 'text' as const, text: 'Create a landing page about the new terms' }];

            const { result } = renderHook(() => useStartSession(), { wrapper });

            act(() => result.current.mutate({ parts: createParts, files: [] }));

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(JSON.parse(requestOf(fetchMock, 0).body as string)).toMatchObject({ workflow: 'content_create' });
        });

        // The uploads go through XMLHttpRequest (`stubUploads`), the other requests through fetch; both
        // write to `log`, so the order of all four can be checked.
        it('with files creates the session, uploads each file, then posts the message with a file_ref per file', async () => {
            const log: string[] = [];
            const fetchMock = stubFetch(
                Response.json({ id: 's-1', status: 'active' }, { status: 201 }),
                Response.json({ message_id: 'm-1', run_id: 'r-1', session_id: 's-1' }, { status: 202 }),
            );

            vi.stubGlobal('fetch', vi.fn<typeof fetch>((url, init) => {
                log.push(`${init?.method} ${String(url)}`);

                return fetchMock(url, init);
            }));

            const uploads = stubUploads([{ status: 201, body: { id: 'f-1' } }, { status: 201, body: { id: 'f-2' } }], log);
            const files = [
                { file: new File(['a'], 'brief.pdf'), mode: 'verbatim' as const },
                { file: new File(['b'], 'notes.txt'), mode: 'source' as const },
            ];

            const { result } = renderHook(() => useStartSession(), { wrapper });

            act(() => result.current.mutate({ parts, files }));

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(log).toEqual([
                'POST /genaix/api/v1/sessions',
                'POST /genaix/api/v1/sessions/s-1/files',
                'POST /genaix/api/v1/sessions/s-1/files',
                'POST /genaix/api/v1/sessions/s-1/messages',
            ]);
            expect(JSON.parse(requestOf(fetchMock, 0).body as string)).toEqual({ workflow: 'content_research', authorizations: [AUTHORIZATION] });
            expect(uploads[0]!.body.get('mode')).toBe('verbatim');
            expect((uploads[1]!.body.get('file') as File).name).toBe('notes.txt');
            expect(JSON.parse(requestOf(fetchMock, 1).body as string)).toEqual({
                parts: [
                    ...parts,
                    { type: 'file_ref', file_id: 'f-1', mode: 'verbatim' },
                    { type: 'file_ref', file_id: 'f-2', mode: 'source' },
                ],
            });
        });

        it('puts the created session into the cache that useSession reads', async () => {
            // Only the POST is answered: useSession's own GET stays pending, so its data can only come
            // from the cache entry the mutation writes.
            vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockImplementation((_url, init) => (init?.method === 'POST'
                ? Promise.resolve(Response.json({ id: 's-1', status: 'active', run_id: 'r-1', message_id: 'm-1' }, { status: 201 }))
                : new Promise(() => {}))));

            const { result } = renderHook(() => ({ start: useStartSession(), session: useSession('s-1') }), { wrapper });

            act(() => result.current.start.mutate({ parts, files: [] }));

            await waitFor(() => expect(result.current.session.data).toMatchObject({ id: 's-1', status: 'active' }));
        });

        it('creates no session when no CMS credential can be obtained', async () => {
            const fetchMock = stubFetch();
            const refused = new Error('CMS refused');

            vi.mocked(newSessionCmsAuthorization).mockRejectedValue(refused);

            const { result } = renderHook(() => useStartSession(), { wrapper });

            act(() => result.current.mutate({ parts, files: [] }));

            await waitFor(() => expect(result.current.isError).toBe(true));
            expect(result.current.error).toBe(refused);
            expect(fetchMock).not.toHaveBeenCalled();
        });

        it('exposes the error and does not retry', async () => {
            const fetchMock = stubFetch(problem(503, 'service_unavailable'));

            const { result } = renderHook(() => useStartSession(), { wrapper });

            act(() => result.current.mutate({ parts, files: [] }));

            await waitFor(() => expect(result.current.isError).toBe(true));
            expect(fetchMock).toHaveBeenCalledTimes(1);
            expect((result.current.error as GenaixApiError).genaixCode).toBe('service_unavailable');
        });
    });

    describe('useSendTurn', () => {
        const parts = [{ type: 'text' as const, text: 'Shorten the intro' }];

        it('posts a turn without files as one message and records it in the store', async () => {
            const fetchMock = stubFetch(Response.json({ message_id: 'm-1', run_id: 'r-1', session_id: 's-1' }, { status: 202 }));

            const { result } = renderHook(() => useSendTurn('s-1'), { wrapper });

            act(() => result.current.mutate({ parts, files: [] }));

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(fetchMock).toHaveBeenCalledTimes(1);
            expect(fetchMock.mock.calls[0]![0]).toBe('/genaix/api/v1/sessions/s-1/messages');
            expect(JSON.parse(fetchMock.mock.calls[0]![1]!.body as string)).toEqual({ parts });
            expect(selectSession('s-1')(useWorkspaceEventStore.getState()).messages).toMatchObject([
                { kind: 'sent', messageId: 'm-1', request: { parts }, status: 'sent' },
            ]);
        });

        it('uploads the files first, then posts the message with a file_ref per file', async () => {
            const log: string[] = [];
            const fetchMock = stubFetch(Response.json({ message_id: 'm-1', run_id: 'r-1', session_id: 's-1' }, { status: 202 }));

            vi.stubGlobal('fetch', vi.fn<typeof fetch>((url, init) => {
                log.push(`${init?.method} ${String(url)}`);

                return fetchMock(url, init);
            }));

            stubUploads([{ status: 201, body: { id: 'f-1' } }], log);

            const { result } = renderHook(() => useSendTurn('s-1'), { wrapper });

            act(() => result.current.mutate({ parts, files: [{ file: new File(['a'], 'brief.pdf'), mode: 'verbatim' }] }));

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            // In the order they were made, across fetch and the upload.
            expect(log.map((entry) => entry.split(' ')[1])).toEqual([
                '/genaix/api/v1/sessions/s-1/files',
                '/genaix/api/v1/sessions/s-1/messages',
            ]);
            expect(JSON.parse(fetchMock.mock.calls[0]![1]!.body as string)).toEqual({
                parts: [...parts, { type: 'file_ref', file_id: 'f-1', mode: 'verbatim' }],
            });
        });

        it('gives a passage quoted from a submitted file that file\'s uploaded id as its source', async () => {
            const fetchMock = stubFetch(Response.json({ message_id: 'm-1', run_id: 'r-1', session_id: 's-1' }, { status: 202 }));

            stubUploads([{ status: 201, body: { id: 'f-1' } }, { status: 201, body: { id: 'f-2' } }]);

            const { result } = renderHook(() => useSendTurn('s-1'), { wrapper });

            act(() => result.current.mutate({
                parts: [{ type: 'verbatim', text: 'Review now', source: pendingFileSource(1) }],
                files: [{ file: new File(['a'], 'brief.pdf'), mode: 'source' }, { file: new File(['b'], 'terms.pdf'), mode: 'source' }],
            }));

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(JSON.parse(fetchMock.mock.calls[0]![1]!.body as string)).toEqual({
                parts: [
                    { type: 'verbatim', text: 'Review now', source: 'f-2' },
                    { type: 'file_ref', file_id: 'f-1', mode: 'source' },
                    { type: 'file_ref', file_id: 'f-2', mode: 'source' },
                ],
            });
        });

        it('exposes a failed upload and sends nothing', async () => {
            const fetchMock = stubFetch();
            const uploads = stubUploads([{ status: 413, body: { type: 't', title: 't', status: 413, genaix_code: 'file_too_large' } }]);

            const { result } = renderHook(() => useSendTurn('s-1'), { wrapper });

            act(() => result.current.mutate({ parts, files: [{ file: new File(['a'], 'big.pdf'), mode: 'source' }] }));

            await waitFor(() => expect(result.current.isError).toBe(true));
            expect(uploads).toHaveLength(1);
            expect(fetchMock).not.toHaveBeenCalled();
            expect((result.current.error as GenaixApiError).genaixCode).toBe('file_too_large');
            expect(selectSession('s-1')(useWorkspaceEventStore.getState()).messages).toEqual([]);
        });

        it('reports the upload progress of each file by its position', async () => {
            stubFetch(Response.json({ message_id: 'm-1', run_id: 'r-1', session_id: 's-1' }, { status: 202 }));
            stubUploads([{ status: 201, body: { id: 'f-1' }, progress: [0.5, 1] }, { status: 201, body: { id: 'f-2' }, progress: [0.4] }]);
            const onFileProgress = vi.fn<(index: number, fraction: number) => void>();
            const files = [
                { file: new File(['a'], 'brief.pdf'), mode: 'source' as const },
                { file: new File(['b'], 'notes.txt'), mode: 'source' as const },
            ];

            const { result } = renderHook(() => useSendTurn('s-1'), { wrapper });

            act(() => result.current.mutate({ parts, files, onFileProgress }));

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(onFileProgress.mock.calls).toEqual([[0, 0.5], [0, 1], [1, 0.4]]);
        });
    });

    it('useCancelRun cancels the run and shows it as cancelling until run.cancelled', async () => {
        const fetchMock = stubFetch(Response.json({ id: 'r-1', status: 'cancelling' }, { status: 202 }));

        useWorkspaceEventStore.getState().applyEvent('s-1', { seq: 1, ts: '2026-10-07T09:00:00Z', session_id: 's-1', type: 'run.started', run: { id: 'r-1', status: 'running', started_at: '2026-10-07T09:00:00Z', step_count: 0 } });

        const { result } = renderHook(() => useCancelRun('s-1'), { wrapper });

        act(() => result.current.mutate('r-1'));

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(fetchMock.mock.calls[0]![0]).toBe('/genaix/api/v1/sessions/s-1/runs/r-1/cancel');
        expect(selectSession('s-1')(useWorkspaceEventStore.getState()).run).toMatchObject({ id: 'r-1', status: 'cancelling' });
    });

    it('useAnswerInteraction posts the answer once and does not retry a refusal', async () => {
        const fetchMock = stubFetch(problem(409, 'interaction_already_answered'));

        const { result } = renderHook(() => useAnswerInteraction('s-1'), { wrapper });

        act(() => result.current.mutate({ interactionId: 'i-1', answer: { approved: true } }));

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(result.current.error).toMatchObject({ genaixCode: 'interaction_already_answered' });
    });

    it('useSession loads GET /sessions/{session_id}', async () => {
        const fetchMock = stubFetch(Response.json({ id: 's-1', status: 'review_requested' }));

        const { result } = renderHook(() => useSession('s-1'), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(fetchMock.mock.calls[0]![0]).toBe('/genaix/api/v1/sessions/s-1');
        expect(result.current.data).toEqual({ id: 's-1', status: 'review_requested' });
    });
});

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { GenaixApiError } from '@/services/apiService/apiService';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import { useMe, useSendMessage, useSessionMessages, useSessions, useWorkflows } from './useGenaixQueries';

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
    });

    afterEach(() => {
        vi.unstubAllGlobals();
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
    });
});

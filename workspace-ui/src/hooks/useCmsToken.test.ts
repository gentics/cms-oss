import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { createElement, type ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { HttpError } from '@/services/httpService/httpService';
import { useCmsTokenStore } from '@/store/useCmsTokenStore';

import { useCmsToken } from './useCmsToken';

const SECRET = 'cmstok_do_not_leak';
const created = { token: SECRET, id: 4, userId: 3, name: 'genaix-workspace-1', cdate: 1_790_752_317, expires: 0, lastUsed: 0, valid: true };

function wrapper({ children }: { children: ReactNode }) {
    // The hook sets its own `retry`; no delay between retries keeps the tests fast.
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, retryDelay: 0 } } });

    return createElement(QueryClientProvider, { client: queryClient }, children);
}

function stubFetch(response?: Response) {
    const fetchMock = vi.fn<typeof fetch>();

    if (response) {
        fetchMock.mockResolvedValue(response);
    }

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

describe('useCmsToken', () => {
    beforeEach(() => {
        useCmsTokenStore.getState().clearCmsToken();
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.restoreAllMocks();
    });

    it('posts for a token when the store is empty and keeps the secret out of the query result', async () => {
        const fetchMock = stubFetch(Response.json(created));

        const { result } = renderHook(() => useCmsToken(), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(fetchMock.mock.calls[0]![0]).toBe('/rest/admin/token');
        expect(fetchMock.mock.calls[0]![1]?.method).toBe('POST');
        expect(result.current.data).toEqual({ id: 4, name: 'genaix-workspace-1', expires: 0 });
        expect(JSON.stringify(result.current.data)).not.toContain(SECRET);
        expect(useCmsTokenStore.getState().cmsToken).toEqual({ token: SECRET, id: 4, name: 'genaix-workspace-1', expires: 0 });
    });

    it('uses a valid token from the store without a request', async () => {
        const fetchMock = stubFetch();

        useCmsTokenStore.getState().setCmsToken({ token: SECRET, id: 9, name: 'genaix-workspace-stored', expires: 0 });

        const { result } = renderHook(() => useCmsToken(), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(fetchMock).not.toHaveBeenCalled();
        expect(result.current.data).toEqual({ id: 9, name: 'genaix-workspace-stored', expires: 0 });
    });

    it('reports a failure as an error and leaves the store empty', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        stubFetch(new Response('Forbidden', { status: 403 }));

        const { result } = renderHook(() => useCmsToken(), { wrapper });

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toBeInstanceOf(HttpError);
        expect(useCmsTokenStore.getState().cmsToken).toBeNull();
    });

    describe('retries', () => {
        it('retries a response that is not 200, three times', async () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);
            const fetchMock = stubFetch();

            fetchMock.mockImplementation(() => Promise.resolve(new Response('Service Unavailable', { status: 503 })));

            const { result } = renderHook(() => useCmsToken(), { wrapper });

            await waitFor(() => expect(result.current.isError).toBe(true));

            expect(fetchMock).toHaveBeenCalledTimes(4);
            expect(result.current.error).toBeInstanceOf(HttpError);
        });

        it('succeeds when a retry gets a 200', async () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);
            const fetchMock = stubFetch();

            fetchMock
                .mockResolvedValueOnce(new Response('Service Unavailable', { status: 503 }))
                .mockResolvedValueOnce(Response.json(created));

            const { result } = renderHook(() => useCmsToken(), { wrapper });

            await waitFor(() => expect(result.current.isSuccess).toBe(true));

            expect(fetchMock).toHaveBeenCalledTimes(2);
            expect(useCmsTokenStore.getState().cmsToken?.token).toBe(SECRET);
        });

        it('does not retry a 200 whose body is not JSON', async () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);
            const fetchMock = stubFetch();

            fetchMock.mockImplementation(() => Promise.resolve(new Response('<html>not json</html>', { status: 200 })));

            const { result } = renderHook(() => useCmsToken(), { wrapper });

            await waitFor(() => expect(result.current.isError).toBe(true));

            expect(fetchMock).toHaveBeenCalledTimes(1);
            expect(result.current.error).toBeInstanceOf(SyntaxError);
        });

        it('does not retry a network error', async () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);
            const fetchMock = stubFetch();

            fetchMock.mockImplementation(() => Promise.reject(new TypeError('Failed to fetch')));

            const { result } = renderHook(() => useCmsToken(), { wrapper });

            await waitFor(() => expect(result.current.isError).toBe(true));

            expect(fetchMock).toHaveBeenCalledTimes(1);
            expect(result.current.error).toBeInstanceOf(TypeError);
        });
    });
});

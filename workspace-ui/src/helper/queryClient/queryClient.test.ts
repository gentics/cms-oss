import { QueryClientProvider, useQuery } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { createElement, type ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { HttpError, httpRequest } from '@/services/httpService/httpService';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

import { createQueryClient } from './queryClient';

const created = { token: 'cmstok', id: 4, userId: 3, name: 'genaix-workspace-1', cdate: 1_790_752_317, expires: 0, lastUsed: 0, valid: true };

function permissionDenied() {
    return Response.json(
        { messages: [{ type: 'CRITICAL', message: 'Not allowed' }], responseInfo: { responseCode: 'PERMISSION' } },
        { status: 403 },
    );
}

// A query that opts into the notification: one CMS request, retried after a failed response up to
// three times.
function useNotifyingQuery() {
    return useQuery({
        queryKey: ['notifying'],
        queryFn: () => httpRequest('/rest/admin/token'),
        retry: (failureCount, error) => error instanceof HttpError && failureCount < 3,
        meta: { errorMessageKey: 'test.queryFailed' },
    });
}

function wrapper({ children }: { children: ReactNode }) {
    // No delay between retries keeps the tests fast; the hooks keep their own `retry`.
    const queryClient = createQueryClient({ defaultOptions: { queries: { retryDelay: 0 } } });

    return createElement(QueryClientProvider, { client: queryClient }, children);
}

describe('createQueryClient error notifications', () => {
    beforeEach(() => {
        useErrorNotificationStore.setState({ errors: [] });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows one notification once all retries have failed, with the mapped detail', async () => {
        const fetchMock = vi.fn<typeof fetch>().mockImplementation(() => Promise.resolve(permissionDenied()));

        vi.stubGlobal('fetch', fetchMock);

        const { result } = renderHook(() => useNotifyingQuery(), { wrapper });

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(fetchMock).toHaveBeenCalledTimes(4);
        expect(useErrorNotificationStore.getState().errors).toEqual([{
            id: expect.any(String),
            messageKey: 'test.queryFailed',
            detailKey: 'errors.cms.PERMISSION',
        }]);
    });

    it('shows no notification when a retry succeeds', async () => {
        vi.stubGlobal('fetch', vi.fn<typeof fetch>()
            .mockResolvedValueOnce(permissionDenied())
            .mockResolvedValueOnce(Response.json(created)));

        const { result } = renderHook(() => useNotifyingQuery(), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(useErrorNotificationStore.getState().errors).toEqual([]);
    });

    it('shows no notification for a query without meta.errorMessageKey', async () => {
        const { result } = renderHook(() => useQuery({
            queryKey: ['no-meta'],
            queryFn: () => Promise.reject(new HttpError('/x', 500)),
            retry: false,
        }), { wrapper });

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(useErrorNotificationStore.getState().errors).toEqual([]);
    });
});

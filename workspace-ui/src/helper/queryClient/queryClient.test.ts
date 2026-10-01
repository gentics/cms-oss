import { QueryClientProvider, useQuery } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { createElement, type ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useCmsToken } from '@/hooks/useCmsToken';
import { HttpError } from '@/services/httpService/httpService';
import { useCmsTokenStore } from '@/store/useCmsTokenStore';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

import { createQueryClient } from './queryClient';

const created = { token: 'cmstok', id: 4, userId: 3, name: 'genaix-workspace-1', cdate: 1_790_752_317, expires: 0, lastUsed: 0, valid: true };

function permissionDenied() {
    return Response.json(
        { messages: [{ type: 'CRITICAL', message: 'Not allowed' }], responseInfo: { responseCode: 'PERMISSION' } },
        { status: 403 },
    );
}

function wrapper({ children }: { children: ReactNode }) {
    // No delay between retries keeps the tests fast; the hooks keep their own `retry`.
    const queryClient = createQueryClient({ defaultOptions: { queries: { retryDelay: 0 } } });

    return createElement(QueryClientProvider, { client: queryClient }, children);
}

describe('createQueryClient error notifications', () => {
    beforeEach(() => {
        useCmsTokenStore.getState().clearCmsToken();
        useErrorNotificationStore.setState({ errors: [] });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows one notification once all retries have failed, with the mapped detail', async () => {
        const fetchMock = vi.fn<typeof fetch>().mockImplementation(() => Promise.resolve(permissionDenied()));

        vi.stubGlobal('fetch', fetchMock);

        const { result } = renderHook(() => useCmsToken(), { wrapper });

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(fetchMock).toHaveBeenCalledTimes(4);
        expect(useErrorNotificationStore.getState().errors).toEqual([{
            id: expect.any(String),
            messageKey: 'errorNotifications.cmsTokenFailed',
            detailKey: 'errors.cms.PERMISSION',
        }]);
    });

    it('shows no notification when a retry succeeds', async () => {
        vi.stubGlobal('fetch', vi.fn<typeof fetch>()
            .mockResolvedValueOnce(permissionDenied())
            .mockResolvedValueOnce(Response.json(created)));

        const { result } = renderHook(() => useCmsToken(), { wrapper });

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

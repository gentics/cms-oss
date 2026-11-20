import { QueryClient } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { taskFixture } from '@/test/cmsTasks';
import { createWrapper } from '@/test/renderWithProviders';

import { cmsKeys } from './useCmsQueries';
import { useTaskAction } from './useCmsTasks';

const task = taskFixture({ id: '5', actions: ['complete'] });

describe('useTaskAction', () => {
    beforeEach(() => {
        useErrorNotificationStore.setState({ errors: [] });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('sets the task done and marks the task list stale', async () => {
        vi.stubGlobal('fetch', vi.fn<typeof fetch>(async () => Response.json({ task: { ...task, status: 'done' }, responseInfo: { responseCode: 'OK' } })));
        const queryClient = new QueryClient();

        queryClient.setQueryData(cmsKeys.tasks(), [task]);
        const { result } = renderHook(() => useTaskAction(), { wrapper: createWrapper(queryClient) });

        result.current.mutate({ task, action: 'complete' });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(queryClient.getQueryState(cmsKeys.tasks())?.isInvalidated).toBe(true);
    });

    it('reports a refused action as an error notification', async () => {
        vi.stubGlobal('fetch', vi.fn<typeof fetch>(async () => Response.json({ responseInfo: { responseCode: 'PERMISSION' } })));
        const { result } = renderHook(() => useTaskAction(), { wrapper: createWrapper() });

        result.current.mutate({ task, action: 'complete' });

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(useErrorNotificationStore.getState().errors).toEqual([expect.objectContaining({ messageKey: 'tasks.actionFailed' })]);
    });
});

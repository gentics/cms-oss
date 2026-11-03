import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import type { SessionContext } from '@/services/apiService/genaix/types';
import { createWrapper } from '@/test/renderWithProviders';

import { useSessionNodeId } from './useSessionNodeId';

// The session `s-1` with `context`; the folder 42 is in node 5.
function stubSession(context: Partial<SessionContext>) {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const url = new URL(String(input), 'http://app.test');

        if (url.pathname === '/rest/proxy/genaix/sessions/s-1') {
            return Response.json({ id: 's-1', context: { references: [], guidelines: [], connection_ids: [], ...context } });
        }

        if (url.pathname === '/rest/folder/load/42') {
            return Response.json({ folder: { id: 42, name: 'Richtlinien', nodeId: 5 } });
        }

        return Response.json({}, { status: 404 });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

const urls = (fetchMock: ReturnType<typeof stubSession>) => fetchMock.mock.calls.map(([input]) => String(input));

describe('useSessionNodeId', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('is the node of the session\'s context, without asking for its folder', async () => {
        const fetchMock = stubSession({ node_id: 3, folder_id: 42 });
        const { result } = renderHook(() => useSessionNodeId('s-1'), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current).toBe(3));
        expect(urls(fetchMock)).toEqual(['/rest/proxy/genaix/sessions/s-1']);
    });

    it('is the node of the session\'s target folder when the context names no node', async () => {
        const fetchMock = stubSession({ folder_id: 42 });
        const { result } = renderHook(() => useSessionNodeId('s-1'), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current).toBe(5));
        expect(urls(fetchMock)).toEqual(['/rest/proxy/genaix/sessions/s-1', '/rest/folder/load/42']);
    });

    it('is undefined for a session with neither', async () => {
        const fetchMock = stubSession({});
        const { result } = renderHook(() => useSessionNodeId('s-1'), { wrapper: createWrapper() });

        await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
        expect(result.current).toBeUndefined();
    });

    it('asks nothing while not enabled', () => {
        const fetchMock = stubSession({ node_id: 3 });

        renderHook(() => useSessionNodeId('s-1', false), { wrapper: createWrapper() });

        expect(fetchMock).not.toHaveBeenCalled();
    });
});

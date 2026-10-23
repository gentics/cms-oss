import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import { usePlanState, useSessionMessages } from './useGenaixQueries';
import { useSessionEvents } from './useSessionEvents';

const SESSION = 's-1';
const encoder = new TextEncoder();

function frame(seq: number, type: string, payload: Record<string, unknown> = {}) {
    const data = { seq, ts: '2026-10-07T09:00:00Z', session_id: SESSION, type, ...payload };

    return `id: ${seq}\nevent: ${type}\ndata: ${JSON.stringify(data)}\n\n`;
}

function wrapper({ children }: { children: ReactNode }) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

/**
 * Routes `/messages` to an empty history, `/plans/…` to a plan, and `/events` to the next of
 * `eventResponses`; when those are used up, an event stream that stays open until aborted. `push` feeds
 * the last opened stream.
 */
function stubGenaix(eventResponses: (() => Response)[] = []) {
    let controller: ReadableStreamDefaultController<Uint8Array> | undefined;
    const fetchMock = vi.fn<typeof fetch>().mockImplementation((url, init) => {
        if (String(url).includes('/messages')) {
            return Promise.resolve(Response.json({ items: [], next_cursor: null }));
        }

        if (String(url).includes('/plans/')) {
            return Promise.resolve(Response.json({ workflows: [{ id: 'content_create', version: '1.0.0' }], plan: { id: 'pl-1' } }));
        }

        const next = eventResponses.shift();

        if (next) {
            return Promise.resolve(next());
        }

        const body = new ReadableStream<Uint8Array>({
            start(c) {
                controller = c;
            },
        });

        init!.signal!.addEventListener('abort', () => controller?.error(new DOMException('Aborted', 'AbortError')), { once: true });

        return Promise.resolve(new Response(body, { headers: { 'Content-Type': 'text/event-stream' } }));
    });

    vi.stubGlobal('fetch', fetchMock);

    return {
        fetchMock,
        push: (text: string) => controller!.enqueue(encoder.encode(text)),
        calls: (path: string) => fetchMock.mock.calls.filter(([url]) => String(url).includes(path)),
    };
}

function session() {
    return selectSession(SESSION)(useWorkspaceEventStore.getState());
}

const text = (value: string) => ({ type: 'text', format: 'markdown', text: value });

describe('useSessionEvents', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('feeds the stream into the store, and drops a completed message once the history is refetched', async () => {
        const genaix = stubGenaix();

        renderHook(() => {
            useSessionMessages(SESSION);
            useSessionEvents(SESSION);
        }, { wrapper });

        await waitFor(() => expect(session().connection).toBe('open'));
        await waitFor(() => expect(genaix.calls('/messages')).toHaveLength(1));

        genaix.push(
            frame(1, 'message.started', { message_id: 'm-1', role: 'assistant' })
            + frame(2, 'part.started', { message_id: 'm-1', part_index: 0, part: text('') })
            + frame(3, 'part.delta', { message_id: 'm-1', part_index: 0, delta: 'Hi', patch_format: 'append' }),
        );

        await waitFor(() => expect(session().messages).toMatchObject([{ id: 'm-1', parts: [text('Hi')], status: 'streaming' }]));

        genaix.push(frame(4, 'message.completed', { message_id: 'm-1' }));

        await waitFor(() => expect(session().messages).toEqual([]));
        expect(genaix.calls('/messages')).toHaveLength(2);
        expect(session().lastSeq).toBe(4);
    });

    it('reads a plan again when an event changes it, and only that plan', async () => {
        const genaix = stubGenaix();

        renderHook(() => {
            usePlanState(SESSION, 'pl-1');
            useSessionEvents(SESSION);
        }, { wrapper });

        await waitFor(() => expect(session().connection).toBe('open'));
        await waitFor(() => expect(genaix.calls('/plans/pl-1')).toHaveLength(1));

        genaix.push(frame(1, 'check.updated', { plan_id: 'pl-1', check: { id: 'chk-1' } }));
        await waitFor(() => expect(genaix.calls('/plans/pl-1')).toHaveLength(2));

        genaix.push(frame(2, 'publications.updated', { plan_id: 'pl-9', publications: [] }) + frame(3, 'heartbeat'));
        await waitFor(() => expect(session().lastSeq).toBe(3));
        expect(genaix.calls('/plans/pl-1')).toHaveLength(2);
    });

    it('resumes from the stored lastSeq', async () => {
        useWorkspaceEventStore.getState().applyEvent(SESSION, { seq: 12, ts: '', session_id: SESSION, type: 'heartbeat' });

        const genaix = stubGenaix();

        renderHook(() => useSessionEvents(SESSION), { wrapper });

        await waitFor(() => expect(genaix.calls('/events')).toHaveLength(1));
        expect(genaix.calls('/events')[0]![0]).toBe('/rest/proxy/genaix/sessions/s-1/events?after=12');
    });

    it('on 410 events_pruned resets the live state, refetches the history and reconnects from 0', async () => {
        useWorkspaceEventStore.getState().applyEvent(SESSION, { seq: 40, ts: '', session_id: SESSION, type: 'message.started', message_id: 'm-old', role: 'assistant' });

        // One frame, then the stream ends; the reconnect 1 s later is answered with 410.
        const genaix = stubGenaix([
            () => new Response(new ReadableStream({
                start(controller) {
                    controller.enqueue(encoder.encode(frame(41, 'heartbeat')));
                    controller.close();
                },
            })),
            () => Response.json({ type: 't', title: 't', status: 410, genaix_code: 'events_pruned' }, { status: 410 }),
        ]);

        renderHook(() => {
            useSessionMessages(SESSION);
            useSessionEvents(SESSION);
        }, { wrapper });

        await waitFor(() => expect(genaix.calls('/messages')).toHaveLength(1));
        await waitFor(() => expect(genaix.calls('/events')).toHaveLength(3), { timeout: 3000 });

        expect(genaix.calls('/events').map(([url]) => url)).toEqual([
            '/rest/proxy/genaix/sessions/s-1/events?after=40',
            '/rest/proxy/genaix/sessions/s-1/events?after=41',
            '/rest/proxy/genaix/sessions/s-1/events?after=0',
        ]);
        expect(session()).toMatchObject({ lastSeq: 0, messages: [] });
        expect(genaix.calls('/messages')).toHaveLength(2);
    });

    it('closes the stream on unmount and connects nothing without a session id', async () => {
        const genaix = stubGenaix();

        const { unmount } = renderHook(() => useSessionEvents(SESSION), { wrapper });

        await waitFor(() => expect(session().connection).toBe('open'));
        unmount();
        await waitFor(() => expect(session().connection).toBe('closed'));

        renderHook(() => useSessionEvents(undefined), { wrapper });
        expect(genaix.calls('/events')).toHaveLength(1);
    });
});

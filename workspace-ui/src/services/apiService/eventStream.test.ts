import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { GenaixEvent } from '@/services/apiService/genaix/types';

import { connectSessionEvents, type EventStreamStatus, HEARTBEAT_TIMEOUT_MS, parseSse, type SseFrame } from './eventStream';

const encoder = new TextEncoder();

function bodyOf(...chunks: string[]) {
    return new ReadableStream<Uint8Array>({
        start(controller) {
            chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)));
            controller.close();
        },
    });
}

async function collect(body: ReadableStream<Uint8Array>): Promise<SseFrame[]> {
    const frames: SseFrame[] = [];

    for await (const frame of parseSse(body)) {
        frames.push(frame);
    }

    return frames;
}

function frame(seq: number, type = 'heartbeat') {
    return `id: ${seq}\nevent: ${type}\ndata: ${JSON.stringify({ seq, ts: '2026-10-07T09:00:00Z', session_id: 's-1', type })}\n\n`;
}

describe('parseSse', () => {
    it('joins data lines, ignores comments and retry, defaults the event name', async () => {
        const frames = await collect(bodyOf(': a comment\nretry: 1000\nid: 7\ndata: first\ndata: second\n\nevent: status\ndata:no space\n\n'));

        expect(frames).toEqual([
            { id: '7', event: 'message', data: 'first\nsecond' },
            // The last id stays in effect for later frames (WHATWG "last event ID").
            { id: '7', event: 'status', data: 'no space' },
        ]);
    });

    it('handles frames split across chunks, including a \\r\\n split in two', async () => {
        const frames = await collect(bodyOf('id: 1\r', '\nevent: heart', 'beat\r\ndata: {"seq"', ':1}\r\n\r', '\n'));

        expect(frames).toEqual([{ id: '1', event: 'heartbeat', data: '{"seq":1}' }]);
    });

    it('does not dispatch a frame without data, nor an unterminated last frame', async () => {
        const frames = await collect(bodyOf('event: nothing\n\ndata: complete\n\ndata: cut off'));

        expect(frames).toEqual([{ id: undefined, event: 'message', data: 'complete' }]);
    });

    it('cancels the body when the consumer stops early', async () => {
        const cancel = vi.fn();
        const body = new ReadableStream<Uint8Array>({
            start(controller) {
                controller.enqueue(encoder.encode('data: one\n\ndata: two\n\n'));
            },
            cancel,
        });

        for await (const _frame of parseSse(body)) {
            break;
        }

        expect(cancel).toHaveBeenCalled();
    });
});

/** An event-stream response the test feeds frame by frame; aborting the request errors it, like fetch. */
function openStream() {
    let controller!: ReadableStreamDefaultController<Uint8Array>;
    const body = new ReadableStream<Uint8Array>({
        start(c) {
            controller = c;
        },
    });

    return {
        respond(signal: AbortSignal) {
            signal.addEventListener('abort', () => controller.error(new DOMException('Aborted', 'AbortError')), { once: true });

            return new Response(body, { headers: { 'Content-Type': 'text/event-stream' } });
        },
        push: (text: string) => controller.enqueue(encoder.encode(text)),
        close: () => controller.close(),
    };
}

type Responder = (signal: AbortSignal) => Response | Promise<Response>;

function stubFetch(...responders: Responder[]) {
    const fetchMock = vi.fn<typeof fetch>().mockImplementation((_url, init) => {
        const next = responders.shift();

        // Once the planned responses are used up, the next connection hangs until it is aborted.
        return Promise.resolve(next ? next(init!.signal!) : openStream().respond(init!.signal!));
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

function problem(status: number, genaixCode: string, extra: Record<string, unknown> = {}): Responder {
    return () => Response.json({ type: 't', title: 't', status, genaix_code: genaixCode, ...extra }, { status });
}

function requestOf(fetchMock: ReturnType<typeof stubFetch>, call: number) {
    const [url, init] = fetchMock.mock.calls[call]!;

    return { url, lastEventId: new Headers(init?.headers).get('Last-Event-ID') };
}

describe('connectSessionEvents', () => {
    let controller: AbortController;
    let events: GenaixEvent[];
    let statuses: EventStreamStatus[];
    let onPruned: ReturnType<typeof vi.fn<() => void>>;
    let done: Promise<void>;

    function connect(lastSeq = 0) {
        done = connectSessionEvents({
            sessionId: 's-1',
            lastSeq,
            signal: controller.signal,
            onEvent: (event) => events.push(event),
            onPruned,
            onStatus: (status) => statuses.push(status),
        });
    }

    beforeEach(() => {
        vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
        controller = new AbortController();
        events = [];
        statuses = [];
        onPruned = vi.fn<() => void>();
    });

    afterEach(async () => {
        controller.abort();
        await done;
        vi.useRealTimers();
        vi.unstubAllGlobals();
    });

    it('resumes after lastSeq with Last-Event-ID and after, and skips events it has passed on', async () => {
        const stream = openStream();
        const fetchMock = stubFetch(stream.respond);

        connect(5);
        await vi.advanceTimersByTimeAsync(0);

        expect(requestOf(fetchMock, 0)).toEqual({ url: '/rest/proxy/genaix/sessions/s-1/events?after=5', lastEventId: '5' });

        stream.push(frame(5) + frame(6, 'run.started') + frame(6) + frame(7));
        await vi.advanceTimersByTimeAsync(0);

        expect(events.map((event) => event.seq)).toEqual([6, 7]);
        expect(events[0]!.type).toBe('run.started');
        expect(statuses).toEqual(['connecting', 'open']);
    });

    it('sends no Last-Event-ID for a fresh session', async () => {
        const fetchMock = stubFetch();

        connect();
        await vi.advanceTimersByTimeAsync(0);

        expect(requestOf(fetchMock, 0)).toEqual({ url: '/rest/proxy/genaix/sessions/s-1/events?after=0', lastEventId: null });
    });

    it('reconnects 1 s after the stream ends, from the last seq seen', async () => {
        const stream = openStream();
        const fetchMock = stubFetch(stream.respond);

        connect();
        await vi.advanceTimersByTimeAsync(0);
        stream.push(frame(1) + frame(2));
        stream.close();
        await vi.advanceTimersByTimeAsync(999);

        expect(fetchMock).toHaveBeenCalledTimes(1);

        await vi.advanceTimersByTimeAsync(1);

        expect(fetchMock).toHaveBeenCalledTimes(2);
        expect(requestOf(fetchMock, 1)).toEqual({ url: '/rest/proxy/genaix/sessions/s-1/events?after=2', lastEventId: '2' });
        expect(statuses).toEqual(['connecting', 'open', 'reconnecting', 'open']);
    });

    it('backs off 1 s, 2 s, 4 s while the connection keeps failing, and resets after a frame', async () => {
        const networkError = () => Promise.reject(new TypeError('Failed to fetch'));
        const stream = openStream();
        const fetchMock = stubFetch(networkError, networkError, networkError, stream.respond, networkError);

        connect();
        await vi.advanceTimersByTimeAsync(0);
        expect(fetchMock).toHaveBeenCalledTimes(1);

        await vi.advanceTimersByTimeAsync(1000);
        expect(fetchMock).toHaveBeenCalledTimes(2);

        await vi.advanceTimersByTimeAsync(1999);
        expect(fetchMock).toHaveBeenCalledTimes(2);
        await vi.advanceTimersByTimeAsync(1);
        expect(fetchMock).toHaveBeenCalledTimes(3);

        await vi.advanceTimersByTimeAsync(4000);
        expect(fetchMock).toHaveBeenCalledTimes(4);

        stream.push(frame(1));
        stream.close();
        await vi.advanceTimersByTimeAsync(1000);
        expect(fetchMock).toHaveBeenCalledTimes(5);
    });

    it('waits retry_after_seconds after a 429', async () => {
        const fetchMock = stubFetch(problem(429, 'rate_limited', { retry_after_seconds: 5 }));

        connect();
        await vi.advanceTimersByTimeAsync(4999);
        expect(fetchMock).toHaveBeenCalledTimes(1);

        await vi.advanceTimersByTimeAsync(1);
        expect(fetchMock).toHaveBeenCalledTimes(2);
    });

    it('reconnects after 45 s without any frame', async () => {
        const fetchMock = stubFetch(openStream().respond);

        connect();
        await vi.advanceTimersByTimeAsync(HEARTBEAT_TIMEOUT_MS - 1);
        expect(fetchMock).toHaveBeenCalledTimes(1);

        await vi.advanceTimersByTimeAsync(1 + 1000);
        expect(fetchMock).toHaveBeenCalledTimes(2);
        expect(statuses).toContain('reconnecting');
    });

    it('keeps the connection while heartbeats arrive', async () => {
        const stream = openStream();
        const fetchMock = stubFetch(stream.respond);

        connect();
        await vi.advanceTimersByTimeAsync(30_000);
        stream.push(frame(1));
        await vi.advanceTimersByTimeAsync(30_000);
        stream.push(frame(2));
        await vi.advanceTimersByTimeAsync(30_000);

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(events.map((event) => event.type)).toEqual(['heartbeat', 'heartbeat']);
    });

    it('on 410 events_pruned re-hydrates and starts again from after=0 at once', async () => {
        const stream = openStream();
        const fetchMock = stubFetch(problem(410, 'events_pruned'), stream.respond);

        connect(40);
        await vi.advanceTimersByTimeAsync(0);

        expect(onPruned).toHaveBeenCalledTimes(1);
        expect(fetchMock).toHaveBeenCalledTimes(2);
        expect(requestOf(fetchMock, 1)).toEqual({ url: '/rest/proxy/genaix/sessions/s-1/events?after=0', lastEventId: null });

        stream.push(frame(1));
        await vi.advanceTimersByTimeAsync(0);
        expect(events.map((event) => event.seq)).toEqual([1]);
    });

    it.each([
        [401, 'invalid_installation_token'],
        [403, 'session_forbidden'],
        [404, 'session_not_found'],
    ])('gives up on %i %s', async (status, code) => {
        const fetchMock = stubFetch(problem(status, code));

        connect();
        await done;
        await vi.advanceTimersByTimeAsync(60_000);

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(statuses).toEqual(['connecting', 'failed']);
    });

    it('retries a 503', async () => {
        const fetchMock = stubFetch(problem(503, 'service_unavailable'));

        connect();
        await vi.advanceTimersByTimeAsync(1000);

        expect(fetchMock).toHaveBeenCalledTimes(2);
    });

    it('skips a frame whose data is not JSON and goes on', async () => {
        const stream = openStream();

        stubFetch(stream.respond);
        connect();
        await vi.advanceTimersByTimeAsync(0);
        stream.push('data: not json\n\n' + frame(1));
        await vi.advanceTimersByTimeAsync(0);

        expect(events.map((event) => event.seq)).toEqual([1]);
    });

    it('stops for good when the signal aborts, also while waiting to reconnect', async () => {
        const fetchMock = stubFetch(() => Promise.reject(new TypeError('Failed to fetch')));

        connect();
        await vi.advanceTimersByTimeAsync(500);
        controller.abort();
        await done;
        await vi.advanceTimersByTimeAsync(60_000);

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(statuses.at(-1)).toBe('closed');
    });

    it('stops an open stream when the signal aborts', async () => {
        const fetchMock = stubFetch(openStream().respond);

        connect();
        await vi.advanceTimersByTimeAsync(0);
        controller.abort();
        await done;

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(statuses).toEqual(['connecting', 'open', 'closed']);
    });
});

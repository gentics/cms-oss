import {
    GENAIX_API_BASE,
    GenaixApiError,
    genaixRetryDelay,
    toGenaixApiError,
} from '@/services/apiService/apiService';
import type { GenaixEvent } from '@/services/apiService/genaix/types';

/** GenAIx sends a `heartbeat` after 15 s of silence; three missed ones mean the connection is dead. */
export const HEARTBEAT_TIMEOUT_MS = 45_000;

/** One Server-Sent Events frame. */
export interface SseFrame {
    id?: string;
    event: string;
    data: string;
}

/**
 * Splits a `text/event-stream` body into frames (WHATWG HTML, "Server-sent events"): `data` lines
 * are joined with `\n`, comment lines (`:`) and `retry` are ignored, and a frame without data is
 * not dispatched. Cancels the body when the consumer stops early.
 */
export async function* parseSse(body: ReadableStream<Uint8Array>): AsyncGenerator<SseFrame> {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let data: string[] = [];
    let event = '';
    let id: string | undefined;

    function* processLine(line: string): Generator<SseFrame> {
        if (line === '') {
            if (data.length > 0) {
                yield { id, event: event || 'message', data: data.join('\n') };
            }

            data = [];
            event = '';

            return;
        }

        if (line.startsWith(':')) {
            return;
        }

        const colon = line.indexOf(':');
        const field = colon === -1 ? line : line.slice(0, colon);
        let value = colon === -1 ? '' : line.slice(colon + 1);

        if (value.startsWith(' ')) {
            value = value.slice(1);
        }

        if (field === 'data') {
            data.push(value);
        } else if (field === 'event') {
            event = value;
        } else if (field === 'id' && !value.includes('\0')) {
            id = value;
        }
    }

    try {
        for (;;) {
            const { done, value } = await reader.read();

            buffer += done ? decoder.decode() : decoder.decode(value, { stream: true });

            // A trailing `\r` may be the first half of `\r\n`, so it waits for the next chunk.
            const lines = buffer.split(/\r\n|\r(?!$)|\n/);

            buffer = done ? '' : lines.pop()!;

            for (const line of lines) {
                yield* processLine(line);
            }

            if (done) {
                return;
            }
        }
    } finally {
        reader.cancel().catch(() => undefined);
    }
}

export type EventStreamStatus = 'connecting' | 'open' | 'reconnecting' | 'failed' | 'closed';

export interface SessionEventStreamOptions {
    sessionId: string;
    /** The highest `seq` already applied; `0` replays the whole session. */
    lastSeq?: number;
    onEvent: (event: GenaixEvent) => void;
    /**
     * `410 events_pruned`: the replay window has moved past `lastSeq`. Re-hydrate here; the stream
     * then reconnects with `after=0`.
     */
    onPruned: () => Promise<void> | void;
    onStatus?: (status: EventStreamStatus, error?: unknown) => void;
    signal: AbortSignal;
}

function isGenaixEvent(value: unknown): value is GenaixEvent {
    return typeof value === 'object' && value !== null
        && typeof (value as { seq?: unknown }).seq === 'number'
        && typeof (value as { type?: unknown }).type === 'string';
}

function wait(ms: number, signal: AbortSignal): Promise<void> {
    return new Promise((resolve) => {
        const timer = setTimeout(done, ms);

        function done() {
            clearTimeout(timer);
            signal.removeEventListener('abort', done);
            resolve();
        }

        signal.addEventListener('abort', done, { once: true });
    });
}

/**
 * Follows `GET /sessions/{session_id}/events` until `signal` aborts or the server refuses for good.
 *
 * - Resumes after the last applied `seq`, as `Last-Event-ID` and `after` (contract §6.1), and skips
 *   events it has already passed on.
 * - Reconnects when the stream ends or breaks, after 1 s, 2 s, 4 s … 30 s (or `Retry-After`); the
 *   delay resets once a frame arrives.
 * - Treats 45 s without any frame (heartbeats included) as a dead connection and reconnects.
 * - On `410 events_pruned` calls `onPruned`, then starts again from `after=0`.
 * - Gives up on any other 4xx except 429 (`onStatus('failed', error)`), for example a session that
 *   is unknown or belongs to someone else.
 *
 * Uses fetch rather than `EventSource`, which cannot expose the status of a refused connection.
 */
export async function connectSessionEvents(options: SessionEventStreamOptions): Promise<void> {
    const { sessionId, onEvent, onPruned, onStatus, signal } = options;
    let lastSeq = options.lastSeq ?? 0;
    let attempt = 0;

    onStatus?.('connecting');

    while (!signal.aborted) {
        const connection = new AbortController();
        const abortConnection = () => connection.abort();
        let watchdog: ReturnType<typeof setTimeout> | undefined;
        let lastError: unknown;

        const resetWatchdog = () => {
            clearTimeout(watchdog);
            watchdog = setTimeout(abortConnection, HEARTBEAT_TIMEOUT_MS);
        };

        signal.addEventListener('abort', abortConnection, { once: true });

        try {
            resetWatchdog();

            const url = `${GENAIX_API_BASE}/sessions/${encodeURIComponent(sessionId)}/events?after=${lastSeq}`;
            const headers = new Headers({ Accept: 'text/event-stream' });

            if (lastSeq > 0) {
                headers.set('Last-Event-ID', String(lastSeq));
            }

            const response = await fetch(url, {
                headers,
                credentials: 'same-origin',
                cache: 'no-store',
                signal: connection.signal,
            });

            if (!response.ok) {
                const error = await toGenaixApiError(url, response);

                if (error.genaixCode === 'events_pruned') {
                    await onPruned();
                    lastSeq = 0;
                    attempt = 0;
                    continue;
                }

                if (error.status >= 400 && error.status < 500 && error.status !== 429) {
                    onStatus?.('failed', error);

                    return;
                }

                throw error;
            }

            if (!response.body) {
                throw new TypeError('The event stream response has no body');
            }

            onStatus?.('open');

            for await (const frame of parseSse(response.body)) {
                resetWatchdog();
                attempt = 0;

                let event: unknown;

                try {
                    event = JSON.parse(frame.data);
                } catch {
                    // Not an event payload; the server never sends one, so skip it rather than fail.
                    continue;
                }

                if (isGenaixEvent(event) && event.seq > lastSeq) {
                    lastSeq = event.seq;
                    onEvent(event);
                }
            }
        } catch (error) {
            lastError = error;
        } finally {
            clearTimeout(watchdog);
            signal.removeEventListener('abort', abortConnection);
        }

        if (signal.aborted) {
            break;
        }

        onStatus?.('reconnecting', lastError);
        await wait(genaixRetryDelay(attempt, lastError instanceof GenaixApiError ? lastError : undefined), signal);
        attempt += 1;
    }

    onStatus?.('closed');
}

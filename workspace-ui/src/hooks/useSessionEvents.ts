import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';

import { reloadPlans } from '@/helper/planReload/planReload';
import { genaixKeys } from '@/hooks/useGenaixQueries';
import { getSession } from '@/services/apiService/apiService';
import { connectSessionEvents, replaySessionEvents } from '@/services/apiService/eventStream';
import type { GenaixEvent } from '@/services/apiService/genaix/types';
import { persistedMessageKeys, selectSession, type SessionLiveState, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

// A session this tab holds no live state of and waits for no run of: opening it replays the whole session.
function isUnloaded(session: SessionLiveState): boolean {
    return session.lastSeq === 0 && !session.awaitedRunIds?.length;
}

/**
 * Follows the event stream of `sessionId` while mounted and applies it to `useWorkspaceEventStore`.
 * When a message completes, the session's message history is refetched and the live copies it now
 * contains are dropped. On `410 events_pruned` the live state is reset, and the history and the plans
 * (`reloadPlans`) are read again before the stream starts again from `after=0`.
 *
 * Every call opens its own stream, so call it once per session, from the chat view. Other
 * components read the live state from `useWorkspaceEventStore` with `selectSession`.
 *
 * `onEvent` gets every event as parsed from the stream, after it was applied. A new callback on
 * each render does not reconnect.
 *
 * With `loadFirst`, a session this tab has no live state of is loaded before it is followed, so the chat
 * shows it whole instead of replaying it: when `GET /sessions/{session_id}` reports no active run, the
 * history is read with `follow=false`, which ends right after the replay (contract §6.1), applied in one
 * go, and only then followed live after its last `seq`. While a run is active the end of the replay is
 * not known, so the stream is followed at once, as without the option; likewise when either read fails.
 * Returns whether the session is being loaded so.
 */
export function useSessionEvents(sessionId: string | undefined, onEvent?: (event: GenaixEvent) => void, { loadFirst = false }: { loadFirst?: boolean } = {}): boolean {
    const queryClient = useQueryClient();
    const onEventRef = useRef(onEvent);
    // The session whose loading has ended, either way.
    const [loadedSessionId, setLoadedSessionId] = useState<string>();
    const isUnloadedSession = useWorkspaceEventStore((state) => Boolean(sessionId) && isUnloaded(selectSession(sessionId ?? '')(state)));

    useEffect(() => {
        onEventRef.current = onEvent;
    });

    useEffect(() => {
        if (!sessionId) {
            return;
        }

        const controller = new AbortController();
        const store = useWorkspaceEventStore.getState;
        const messagesKey = genaixKeys.messages(sessionId);
        const toClear = new Set<string>();
        let refetching = false;

        // Refetches the history and, if that worked, drops the live copies it contains. Calls that
        // arrive meanwhile (a replay completes many messages at once) share one more refetch.
        async function refetchAndClear() {
            if (refetching) {
                return;
            }

            refetching = true;

            while (toClear.size > 0 && !controller.signal.aborted) {
                const keys = [...toClear];

                toClear.clear();
                await queryClient.invalidateQueries({ queryKey: messagesKey });

                if (queryClient.getQueryState(messagesKey)?.status !== 'error') {
                    store().clearMessages(sessionId!, keys);
                }
            }

            refetching = false;
        }

        // The events that came since the last flush. A replay (`after=0` for a session this tab has no
        // live state of) brings the whole session at once: applied one by one, the chat would render
        // each step of it, the history partly replaced by half-replayed messages and runs that ended
        // long ago thinking again, and the page would hang meanwhile. So everything that arrives before
        // the next frame is applied together then, and React renders once. A tab in the background
        // draws no frames: its events wait until it is shown again.
        const pending: GenaixEvent[] = [];

        function flush() {
            const events = pending.splice(0);

            if (controller.signal.aborted) {
                return;
            }

            events.forEach((event) => store().applyEvent(sessionId!, event));
            events.forEach((event) => onEventRef.current?.(event));

            if (events.some((event) => event.type === 'message.completed')) {
                persistedMessageKeys(selectSession(sessionId!)(store())).forEach((key) => toClear.add(key));
                void refetchAndClear();
            }
        }

        function follow() {
            void connectSessionEvents({
                sessionId: sessionId!,
                lastSeq: selectSession(sessionId!)(store()).lastSeq,
                signal: controller.signal,
                onStatus: (status) => store().setConnection(sessionId!, status),
                onEvent: (event) => {
                    pending.push(event);

                if (event.type === 'message.completed') {
                    persistedMessageKeys(selectSession(sessionId)(store())).forEach((key) => toClear.add(key));
                    void refetchAndClear();
                }
            },
            onPruned: async () => {
                store().resetSession(sessionId);
                // The plans are not in the history; without them the stream would bring back only the
                // plans whose events are still kept. A failed read leaves those (contract, "Retention").
                await Promise.all([
                    queryClient.invalidateQueries({ queryKey: messagesKey }),
                    reloadPlans(sessionId).catch((error: unknown) => console.warn('Reading the plans again failed', error)),
                ]);
            },
        });

        return () => controller.abort();
    }, [sessionId, queryClient, loadFirst]);

    return loadFirst && isUnloadedSession && loadedSessionId !== sessionId;
}

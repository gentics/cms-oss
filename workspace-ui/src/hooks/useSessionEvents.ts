import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';

import { reloadPlans } from '@/helper/planReload/planReload';
import { genaixKeys } from '@/hooks/useGenaixQueries';
import { connectSessionEvents } from '@/services/apiService/eventStream';
import type { GenaixEvent } from '@/services/apiService/genaix/types';
import { persistedMessageKeys, selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

// The events that change a plan's state (`PlanState`): its plan, checks, publications, approval
// chain and previews. Gate results arrive with no event of their own; a gate-backed check stands in.
const PLAN_EVENTS = new Set<string>(['plan.updated', 'check.updated', 'publications.updated', 'approval_chain.updated', 'preview.updated']);

/**
 * Follows the event stream of `sessionId` while mounted and applies it to `useWorkspaceEventStore`.
 * When a message completes, the session's message history is refetched and the live copies it now
 * contains are dropped. On `410 events_pruned` the live state is reset, and the history and the plans
 * (`reloadPlans`) are read again before the stream starts again from `after=0`.
 *
 * Every call opens its own stream, so call it once per session, from the chat view. Other
 * components read the live state from `useWorkspaceEventStore` with `selectSession`.
 *
 * Events that change a plan refetch its full state (`genaixKeys.plan`, the review page's `usePlanState`).
 *
 * `onEvent` gets every event as parsed from the stream, after it was applied. A new callback on
 * each render does not reconnect.
 */
export function useSessionEvents(sessionId: string | undefined, onEvent?: (event: GenaixEvent) => void) {
    const queryClient = useQueryClient();
    const onEventRef = useRef(onEvent);

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

        void connectSessionEvents({
            sessionId,
            lastSeq: selectSession(sessionId)(store()).lastSeq,
            signal: controller.signal,
            onStatus: (status) => store().setConnection(sessionId, status),
            onEvent: (event) => {
                store().applyEvent(sessionId, event);
                onEventRef.current?.(event);

                if (event.type === 'message.completed') {
                    persistedMessageKeys(selectSession(sessionId)(store())).forEach((key) => toClear.add(key));
                    void refetchAndClear();
                }

                // A plan's state (`GET …/plans/{plan_id}`) is read again when an event changes it;
                // a refetch already running is kept, so a burst of events shares it.
                if (event.plan_id && PLAN_EVENTS.has(event.type)) {
                    void queryClient.invalidateQueries({ queryKey: genaixKeys.plan(sessionId, event.plan_id) }, { cancelRefetch: false });
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
    }, [sessionId, queryClient]);
}

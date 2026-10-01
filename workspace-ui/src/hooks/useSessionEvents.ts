import { useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';

import { genaixKeys } from '@/hooks/useGenaixQueries';
import { connectSessionEvents } from '@/services/apiService/eventStream';
import { persistedMessageKeys, selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

/**
 * Follows the event stream of `sessionId` while mounted and applies it to `useWorkspaceEventStore`.
 * When a message completes, the session's message history is refetched and the live copies it now
 * contains are dropped. On `410 events_pruned` the live state is reset and the history refetched
 * before the stream starts again from `after=0`.
 *
 * Every call opens its own stream, so call it once per session, from the chat view. Other
 * components read the live state from `useWorkspaceEventStore` with `selectSession`.
 */
export function useSessionEvents(sessionId: string | undefined) {
    const queryClient = useQueryClient();

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

                if (event.type === 'message.completed') {
                    persistedMessageKeys(selectSession(sessionId)(store())).forEach((key) => toClear.add(key));
                    void refetchAndClear();
                }
            },
            onPruned: async () => {
                store().resetSession(sessionId);
                await queryClient.invalidateQueries({ queryKey: messagesKey });
            },
        });

        return () => controller.abort();
    }, [sessionId, queryClient]);
}

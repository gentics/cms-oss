import { useParams } from '@tanstack/react-router';
import { useRef, useState } from 'react';

import { AppShell } from '@/components/AppShell/AppShell';
import { ChatStream } from '@/components/ChatStream/ChatStream';
import { Composer, type ComposerHandle } from '@/components/Composer/Composer';
import { HistoryColumn } from '@/components/HistoryColumn/HistoryColumn';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { type SendTurnInput, type StartSessionInput, useCancelRun, useSendTurn } from '@/hooks/useGenaixQueries';
import { useSessionEvents } from '@/hooks/useSessionEvents';
import type { InteractionAnswer, UserMessagePart } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { useInteractionDraftStore } from '@/store/useInteractionDraftStore';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import styles from './SessionPage.module.css';

// The written text of a turn, as the answer to an `ask_user` question that the card holds none for.
function textAnswer(parts: UserMessagePart[]): InteractionAnswer | undefined {
    const text = parts.map((part) => (part.type === 'text' || part.type === 'verbatim' ? part.text : '')).join('').trim();

    return text ? { text } : undefined;
}

/**
 * The chat composer of one session. Enter or send posts the turn (`useSendTurn`); on success the
 * field is emptied, on failure an error is shown and the input stays. `SessionPage` keys it by the
 * session, so going on to another session from the left column starts with an empty composer.
 * While the session's run is working, Stop cancels it (`POST …/runs/{run_id}/cancel`). While it waits
 * for an answer, a turn answers the pending interaction (`reply_to_interaction`) with what its card
 * holds, or for `ask_user` with the turn's text; with nothing to answer, it asks for the card first.
 */
function SessionComposer({ sessionId }: { sessionId: string }) {
    const composerRef = useRef<ComposerHandle>(null);
    const sendTurn = useSendTurn(sessionId);
    const cancelRun = useCancelRun(sessionId);
    const run = useWorkspaceEventStore((state) => selectSession(sessionId)(state).run);
    const pending = useWorkspaceEventStore((state) => selectSession(sessionId)(state).interactions?.at(-1));
    const isWaiting = run?.status === 'waiting_for_input' && Boolean(pending);
    // Upload progress of the files sent, by their position, for the composer's attachment list.
    const [uploadProgress, setUploadProgress] = useState<number[]>([]);

    function reportUploadProgress(index: number, fraction: number) {
        setUploadProgress((current) => {
            const next = [...current];

            next[index] = fraction;

            return next;
        });
    }

    function handleSubmit(input: StartSessionInput) {
        let replyTo: SendTurnInput['replyTo'];

        if (isWaiting && pending) {
            const answer = useInteractionDraftStore.getState().drafts[pending.id] ?? (pending.kind === 'ask_user' ? textAnswer(input.parts) : undefined);

            if (!answer) {
                useErrorNotificationStore.getState().addError({ messageKey: 'chat.interaction.answerFirst' });

                return;
            }

            replyTo = { interaction_id: pending.id, answer };
        }

        setUploadProgress([]);
        // The promise, not `mutate` callbacks: those are dropped once the user has gone on to another
        // session, and a failed send would go unreported.
        sendTurn.mutateAsync({ ...input, replyTo, onFileProgress: reportUploadProgress }).then(
            () => composerRef.current?.reset(),
            (error: unknown) => {
                useErrorNotificationStore.getState().addError({ messageKey: 'chat.sendFailed', detailKey: errorMessageKey(error) });
            },
        );
    }

    function handleStop() {
        if (!run) {
            return;
        }

        cancelRun.mutate(run.id, {
            onError: (error) => {
                useErrorNotificationStore.getState().addError({ messageKey: 'chat.stopFailed', detailKey: errorMessageKey(error) });
            },
        });
    }

    return (
        <Composer
            ref={composerRef}
            variant="chat"
            onSubmit={handleSubmit}
            isSubmitting={sendTurn.isPending}
            uploadProgress={uploadProgress}
            sessionId={sessionId}
            isRunning={Boolean(run) && !isWaiting}
            isStopping={run?.status === 'cancelling' || cancelRun.isPending}
            onStop={handleStop}
        />
    );
}

/**
 * The page of the session routes (`/sessions/$id`, `/sessions/$id/review`): the workspace with the
 * session's conversation in the middle column and its chat composer at the bottom. It follows the
 * session's event stream, so sent turns and replies land in `useWorkspaceEventStore`. The left column
 * is the searchable session history; the preview (right) comes later.
 */
export function SessionPage() {
    const { id } = useParams({ strict: false });

    // The chat view is the one consumer of the stream (other components read the store).
    useSessionEvents(id);

    // The composer floats over the bottom of the middle column, the column scrolls underneath (draft
    // `.composerwrap`). Both routes have an `id`; the composer that sends renders only with one.
    const chat = (
        <div className={styles.chat}>
            {id && <ChatStream key={id} sessionId={id} />}
            {id && (
                <div className={styles.dock}>
                    <SessionComposer key={id} sessionId={id} />
                </div>
            )}
        </div>
    );

    // A session starts without a preview; the right column appears once GenAIx sends one
    // (`preview.updated`, not wired up yet).
    return <AppShell left={<HistoryColumn />} center={chat} right={null} isRightColumnVisible={false} />;
}

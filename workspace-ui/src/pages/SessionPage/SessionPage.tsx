import { useNavigate, useParams } from '@tanstack/react-router';
import { MessageSquarePlusIcon, XIcon } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { AppShell } from '@/components/AppShell/AppShell';
import { ChatStream } from '@/components/ChatStream/ChatStream';
import { Composer, type ComposerHandle } from '@/components/Composer/Composer';
import { HistoryColumn } from '@/components/HistoryColumn/HistoryColumn';
import { IconButton } from '@/components/IconButton/IconButton';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { type SendTurnInput, type StartSessionInput, useCancelRun, useHandOffSession, useSendTurn } from '@/hooks/useGenaixQueries';
import { useSessionEvents } from '@/hooks/useSessionEvents';
import type { ContextReference, InteractionAnswer, UserMessagePart } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { useHandOffStore } from '@/store/useHandOffStore';
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
 *
 * "Edit these" in a result card (`useHandOffStore`) puts the selected objects into the field, in
 * place of any handed over before, and says so above it: the next message then starts a new session
 * with the objects in the field (`useHandOffSession`) and opens it. The notice's X turns that off and
 * takes the objects out again; with no object left in the field it is off as well.
 */
function SessionComposer({ sessionId }: { sessionId: string }) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const composerRef = useRef<ComposerHandle>(null);
    const sendTurn = useSendTurn(sessionId);
    const handOffSession = useHandOffSession();
    const cancelRun = useCancelRun(sessionId);
    const run = useWorkspaceEventStore((state) => selectSession(sessionId)(state).run);
    const pending = useWorkspaceEventStore((state) => selectSession(sessionId)(state).interactions?.at(-1));
    // What "Edit these" handed over, while the next message starts a new session.
    const handOff = useHandOffStore((state) => state.handOffs[sessionId]);
    const [referenceCount, setReferenceCount] = useState(0);
    const isWaiting = !handOff && run?.status === 'waiting_for_input' && Boolean(pending);
    // Upload progress of the files sent, by their position, for the composer's attachment list.
    const [uploadProgress, setUploadProgress] = useState<number[]>([]);

    function reportUploadProgress(index: number, fraction: number) {
        setUploadProgress((current) => {
            const next = [...current];

            next[index] = fraction;

            return next;
        });
    }

    // Takes the objects of "Edit these" over into the field, in place of those handed over before.
    useEffect(() => {
        if (!handOff || handOff.isInField) {
            return;
        }

        composerRef.current?.removeReferences(handOff.replaces);
        composerRef.current?.addReferences(handOff.references);
        useHandOffStore.getState().markInField(sessionId);
    }, [handOff, sessionId]);

    const handleReferencesChange = useCallback((references: ContextReference[]) => {
        setReferenceCount(references.length);

        // Nothing left to hand off; also a composer that opens empty again ends one it had.
        if (references.length === 0 && useHandOffStore.getState().handOffs[sessionId]?.isInField) {
            useHandOffStore.getState().end(sessionId);
        }
    }, [sessionId]);

    function cancelHandOff() {
        if (handOff) {
            composerRef.current?.removeReferences(handOff.references);
        }

        useHandOffStore.getState().end(sessionId);
    }

    function startHandOff(input: StartSessionInput, nodeId: number | undefined) {
        // The promise, not `mutate` callbacks, as for a turn below.
        handOffSession.mutateAsync({ ...input, nodeId, onFileProgress: reportUploadProgress }).then(
            (session) => {
                composerRef.current?.reset();
                useHandOffStore.getState().end(sessionId);
                void navigate({ to: '/sessions/$id', params: { id: session.id } });
            },
            (error: unknown) => {
                useErrorNotificationStore.getState().addError({ messageKey: 'handOff.startFailed', detailKey: errorMessageKey(error) });
            },
        );
    }

    function handleSubmit(input: StartSessionInput) {
        if (handOff) {
            setUploadProgress([]);
            startHandOff(input, handOff.nodeId);

            return;
        }

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
            isSubmitting={sendTurn.isPending || handOffSession.isPending}
            uploadProgress={uploadProgress}
            // Files are per session: the new one cannot refer to this one's uploads (`SessionCreate`).
            sessionId={handOff ? undefined : sessionId}
            onReferencesChange={handleReferencesChange}
            notice={handOff && (
                <>
                    <MessageSquarePlusIcon size={14} className={styles.noticeIcon} aria-hidden />
                    <span className={styles.noticeText}>{t('handOff.notice', { count: referenceCount })}</span>
                    <IconButton variant="ghost" size="icon" label={t('handOff.cancel')} onClick={cancelHandOff}>
                        <XIcon size={14} />
                    </IconButton>
                </>
            )}
            // A new session can start while this one's run works; Stop is for this session only.
            isRunning={Boolean(run) && !isWaiting && !handOff}
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

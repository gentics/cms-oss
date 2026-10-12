import { useNavigate, useParams } from '@tanstack/react-router';
import { MessageSquarePlusIcon, XIcon } from 'lucide-react';
import { type RefObject, useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { AppShell } from '@/components/AppShell/AppShell';
import { ChatStream } from '@/components/ChatStream/ChatStream';
import { Composer, type ComposerHandle } from '@/components/Composer/Composer';
import { HistoryColumn } from '@/components/HistoryColumn/HistoryColumn';
import { IconButton } from '@/components/IconButton/IconButton';
import { PreviewColumn } from '@/components/PreviewColumn/PreviewColumn';
import { pageElementEditParts, pageElementImageParts, pageElementNaming, pageElementParts, pageElementReplaceImageNaming, pageElementReplaceNaming, pageSettingsParts } from '@/helper/composerParts/composerParts';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { type SendTurnInput, type StartSessionInput, useCancelRun, useHandOffSession, useSendTurn } from '@/hooks/useGenaixQueries';
import { lockAfterFailure, useInteractionLock } from '@/hooks/useInteractionLock';
import { useSessionEvents } from '@/hooks/useSessionEvents';
import type { ContextReference, InteractionAnswer, UserMessagePart } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { useHandOffStore } from '@/store/useHandOffStore';
import { useInteractionDraftStore } from '@/store/useInteractionDraftStore';
import { type LivePreview, selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

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
 * Once the interaction is answered, from the card or here, or has expired (`useInteractionLock`), the
 * run counts as working again: no second answer is sent, and Stop is offered.
 *
 * "Edit these" in a result card (`useHandOffStore`) puts the selected objects into the field, in
 * place of any handed over before, and says so above it: the next message then starts a new session
 * with the objects in the field (`useHandOffSession`) and opens it. The notice's X turns that off and
 * takes the objects out again; with no object left in the field it is off as well.
 */
function SessionComposer({ sessionId, composerRef }: { sessionId: string; composerRef: RefObject<ComposerHandle | null> }) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const sendTurn = useSendTurn(sessionId);
    const handOffSession = useHandOffSession();
    const cancelRun = useCancelRun(sessionId);
    const run = useWorkspaceEventStore((state) => selectSession(sessionId)(state).run);
    const pending = useWorkspaceEventStore((state) => selectSession(sessionId)(state).interactions?.at(-1));
    // What "Edit these" handed over, while the next message starts a new session.
    const handOff = useHandOffStore((state) => state.handOffs[sessionId]);
    const [referenceCount, setReferenceCount] = useState(0);
    const pendingLock = useInteractionLock(pending);
    const isWaiting = !handOff && run?.status === 'waiting_for_input' && Boolean(pending) && !pendingLock;
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
    }, [composerRef, handOff, sessionId]);

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
            useInteractionDraftStore.getState().lock(pending.id, 'sending');
        }

        // The interaction still locked as `sending`, unless `interaction.resolved` came first and took it away.
        const repliedTo = () => (replyTo && useInteractionDraftStore.getState().locks[replyTo.interaction_id] ? replyTo.interaction_id : undefined);

        setUploadProgress([]);
        // The promise, not `mutate` callbacks: those are dropped once the user has gone on to another
        // session, and a failed send would go unreported.
        sendTurn.mutateAsync({ ...input, replyTo, onFileProgress: reportUploadProgress }).then(
            () => {
                const interactionId = repliedTo();

                if (interactionId) {
                    useInteractionDraftStore.getState().lock(interactionId, 'answered');
                }

                composerRef.current?.reset();
            },
            (error: unknown) => {
                const interactionId = repliedTo();

                if (interactionId) {
                    lockAfterFailure(interactionId, error);
                }

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
 * The preview of one session's page (`PreviewColumn`): a part of it goes into the chat composer, and
 * feedback on a part is sent as a turn of its own, as the page plus the part named in the text
 * (`pageElementParts`). New text written for a part by hand is sent the same way, as the page, a text
 * asking to replace the part's text, and the new text as a `verbatim` part (`pageElementEditParts`), and
 * so is a CMS image picked for a part, as a `reference` part (`pageElementImageParts`). Changed page
 * settings go as the page, a text asking to change them, and each new value (`pageSettingsParts`). None needs a run
 * to be working or waiting: a second message would be refused (`409 run_already_active`), and none is an
 * answer to the run's question.
 */
function SessionPreview({ sessionId, preview, composerRef }: { sessionId: string; preview: LivePreview; composerRef: RefObject<ComposerHandle | null> }) {
    const { t } = useTranslation();
    const sendTurn = useSendTurn(sessionId);
    const run = useWorkspaceEventStore((state) => selectSession(sessionId)(state).run);

    async function send(parts: UserMessagePart[]) {
        try {
            await sendTurn.mutateAsync({ parts, files: [] });
        } catch (error) {
            useErrorNotificationStore.getState().addError({ messageKey: 'chat.sendFailed', detailKey: errorMessageKey(error) });

            throw error;
        }
    }

    return (
        <PreviewColumn
            preview={preview}
            onReferenceInChat={(element, text) => composerRef.current?.insertPageElement(element, text)}
            onSendFeedback={(element, text) => send(pageElementParts({ ...element, text: pageElementNaming(t, element) }, text))}
            onSendEdit={(element, text) => send(pageElementEditParts({ ...element, text: pageElementReplaceNaming(t, element) }, text))}
            onSendImage={(element, image) => send(pageElementImageParts({ ...element, text: pageElementReplaceImageNaming(t, element) }, image))}
            onSendSettings={(page, changes) => send(pageSettingsParts(t, page, changes))}
            canSendFeedback={!run && !sendTurn.isPending}
        />
    );
}

/**
 * The page of the session routes (`/sessions/$id`, `/sessions/$id/review`): the workspace with the
 * session's conversation in the middle column and its chat composer at the bottom. It follows the
 * session's event stream, so sent turns and replies land in `useWorkspaceEventStore`. The left column
 * is the searchable session history, the right one the CMS preview once there is one.
 */
export function SessionPage() {
    const { id } = useParams({ strict: false });

    // The chat view is the one consumer of the stream (other components read the store).
    useSessionEvents(id);
    // The chat composer, for the preview to add a part of the page to it.
    const composerRef = useRef<ComposerHandle>(null);
    const preview = useWorkspaceEventStore((state) => (id ? selectSession(id)(state).preview : undefined));

    // The composer floats over the bottom of the middle column, the column scrolls underneath (draft
    // `.composerwrap`). Both routes have an `id`; the composer that sends renders only with one.
    const chat = (
        <div className={styles.chat}>
            {id && <ChatStream key={id} sessionId={id} />}
            {id && (
                <div className={styles.dock}>
                    <SessionComposer key={id} sessionId={id} composerRef={composerRef} />
                </div>
            )}
        </div>
    );

    // A session starts without a preview; the right column appears once GenAIx sends one
    // (`preview.updated`, design.md §11 "Vorschau erst bei Bedarf").
    return (
        <AppShell
            left={<HistoryColumn />}
            center={chat}
            right={id && preview && <SessionPreview key={id} sessionId={id} preview={preview} composerRef={composerRef} />}
            isRightColumnVisible={Boolean(preview)}
        />
    );
}

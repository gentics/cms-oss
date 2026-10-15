import { useParams } from '@tanstack/react-router';
import { type CSSProperties, type RefObject, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { AppShell } from '@/components/AppShell/AppShell';
import { ChatStream } from '@/components/ChatStream/ChatStream';
import { Composer, type ComposerHandle } from '@/components/Composer/Composer';
import { EventLog } from '@/components/EventLog/EventLog';
import { HistoryColumn } from '@/components/HistoryColumn/HistoryColumn';
import { PlanBar } from '@/components/PlanBar/PlanBar';
import { PreviewColumn } from '@/components/PreviewColumn/PreviewColumn';
import { RevisePlanBanner } from '@/components/RevisePlanBanner/RevisePlanBanner';
import { pageElementEditParts, pageElementImageParts, pageElementNaming, pageElementParts, pageElementReplaceImageNaming, pageElementReplaceNaming, pageSettingsParts } from '@/helper/composerParts/composerParts';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { isRevisable, revisionOf } from '@/helper/planActions/planActions';
import { reloadPlans } from '@/helper/planReload/planReload';
import { type SendTurnInput, type StartSessionInput, useCancelRun, useSendTurn } from '@/hooks/useGenaixQueries';
import { lockAfterFailure, useInteractionLock } from '@/hooks/useInteractionLock';
import { useOpenPlan } from '@/hooks/useOpenPlan';
import { useSessionEvents } from '@/hooks/useSessionEvents';
import { GenaixApiError } from '@/services/apiService/apiService';
import type { InteractionAnswer, UserMessagePart } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { isEventLogEnabled, useEventLogStore } from '@/store/useEventLogStore';
import { useHandOffStore } from '@/store/useHandOffStore';
import { useInteractionDraftStore } from '@/store/useInteractionDraftStore';
import { useRevisePlanStore } from '@/store/useRevisePlanStore';
import { type LivePreview, selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import styles from './SessionPage.module.css';

// The codes of a revision that named a plan which cannot be revised (any more) or the session does not
// have: the mode ends and the plans are read again (rc.5).
const REVISE_REFUSALS: ReadonlySet<string> = new Set(['plan_not_revisable', 'plan_not_found']);

// The written text of a turn, as the answer to an `ask_user` question that the card holds none for.
function textAnswer(parts: UserMessagePart[]): InteractionAnswer | undefined {
    const text = parts.map((part) => (part.type === 'text' || part.type === 'verbatim' ? part.text : '')).join('').trim();

    return text ? { text } : undefined;
}

/**
 * The chat composer of one session. Enter or send posts the turn (`useSendTurn`); on success the
 * field is emptied, on failure an error is shown and the input stays. `SessionPage` keys it by the
 * session, so going on to another session from the left column starts with an empty composer.
 * While a run is working, Stop cancels it (`POST …/runs/{run_id}/cancel`): a run of the session agent
 * or a side run first, else the plan's run. Only the first two hold the composer; beside a plan's own
 * run a turn is a read-only side run (rc.4), so send stays next to Stop. While a run waits for an
 * answer, a turn answers the pending interaction (`reply_to_interaction`) with what its card holds, or
 * for `ask_user` with the turn's text; with nothing to answer, it asks for the card first. Once the
 * interaction is answered, from the card or here, or has expired (`useInteractionLock`), the run counts
 * as working again: no second answer is sent, and Stop is offered.
 *
 * "Edit these" in a result card (`useHandOffStore`) puts the selected objects into the field. The
 * next message is an ordinary turn of this session, whose session agent drafts a plan for them (rc.4).
 *
 * "Ask for a change" or "Stop and change" on a plan (`useRevisePlanStore`) put the composer into revising
 * that plan: a banner at the top of the box says so (`RevisePlanBanner`), and the next turn goes out with
 * `options: { mode: 'revise', plan_id }` (rc.5), which redirects the plan while it executes. Sent, or
 * left with × or Esc, the mode ends; it holds only while the plan can be revised, and not for a turn
 * that answers a pending interaction. Refused because the plan cannot be revised or is unknown
 * (`409 plan_not_revisable`, `404 plan_not_found`), it ends and the plans are read again.
 */
function SessionComposer({ sessionId, composerRef }: { sessionId: string; composerRef: RefObject<ComposerHandle | null> }) {
    const sendTurn = useSendTurn(sessionId);
    const cancelRun = useCancelRun(sessionId);
    // The latest run without a plan (the session agent's, or a side run beside a plan's run), which holds
    // the composer, and the latest plan's run, which does not.
    const sessionRun = useWorkspaceEventStore((state) => selectSession(sessionId)(state).runs?.findLast((live) => !live.planId));
    const planRun = useWorkspaceEventStore((state) => selectSession(sessionId)(state).runs?.findLast((live) => Boolean(live.planId)));
    const run = sessionRun ?? planRun;
    const pending = useWorkspaceEventStore((state) => selectSession(sessionId)(state).interactions?.at(-1));
    const pendingRunStatus = useWorkspaceEventStore((state) => selectSession(sessionId)(state).runs?.find((live) => live.id === pending?.run_id)?.status);
    // What "Edit these" handed over, until it is in the field.
    const handOff = useHandOffStore((state) => state.handOffs[sessionId]);
    const pendingLock = useInteractionLock(pending);
    const isWaiting = pendingRunStatus === 'waiting_for_input' && Boolean(pending) && !pendingLock;
    // Upload progress of the files sent, by their position, for the composer's attachment list.
    const [uploadProgress, setUploadProgress] = useState<number[]>([]);
    const revisePlanId = useRevisePlanStore((state) => state.planIds[sessionId]);
    const revisePlan = useWorkspaceEventStore((state) => selectSession(sessionId)(state).plans?.find(({ plan }) => plan.id === revisePlanId)?.plan);
    const revising = revisePlan && isRevisable(revisePlan.status) ? revisePlan : undefined;

    function reportUploadProgress(index: number, fraction: number) {
        setUploadProgress((current) => {
            const next = [...current];

            next[index] = fraction;

            return next;
        });
    }

    // Takes the objects of "Edit these" over into the field.
    useEffect(() => {
        if (!handOff) {
            return;
        }

        composerRef.current?.addReferences(handOff);
        useHandOffStore.getState().end(sessionId);
    }, [composerRef, handOff, sessionId]);

    // The field takes the focus once a plan is to be revised, for the user to say what should change.
    useEffect(() => {
        if (revisePlanId) {
            composerRef.current?.focus();
        }
    }, [composerRef, revisePlanId]);

    function endRevision() {
        useRevisePlanStore.getState().end(sessionId);
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
            useInteractionDraftStore.getState().lock(pending.id, 'sending');
        }

        const revisedPlanId = replyTo ? undefined : revising?.id;

        // The interaction still locked as `sending`, unless `interaction.resolved` came first and took it away.
        const repliedTo = () => (replyTo && useInteractionDraftStore.getState().locks[replyTo.interaction_id] ? replyTo.interaction_id : undefined);

        setUploadProgress([]);
        // The promise, not `mutate` callbacks: those are dropped once the user has gone on to another
        // session, and a failed send would go unreported.
        sendTurn.mutateAsync({ ...input, replyTo, revisePlanId: revisedPlanId, onFileProgress: reportUploadProgress }).then(
            () => {
                const interactionId = repliedTo();

                if (interactionId) {
                    useInteractionDraftStore.getState().lock(interactionId, 'answered');
                }

                if (revisedPlanId) {
                    endRevision();
                }

                composerRef.current?.reset();
            },
            (error: unknown) => {
                const interactionId = repliedTo();

                if (interactionId) {
                    lockAfterFailure(interactionId, error);
                }

                if (revisedPlanId && error instanceof GenaixApiError && error.genaixCode && REVISE_REFUSALS.has(error.genaixCode)) {
                    endRevision();
                    reloadPlans(sessionId).catch((reloadError: unknown) => console.warn('Reading the plans again failed', reloadError));
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
            isSubmitting={sendTurn.isPending}
            uploadProgress={uploadProgress}
            sessionId={sessionId}
            isRunning={Boolean(run) && !isWaiting}
            canSendMeanwhile={!sessionRun}
            isStopping={run?.status === 'cancelling' || cancelRun.isPending}
            onStop={handleStop}
            header={revising && <RevisePlanBanner plan={revising} revision={revisionOf(revising.status) === 'stop' ? 'stop' : 'ask'} onCancel={endRevision} />}
            onEscape={revising ? endRevision : undefined}
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

    // The chat view is the one consumer of the stream (other components read the store). With
    // `npm run dev:debug` every event also goes to the event log beside the chat. A session opened
    // without live state is loaded whole first (`loadFirst`), so the chat does not replay it.
    const isLoading = useSessionEvents(id, isEventLogEnabled && id ? (event) => useEventLogStore.getState().add(id, event) : undefined, { loadFirst: true });
    // The chat composer, for the preview to add a part of the page to it.
    const composerRef = useRef<ComposerHandle>(null);
    const preview = useWorkspaceEventStore((state) => (id ? selectSession(id)(state).preview : undefined));
    // The plan the bar above the composer is for, if any; the stream keeps room for the bar meanwhile,
    // as high as it is, expanded or not.
    const openPlan = useOpenPlan(id ?? '');
    const [planBarHeight, setPlanBarHeight] = useState(0);
    const chatStyle = openPlan ? ({ '--plan-bar-space': `${planBarHeight}px` } as CSSProperties) : undefined;

    // The composer floats over the bottom of the middle column, the column scrolls underneath (draft
    // `.composerwrap`), with the plan bar right above it (design.md §11). Both routes have an `id`; the
    // composer that sends renders only with one.
    const chat = (
        <div className={styles.chat} style={chatStyle}>
            {id && <ChatStream key={id} sessionId={id} isLoading={isLoading} />}
            {id && (
                <div className={styles.dock}>
                    {openPlan && <PlanBar key={openPlan.id} sessionId={id} plan={openPlan} onHeightChange={setPlanBarHeight} />}
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
            center={isEventLogEnabled && id
                ? (
                    <div className={styles.debugRow}>
                        {chat}
                        <EventLog key={id} sessionId={id} />
                    </div>
                )
                : chat}
            right={id && preview && <SessionPreview key={id} sessionId={id} preview={preview} composerRef={composerRef} />}
            isRightColumnVisible={Boolean(preview)}
        />
    );
}

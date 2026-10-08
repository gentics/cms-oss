import { create } from 'zustand';

import type { EventStreamStatus } from '@/services/apiService/eventStream';
import type {
    GenaixEvent,
    Interaction,
    MessageCreateBody,
    MessagePart,
    MessageRole,
    Plan,
    Problem,
    RunStatus,
    Step,
    UserMessagePart,
} from '@/services/apiService/genaix/types';
import { applyMergePatch } from '@/services/apiService/mergePatch';

/** A user turn this client sent with `POST /sessions/{session_id}/messages`. */
export interface SentMessage {
    kind: 'sent';
    /** Client-side id, known before the server answers. */
    localId: string;
    /** The server's `message_id`, once the POST was accepted. */
    messageId?: string;
    request: MessageCreateBody;
    status: 'sending' | 'sent' | 'send_failed';
    error?: unknown;
}

/**
 * A message arriving on the event stream (`message.started` … `message.completed`), or the user message
 * a confirmed `settings_review` became: `interaction.resolved` with `message_id` is its only event, its
 * parts the `settings` of the answer (contract, `InteractionResolvedEvent.message_id`).
 */
export interface ReceivedMessage {
    kind: 'received';
    id: string;
    role: MessageRole;
    /** The run that produced it, from the events' `run_id`. */
    runId?: string;
    /** The `settings_review` a confirmation answers (`Message.interaction_id`). */
    interactionId?: string;
    /** By `part_index`; a part not started yet is `undefined`. User parts on a user message (`role`). */
    parts: (MessagePart | UserMessagePart | undefined)[];
    status: 'streaming' | 'completed';
}

export type LiveMessage = SentMessage | ReceivedMessage;

/** The run in progress: from `run.started` until its terminal event. */
export interface LiveRun {
    id: string;
    status: RunStatus;
    /** The latest `status` event's text, for the thinking indicator. */
    statusText?: string;
}

/** Something about a run the chat shows: how it ended, or an error it reported. */
export type RunNotice = { seq: number; runId?: string } & (
    | { kind: 'cancelled' }
    | { kind: 'failed'; error: Problem }
    /** An `error` event with `recoverable: true`; an unrecoverable one is followed by `run.failed`. */
    | { kind: 'error'; error: Problem }
    | { kind: 'auth'; connector: string; howToFix: string }
);

/**
 * Where in the stream something came that is not a message of its own: after which of its run's agent
 * messages (none while the run had not answered yet), and after how many of that message's parts. A
 * message that goes on streaming afterwards (one agent message can span a whole run) is shown split
 * there.
 */
export interface StreamAnchor {
    afterMessageId?: string;
    afterPart?: number;
}

/** One `plan.updated`: the whole plan as it was then, and where in the stream it came. */
export interface LivePlan extends StreamAnchor {
    seq: number;
    plan: Plan;
    runId?: string;
}

/** The user message a confirmed `settings_review` became (`interaction.resolved` `message_id`), and where it came. */
export interface LiveConfirmation extends StreamAnchor {
    seq: number;
    messageId: string;
}

/**
 * The live state of one session: what is not in `GET /sessions/{session_id}/messages` yet. The
 * history itself stays in TanStack Query.
 */
export interface SessionLiveState {
    /** The highest `seq` applied, the next `after` of the event stream. */
    lastSeq: number;
    connection: EventStreamStatus;
    messages: LiveMessage[];
    /** The active run; absent or `null` while none runs. */
    run?: LiveRun | null;
    notices?: RunNotice[];
    /** Interactions waiting for an answer (`interaction.requested`, not resolved yet). */
    interactions?: Interaction[];
    /** The workflow's main steps by id, as `step.updated` last reported them. */
    steps?: Record<string, Step>;
    /** Every version of the work plan, in the order `plan.updated` sent them. */
    plans?: LivePlan[];
    /** Every confirmed settings review, in the order `interaction.resolved` sent them. */
    confirmations?: LiveConfirmation[];
}

interface WorkspaceEventState {
    sessions: Record<string, SessionLiveState>;
    addSentMessage: (sessionId: string, localId: string, request: MessageCreateBody) => void;
    markMessageSent: (sessionId: string, localId: string, messageId: string) => void;
    markSendFailed: (sessionId: string, localId: string, error: unknown) => void;
    /**
     * Applies one event of the session's stream. Events at or below `lastSeq` are ignored, so a
     * replay is a no-op. Types not handled here only advance `lastSeq` (contract rule 1).
     */
    applyEvent: (sessionId: string, event: GenaixEvent) => void;
    setConnection: (sessionId: string, connection: EventStreamStatus) => void;
    /** After `POST …/runs/{run_id}/cancel` was accepted: the run is `cancelling` until `run.cancelled`. */
    markRunCancelling: (sessionId: string, runId: string) => void;
    /** Drops the given messages (`persistedMessageKeys`), once the refetched history contains them. */
    clearMessages: (sessionId: string, keys: string[]) => void;
    /** After `410 events_pruned`: back to `lastSeq` 0, keeping only unsent turns. */
    resetSession: (sessionId: string) => void;
}

const EMPTY_SESSION: SessionLiveState = { lastSeq: 0, connection: 'closed', messages: [] };

/** Selector for one session's live state; a stable empty state while there is none. */
export function selectSession(sessionId: string) {
    return (state: WorkspaceEventState): SessionLiveState => state.sessions[sessionId] ?? EMPTY_SESSION;
}

/** `localId` of a sent message, `id` of a received one. */
export function messageKey(message: LiveMessage): string {
    return message.kind === 'sent' ? message.localId : message.id;
}

/** The messages the server has persisted: sent turns it accepted, received messages it completed. */
export function persistedMessageKeys(session: SessionLiveState): string[] {
    return session.messages
        .filter((message) => (message.kind === 'sent' ? message.status === 'sent' : message.status === 'completed'))
        .map(messageKey);
}

function updateSession(
    sessions: Record<string, SessionLiveState>,
    sessionId: string,
    update: (session: SessionLiveState) => SessionLiveState,
): { sessions: Record<string, SessionLiveState> } {
    return { sessions: { ...sessions, [sessionId]: update(sessions[sessionId] ?? EMPTY_SESSION) } };
}

function updateSent(messages: LiveMessage[], localId: string, update: (message: SentMessage) => SentMessage): LiveMessage[] {
    return messages.map((message) => (message.kind === 'sent' && message.localId === localId ? update(message) : message));
}

// Updates the received message `messageId`, created as an assistant message when its
// `message.started` was missed. Leaves `messages` unchanged for the id of a turn this client sent,
// which is already shown.
function updateReceived(
    messages: LiveMessage[],
    messageId: string,
    update: (message: ReceivedMessage) => ReceivedMessage,
    role: MessageRole = 'assistant',
): LiveMessage[] {
    if (messages.some((message) => message.kind === 'sent' && message.messageId === messageId)) {
        return messages;
    }

    const index = messages.findIndex((message) => message.kind === 'received' && message.id === messageId);

    if (index === -1) {
        return [...messages, update({ kind: 'received', id: messageId, role, parts: [], status: 'streaming' })];
    }

    return messages.map((message, i) => (i === index ? update(message as ReceivedMessage) : message));
}

function withPart(message: ReceivedMessage, partIndex: number, part: ReceivedMessage['parts'][number]): ReceivedMessage {
    const parts = [...message.parts];

    parts[partIndex] = part;

    return { ...message, parts };
}

// `part.delta`: a string appended to `text` (`append`), or a merge patch of the part (`merge_patch`).
function applyDelta(part: ReceivedMessage['parts'][number], delta: string | Record<string, unknown>, patchFormat: 'append' | 'merge_patch'): ReceivedMessage['parts'][number] {
    if (part === undefined) {
        return part;
    }

    if (patchFormat === 'merge_patch') {
        // A merge patch of a part is a part again (contract, `PartDeltaEvent`); the next
        // `part.completed` replaces it with the authoritative copy either way.
        return applyMergePatch(part, delta) as MessagePart;
    }

    if (typeof delta === 'string' && 'text' in part && typeof part.text === 'string') {
        return { ...part, text: part.text + delta };
    }

    return part;
}

// The run of a received message, kept from the first event that names one.
function withRun(message: ReceivedMessage, runId: string | null | undefined): ReceivedMessage {
    return message.runId || !runId ? message : { ...message, runId };
}

function applyToMessages(messages: LiveMessage[], event: GenaixEvent): LiveMessage[] {
    switch (event.type) {
        case 'message.started':
            return updateReceived(messages, event.message_id, (message) => withRun(message, event.run_id), event.role);
        case 'part.started':
        case 'part.completed':
            return updateReceived(messages, event.message_id, (message) => withPart(withRun(message, event.run_id), event.part_index, event.part));
        case 'part.delta':
            return updateReceived(messages, event.message_id, (message) => withPart(
                message,
                event.part_index,
                applyDelta(message.parts[event.part_index], event.delta, event.patch_format),
            ));
        case 'message.completed':
            return updateReceived(messages, event.message_id, (message) => ({ ...message, status: 'completed' }));
        case 'interaction.resolved': {
            // A confirmed `settings_review`: GenAIx stored the answer's `settings` as a user message.
            const { message_id: messageId, answer } = event;

            if (!messageId || !answer || !('settings' in answer)) {
                return messages;
            }

            return updateReceived(messages, messageId, (message) => ({
                ...withRun(message, event.run_id),
                interactionId: event.interaction_id,
                parts: answer.settings,
                status: 'completed',
            }), 'user');
        }
        default:
            return messages;
    }
}

function applyToRun(run: LiveRun | null | undefined, event: GenaixEvent): LiveRun | null | undefined {
    switch (event.type) {
        case 'run.started':
            return { id: event.run.id, status: event.run.status };
        case 'run.completed':
        case 'run.failed':
        case 'run.cancelled':
            return run && run.id !== event.run.id ? run : null;
        case 'status':
            return run ? { ...run, statusText: event.text } : run;
        case 'part.started':
            // A `status` has no end event. Once the agent writes a part, what it said it was doing is over.
            return run?.statusText ? { id: run.id, status: run.status } : run;
        case 'interaction.requested':
            return run ? { ...run, status: 'waiting_for_input' } : run;
        case 'interaction.resolved':
            return run?.status === 'waiting_for_input' ? { ...run, status: 'running' } : run;
        default:
            return run;
    }
}

function noticeOf(event: GenaixEvent): RunNotice | undefined {
    const base = { seq: event.seq, runId: event.run_id ?? undefined };

    switch (event.type) {
        case 'run.cancelled':
            return { ...base, kind: 'cancelled' };
        case 'run.failed':
            return { ...base, kind: 'failed', error: event.error };
        case 'error':
            // An unrecoverable error is the `error` of the `run.failed` that follows at once.
            return event.recoverable ? { ...base, kind: 'error', error: event.error } : undefined;
        case 'auth.required':
            return { ...base, kind: 'auth', connector: event.connector, howToFix: event.how_to_fix };
        default:
            return undefined;
    }
}

function applyToInteractions(interactions: Interaction[] | undefined, event: GenaixEvent): Interaction[] | undefined {
    switch (event.type) {
        case 'interaction.requested':
            return [...(interactions ?? []).filter(({ id }) => id !== event.interaction.id), event.interaction];
        case 'interaction.resolved':
            return interactions?.filter(({ id }) => id !== event.interaction_id);
        case 'run.completed':
        case 'run.failed':
        case 'run.cancelled':
            // A run that ended waits for nothing any more.
            return interactions?.filter(({ run_id }) => run_id !== event.run.id);
        default:
            return interactions;
    }
}

// `step.updated` is a full replacement keyed by id. Only the main steps are kept: an update of a
// substep (`parent_step_id`) may be ignored by a client that shows only those (contract).
function applyToSteps(steps: Record<string, Step> | undefined, event: GenaixEvent): Record<string, Step> | undefined {
    if (event.type !== 'step.updated' || event.parent_step_id) {
        return steps;
    }

    return { ...steps, [event.step.id]: event.step };
}

// The place of what comes now: after the agent message of the run that had started by then, if any,
// and after the parts it had by then.
function anchorIn(messages: LiveMessage[], runId: string | undefined): StreamAnchor {
    const after = messages.findLast((message): message is ReceivedMessage => message.kind === 'received' && message.role === 'assistant' && message.runId === runId);

    return after ? { afterMessageId: after.id, afterPart: after.parts.length } : {};
}

function applyToSession(session: SessionLiveState, event: GenaixEvent): SessionLiveState {
    const next: SessionLiveState = { ...session, lastSeq: event.seq, messages: applyToMessages(session.messages, event) };
    const run = applyToRun(session.run, event);
    const notice = noticeOf(event);
    const interactions = applyToInteractions(session.interactions, event);
    const steps = applyToSteps(session.steps, event);

    // `plan.updated` carries the whole plan every time, never a delta. Each one is kept with its place
    // in the stream.
    if (event.type === 'plan.updated') {
        const runId = event.run_id ?? undefined;

        next.plans = [...(session.plans ?? []), { seq: event.seq, plan: event.plan, runId, ...anchorIn(session.messages, runId) }];
    }

    if (event.type === 'interaction.resolved' && event.message_id) {
        next.confirmations = [
            ...(session.confirmations ?? []),
            { seq: event.seq, messageId: event.message_id, ...anchorIn(session.messages, event.run_id ?? undefined) },
        ];
    }

    if (run !== session.run) {
        next.run = run;
    }

    if (notice) {
        next.notices = [...(session.notices ?? []), notice];
    }

    if (interactions !== session.interactions) {
        next.interactions = interactions;
    }

    if (steps !== session.steps) {
        next.steps = steps;
    }

    return next;
}

export const useWorkspaceEventStore = create<WorkspaceEventState>((set) => ({
    sessions: {},

    addSentMessage: (sessionId, localId, request) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        messages: [...session.messages, { kind: 'sent', localId, request, status: 'sending' }],
    }))),

    markMessageSent: (sessionId, localId, messageId) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        messages: updateSent(session.messages, localId, (message) => ({ ...message, messageId, status: 'sent' })),
    }))),

    markSendFailed: (sessionId, localId, error) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        messages: updateSent(session.messages, localId, (message) => ({ ...message, error, status: 'send_failed' })),
    }))),

    applyEvent: (sessionId, event) => set((state) => {
        const session = state.sessions[sessionId] ?? EMPTY_SESSION;

        if (event.seq <= session.lastSeq) {
            return state;
        }

        return updateSession(state.sessions, sessionId, () => applyToSession(session, event));
    }),

    setConnection: (sessionId, connection) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        connection,
    }))),

    markRunCancelling: (sessionId, runId) => set((state) => updateSession(state.sessions, sessionId, (session) => (
        session.run?.id === runId ? { ...session, run: { ...session.run, status: 'cancelling' } } : session
    ))),

    clearMessages: (sessionId, keys) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        messages: session.messages.filter((message) => !keys.includes(messageKey(message))),
    }))),

    resetSession: (sessionId) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        lastSeq: 0,
        run: undefined,
        notices: undefined,
        interactions: undefined,
        steps: undefined,
        plans: undefined,
        confirmations: undefined,
        messages: session.messages.filter((message) => message.kind === 'sent' && message.status !== 'sent'),
    }))),
}));

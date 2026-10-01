import { create } from 'zustand';

import type { EventStreamStatus } from '@/services/apiService/eventStream';
import type {
    GenaixEvent,
    MessageCreateBody,
    MessagePart,
    MessageRole,
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

/** A message arriving on the event stream (`message.started` … `message.completed`). */
export interface ReceivedMessage {
    kind: 'received';
    id: string;
    role: MessageRole;
    /** By `part_index`; a part not started yet is `undefined`. */
    parts: (MessagePart | undefined)[];
    status: 'streaming' | 'completed';
}

export type LiveMessage = SentMessage | ReceivedMessage;

/**
 * The live state of one session: what is not in `GET /sessions/{session_id}/messages` yet. The
 * history itself stays in TanStack Query.
 */
export interface SessionLiveState {
    /** The highest `seq` applied, the next `after` of the event stream. */
    lastSeq: number;
    connection: EventStreamStatus;
    messages: LiveMessage[];
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

function withPart(message: ReceivedMessage, partIndex: number, part: MessagePart | undefined): ReceivedMessage {
    const parts = [...message.parts];

    parts[partIndex] = part;

    return { ...message, parts };
}

// `part.delta`: a string appended to `text` (`append`), or a merge patch of the part (`merge_patch`).
function applyDelta(part: MessagePart | undefined, delta: string | Record<string, unknown>, patchFormat: 'append' | 'merge_patch'): MessagePart | undefined {
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

function applyToMessages(messages: LiveMessage[], event: GenaixEvent): LiveMessage[] {
    switch (event.type) {
        case 'message.started':
            return updateReceived(messages, event.message_id, (message) => message, event.role);
        case 'part.started':
        case 'part.completed':
            return updateReceived(messages, event.message_id, (message) => withPart(message, event.part_index, event.part));
        case 'part.delta':
            return updateReceived(messages, event.message_id, (message) => withPart(
                message,
                event.part_index,
                applyDelta(message.parts[event.part_index], event.delta, event.patch_format),
            ));
        case 'message.completed':
            return updateReceived(messages, event.message_id, (message) => ({ ...message, status: 'completed' }));
        default:
            return messages;
    }
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

        return updateSession(state.sessions, sessionId, () => ({
            ...session,
            lastSeq: event.seq,
            messages: applyToMessages(session.messages, event),
        }));
    }),

    setConnection: (sessionId, connection) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        connection,
    }))),

    clearMessages: (sessionId, keys) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        messages: session.messages.filter((message) => !keys.includes(messageKey(message))),
    }))),

    resetSession: (sessionId) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        lastSeq: 0,
        messages: session.messages.filter((message) => message.kind === 'sent' && message.status !== 'sent'),
    }))),
}));

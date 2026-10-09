import { skipToken, useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { referencedObjects, resolvePendingFileSources } from '@/helper/composerParts/composerParts';
import { ensureSessionCmsAuthorization, newSessionCmsAuthorization } from '@/helper/sessionAuthorization/sessionAuthorization';
import { guessWorkflow, handOffWorkflow } from '@/helper/workflowGuess/workflowGuess';
import {
    answerInteraction,
    archiveSession,
    cancelRun,
    createSession,
    genaixRetry,
    genaixRetryDelay,
    getMe,
    getSession,
    listMessages,
    listSessions,
    listSessionUploads,
    listWorkflows,
    postMessage,
    type SessionFilters,
    uploadSessionFile,
} from '@/services/apiService/apiService';
import type {
    FileMode,
    InteractionAnswer,
    MessageCreateBody,
    Session,
    SessionContext,
    UserFileRefPart,
    UserMessagePart,
} from '@/services/apiService/genaix/types';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

/** Query keys of the GenAIx server state. */
export const genaixKeys = {
    all: ['genaix'] as const,
    me: () => ['genaix', 'me'] as const,
    workflows: () => ['genaix', 'workflows'] as const,
    /** Prefix of every session list, whatever its filters. */
    sessionLists: () => ['genaix', 'sessions', 'list'] as const,
    sessionList: (filters: SessionFilters = {}) => ['genaix', 'sessions', 'list', filters] as const,
    session: (sessionId: string) => ['genaix', 'sessions', sessionId] as const,
    messages: (sessionId: string) => ['genaix', 'sessions', sessionId, 'messages'] as const,
    uploads: (sessionId: string) => ['genaix', 'sessions', sessionId, 'uploads'] as const,
};

// Retries only what is worth retrying (`isRetryableGenaixError`), honouring `Retry-After`.
const retryOptions = { retry: genaixRetry, retryDelay: genaixRetryDelay };

/** GET /me: identity, capabilities and MCP connections. */
export function useMe() {
    return useQuery({ queryKey: genaixKeys.me(), queryFn: getMe, ...retryOptions });
}

/** GET /workflows, "once, cached" (session lifecycle, §1a). */
export function useWorkflows() {
    return useQuery({ queryKey: genaixKeys.workflows(), queryFn: listWorkflows, staleTime: Infinity, ...retryOptions });
}

/** The files uploaded into a session (`listSessionUploads`); off without a session. */
export function useSessionUploads(sessionId: string | undefined) {
    return useQuery({
        queryKey: genaixKeys.uploads(sessionId ?? ''),
        queryFn: sessionId === undefined ? skipToken : () => listSessionUploads(sessionId),
        select: (page) => page.items,
        ...retryOptions,
    });
}

/** GET /sessions, newest first; `fetchNextPage` follows `next_cursor`. */
export function useSessions(filters: SessionFilters = {}) {
    return useInfiniteQuery({
        queryKey: genaixKeys.sessionList(filters),
        queryFn: ({ pageParam }) => listSessions(filters, { cursor: pageParam }),
        initialPageParam: undefined as string | undefined,
        getNextPageParam: (lastPage) => lastPage.next_cursor ?? undefined,
        ...retryOptions,
    });
}
/** GET /sessions/{session_id}: the session, its `status` included. */
export function useSession(sessionId: string) {
    return useQuery({ queryKey: genaixKeys.session(sessionId), queryFn: () => getSession(sessionId), ...retryOptions });
}

/**
 * DELETE /sessions/{session_id}: archives the session, then refetches every session list, which no
 * longer contains it. Retried like a query: the call is idempotent.
 */
export function useArchiveSession() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (sessionId: string) => archiveSession(sessionId),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: genaixKeys.sessionLists() }),
        ...retryOptions,
    });
}

/** GET /sessions/{session_id}/messages, oldest first; `fetchNextPage` follows `next_cursor`. */
export function useSessionMessages(sessionId: string) {
    return useInfiniteQuery({
        queryKey: genaixKeys.messages(sessionId),
        queryFn: ({ pageParam }) => listMessages(sessionId, { cursor: pageParam }),
        initialPageParam: undefined as string | undefined,
        getNextPageParam: (lastPage) => lastPage.next_cursor ?? undefined,
        ...retryOptions,
    });
}

/**
 * POST /sessions/{session_id}/messages, after making sure the session holds a CMS credential
 * (`ensureSessionCmsAuthorization`). The turn appears in `useWorkspaceEventStore` at once as
 * `sending`, then `sent` or `send_failed`. Never retried: a repeat would send the turn twice.
 */
export function useSendMessage(sessionId: string) {
    return useMutation({
        mutationFn: async (body: MessageCreateBody) => {
            await ensureSessionCmsAuthorization(sessionId);

            return postMessage(sessionId, body);
        },
        onMutate: (body) => {
            const localId = crypto.randomUUID();

            useWorkspaceEventStore.getState().addSentMessage(sessionId, localId, body);

            return { localId };
        },
        onSuccess: (accepted, _body, context) => {
            useWorkspaceEventStore.getState().markMessageSent(sessionId, context.localId, accepted.message_id);
        },
        onError: (error, _body, context) => {
            if (context) {
                useWorkspaceEventStore.getState().markSendFailed(sessionId, context.localId, error);
            }
        },
        retry: false,
    });
}

/** A file attached to the first message, and how its wording is treated. */
export interface StartFile {
    file: File;
    mode: FileMode;
}

export interface StartSessionInput {
    parts: UserMessagePart[];
    files: StartFile[];
    /** Upload progress of `files[index]`, 0 to 1, while the files are uploaded on sending. */
    onFileProgress?: (index: number, fraction: number) => void;
}

// Without files, one call creates the session with its first message. Files are per session, so
// with files the session is created first, then the files are uploaded, then the message is posted
// with a `file_ref` part per file (contract, `SessionCreate`). Either way the session is created with
// its CMS credential (`newSessionCmsAuthorization`), which GenAIx verifies before creating anything,
// and with the workflow its first message suggests (`guessWorkflow`, TEMPORARY).
// Uploads the files into the session, one after the other, and returns a `file_ref` part per file
// for the message that refers to them.
async function uploadFileParts(sessionId: string, files: StartFile[], onFileProgress?: StartSessionInput['onFileProgress']): Promise<UserFileRefPart[]> {
    const fileParts: UserFileRefPart[] = [];

    for (const [index, { file, mode }] of files.entries()) {
        const stored = await uploadSessionFile(sessionId, file, mode, (fraction) => onFileProgress?.(index, fraction));

        fileParts.push({ type: 'file_ref', file_id: stored.id, mode });
    }

    return fileParts;
}

async function startSession({ parts, files, onFileProgress }: StartSessionInput, workflow = guessWorkflow(parts), context?: SessionContext): Promise<Session> {
    const authorizations = [await newSessionCmsAuthorization()];

    if (files.length === 0) {
        return createSession({ workflow, context, message: { parts }, authorizations });
    }

    const session = await createSession({ workflow, context, authorizations });
    const fileParts = await uploadFileParts(session.id, files, onFileProgress);
    const fileIds = fileParts.map((part) => part.file_id);

    await postMessage(session.id, { parts: [...resolvePendingFileSources(parts, fileIds), ...fileParts] });

    return session;
}

/**
 * Starts a session from the dashboard and puts it into the `genaixKeys.session` cache, so the
 * session view has it, its `status` included, without another request. Never retried: a repeat
 * would create a second session.
 */
export function useStartSession() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (input: StartSessionInput) => startSession(input),
        onSuccess: (session) => {
            queryClient.setQueryData(genaixKeys.session(session.id), session);
        },
        retry: false,
    });
}

/** The first message of a session handed off from a read-only one, and where that one worked. */
export interface HandOffSessionInput extends StartSessionInput {
    /** The read-only session's `context.node_id`, which the new session works in too. */
    nodeId?: number;
}

/**
 * Starts a session that acts on objects selected in a read-only session ("Edit these"): its
 * instruction as the first message, every object the message refers to in `context.references`, in
 * the read-only session's node, with a workflow that is not read-only (`handOffWorkflow`). Like
 * `useStartSession`, it puts the session into the cache and is never retried.
 */
export function useHandOffSession() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ nodeId, ...input }: HandOffSessionInput) => startSession(input, handOffWorkflow(input.parts), {
            ...(nodeId !== undefined && { node_id: nodeId }),
            references: referencedObjects(input.parts),
            // Their contract defaults, the same as leaving them out; the generated type requires them.
            guidelines: [],
            connection_ids: [],
        }),
        onSuccess: (session) => {
            queryClient.setQueryData(genaixKeys.session(session.id), session);
            // The left column stays while the new session opens; it lists the new one too.
            void queryClient.invalidateQueries({ queryKey: genaixKeys.sessionLists() });
        },
        retry: false,
    });
}

/** A turn from the chat composer; `replyTo` answers the run's pending interaction with it. */
export interface SendTurnInput extends StartSessionInput {
    replyTo?: { interaction_id: string; answer: InteractionAnswer };
}

/**
 * Sends a turn from the session's chat composer: uploads its files, then posts the message through
 * `useSendMessage`, which records the turn in `useWorkspaceEventStore`. With `replyTo` the message
 * carries `reply_to_interaction`: it answers the question the run waits on and joins that run, the one
 * message accepted while a run is active (contract, `MessageCreate.reply_to_interaction`). Never
 * retried, like `useSendMessage`: a repeat would upload and send twice.
 */
export function useSendTurn(sessionId: string) {
    const queryClient = useQueryClient();
    const { mutateAsync: sendMessage } = useSendMessage(sessionId);

    return useMutation({
        mutationFn: async ({ parts, files, onFileProgress, replyTo }: SendTurnInput) => {
            const fileParts = await uploadFileParts(sessionId, files, onFileProgress);
            const fileIds = fileParts.map((part) => part.file_id);

            // The new uploads can be the source of a verbatim passage in the next turn.
            if (fileParts.length > 0) {
                void queryClient.invalidateQueries({ queryKey: genaixKeys.uploads(sessionId) });
            }

            return sendMessage({
                parts: [...resolvePendingFileSources(parts, fileIds), ...fileParts],
                ...(replyTo && { reply_to_interaction: replyTo }),
            });
        },
        retry: false,
    });
}

/**
 * POST /sessions/{session_id}/runs/{run_id}/cancel: Stop. Once accepted the run shows as
 * `cancelling` in `useWorkspaceEventStore` until `run.cancelled` arrives. Retried like a query: the
 * call is idempotent.
 */
export function useCancelRun(sessionId: string) {
    return useMutation({
        mutationFn: (runId: string) => cancelRun(sessionId, runId, 'User pressed Stop.'),
        onSuccess: (_run, runId) => useWorkspaceEventStore.getState().markRunCancelling(sessionId, runId),
        ...retryOptions,
    });
}

/**
 * POST /sessions/{session_id}/interactions/{interaction_id}: answers a pending interaction; the run
 * continues with `interaction.resolved` on the event stream. Never retried: a second answer is
 * `409 interaction_already_answered`.
 */
export function useAnswerInteraction(sessionId: string) {
    return useMutation({
        mutationFn: ({ interactionId, answer }: { interactionId: string; answer: InteractionAnswer }) => (
            answerInteraction(sessionId, interactionId, answer)
        ),
        retry: false,
    });
}

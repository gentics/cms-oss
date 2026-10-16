import { skipToken, useInfiniteQuery, useMutation, useMutationState, useQuery, useQueryClient } from '@tanstack/react-query';

import { resolvePendingFileSources } from '@/helper/composerParts/composerParts';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { ensureSessionCmsAuthorization, newSessionCmsAuthorization } from '@/helper/sessionAuthorization/sessionAuthorization';
import {
    answerInteraction,
    archiveSession,
    cancelRun,
    createSession,
    GenaixApiError,
    genaixRetry,
    genaixRetryDelay,
    getMe,
    getPlanState,
    getSession,
    listMessages,
    listSessions,
    listSessionUploads,
    listWorkflows,
    postMessage,
    type SessionFilters,
    transitionPlan,
    unarchiveSession,
    uploadSessionFile,
} from '@/services/apiService/apiService';
import type {
    FileMode,
    InteractionAnswer,
    MessageCreateBody,
    Problem,
    Session,
    TransitionRequest,
    UserFileRefPart,
    UserMessagePart,
} from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
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
    plan: (sessionId: string, planId: string) => ['genaix', 'sessions', sessionId, 'plans', planId] as const,
    /** Mutation key of every plan transition of a session (`useTransitionPlan`, `usePlanTransition`). */
    transitions: (sessionId: string) => ['genaix', 'sessions', sessionId, 'transitions'] as const,
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

/** GET /sessions/{session_id}/plans/{plan_id}: the plan's state; off without a plan. */
export function usePlanState(sessionId: string, planId: string | undefined) {
    return useQuery({
        queryKey: genaixKeys.plan(sessionId, planId ?? ''),
        queryFn: planId === undefined ? skipToken : () => getPlanState(sessionId, planId),
        ...retryOptions,
    });
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

/**
 * POST /sessions/{session_id}/transitions `unarchive`: the archived session is active again, then every
 * session list is refetched, which holds it again. Retried like a query: the call is idempotent.
 */
export function useUnarchiveSession() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (sessionId: string) => unarchiveSession(sessionId),
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
 * `sending`, then `sent` or `send_failed`; once sent, the run it started is awaited (`awaitRun`).
 * Never retried: a repeat would send the turn twice.
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
            useWorkspaceEventStore.getState().markMessageSent(sessionId, context.localId, accepted.message_id, accepted.run_id);
            useWorkspaceEventStore.getState().awaitRun(sessionId, accepted.run_id);
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
// and without a `workflow`: the session agent answers or drafts a plan under the module it picks (rc.4).
// The run the first message started is awaited (`awaitRun`), so the session view shows it working.
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

async function startSession({ parts, files, onFileProgress }: StartSessionInput): Promise<Session> {
    const authorizations = [await newSessionCmsAuthorization()];

    if (files.length === 0) {
        const created = await createSession({ message: { parts }, authorizations });

        if (created.run_id) {
            useWorkspaceEventStore.getState().awaitRun(created.id, created.run_id);
        }

        return created;
    }

    const session = await createSession({ authorizations });
    const fileParts = await uploadFileParts(session.id, files, onFileProgress);
    const fileIds = fileParts.map((part) => part.file_id);
    const accepted = await postMessage(session.id, { parts: [...resolvePendingFileSources(parts, fileIds), ...fileParts] });

    useWorkspaceEventStore.getState().awaitRun(session.id, accepted.run_id);

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

/** One plan transition: which plan, and the request (`action`, `acknowledge_checks`). */
export interface TransitionPlanInput {
    planId: string;
    request: TransitionRequest;
    /**
     * Sent to have the quality gates check the plan rather than as the user's own decision: the release
     * "Review & release" sends on the way and "Check again". Only these show the check in progress
     * (`ReviewPanel`); the request is the same.
     */
    isCheck?: boolean;
}

/** The `409 quality_gate_failed` problem a transition was refused with, or `undefined` for any other outcome. */
export function gateRefusalOf(error: unknown): Problem | undefined {
    return error instanceof GenaixApiError && error.genaixCode === 'quality_gate_failed' ? error.problem : undefined;
}

/**
 * POST /sessions/{session_id}/plans/{plan_id}/transitions, after making sure the session holds a CMS
 * credential (`publish` acts through the session's CMS connection). The plan's new status arrives as
 * `plan.updated` on the event stream; the session lists are refetched, since the to-dos filter on the
 * plan's status. A transition that runs (`202`) awaits that run (`awaitRun`). Never retried: a
 * repeated `publish` would start a second run.
 *
 * Every transition is kept under `genaixKeys.transitions`, so that its outcome is seen where the plan
 * is shown (`usePlanTransition`), also when it was sent elsewhere, e.g. the release that "Review &
 * release" sends on its way to the review page. A refusal by the quality gates is shown there
 * (`PlanHead`); any other failure is an error notification, also once the sender is gone.
 */
export function useTransitionPlan(sessionId: string) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationKey: genaixKeys.transitions(sessionId),
        mutationFn: async ({ planId, request }: TransitionPlanInput) => {
            await ensureSessionCmsAuthorization(sessionId);

            return transitionPlan(sessionId, planId, request);
        },
        onSuccess: (result) => {
            // `publish`, `request_review` and a restoring `discard` answer `202` with the run that does it.
            if ('run_id' in result) {
                useWorkspaceEventStore.getState().awaitRun(sessionId, result.run_id);
            }

            return queryClient.invalidateQueries({ queryKey: genaixKeys.sessionLists() });
        },
        onError: (error) => {
            if (!gateRefusalOf(error)) {
                useErrorNotificationStore.getState().addError({ messageKey: 'planBar.transitionFailed', detailKey: errorMessageKey(error) });
            }
        },
        retry: false,
    });
}

function isTransitionInput(variables: unknown): variables is TransitionPlanInput {
    return typeof variables === 'object' && variables !== null && 'planId' in variables && 'request' in variables;
}

/** Where the transitions of one plan stand. */
export interface PlanTransition {
    /** The transition on its way, if any. */
    pending?: TransitionPlanInput;
    /** The latest one: what it asked for, when, and how it ended (`status`, `error`). */
    latest?: { input: TransitionPlanInput; status: 'idle' | 'pending' | 'success' | 'error'; error: unknown; submittedAt: number };
}

/** The transitions of `planId`, from whichever component sent them (`useTransitionPlan`). */
export function usePlanTransition(sessionId: string, planId: string): PlanTransition {
    const states = useMutationState({
        filters: { mutationKey: genaixKeys.transitions(sessionId) },
        select: ({ state }) => state,
    });
    const own = states.flatMap(({ variables, status, error, submittedAt }) => (
        isTransitionInput(variables) && variables.planId === planId ? [{ input: variables, status, error, submittedAt }] : []
    ));

    return { pending: own.find(({ status }) => status === 'pending')?.input, latest: own.at(-1) };
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

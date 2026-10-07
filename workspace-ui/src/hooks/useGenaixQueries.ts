import { skipToken, useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { resolvePendingFileSources } from '@/helper/composerParts/composerParts';
import {
    archiveSession,
    createSession,
    genaixRetry,
    genaixRetryDelay,
    getMe,
    getSession,
    listMcpConnections,
    listMessages,
    listSessions,
    listSessionUploads,
    listWorkflows,
    NoCmsConnectionError,
    postMessage,
    type SessionFilters,
    uploadSessionFile,
} from '@/services/apiService/apiService';
import type {
    FileMode,
    MessageCreateBody,
    Session,
    SessionCreateBody,
    SessionCreated,
    UserFileRefPart,
    UserMessagePart,
} from '@/services/apiService/genaix/types';
import { createCmsToken } from '@/services/cmsApiService/cmsApiService';
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
 * POST /sessions/{session_id}/messages. The turn appears in `useWorkspaceEventStore` at once as
 * `sending`, then `sent` or `send_failed`. Never retried: a repeat would send the turn twice.
 */
export function useSendMessage(sessionId: string) {
    return useMutation({
        mutationFn: (body: MessageCreateBody) => postMessage(sessionId, body),
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

/** The workflow a session started from the dashboard runs. */
export const START_WORKFLOW = 'content_research';

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
// with a `file_ref` part per file (contract, `SessionCreate`).
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

/**
 * POST /sessions with the session's own CMS authorization (09-integration-guide.md, section 4): the
 * user's `cms` connection (the default one, else the first), and a new CMS API token for this session
 * alone, never one reused from another. GenAIx verifies the token before it creates anything, so a
 * rejected token means no session (`422 mcp_authorization_rejected`). The token is not kept here:
 * GenAIx stores it for the session's runs and cleans it up, and the CMS prunes it once it expires.
 */
async function createAuthorizedSession(body: SessionCreateBody): Promise<SessionCreated> {
    // The connection first: without one, no token is created for nothing.
    const connections = await listMcpConnections('cms');
    const connection = connections.find((candidate) => candidate.default) ?? connections[0];

    if (!connection) {
        throw new NoCmsConnectionError();
    }

    const { token, name, expires } = await createCmsToken(`genaix-workspace-${crypto.randomUUID()}`);

    return createSession({
        ...body,
        authorizations: [{
            connection_id: connection.id,
            auth_type: 'bearer',
            token,
            token_name: name,
            expires_at: new Date(expires * 1000).toISOString(),
        }],
    });
}

async function startSession({ parts, files, onFileProgress }: StartSessionInput): Promise<Session> {
    if (files.length === 0) {
        return createAuthorizedSession({ workflow: START_WORKFLOW, message: { parts } });
    }

    const session = await createAuthorizedSession({ workflow: START_WORKFLOW });
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
        mutationFn: startSession,
        onSuccess: (session) => {
            queryClient.setQueryData(genaixKeys.session(session.id), session);
        },
        retry: false,
    });
}

/**
 * Sends a turn from the session's chat composer: uploads its files, then posts the message through
 * `useSendMessage`, which records the turn in `useWorkspaceEventStore`. Never retried, like
 * `useSendMessage`: a repeat would upload and send twice.
 */
export function useSendTurn(sessionId: string) {
    const queryClient = useQueryClient();
    const { mutateAsync: sendMessage } = useSendMessage(sessionId);

    return useMutation({
        mutationFn: async ({ parts, files, onFileProgress }: StartSessionInput) => {
            const fileParts = await uploadFileParts(sessionId, files, onFileProgress);
            const fileIds = fileParts.map((part) => part.file_id);

            // The new uploads can be the source of a verbatim passage in the next turn.
            if (fileParts.length > 0) {
                void queryClient.invalidateQueries({ queryKey: genaixKeys.uploads(sessionId) });
            }

            return sendMessage({ parts: [...resolvePendingFileSources(parts, fileIds), ...fileParts] });
        },
        retry: false,
    });
}

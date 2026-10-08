import type {
    FileMode,
    GenaixCode,
    Interaction,
    InteractionAnswer,
    McpConnection,
    Me,
    MessageAccepted,
    MessageCreateBody,
    MessagePage,
    Problem,
    Run,
    Session,
    SessionAuthorization,
    SessionAuthorizationRequest,
    SessionCreateBody,
    SessionCreated,
    SessionFile,
    SessionPage,
    SessionStatus,
    Workflow,
} from '@/services/apiService/genaix/types';
import { HttpError, httpRequest } from '@/services/httpService/httpService';

// Same-origin path of the CMS proxy, which adds the installation token and X-GCMS-Subject. Never
// GenAIx itself: that would need the installation token in the browser. In development the Vite dev
// server plays the proxy (see `vite.config.ts`). Set `VITE_GENAIX_API_BASE` to override (`.env.example`).
export const GENAIX_API_BASE = import.meta.env.VITE_GENAIX_API_BASE || '/genaix/api/v1';

/** How many times a retryable GenAIx request is retried (queries and the event stream). */
export const GENAIX_MAX_RETRIES = 3;

const MAX_RETRY_DELAY_MS = 30_000;

/**
 * A GenAIx response that was not successful. `problem` is the RFC 9457 body when there was one
 * (contract §11); branch on `genaixCode`, not on `status`.
 */
export class GenaixApiError extends HttpError {
    readonly problem?: Problem;
    readonly genaixCode?: GenaixCode;
    /** `X-GenAIx-Request-Id`, the support correlation key. */
    readonly requestId?: string;
    readonly retryAfterSeconds?: number;

    constructor(url: string, status: number, problem: Problem | undefined, headers: Headers) {
        super(url, status, problem, headers);
        this.name = 'GenaixApiError';
        this.problem = problem;
        this.genaixCode = problem?.genaix_code;
        this.requestId = problem?.request_id ?? headers.get('X-GenAIx-Request-Id') ?? undefined;

        const retryAfterHeader = Number(headers.get('Retry-After'));

        this.retryAfterSeconds = problem?.retry_after_seconds
            ?? (Number.isFinite(retryAfterHeader) && retryAfterHeader > 0 ? retryAfterHeader : undefined);
    }
}

function isProblem(body: unknown): body is Problem {
    return typeof body === 'object' && body !== null && typeof (body as { genaix_code?: unknown }).genaix_code === 'string';
}

/** Reads the error response into a `GenaixApiError`; a body that is not a Problem is left out. */
export async function toGenaixApiError(url: string, response: Response): Promise<GenaixApiError> {
    let problem: Problem | undefined;

    try {
        const body: unknown = await response.json();

        problem = isProblem(body) ? body : undefined;
    } catch {
        problem = undefined;
    }

    return new GenaixApiError(url, response.status, problem, response.headers);
}

// Sends a request to a GenAIx route, for example `genaixRequest<Me>('/me')`, and returns its JSON
// body. The body is typed by the caller and not validated at runtime.
export async function genaixRequest<T>(path: string, init?: RequestInit): Promise<T> {
    const url = `${GENAIX_API_BASE}${path}`;
    const headers = new Headers(init?.headers);

    headers.set('Accept', 'application/json');

    return httpRequest<T>(url, { credentials: 'same-origin', ...init, headers }, toGenaixApiError);
}

/**
 * Network errors (fetch rejects with a `TypeError`), `429 rate_limited` and `503
 * service_unavailable` are worth retrying (contract §11); every other status is not.
 */
export function isRetryableGenaixError(error: unknown): boolean {
    if (error instanceof GenaixApiError) {
        return error.status === 429 || error.status === 503;
    }

    return error instanceof TypeError;
}

/** TanStack Query `retry`: retryable errors only, at most `GENAIX_MAX_RETRIES` times. */
export function genaixRetry(failureCount: number, error: unknown): boolean {
    return failureCount < GENAIX_MAX_RETRIES && isRetryableGenaixError(error);
}

/** TanStack Query `retryDelay`: the server's `Retry-After` when it sent one, otherwise 1 s, 2 s, 4 s … 30 s. */
export function genaixRetryDelay(failureCount: number, error: unknown): number {
    if (error instanceof GenaixApiError && error.retryAfterSeconds !== undefined) {
        return error.retryAfterSeconds * 1000;
    }

    return Math.min(1000 * 2 ** failureCount, MAX_RETRY_DELAY_MS);
}

function queryString(params: Record<string, string | number | string[] | undefined>): string {
    const search = new URLSearchParams();

    for (const [key, value] of Object.entries(params)) {
        if (Array.isArray(value)) {
            value.forEach((item) => search.append(key, item));
        } else if (value !== undefined) {
            search.set(key, String(value));
        }
    }

    const query = search.toString();

    return query ? `?${query}` : '';
}

export interface PageParams {
    limit?: number;
    cursor?: string;
}

export interface SessionFilters {
    status?: SessionStatus[];
    workflow?: string[];
    q?: string;
}

// GET /me: identity, capabilities and MCP connection status.
export function getMe(): Promise<Me> {
    return genaixRequest<Me>('/me');
}

// GET /mcp/connections: the caller's connections, default first within each connector type. When
// the installation names a default URL, the first call creates the default connection.
export async function listMcpConnections(connector?: string): Promise<McpConnection[]> {
    const { items } = await genaixRequest<{ items: McpConnection[] }>(`/mcp/connections${queryString({ connector })}`);

    return items;
}

// GET /workflows: the workflow modules, read once and cached.
export async function listWorkflows(): Promise<Workflow[]> {
    const { items } = await genaixRequest<{ items: Workflow[] }>('/workflows');

    return items;
}

// GET /sessions: newest `last_activity_at` first, paged by `next_cursor`.
export function listSessions(filters: SessionFilters = {}, page: PageParams = {}): Promise<SessionPage> {
    return genaixRequest<SessionPage>(`/sessions${queryString({ ...filters, ...page })}`);
}

// POST /sessions: `201` with the new session. With a `message` it also carries the ids of that
// message and of its run, whose events arrive on the session's event stream.
export function createSession(body: SessionCreateBody): Promise<SessionCreated> {
    return genaixRequest<SessionCreated>('/sessions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });
}

// PUT /sessions/{session_id}/authorizations/{connection_id}: registers the credential this session
// uses for one connection. GenAIx verifies it before storing it; `422` `mcp_authorization_rejected`
// when the connection refuses it. Idempotent: registering again replaces the credential.
export function putSessionAuthorization(sessionId: string, connectionId: string, body: SessionAuthorizationRequest): Promise<SessionAuthorization> {
    return genaixRequest<SessionAuthorization>(`/sessions/${encodeURIComponent(sessionId)}/authorizations/${encodeURIComponent(connectionId)}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });
}

// GET /sessions/{session_id}
export function getSession(sessionId: string): Promise<Session> {
    return genaixRequest<Session>(`/sessions/${encodeURIComponent(sessionId)}`);
}

// The headers of an XHR response, for the `Response` its error is read from.
function responseHeaders(request: XMLHttpRequest): Headers {
    const headers = new Headers();

    for (const line of request.getAllResponseHeaders().trim().split(/[\r\n]+/)) {
        const separator = line.indexOf(':');

        if (separator > 0) {
            headers.append(line.slice(0, separator).trim(), line.slice(separator + 1).trim());
        }
    }

    return headers;
}

// POST /sessions/{session_id}/files as multipart: `201` with the stored file. No `Content-Type` is
// set, so the browser adds the multipart boundary. An `XMLHttpRequest`, not `fetch`, because only it
// reports upload progress: `onProgress` gets the share sent so far, 0 to 1. Errors are thrown as
// `genaixRequest` throws them: a `GenaixApiError` for a response, a `TypeError` without one.
export function uploadSessionFile(sessionId: string, file: File, mode: FileMode, onProgress?: (fraction: number) => void): Promise<SessionFile> {
    const url = `${GENAIX_API_BASE}/sessions/${encodeURIComponent(sessionId)}/files`;
    const body = new FormData();

    body.append('file', file);
    body.append('mode', mode);

    return new Promise((resolve, reject) => {
        const request = new XMLHttpRequest();

        request.open('POST', url);
        request.setRequestHeader('Accept', 'application/json');
        request.upload.onprogress = (event) => {
            if (event.lengthComputable && event.total > 0) {
                onProgress?.(event.loaded / event.total);
            }
        };
        request.onload = () => {
            if (request.status >= 200 && request.status < 300) {
                try {
                    resolve(JSON.parse(request.responseText) as SessionFile);
                } catch (error) {
                    reject(error);
                }

                return;
            }

            // Through a `Response`, so the error is read exactly as for every other GenAIx request.
            const response = new Response(request.responseText || null, { status: request.status, headers: responseHeaders(request) });

            void toGenaixApiError(url, response).then(reject, reject);
        };
        request.onerror = () => reject(new TypeError(`Upload to ${url} failed`));
        request.onabort = () => reject(new TypeError(`Upload to ${url} was aborted`));
        request.send(body);
    });
}

// DELETE /sessions/{session_id}: archives the session (soft delete, `204`). Idempotent; an active
// run is cancelled first. Archived sessions drop out of the default `GET /sessions`.
export function archiveSession(sessionId: string): Promise<void> {
    return genaixRequest<void>(`/sessions/${encodeURIComponent(sessionId)}`, { method: 'DELETE' });
}

// GET /sessions/{session_id}/messages: oldest first, paged by `next_cursor`.
export function listMessages(sessionId: string, page: PageParams = {}): Promise<MessagePage> {
    return genaixRequest<MessagePage>(`/sessions/${encodeURIComponent(sessionId)}/messages${queryString({ ...page })}`);
}

// POST /sessions/{session_id}/messages with `Accept: application/json`: `202` with the ids of the
// new message and run. The run's events arrive on the session's event stream.
export function postMessage(sessionId: string, body: MessageCreateBody): Promise<MessageAccepted> {
    return genaixRequest<MessageAccepted>(`/sessions/${encodeURIComponent(sessionId)}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });
}

// POST /sessions/{session_id}/runs/{run_id}/cancel: `202` with the run, now `cancelling`; the stream
// ends it with `run.cancelled`. Idempotent: a run that already ended is `202` with no effect. CMS
// writes already made are not rolled back.
export function cancelRun(sessionId: string, runId: string, reason?: string): Promise<Run> {
    return genaixRequest<Run>(`/sessions/${encodeURIComponent(sessionId)}/runs/${encodeURIComponent(runId)}/cancel`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(reason === undefined ? {} : { reason }),
    });
}

// POST /sessions/{session_id}/interactions/{interaction_id}: answers a pending interaction; the run
// continues on the event stream with `interaction.resolved`. `410 interaction_expired` after its
// lifetime, `409 interaction_already_answered` on a second answer. The `answer` shape follows the
// interaction's `kind` (contract §9).
export function answerInteraction(sessionId: string, interactionId: string, answer: InteractionAnswer): Promise<Interaction> {
    return genaixRequest<Interaction>(`/sessions/${encodeURIComponent(sessionId)}/interactions/${encodeURIComponent(interactionId)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ answer }),
    });
}

// GET /sessions/{session_id}/files/{file_id}/content, as a link target: the browser downloads the file
// through the same-origin proxy, which adds the credentials.
export function sessionFileContentUrl(sessionId: string, fileId: string): string {
    return `${GENAIX_API_BASE}/sessions/${encodeURIComponent(sessionId)}/files/${encodeURIComponent(fileId)}/content`;
}

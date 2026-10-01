import type {
    GenaixCode,
    Me,
    MessageAccepted,
    MessageCreateBody,
    MessagePage,
    Problem,
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

// GET /workflows: the workflow modules, read once and cached.
export async function listWorkflows(): Promise<Workflow[]> {
    const { items } = await genaixRequest<{ items: Workflow[] }>('/workflows');

    return items;
}

// GET /sessions: newest `last_activity_at` first, paged by `next_cursor`.
export function listSessions(filters: SessionFilters = {}, page: PageParams = {}): Promise<SessionPage> {
    return genaixRequest<SessionPage>(`/sessions${queryString({ ...filters, ...page })}`);
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

import { GenaixApiError, NoCmsConnectionError } from '@/services/apiService/apiService';
import type { GenaixCode, Problem } from '@/services/apiService/genaix/types';
import type { CmsResponseCode } from '@/services/cmsApiService/cmsApiService';
import { HttpError } from '@/services/httpService/httpService';

// Every member, so the compiler flags a code the contract adds (`npm run generate:api`).
const GENAIX_CODES: Record<GenaixCode, true> = {
    malformed_request: true,
    invalid_cursor: true,
    subject_missing: true,
    identity_invalid: true,
    unsupported_accept: true,
    invalid_installation_token: true,
    session_forbidden: true,
    session_not_found: true,
    run_not_found: true,
    file_not_found: true,
    interaction_not_found: true,
    unknown_workflow: true,
    run_already_active: true,
    session_released: true,
    workflow_transition_invalid: true,
    interaction_already_answered: true,
    interaction_expired: true,
    events_pruned: true,
    file_too_large: true,
    unsupported_media_type: true,
    validation_failed: true,
    quality_gate_failed: true,
    cms_object_locked: true,
    mcp_connection_not_found: true,
    mcp_connection_limit: true,
    mcp_authorization_required: true,
    mcp_authorization_rejected: true,
    mcp_unreachable: true,
    cms_request_failed: true,
    model_error: true,
    model_rate_limited: true,
    rate_limited: true,
    service_unavailable: true,
};

const CMS_RESPONSE_CODES: Record<CmsResponseCode, true> = {
    AUTHREQUIRED: true,
    PERMISSION: true,
    NOTFOUND: true,
    INVALIDDATA: true,
    LOCKED: true,
    MAINTENANCEMODE: true,
    NOTLICENSED: true,
    FAILURE: true,
};

function isKnown<T extends string>(codes: Record<T, true>, code: unknown): code is T {
    return typeof code === 'string' && Object.hasOwn(codes, code);
}

// `responseInfo.responseCode` of a CMS response body, if the body has one.
function cmsResponseCode(body: unknown): unknown {
    if (typeof body !== 'object' || body === null) {
        return undefined;
    }

    const { responseInfo } = body as { responseInfo?: unknown };

    return typeof responseInfo === 'object' && responseInfo !== null
        ? (responseInfo as { responseCode?: unknown }).responseCode
        : undefined;
}

function statusMessageKey(status: number): string {
    switch (status) {
        case 401:
            return 'errors.http.unauthorized';
        case 403:
            return 'errors.http.forbidden';
        case 404:
            return 'errors.http.notFound';
        case 429:
            return 'errors.http.tooManyRequests';
        default:
            return status >= 500 ? 'errors.http.server' : 'errors.http.failed';
    }
}

/**
 * The i18n key of a readable sentence for `error`: a quoted passage missing from its file, then by
 * GenAIx `genaix_code`, then by CMS
 * `responseInfo.responseCode`, then by HTTP status. A code this client does not know falls back to
 * the status (contract, `GenaixCode`).
 */
export function errorMessageKey(error: unknown): string {
    if (error instanceof NoCmsConnectionError) {
        return 'errors.noCmsConnection';
    }

    // A verbatim passage named a file it does not occur in (contract, `Problem.errors[].code`).
    if (error instanceof GenaixApiError && error.problem?.errors?.some((fieldError) => fieldError.code === 'passage_not_found')) {
        return 'errors.genaix.passage_not_found';
    }

    if (error instanceof GenaixApiError && isKnown(GENAIX_CODES, error.genaixCode)) {
        return `errors.genaix.${error.genaixCode}`;
    }

    if (error instanceof HttpError) {
        const responseCode = cmsResponseCode(error.body);

        if (!(error instanceof GenaixApiError) && isKnown(CMS_RESPONSE_CODES, responseCode)) {
            return `errors.cms.${responseCode}`;
        }

        return statusMessageKey(error.status);
    }

    // fetch rejects with a TypeError when the request did not get through.
    if (error instanceof TypeError) {
        return 'errors.network';
    }

    // `response.json()` rejects with a SyntaxError for a body that is not JSON.
    if (error instanceof SyntaxError) {
        return 'errors.invalidResponse';
    }

    return 'errors.unknown';
}

/**
 * The i18n key for a `Problem` that arrived on the event stream rather than as a response (`run.failed`,
 * `error`): by `genaix_code`, else by `status`, as `errorMessageKey` does for a response.
 */
export function problemMessageKey(problem: Problem): string {
    return isKnown(GENAIX_CODES, problem.genaix_code) ? `errors.genaix.${problem.genaix_code}` : statusMessageKey(problem.status);
}

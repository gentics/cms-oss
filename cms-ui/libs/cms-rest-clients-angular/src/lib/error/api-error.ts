import { IndexByKey, Response } from '@gentics/cms-models';

/**
 * Defines the reason why an API request failed.
 * - "failed":      The request failed for expectable reasons like "Page is locked", "Failed to save item"
 * - "http"  :      A HTTP error code was returned by the API.
 * - "auth":        The user was logged out, either due to a timeout or by the Maintenance mode
 * - "permissions": An action was requested which the active user has no permissions for.
 * - "exception":   An exception was thrown while processing the response, e.g. parsing invalid JSON.
 * - "invalid_data": An exception was thrown because POST request body was malformed.
 */
export type ApiErrorReason = 'failed' | 'http' | 'auth' | 'permissions' | 'exception' | 'invalid_data';

export interface ApiRequestInfo {
    method: 'GET' | 'POST' | 'PUT' | 'DELETE';
    url: string;
    params: IndexByKey<any>;
    body?: { [key: string]: string | number | boolean };
}

export interface ApiErrorDetails {
    request?: ApiRequestInfo;
    response?: Response | string;
    statusCode?: number;
    originalError?: Error;
}

/**
 * Error class to handle Errors of the Content.Node API
 */
export class ApiError extends Error {
    /** The reason for the Error. One of "failed" / "http" / "auth" / "permissions" / "exception". */
    reason: ApiErrorReason;

    /** An error that was caught, if any. */
    originalError: Error;

    /** Information about the failed request. */
    request: ApiRequestInfo;

    /** The response of the failed request. */
    response: Response;

    /** The status code of the response. */
    statusCode: number;

    /** The stack trace of the error */
    stack: string;

    constructor(message: string, reason: ApiErrorReason, details: ApiErrorDetails = {}) {
        super(message);
        const set = (name: string, value: any) => Object.defineProperty(this, name, { value, enumerable: true, configurable: true });

        // Set the prototype explicitly.
        // See https://github.com/Microsoft/TypeScript/wiki/FAQ#why-doesnt-extending-built-ins-like-error-array-and-map-work
        Object.setPrototypeOf(this, ApiError.prototype);

        set('message', message);
        set('name', 'ApiError');
        set('originalError', details.originalError);
        set('reason', reason);
        set('request', details.request);
        set('response', details.response);
        set('statusCode', details.statusCode);

        if (typeof (Error as any).captureStackTrace === 'function') {
            (Error as any).captureStackTrace(this, this.constructor);
        } else {
            let stack: string = (new Error() as any).stack || '';
            set('stack', stack.replace(/Error\n    at[^\n]+/, 'ApiError'));
        }
    }
}

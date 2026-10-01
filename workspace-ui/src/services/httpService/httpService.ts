// A response that was not successful. Carries the status and, when there was one, the response
// body parsed as JSON and the response headers.
export class HttpError extends Error {
    readonly status: number;
    /** The response body parsed as JSON; `undefined` when it was empty or not JSON. */
    readonly body?: unknown;
    readonly headers?: Headers;

    constructor(url: string, status: number, body?: unknown, headers?: Headers) {
        super(`Request to ${url} failed with status ${status}`);
        this.name = 'HttpError';
        this.status = status;
        this.body = body;
        this.headers = headers;
    }
}

/** Builds the error `httpRequest` throws for a response that was not successful. */
export type ToHttpError = (url: string, response: Response) => Promise<Error>;

// Reads the body as JSON, if it is JSON, into an `HttpError`.
export async function toHttpError(url: string, response: Response): Promise<HttpError> {
    let body: unknown;

    try {
        body = await response.json();
    } catch {
        body = undefined;
    }

    return new HttpError(url, response.status, body, response.headers);
}

// Sends a request and returns its JSON body. The body is typed by the caller and not
// validated at runtime. A response that is not successful is thrown as `toError` builds it, by
// default an `HttpError`.
export async function httpRequest<T>(url: string, init?: RequestInit, toError: ToHttpError = toHttpError): Promise<T> {
    const response = await fetch(url, init);

    if (!response.ok) {
        throw await toError(url, response);
    }

    return response.json() as Promise<T>;
}

// A response that was not successful. Carries the status, never the response body.
export class HttpError extends Error {
    readonly status: number;

    constructor(url: string, status: number) {
        super(`Request to ${url} failed with status ${status}`);
        this.name = 'HttpError';
        this.status = status;
    }
}

// Sends a request and returns its JSON body. The body is typed by the caller and not
// validated at runtime.
export async function httpRequest<T>(url: string, init?: RequestInit): Promise<T> {
    const response = await fetch(url, init);

    if (!response.ok) {
        throw new HttpError(url, response.status);
    }

    return response.json() as Promise<T>;
}

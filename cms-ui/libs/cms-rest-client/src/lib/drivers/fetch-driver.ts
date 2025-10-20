import { Response as GCMSResponse } from '@gentics/cms-models';
import { GCMSRestClientAbortError, GCMSRestClientRequestError } from '../errors';
import { GCMSClientDriver, GCMSRestClientRequest, GCMSRestClientRequestData } from '../models';
import { validateResponseObject } from '../utils';

export async function parseFetchErrorFromAPI(request: GCMSRestClientRequestData, res: Response): Promise<never> {
    let raw: string;
    let parsed: GCMSResponse;
    let bodyError: Error;

    try {
        raw = await res.text();
        try {
            parsed = JSON.parse(raw);
        } catch (err) {
            bodyError = err;
        }
    } catch (err) {
        bodyError = err;
    }

    throw new GCMSRestClientRequestError(
        `Request "${request.method} ${request.url}" responded with error code ${res.status}: "${res.statusText}"`,
        request,
        res.status,
        raw,
        parsed,
        bodyError,
    );
}

export async function jsonFetchResponseHandler<T>(request: GCMSRestClientRequestData, res: Response): Promise<T> {
    if (res.ok) {
        return res.json().then(json => {
            validateResponseObject(request, json, res.status);
            return json;
        }).catch(err => {
            throw new Error(`Unexpected error while parsing response-data from "${request.method} ${request.url}"`, { cause: err });
        });
    }

    await parseFetchErrorFromAPI(request, res);
}

export async function textFetchResponseHandler(request: GCMSRestClientRequestData, res: Response): Promise<string> {
    if (res.ok) {
        return res.text();
    }

    await parseFetchErrorFromAPI(request, res);
}

export async function blobFetchResponseHandler(request: GCMSRestClientRequestData, res: Response): Promise<Blob> {
    if (res.ok) {
        return res.blob();
    }

    await parseFetchErrorFromAPI(request, res);
}

export class GCMSFetchDriver implements GCMSClientDriver {

    /**
     * The session secret which should be sent when this driver is `encapsulated`.
     */
    // private sessionSecret: string | null;

    /*
     * @param encapsulated If this driver should not rely on the browser/fetch cookie handling,
     * but should handle the session-cookie manually. Useful when having to use multiple instances
     * of the client or when no regular cookie management exists on the platform.
     */
    constructor(
        // private encapsulated: boolean = false,
    ) {}

    performMappedRequest<T>(
        request: GCMSRestClientRequestData,
        body?: null | string | FormData,
    ): GCMSRestClientRequest<T> {
        // If the content is a simple object and not a form-data, then we convert it
        // to json, since `fetch` doesn't do it automatically.
        if (body != null && typeof body === 'object' && !(body instanceof FormData)) {
            body = JSON.stringify(body);
        }

        return this.prepareRequest(request, (fullUrl) => {
            return {
                method: request.method,
                url: fullUrl,
                headers: request.headers,
                body: body,
            } as any;
        }, (res) => jsonFetchResponseHandler<T>(request, res));
    }

    performFormRequest<T>(
        request: GCMSRestClientRequestData,
        form: FormData,
    ): GCMSRestClientRequest<T> {
        return this.prepareRequest(request, (fullUrl) => {
            return {
                method: request.method,
                url: fullUrl,
                headers: request.headers,
                body: form,
            } as any;
        }, (res) => jsonFetchResponseHandler<T>(request, res));
    }

    performRawRequest(
        request: GCMSRestClientRequestData,
        body?: null | any,
    ): GCMSRestClientRequest<string> {
        return this.prepareRequest(request, (fullUrl) => {
            return {
                method: request.method,
                url: fullUrl,
                headers: request.headers,
                body: body,
            } as any;
        }, (res) => textFetchResponseHandler(request, res));
    }

    performDownloadRequest(
        request: GCMSRestClientRequestData,
        body?: string | FormData,
    ): GCMSRestClientRequest<Blob> {
        // If the content is a simple object and not a form-data, then we convert it
        // to json, since `fetch` doesn't do it automatically.
        if (body != null && typeof body === 'object' && !(body instanceof FormData)) {
            body = JSON.stringify(body);
        }

        return this.prepareRequest(request, (fullUrl) => {
            return {
                method: request.method,
                url: fullUrl,
                headers: request.headers,
                body: body,
            } as any;
        }, (res) => blobFetchResponseHandler(request, res));
    }

    // protected handleEncapsulatedResponse(response: Response): void {
    //     if (!this.encapsulated) {
    //         return;
    //     }
    //     if (response.headers.has(SESSION_RESPONSE_HEADER)) {
    //         this.sessionSecret = response.headers.get(SESSION_RESPONSE_HEADER);
    //     }
    // }

    /**
     * Interceptor function which can be overriden.
     * Useful for modifications from/to the response data (Headers, Response-Code, etc) which would be
     * absent from the parsed JSON body.
     *
     * @param request The request that has been sent.
     * @param response The response from the API without any prior handling.
     * @returns The response that should be processed/forwarded to the client.
     */
    protected responseInterceptor(request: GCMSRestClientRequestData, response: Response): Promise<Response> {
        // this.handleEncapsulatedResponse(response);
        return Promise.resolve(response);
    }

    protected prepareRequest<T>(
        request: GCMSRestClientRequestData,
        fn: (fullUrl: string) => RequestInfo,
        handler: (res: Response) => Promise<T>,
    ): GCMSRestClientRequest<T> {
        let fullUrl = request.url;
        if (request.params) {
            const q = new URLSearchParams();

            Object.entries(request.params).forEach(([key, value]) => {
                if (Array.isArray(value)) {
                    value.forEach(v => q.append(key, v));
                } else {
                    q.append(key, value);
                }
            });

            const params = q.toString();
            if (params) {
                fullUrl += `?${params}`;
            }
        }

        const abortController = new AbortController();
        let sentRequest: Promise<T> | null = null;

        const sendRequest = () => {
            if (sentRequest != null) {
                return sentRequest;
            }

            const options: RequestInfo = {
                ...fn(fullUrl) as any,
                signal: abortController.signal,
                // credentials: this.encapsulated ? 'omit' : 'same-origin',
            };

            // if (this.encapsulated) {
            //     (options as RequestInit).headers ??= {};
            //     (options as RequestInit).headers[SESSION_REQUEST_HEADER] = this.sessionSecret;
            // }

            sentRequest = fetch(options)
                .then(res => handler(res));

            return sentRequest;
        }

        return {
            cancel: () => abortController.abort(new GCMSRestClientAbortError(request)),
            send: () => sendRequest(),
        };
    }
}

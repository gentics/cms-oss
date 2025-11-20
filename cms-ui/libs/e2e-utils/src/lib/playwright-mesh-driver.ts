import { GenericErrorResponse } from '@gentics/mesh-models';
import {
    MeshClientDriver,
    MeshRestClientAbortError,
    MeshRestClientRequestData,
    MeshRestClientRequestError,
    MeshRestClientResponse,
} from '@gentics/mesh-rest-client';
import { APIRequestContext, APIResponse } from '@playwright/test';

async function parseFetchErrorFromAPI(request: MeshRestClientRequestData, res: APIResponse): Promise<never> {
    let raw: string;
    let parsed: GenericErrorResponse;
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

    throw new MeshRestClientRequestError(
        `Request "${request.method} ${request.url}" responded with error code ${res.status()}: "${res.statusText()}"`,
        request,
        res.status(),
        raw,
        parsed,
        bodyError,
    );
}

async function jsonFetchResponseHandler<T>(request: MeshRestClientRequestData, res: APIResponse): Promise<T> {
    if (res.ok) {
        return res.json()
            .catch((err) => {
                if (err instanceof SyntaxError) {
                    return res.text().then((data) => {
                        if (!data) {
                            return {};
                        }
                        throw err;
                    });
                }
                throw err;
            });
    }

    return await parseFetchErrorFromAPI(request, res);
}

async function textFetchResponseHandler(request: MeshRestClientRequestData, res: APIResponse): Promise<string> {
    if (res.ok) {
        return res.text();
    }

    return await parseFetchErrorFromAPI(request, res);
}

async function blobFetchResponseHandler(request: MeshRestClientRequestData, res: APIResponse): Promise<Blob> {
    if (res.ok) {
        return res.body().then((buffer) => new Blob([buffer as any]));
    }

    return await parseFetchErrorFromAPI(request, res);
}

interface RequestData {
    url: string;
    body?: string | any;
    headers?: Record<string, string>;
    method?: string;
}

export class PlaywrightMeshDriver implements MeshClientDriver {

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
        public context: APIRequestContext,
    ) {}

    performJsonRequest<T>(
        request: MeshRestClientRequestData,
        body?: string | any,
    ): MeshRestClientResponse<T> {
        if (body != null && typeof body === 'object') {
            body = JSON.stringify(body);
        }

        return this.prepareRequest(request, (fullUrl) => {
            return {
                method: request.method,
                url: fullUrl,
                headers: request.headers,
                body: body,
            };
        }, (res) => jsonFetchResponseHandler<T>(request, res));
    }

    performFormRequest<T>(
        request: MeshRestClientRequestData,
        form: FormData,
    ): MeshRestClientResponse<T> {
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
        request: MeshRestClientRequestData,
        body?: string | FormData,
    ): MeshRestClientResponse<string> {
        return this.prepareRequest(request, (fullUrl) => {
            return {
                method: request.method,
                url: fullUrl,
                headers: request.headers,
                body: body,
            };
        }, (res) => textFetchResponseHandler(request, res));
    }

    performDownloadRequest(
        request: MeshRestClientRequestData,
        body?: string | FormData,
    ): MeshRestClientResponse<Blob> {
        return this.prepareRequest(request, (fullUrl) => {
            return {
                method: request.method,
                url: fullUrl,
                headers: request.headers,
                body: body,
            };
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
     * @param request The request that has been sent.
     * @param response The response from the API without any prior handling.
     * @returns The response that should be processed/forwarded to the client.
     */
    protected responseInterceptor(request: MeshRestClientRequestData, response: Response): Promise<Response> {
        // this.handleEncapsulatedResponse(response);
        return Promise.resolve(response);
    }

    protected prepareRequest<T>(
        request: MeshRestClientRequestData,
        fn: (fullUrl: string) => RequestData,
        handler: (res: APIResponse) => Promise<T>,
    ): MeshRestClientResponse<T> {
        let fullUrl = request.url;
        if (request.params) {
            const q = new URLSearchParams();

            Object.entries(request.params).forEach(([key, value]) => {
                if (Array.isArray(value)) {
                    value.forEach((v) => q.append(key, v));
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

            const options: RequestData = {
                ...fn(fullUrl) as any,
                signal: abortController.signal,
            };

            sentRequest = this.context.fetch(options.url, {
                data: options.body,
                headers: options.headers,
                method: options.method,
            })
                .then((res) => handler(res));

            return sentRequest;
        };

        return {
            cancel: () => abortController.abort(new MeshRestClientAbortError(request)),
            send: () => sendRequest(),
        };
    }
}

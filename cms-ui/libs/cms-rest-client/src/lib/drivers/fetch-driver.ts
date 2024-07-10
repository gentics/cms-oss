import { Response as GCMSResponse } from '@gentics/cms-models';
import { GCMSRestClientRequestError } from '../errors';
import { GCMSClientDriver, GCMSRestClientRequestData, GCMSRestClientRequest } from '../models';
import { validateResponseObject } from '../utils';

async function parseErrorFromAPI<T>(request: GCMSRestClientRequestData, res: Response): Promise<T> {
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

async function jsonResponseHandler<T>(request: GCMSRestClientRequestData, res: Response): Promise<T> {
    if (res.ok) {
        return res.json().then(json => {
            validateResponseObject(request, json);
            return json;
        });
    }

    return parseErrorFromAPI(request, res);
}

async function textResponseHandler(request: GCMSRestClientRequestData, res: Response): Promise<string> {
    if (res.ok) {
        return res.text();
    }

    return parseErrorFromAPI(request, res);
}

async function blobResponseHandler(request: GCMSRestClientRequestData, res: Response): Promise<Blob> {
    if (res.ok) {
        return res.blob();
    }

    return parseErrorFromAPI(request, res);
}

export class GCMSFetchDriver implements GCMSClientDriver {

    performMappedRequest<T>(
        request: GCMSRestClientRequestData,
        body?: string | any,
    ): GCMSRestClientRequest<T> {
        if (body != null && typeof body === 'object') {
            body = JSON.stringify(body);
        }

        return this.prepareRequest(request, (fullUrl) => {
            return {
                method: request.method,
                url: fullUrl,
                headers: request.headers,
                body: body,
            } as any;
        }, (res) => jsonResponseHandler<T>(request, res));
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
        }, (res) => jsonResponseHandler<T>(request, res));
    }

    performRawRequest(
        request: GCMSRestClientRequestData,
        body?: string | FormData,
    ): GCMSRestClientRequest<string> {
        return this.prepareRequest(request, (fullUrl) => {
            return {
                method: request.method,
                url: fullUrl,
                headers: request.headers,
                body: body,
            } as any;
        }, (res) => textResponseHandler(request, res));
    }

    performDownloadRequest(
        request: GCMSRestClientRequestData,
        body?: string | FormData,
    ): GCMSRestClientRequest<Blob> {
        return this.prepareRequest(request, (fullUrl) => {
            return {
                method: request.method,
                url: fullUrl,
                headers: request.headers,
                body: body,
            } as any;
        }, (res) => blobResponseHandler(request, res));
    }

    private prepareRequest<T>(
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

        function sendRequest() {
            const options: RequestInfo = {
                ...fn(fullUrl) as any,
                signal: abortController.signal,
            }
            return fetch(options)
                .then(res => handler(res));
        }

        return {
            cancel: () => abortController.abort(),
            send: () => sendRequest(),
        };
    }
}

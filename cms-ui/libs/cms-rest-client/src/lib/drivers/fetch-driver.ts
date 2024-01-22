import { Response as GCMSResponse } from '@gentics/cms-models';
import { RequestFailedError } from '../errors';
import { GCMSClientDriver, GCMSRestClientRequest, GCMSRestClientResponse } from '../models';
import { validateResponseObject } from '../utils';

async function parseErrorFromAPI<T>(request: GCMSRestClientRequest, res: Response): Promise<T> {
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

    throw new RequestFailedError(
        `Request "${request.method} ${request.url}" responded with error code ${res.status}: "${res.statusText}"`,
        request,
        res.status,
        raw,
        parsed,
        bodyError,
    );
}

async function jsonResponseHandler<T>(request: GCMSRestClientRequest, res: Response): Promise<T> {
    if (res.ok) {
        return res.json().then(json => {
            validateResponseObject(request, json);
            return json;
        });
    }

    return parseErrorFromAPI(request, res);
}

async function textResponseHandler(request: GCMSRestClientRequest, res: Response): Promise<string> {
    if (res.ok) {
        return res.text();
    }

    return parseErrorFromAPI(request, res);
}

async function blobResponseHandler(request: GCMSRestClientRequest, res: Response): Promise<Blob> {
    if (res.ok) {
        return res.blob();
    }

    return parseErrorFromAPI(request, res);
}

export class GCMSFetchDriver implements GCMSClientDriver {

    performMappedRequest<T>(
        request: GCMSRestClientRequest,
        body?: string | any,
    ): GCMSRestClientResponse<T> {
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
        request: GCMSRestClientRequest,
        form: FormData,
    ): GCMSRestClientResponse<T> {
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
        request: GCMSRestClientRequest,
        body?: string | FormData,
    ): GCMSRestClientResponse<string> {
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
        request: GCMSRestClientRequest,
        body?: string | FormData,
    ): GCMSRestClientResponse<Blob> {
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
        request: GCMSRestClientRequest,
        fn: (fullUrl: string) => RequestInfo,
        handler: (res: Response) => Promise<T>,
    ): GCMSRestClientResponse<T> {
        let fullUrl = request.url;
        if (request.params) {
            const params = new URLSearchParams(request.params).toString();
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

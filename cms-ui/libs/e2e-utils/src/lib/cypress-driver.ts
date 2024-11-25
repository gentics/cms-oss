import { Response as GCMSResponse } from '@gentics/cms-models';
import {
    GCMS_ERROR_INSTANCE,
    GCMSRestClientRequestError,
    validateResponseObject,
    type GCMSClientDriver,
    type GCMSRestClientRequest,
    type GCMSRestClientRequestData,
} from '@gentics/cms-rest-client';

type ErrObj = {
    [GCMS_ERROR_INSTANCE]: true,
    message: string,
    request: GCMSRestClientRequestData,
    responseCode: number,
    rawBody?: string,
    data?: GCMSResponse,
    bodyError?: Error,
}

/**
 * This hacky function exists, because when creating a new Error with the regular constructor:
 * ```ts
 * return new GCMSRestClientRequestError(...);
 * ```
 * is somehow automatically catched by Cypress, and causes the entire test to instantly fail.
 * Additionally, setting the prototype afterwards also seems to have the same effect.
 * Therefore we override the `Symbol.isInstance` behavior of the class to make it possible to use it like this.
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/instanceof#instanceof_and_symbol.hasinstance
 */
function createError(request: GCMSRestClientRequestData, res: Cypress.Response<GCMSResponse>): GCMSRestClientRequestError {
    let message = `Request "${request.method} ${request.url}" responded with error code "${res.status} "${res.statusText}"`;
    let resMsg = res.body?.responseInfo?.responseMessage;

    if (!resMsg) {
        resMsg = res.body?.messages?.[0]?.message;
    }

    if (resMsg) {
        message += `. Details: "${resMsg}"`;
    }

    const obj: ErrObj = {
        [GCMS_ERROR_INSTANCE]: true,
        message,
        request,
        responseCode: res.status,
        data: res.body,
    };

    return obj as any;
}

/**
 * Driver which has to be used when doing setup work via the GCMS-REST API
 * in cypress tests.
 * Note that this driver should stictly only be used in the cypress tests/commands,
 * but *not* in any other context.
 * Uses the `cy.request` function to perform the requests correctly.
 */
export class CypressDriver implements GCMSClientDriver {

    constructor(
        public log: boolean,
    ) {}

    protected prepareRequest<T>(
        request: GCMSRestClientRequestData,
        fn: (fullUrl: string) => Partial<Cypress.RequestOptions>,
        isBinary: boolean,
    ): GCMSRestClientRequest<T> {
        let sentRequest: Promise<T> | null = null;

        const sendRequest = () => {
            if (sentRequest != null) {
                return sentRequest;
            }

            sentRequest = new Promise((resolve, reject) => {
                cy.request({
                    log: this.log,
                    url: request.url,
                    qs: request.params,
                    method: request.method,
                    headers: request.headers,
                    failOnStatusCode: false,
                    ...fn(request.url),
                }).then(res => {
                    let body = res.body;
                    let valid = res.isOkStatusCode;

                    // If it isn't a binary response, then we can properly parse the body and inspect it for potential error messages
                    if (!isBinary && body != null) {
                        if (Cypress.Buffer.isBuffer(body)) {
                            body = body.toJSON();
                        } else if (Buffer.isBuffer(body)) {
                            body = body.toJSON();
                        } else if (
                            // Very hacky, but the types are different between the runtimes which causes this issue
                            body instanceof ArrayBuffer
                            || Object.getPrototypeOf(body).constructor.name === 'ArrayBuffer'
                        ) {
                            const decoder = new TextDecoder('utf-8', { fatal: true });
                            body = decoder.decode(body);
                            body = JSON.parse(body);
                        } else if (typeof body === 'string') {
                            try {
                                body = JSON.parse(body);
                            } catch (error) {
                                valid = false;
                                const err = createError(request, res);
                                cy.log('Error while from the response body', error, body, err);
                                reject(err);
                                return Promise.resolve(body);
                            }
                        }

                        try {
                            const err = validateResponseObject(request, body);
                            valid = err == null;
                        } catch (err) {
                            valid = false;
                        }
                    }

                    if (!valid) {
                        const err = createError(request, res);
                        reject(err);
                        return Promise.resolve(body);
                    }

                    resolve(body);
                    return Promise.resolve(body);
                });
            });

            return sentRequest;
        }

        return {
            send: () => sendRequest(),
            cancel: () => {
                // Cypress-Requests can't be canceled
            },
        };
    }

    performMappedRequest<T>(
        request: GCMSRestClientRequestData,
        body?: string | FormData | null | undefined,
    ): GCMSRestClientRequest<T> {
        return this.prepareRequest(request, () => ({
            body: body,
        }), false);
    }

    performRawRequest(
        request: GCMSRestClientRequestData,
        body?: string | FormData | null | undefined,
    ): GCMSRestClientRequest<string> {
        return this.prepareRequest(request, () => ({
            body: body,
        }), false);
    }

    performDownloadRequest(
        request: GCMSRestClientRequestData,
        body?: string | FormData | null | undefined,
    ): GCMSRestClientRequest<Blob> {
        return this.prepareRequest(request, () => ({
            body: body,
            encoding: 'binary',
        }), true);
    }
}

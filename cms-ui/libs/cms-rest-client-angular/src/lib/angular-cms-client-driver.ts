import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Response as GCMSResponse } from '@gentics/cms-models';
import {
    GCMSClientDriver,
    GCMSRestClientAbortError,
    GCMSRestClientRequestData,
    GCMSRestClientRequestError,
    validateResponseObject,
} from '@gentics/cms-rest-client';
import { Observable, OperatorFunction, Subscription, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { NGGCMSRestClientRequest } from './models';

function asSafeJSON(request: GCMSRestClientRequestData, str: string | null, statusCode: number) {
    const value = typeof str !== 'string' || str.trim().length === 0 ? str : JSON.parse(str);
    validateResponseObject(request, value, statusCode);
    return value;
}

@Injectable()
export class AngularGCMSClientDriver implements GCMSClientDriver {

    constructor(
        private http: HttpClient,
    ) {}

    protected createStringRequest<T>(
        request: GCMSRestClientRequestData,
        body: null | string | FormData,
        bodyHandler: (body: string, status: number) => T,
    ): Observable<T> {
        let q = new HttpParams();

        Object.entries(request.params).forEach(([key, value]) => {
            if (Array.isArray(value)) {
                value.forEach((v, i) => q = (i === 0 ? q.set(key, v) : q.append(key, v)));
            } else {
                q = q.set(key, value);
            }
        });

        return this.http.request(request.method, request.url, {
            body,
            headers: request.headers,
            params: q,
            observe: 'response',
            responseType: 'text',
        }).pipe(
            map(res => {
                if (res.ok) {
                    return bodyHandler(res.body, res.status);
                }

                throw new HttpErrorResponse({
                    headers: new HttpHeaders(request.headers),
                    status: res.status,
                    statusText: res.statusText,
                    error: res.body,
                    url: request.url,
                });
            }),
            this.errorHandler(request),
        );
    }

    protected createBlobRequest(
        request: GCMSRestClientRequestData,
        body: null | string | FormData,
    ): Observable<Blob> {
        return this.http.request(request.method, request.url, {
            body,
            headers: request.headers,
            params: request.params,
            observe: 'response',
            responseType: 'blob',
        }).pipe(
            map(res => {
                if (res.ok) {
                    return res.body;
                }

                throw new HttpErrorResponse({
                    headers: new HttpHeaders(request.headers),
                    status: res.status,
                    statusText: res.statusText,
                    error: res.body,
                    url: request.url,
                });
            }),
            this.errorHandler(request),
        );
    }

    protected errorHandler<T>(request: GCMSRestClientRequestData): OperatorFunction<T, T> {
        return catchError(err => {
            if (!(err instanceof HttpErrorResponse)) {
                return throwError(() => new Error(`Unexpected error while performing request "${request.method} ${request.url}"`, { cause: err }));
            }

            let raw: string;
            let parsed: GCMSResponse;
            let bodyError: Error;

            try {
                raw = err.error;
                try {
                    parsed = JSON.parse(raw);
                } catch (err) {
                    bodyError = err;
                }
            } catch (err) {
                bodyError = err;
            }

            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            return throwError(() => new GCMSRestClientRequestError(
                // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                `Request "${request.method} ${request.url}" responded with error code ${err.status}: "${err.statusText}"`,
                request,
                err.status,
                raw,
                parsed,
                bodyError,
            ));
        });
    }

    protected createClientResponse<T>(obs: Observable<T>, request: GCMSRestClientRequestData): NGGCMSRestClientRequest<T> {
        let promiseSub: Subscription;
        let canceled = false;

        return {
            rx: () => obs,
            send: () => {
                if (canceled) {
                    return Promise.reject(new GCMSRestClientAbortError(request));
                }
                return new Promise((resolve, reject) => {
                    let tmpValue;
                    let hasValue = false;
                    let isMultiValue = false;

                    promiseSub = obs.subscribe({
                        next: (value) => {
                            if (!hasValue) {
                                tmpValue = value;
                                hasValue = true;
                            } else if (!isMultiValue) {
                                tmpValue = [tmpValue, value];
                                isMultiValue = true;
                            } else {
                                (tmpValue as any[]).push(value);
                            }
                        },
                        complete: () => {
                            resolve(tmpValue);
                        },
                        error: (err) => reject(err),
                    });
                });
            },
            cancel: () => {
                if (promiseSub) {
                    promiseSub.unsubscribe();
                    promiseSub = null;
                }
                canceled = true;
            },
        };
    }

    performMappedRequest<T>(
        request: GCMSRestClientRequestData,
        body?: string,
    ): NGGCMSRestClientRequest<T> {
        const obs = this.createStringRequest(request, body, (res, status) => asSafeJSON(request, res, status));
        return this.createClientResponse(obs, request);
    }

    performRawRequest(
        request: GCMSRestClientRequestData,
        body?: string | FormData,
    ): NGGCMSRestClientRequest<string> {
        const obs = this.createStringRequest(request, body, (str) => str);
        return this.createClientResponse(obs, request);
    }

    performDownloadRequest(
        request: GCMSRestClientRequestData,
        body?: string | FormData,
    ): NGGCMSRestClientRequest<Blob> {
        const obs = this.createBlobRequest(request, body);
        return this.createClientResponse(obs, request);
    }
}

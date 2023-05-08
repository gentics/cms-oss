import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams, HttpResponse, HttpResponseBase } from '@angular/common/http';
import { Inject, Injectable, InjectionToken, OnDestroy } from '@angular/core';

import {
    ElasticSearchQueryResponse,
    FolderListOptions,
    FolderListRequest,
    FormListRequest,
    InheritableItem,
    PageListOptions,
    PageListRequest,
    PagingSortOption,
    Raw,
    Response,
    ResponseMessage,
} from '@gentics/cms-models';
import { Observable, Subscription, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ApiError, ApiErrorReason, ApiRequestInfo } from '../error/api-error';
import { GcmsApiErrorHandler } from '../error/error-handler';
import { FileUploaderFactory } from '../util/file-uploader/file-uploader.factory';
import { FileUploader } from '../util/file-uploader/file-uploader.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * This injection token is used to declare the base URL of the CMS.
 */
export const GCMS_API_BASE_URL = new InjectionToken<string>('API_BASE_URL');

/**
 * This injection token is used to provide an Observable for the session ID to be used in the GCMS API requests.
 */
export const GCMS_API_SID = new InjectionToken<Observable<number>>('GCMS_API_SID');

/**
 * This injection token is used to provide the error handler used by the GcmsApi classes.
 */
export const GCMS_API_ERROR_HANDLER = new InjectionToken<GcmsApiErrorHandler>('GCMS_API_ERROR_HANDLER');

export const GENERIC_REQUEST_FAILED_ERROR = 'Request failed error';

/**
 * Possible options for an HttpRequest.
 * Copied and modified from https://github.com/angular/angular/blob/7.2.4/packages/common/http/src/client.ts
 * This is exported only so that it can be reused in the tests.
 */
export type HttpRequestOptions = HttpFullJsonRequestOptions | HttpBodyOnlyJsonRequestOptions | HttpBodyOnlyBlobRequestOptions;

export interface HttpFullJsonRequestOptions {
    body?: any;
    /** Immutable set of HTTP headers. Each modification returns a new set. */
    headers?: HttpHeaders | {
        [header: string]: string | string[];
    };
    observe: 'response';
    /** Immutable set of query parameters. Each modification returns a new set. */
    params?: HttpParams;
    reportProgress?: boolean;
    responseType: 'json';
    withCredentials?: boolean;
}

export interface HttpBodyOnlyJsonRequestOptions {
    body?: any;
    /** Immutable set of HTTP headers. Each modification returns a new set. */
    headers?: HttpHeaders | {
        [header: string]: string | string[];
    };
    observe: 'body';
    /** Immutable set of query parameters. Each modification returns a new set. */
    params?: HttpParams;
    reportProgress?: boolean;
    responseType: 'json';
    withCredentials?: boolean;
}

export interface HttpBodyOnlyBlobRequestOptions {
    body?: any;
    /** Immutable set of HTTP headers. Each modification returns a new set. */
    headers?: HttpHeaders | {
        [header: string]: string | string[];
    };
    observe: 'body';
    /** Immutable set of query parameters. Each modification returns a new set. */
    params?: HttpParams;
    reportProgress?: boolean;
    responseType: 'blob';
    withCredentials?: boolean;
}

/**
 * This is the service that actually makes HTTP calls. It should only be used
 * by the other API classes - not inside the rest of the app.
 */
@Injectable()
export class ApiBase implements OnDestroy {

    private subscriptions = new Subscription();
    private sid: number;

    constructor(
        private http: HttpClient,
        private uploaderFactory: FileUploaderFactory,
        @Inject(GCMS_API_BASE_URL) private apiBaseUrl: string,
        @Inject(GCMS_API_SID) sid$: Observable<number>,
        @Inject(GCMS_API_ERROR_HANDLER) private errorHandler: GcmsApiErrorHandler,
    ) {
        this.subscriptions.add(sid$.subscribe(sid => { this.sid = sid; }));
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    get<T extends Response>(url: string, params: any = {}): Observable<T> {
        const options = this.getDefaultOptions();

        // Append a timestamp to all GET requests to force cache-busting in IE11
        params['gcms_ts'] = Date.now().toString(10);

        options.params = this.encodeParamsAndAddSid(params);
        let response: HttpResponse<T>;

        return this.http.get<T>(`${this.apiBaseUrl}/${url}`, options).pipe(
            map(res => (response = res).body),
            catchError(err => this.handleRequestError(err, response, { method: 'GET', url, params })),
            map(json => this.checkResponseForError(json, response, { method: 'GET', url, params })),
        );
    }

    getBlob(url: string, params: any = {}): Observable<Blob> {
        // Append a timestamp to all GET requests to force cache-busting in IE11
        params['gcms_ts'] = Date.now().toString(10);

        const options: HttpBodyOnlyBlobRequestOptions = {
            observe: 'body',
            responseType: 'blob',
            headers: {
                'Content-Type': 'application/json',
            },
            params: this.encodeParamsAndAddSid(params),
        }

        return this.http.get(`${this.apiBaseUrl}/${url}`, options);
    }

    post<T extends Response>(url: string, body: any, params: any = {}): Observable<T> {
        const bodyJSON: string = typeof body === 'string' ? body : JSON.stringify(body);
        const options = this.getDefaultOptions();
        let response: HttpResponse<T>;
        options.params = this.encodeParamsAndAddSid(params);

        return this.http.post<T>(`${this.apiBaseUrl}/${url}`, bodyJSON, options).pipe(
            map(res => (response = res).body),
            catchError(error => {
                if (!response && error instanceof HttpResponse) {
                    response = error;
                }
                return this.handleRequestError(error, response, { method: 'POST', url, params, body });
            }),
            map(json => {
                if (this.isElasticSearchResponse(json)) {
                    return json;
                } else {
                    return this.checkResponseForError(json, response, { method: 'POST', url, params, body });
                }
            }),
        );
    }

    put<T extends Response>(url: string, body: any, params: any = {}): Observable<T> {
        const bodyJSON: string = typeof body === 'string' ? body : JSON.stringify(body);
        const options = this.getDefaultOptions();
        let response: HttpResponse<T>;
        options.params = this.encodeParamsAndAddSid(params);

        return this.http.put<T>(`${this.apiBaseUrl}/${url}`, bodyJSON, options).pipe(
            map(res => (response = res).body),
            catchError(error => {
                if (!response && error instanceof HttpResponse) {
                    response = error;
                }
                return this.handleRequestError(error, response, { method: 'PUT', url, params, body });
            }),
            map(json => this.checkResponseForError(json, response, { method: 'PUT', url, params, body })),
        );
    }

    delete<T extends Response | void>(url: string, params: any = {}): Observable<T> {
        const options = this.getDefaultOptions();
        options.params = this.encodeParamsAndAddSid(params);
        let response: HttpResponse<T>;

        return this.http.delete<T>(`${this.apiBaseUrl}/${url}`, options).pipe(
            map(res => {
                response = res;
                // If the status code is 200, the response has a body (T is not void), otherwise it does not (T is void).
                return res.status === 200 ? res.body : undefined;
            }),
            catchError(err => this.handleRequestError(err, response, { method: 'DELETE', url, params })),
            map(json => {
                if (json && response) {
                    // If we have a JSON body, T cannot be void.
                    return this.checkResponseForError(json as Exclude<T, void>, response as HttpResponse<Exclude<T, void>>, {
                        method: 'DELETE',
                        url,
                        params,
                    });
                } else {
                    return undefined;
                }
            }),
        );
    }

    upload(files: File[], url: string, options: { fileField: string, fileNameField?: string, params?: any }, fileName?: string)
        : FileUploader {
        const uploader = this.uploaderFactory.create();
        uploader.setOptions({
            url: `${this.apiBaseUrl}/${url}`,
            fileField: options.fileField,
            fileNameField: options.fileNameField,
            parameters: Object.assign({ sid: this.sid }, options.params),
        });

        files.forEach(file => uploader.upload(file, {}, fileName));

        return uploader;
    }


    createListRequest(parentId: number, options?: PageListOptions): PageListRequest;
    createListRequest(parentId: number, options?: PageListOptions): FormListRequest;
    createListRequest(parentId: number, options?: FolderListOptions): FolderListRequest;
    createListRequest(parentId: number, options?: any): any {
        const request: FolderListRequest = {
            privilegeMap: true,
            id: parentId,
            maxItems: -1,
            recursive: false,
            skipCount: 0,
            sortby: 'name',
            sortorder: 'asc',
            tree: false,
            wastebin: 'exclude',
        };
        Object.assign(request, options || {});
        return request;
    }

    private isElasticSearchResponse(responseBody: any): responseBody is ElasticSearchQueryResponse<InheritableItem<Raw>> {
        return responseBody.hasOwnProperty('_shards') &&
            responseBody.hasOwnProperty('hits');
    }

    /**
     * Sets the basic options required to work with the GCMS Rest Api.
     */
    private getDefaultOptions(): HttpFullJsonRequestOptions {
        const options: HttpRequestOptions = {
            headers: new HttpHeaders({
                'Content-Type': 'application/json',
                Accept: 'application/json',
            }),
            observe: 'response',
            responseType: 'json',
        };
        return options;
    }

    /**
     * Given an object of key-value pairs, construct an HttpParams
     * object from them to be used in an HTTP request.
     */
    private encodeParamsAndAddSid(paramsObject: any): HttpParams {
        // all requests need the current session id.
        if (this.sid) {
            paramsObject['sid'] = String(this.sid);
        }

        // it is possible to specify multiple "type" keys for list requests, e.g.
        // `&type=file&type=image`, but HttpParams handles this already.
        return new HttpParams({ fromObject: paramsObject });
    }

    /** Checks for errors as they are returned from the API with HTTP 200. */
    private checkResponseForError<T extends Response>(responseBody: T, response: HttpResponse<T>, request: ApiRequestInfo): T {
        try {
            let reason: ApiErrorReason;

            // REST API DevTools packages entities response lacks data envelopment. Thus `responseInfo` is not available.
            if (responseBody && responseBody.responseInfo) {
                const { responseCode } = responseBody.responseInfo || ({} as any);
                const messages: ResponseMessage[] = responseBody.messages || [];

                switch (responseCode) {
                    case 'OK':
                        // if any error messages, pipe them to the user
                        const criticalMsgs = messages.filter(msg => msg.type !== 'INFO' && msg.type !== 'SUCCESS');
                        if (criticalMsgs.length > 0) {
                            criticalMsgs.forEach(msg => {
                                this.errorHandler.catch(new Error(msg.message), { notification: true });
                            });
                        }
                        return responseBody;

                    case 'AUTHREQUIRED':
                    case 'MAINTENANCEMODE':
                        reason = 'auth'; break;

                    case 'PERMISSION':
                        reason = 'permissions'; break;

                    case 'NOTFOUND':
                    case 'FAILURE':
                        reason = 'failed'; break;

                    case 'INVALIDDATA':
                        reason = 'invalid_data'; break;

                    default:
                        console.error('Unknown responseCode: ' + responseCode);
                        reason = 'failed'; break;
                }
            } else if (response.status === 200 || response.status === 201 || response.status === 204) {
                // If any kind of valid object contained, just return it, beacuse some endpoints don't envelope response.
                return responseBody;
            }

            // Some actions only report "Error", but contain more information in the "messages" array
            const errorMessage = this.assembleResponseErrorMessage(responseBody);

            throw new ApiError(errorMessage, reason, { request, response: responseBody, statusCode: response.status });

        } catch (err) {
            // TODO: remove the `err.name` check. See note at top of the spec file for this service.
            if (err instanceof ApiError || err.name === 'ApiError') {
                throw err;
            } else {
                const message = err instanceof Error ? err.message : err.toString();
                throw new ApiError(message, 'exception', { request, response: responseBody, originalError: err });
            }
        }
    }

    private handleRequestError(err: any, response: HttpResponseBase, request: ApiRequestInfo): Observable<never> {
        if (err instanceof HttpErrorResponse) {
            response = response || err;

            // On critical errors when requests fail at the receiving server before being forwarded to ContentNode,
            // the server returns an HTML error page, which the Angular HttpClient fails to parse as JSON.
            if (response && !response.ok && response.headers && /^text\/html/.test(response.headers.get('Content-Type'))) {
                return throwError(new ApiError(`Server returned an unexpected error (HTTP ${response.status})`, 'http', {
                    request,
                    response: response.statusText,
                    statusCode: response.status,
                }));
            }

            if (!err.ok && err.status === 0) {
                // When the user agent blocks an HTTP call (e.g. an adblocker preventing the request),
                // Angular throws a `ProgressEvent` with the response status 0 and the readyState 0.
                // To prevent this from being handled by the rest of the error handling, return an ApiError.
                return throwError(new ApiError('Failed to connect to the server.', 'http', {
                    request,
                    originalError: err,
                    statusCode: 0,
                }));
            }

            // Examine the body of the error.
            const result = err.error as Response;
            const message = err instanceof Error ? err.message : this.assembleResponseErrorMessage(result);

            // Handle remaining cases with an error status code.
            let statusCode: number = response.status;
            if ((response && !response.ok) || ((statusCode < 200 || statusCode >= 300) && statusCode !== 304)) {
                return throwError(new ApiError(message, 'http', { request, response: result, originalError: err, statusCode }));
            }
        } else if (err instanceof ApiError) {
            return throwError(err);
        } else if (err instanceof Error) {
            const statusCode = response ? response.status : 0;
            return throwError(new ApiError(err.message, 'exception', { request, originalError: err, statusCode }));
        }

        // TODO: Are there any other errors we need to catch (and augment with context)?
        return throwError(err);
    }

    private assembleResponseErrorMessage(response: Response): string {
        if (!response) {
            return GENERIC_REQUEST_FAILED_ERROR;
        }
        let errorMessage = response.responseInfo && response.responseInfo.responseMessage || GENERIC_REQUEST_FAILED_ERROR;

        if (Array.isArray(response.messages)) {
            const errorMessages = response.messages.filter(msg => msg.message && msg.type !== 'INFO' && msg.type !== 'SUCCESS');
            if (errorMessages.length > 0) {
                errorMessage = errorMessages
                    .map(msg => msg.message)
                    .join('\n');
            }
        }

        return errorMessage;
    }

}

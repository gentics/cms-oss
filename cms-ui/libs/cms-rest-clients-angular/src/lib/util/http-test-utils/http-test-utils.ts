import { HttpHeaders, HttpParams } from '@angular/common/http';
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { Response } from '@gentics/cms-models';

export const API_BASE_URL = '/rest';

/** HTTP verbs used by ApiBase. */
export type HttpVerb = 'GET' | 'POST' | 'PUT' | 'DELETE';

/** Interface for conveniently defining a mocked response. */
export interface MockResponseInfo<T extends Response | string> {
    /**
     * The response's body. This object is passed directly to the subscriber of
     * the HTTP request (it is not serialized and deserialized if it is an object).
     *
     * This means that it is also not possible to pass a JSON string as body and
     * expect it to be deserialized before being passed to the request's subscriber.
     *
     * If an HttpClient expects JSON, but gets invalid JSON, the observable will
     * emit an error.
     */
    body?: T;
    status?: number;
    statusText?: string;
    headers?: HttpHeaders;
}

/** Responds to the specified request with the specified mocked response. */
export function respondTo<T extends Response | string>(req: TestRequest, mockRes: MockResponseInfo<T>): void {
    const status = mockRes.status || 200;
    req.flush(mockRes.body, {
        status: status,
        statusText: mockRes.statusText || `HTTP/1.1 ${status}`,
        headers: mockRes.headers || new HttpHeaders({
            'Content-Type': typeof mockRes.body === 'object' ? 'application/json' : 'text/plain',
        }),
    });
}

/**
 * Verifies that there has been exactly one request to the httpTestingController
 * with the specified url, HTTP method, and optionally query parameters.
 * @param httpTestingController The HttpTestingController on which to expect the request.
 * @param url The URL (without `API_BASE_URL` and query parameters) that is expected to be requested.
 * @param method The HTTP method of the request.
 * @param queryParams (optional) The parameters contained in the HTTP query string (`sid` is automatically appended to all requests,
 *      the `gcms_ts` parameter is appended to GET requests).
 * @returns The `TestRequest` returned by `HttpTestingController.expectOne()`.
 */
export function expectOneRequest(httpTestingController: HttpTestingController, url: string, method: HttpVerb, queryParams?: HttpParams): TestRequest {
    const expectedUrl = `${API_BASE_URL}/${url}`;
    const req = httpTestingController.expectOne(
        // We need to implement the matcher manually, because by default expectOne() doesn't know about the gcms_ts query parameter.
        req => req.url === expectedUrl && checkHttpParamsEqual(req.params, queryParams, method === 'GET'),
        `${expectedUrl}?sid=<any>${method === 'GET' ? '&gcms_ts=<any>' : ''}${queryParams ? '&' + queryParams.toString() : ''}`,
    );
    expect(req.request.method).toEqual(method);
    return req;
}

/**
 * Checks if the actual HttpParams contain all expected HttpParams (keys and values).
 */
function checkHttpParamsEqual(actual: HttpParams, expected: HttpParams, isGetRequest: boolean): boolean {
    const defaultParams = isGetRequest ? ['gcms_ts', 'sid'] : ['sid'];
    const expectedKeys = expected ? expected.keys() : [];
    if (actual.keys().length !== expectedKeys.length + defaultParams.length) {
        return false;
    }

    for (let key of expectedKeys) {
        const actualValues = actual.getAll(key);
        const expectedValues = expected.getAll(key);
        if (!actualValues || actualValues.length !== expectedValues.length) {
            return false;
        }
        const equal = actualValues.every((value, index) => value === expectedValues[index]);
        if (!equal) {
            return false;
        }
    }
    for (let key of defaultParams) {
        if (!actual.get(key)) {
            return false;
        }
    }

    return true;
}

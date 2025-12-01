import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FolderResponse, ResponseCode } from '@gentics/cms-models';
import { getExampleFolderData } from '@gentics/cms-models/testing';
import { API_BASE_URL, expectOneRequest, respondTo } from './http-test-utils';

describe('http-test-utils', () => {

    let http: HttpClient;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ HttpClientTestingModule ]
        });
        http = TestBed.inject(HttpClient);
        httpTestingController = TestBed.inject(HttpTestingController);
    });

    describe('expectOneRequest()', () => {
        it('matches GET request with standard query params only', () => {
            http.get(`${API_BASE_URL}/test1/test2`, { params: assembleGetParams() })
                .subscribe();
            let req: TestRequest;
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'GET')).not.toThrow();
            expect(req).toBeTruthy();
        });

        it('matches GET request with additional query params', () => {
            http.get(`${API_BASE_URL}/test1/test2`, { params: assembleGetParams({ param1: 'bla' }) })
                .subscribe();
            let req: TestRequest;
            const expectedAdditionalParams = new HttpParams({ fromObject: { param1: 'bla' } });
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'GET', expectedAdditionalParams)).not.toThrow();
            expect(req).toBeTruthy();
        });

        it('does not match GET request without API_BASE_URL', () => {
            http.get(`test1/test2`, { params: assembleGetParams() })
                .subscribe();
            let req: TestRequest;
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'GET')).toThrow();
            expect(req).toBeFalsy();
        });

        it('does not match GET request without sid query param', () => {
            http.get(`${API_BASE_URL}/test1/test2`, { params: assembleGetParams().delete('sid') })
                .subscribe();
            let req: TestRequest;
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'GET')).toThrow();
            expect(req).toBeFalsy();
        });

        it('does not match GET request without gcms_ts query param', () => {
            http.get(`${API_BASE_URL}/test1/test2`, { params: assembleGetParams().delete('gcms_ts') })
                .subscribe();
            let req: TestRequest;
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'GET')).toThrow();
            expect(req).toBeFalsy();
        });

        it('does not match GET request with too few query params', () => {
            http.get(`${API_BASE_URL}/test1/test2`, { params: assembleGetParams({ param1: 'value1' }) })
                .subscribe();
            let req: TestRequest;
            const expectedAdditionalParams = new HttpParams({ fromObject: { param1: 'value2' } });
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'GET', expectedAdditionalParams)).toThrow();
            expect(req).toBeFalsy();
        });

        it('does not match GET request with incorrect query params', () => {
            http.get(`${API_BASE_URL}/test1/test2`, { params: assembleGetParams({ param1: 'bla' }) })
                .subscribe();
            let req: TestRequest;
            const expectedAdditionalParams = new HttpParams({ fromObject: { param1: 'bla', param2: 'bla' } });
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'GET', expectedAdditionalParams)).toThrow();
            expect(req).toBeFalsy();
        });

        it('matches POST request with standard query param only', () => {
            http.post(`${API_BASE_URL}/test1/test2`, { test: 'value' }, { params: assemblePostParams() })
                .subscribe();
            let req: TestRequest;
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'POST')).not.toThrow();
            expect(req).toBeTruthy();
        });

        it('matches POST request with additional query params', () => {
            http.post(`${API_BASE_URL}/test1/test2`, { test: 'value' }, { params: assemblePostParams({ param1: 'bla' }) })
                .subscribe();
            let req: TestRequest;
            const expectedAdditionalParams = new HttpParams({ fromObject: { param1: 'bla' } });
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'POST', expectedAdditionalParams)).not.toThrow();
            expect(req).toBeTruthy();
        });

        it('does not match POST request without sid query param', () => {
            http.post(`${API_BASE_URL}/test1/test2`, { test: 'value' }, { params: assemblePostParams().delete('sid') })
                .subscribe();
            let req: TestRequest;
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'POST')).toThrow();
            expect(req).toBeFalsy();
        });

        it('does not match POST request with gcms_ts query param', () => {
            http.post(`${API_BASE_URL}/test1/test2`, { test: 'value' }, { params: assemblePostParams().delete('sid') })
                .subscribe();
            let req: TestRequest;
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'POST')).toThrow();
            expect(req).toBeFalsy();
        });

        it('does not match POST request with too few query params', () => {
            http.post(`${API_BASE_URL}/test1/test2`, { test: 'value' }, { params: assemblePostParams({ param1: 'bla' }) })
                .subscribe();
            let req: TestRequest;
            const expectedAdditionalParams = new HttpParams({ fromObject: { param1: 'bla', param2: 'bla' } });
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'POST', expectedAdditionalParams)).toThrow();
            expect(req).toBeFalsy();
        });

        it('does not match POST request with incorrect query params', () => {
            http.post(`${API_BASE_URL}/test1/test2`, { test: 'value' }, { params: assemblePostParams({ param1: 'value1' }) })
                .subscribe();
            let req: TestRequest;
            const expectedAdditionalParams = new HttpParams({ fromObject: { param1: 'value2' } });
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'POST', expectedAdditionalParams)).toThrow();
            expect(req).toBeFalsy();
        });

        it('matches PUT request', () => {
            http.put(`${API_BASE_URL}/test1/test2`, { test: 'value' }, { params: assemblePostParams() })
                .subscribe();
            let req: TestRequest;
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'PUT')).not.toThrow();
            expect(req).toBeTruthy();
        });

        it('matches DELETE request', () => {
            http.delete(`${API_BASE_URL}/test1/test2`, { params: assemblePostParams() })
                .subscribe();
            let req: TestRequest;
            expect(() => req = expectOneRequest(httpTestingController, 'test1/test2', 'DELETE')).not.toThrow();
            expect(req).toBeTruthy();
        });

    });

    describe('respondTo()', () => {

        it('works for a JSON body', () => {
            let actualResponse: FolderResponse;
            http.get<FolderResponse>('test1/test2').subscribe(response => actualResponse = response);
            const req = httpTestingController.expectOne('test1/test2');
            respondTo(req, { body: assembleFolderResponse() });
            expect(actualResponse).toEqual(assembleFolderResponse());
        });

        it('works for a string body', () => {
            let actualResponse: string;
            http.get<string>('test1/test2').subscribe(response => actualResponse = response);
            const req = httpTestingController.expectOne('test1/test2');
            respondTo(req, { body: 'test' });
            expect(actualResponse).toEqual('test');
        });

        it('correctly sets default parameters for JSON', () => {
            let actualResponse: HttpResponse<FolderResponse>;
            http.get<FolderResponse>('test1/test2', { observe: 'response' }).subscribe(response => actualResponse = response);
            const req = httpTestingController.expectOne('test1/test2');
            respondTo(req, { body: assembleFolderResponse() });

            expect(actualResponse.body).toEqual(assembleFolderResponse());
            expect(actualResponse.ok).toBe(true);
            expect(actualResponse.status).toEqual(200);
            expect(actualResponse.statusText).toEqual('HTTP/1.1 200');
            expect(actualResponse.headers.keys()).toEqual(['Content-Type']);
            expect(actualResponse.headers.getAll('Content-Type')).toEqual(['application/json']);
        });

        it('correctly sets default parameters for string', () => {
            let actualResponse: HttpResponse<string>;
            http.get<string>('test1/test2', { observe: 'response' }).subscribe(response => actualResponse = response);
            const req = httpTestingController.expectOne('test1/test2');
            respondTo(req, { body: 'test' });

            expect(actualResponse.body).toEqual('test');
            expect(actualResponse.ok).toBe(true);
            expect(actualResponse.status).toEqual(200);
            expect(actualResponse.statusText).toEqual('HTTP/1.1 200');
            expect(actualResponse.headers.keys()).toEqual(['Content-Type']);
            expect(actualResponse.headers.getAll('Content-Type')).toEqual(['text/plain']);
        });

        it('uses the specified status code', () => {
            let actualResponse: HttpResponse<FolderResponse>;
            http.get<FolderResponse>('test1/test2', { observe: 'response' }).subscribe(response => actualResponse = response);
            const req = httpTestingController.expectOne('test1/test2');
            respondTo(req, {
                body: assembleFolderResponse(),
                status: 201
            });

            expect(actualResponse.body).toEqual(assembleFolderResponse());
            expect(actualResponse.ok).toBe(true);
            expect(actualResponse.status).toEqual(201);
            expect(actualResponse.statusText).toEqual('HTTP/1.1 201');
            expect(actualResponse.headers.keys()).toEqual(['Content-Type']);
            expect(actualResponse.headers.getAll('Content-Type')).toEqual(['application/json']);
        });

        it('an error status code calls the subscription\'s error handler', () => {
            let actualResponse: HttpResponse<FolderResponse>;
            let error: Error;
            http.get<FolderResponse>('test1/test2', { observe: 'response' }).subscribe(
                response => actualResponse = response,
                err => error = err
            );
            const req = httpTestingController.expectOne('test1/test2');
            respondTo(req, {
                body: assembleFolderResponse(),
                status: 404
            });

            expect(actualResponse).toBeFalsy();
            expect(error).toBeTruthy();
        });

        it('uses the specified headers', () => {
            let actualResponse: HttpResponse<FolderResponse>;
            http.get<FolderResponse>('test1/test2', { observe: 'response' }).subscribe(response => actualResponse = response);
            const req = httpTestingController.expectOne('test1/test2');
            respondTo(req, {
                body: assembleFolderResponse(),
                headers: new HttpHeaders({
                    'Content-Encoding': 'gzip',
                    'Content-Language': 'en'
                })
            });

            expect(actualResponse.body).toEqual(assembleFolderResponse());
            expect(actualResponse.ok).toBe(true);
            expect(actualResponse.status).toEqual(200);
            expect(actualResponse.statusText).toEqual('HTTP/1.1 200');
            expect(actualResponse.headers.keys()).toEqual([ 'Content-Encoding', 'Content-Language' ]);
            expect(actualResponse.headers.getAll('Content-Encoding')).toEqual(['gzip']);
            expect(actualResponse.headers.getAll('Content-Language')).toEqual(['en']);
        });

    });

});

function assembleGetParams(additionalParams: any = {}): HttpParams {
    const params = {
        sid: 4711,
        gcms_ts: 1234,
        ...additionalParams
    };
    return new HttpParams({ fromObject: params });
}

function assemblePostParams(additionalParams: any = {}): HttpParams {
    const params = {
        sid: 4711,
        ...additionalParams
    };
    return new HttpParams({ fromObject: params });
}

function assembleFolderResponse(): FolderResponse {
    return {
        folder: getExampleFolderData(),
        responseInfo: {
            responseCode: ResponseCode.OK
        }
    };
}

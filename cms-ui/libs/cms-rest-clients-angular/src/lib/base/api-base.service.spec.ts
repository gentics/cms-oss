import { HttpClient, HttpHeaders } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PagingSortOption, PagingSortOrder, User } from '@gentics/cms-models';
import { BehaviorSubject, NEVER, Observable } from 'rxjs';
import { ApiError } from '../error/api-error';
import { FileUploaderFactory } from '../util/file-uploader/file-uploader.factory';
import { API_BASE_URL, expectOneRequest, respondTo } from '../util/http-test-utils/http-test-utils';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';
import { ApiBase, CONTENT_TYPE_JSON, HTTP_HEADER_CONTENT_TYPE, HttpRequestOptions } from './api-base.service';

const TEST_SID = 1234;
const TEST_URL = 'some/url';

describe('ApiBase', () => {

    let apiBase: ApiBase;
    let sid$: Observable<number>;
    let http: HttpClient;
    let httpTestingController: HttpTestingController;
    let uploaderFactory: MockUploaderFactory;
    let errorHandler: MockErrorHandler;
    let originalConsoleError: (msg: string, ...args: any[]) => void;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ HttpClientTestingModule ],
        });
        http = TestBed.get(HttpClient);
        httpTestingController = TestBed.get(HttpTestingController);
        uploaderFactory = new MockUploaderFactory();
        errorHandler = new MockErrorHandler();
        sid$ = new BehaviorSubject(TEST_SID);
        apiBase = new ApiBase(http, uploaderFactory as any as FileUploaderFactory, API_BASE_URL, sid$, errorHandler);

        // Don't show debug error log in karma output
        originalConsoleError = console.error;
        console.error = (msg: string, ...args: any[]) => {
            if (!msg.startsWith('Unknown responseCode: ')) {
                originalConsoleError.apply(console, args);
            }
        };
    });

    afterEach(() => {
        let errorThrown: any;
        try {
            // After every test, assert that there are no more pending requests.
            httpTestingController.verify();
        } catch (error) {
            errorThrown = error;
        } finally {
            console.error = originalConsoleError;
        }

        if (errorThrown) {
            throw errorThrown;
        }
    });

    describe('get()', () => {

        it('forwards the passed url and parameters to Http.get', () => {
            http.get = jasmine.createSpy('HttpClient.get').and.callFake(
                (url: string, options: HttpRequestOptions) => {
                    expect(url).toEqual(API_BASE_URL + '/' + TEST_URL);
                    expect(options.params.get('paramA')).toBe('a');
                    expect(options.params.get('paramB')).toBe('2');
                    expect(options.params.get('paramC')).toBe('true');
                    expect((options.headers as HttpHeaders).get(HTTP_HEADER_CONTENT_TYPE)).toBe(CONTENT_TYPE_JSON);

                    return NEVER;
                });

            apiBase.get(TEST_URL, { paramA: 'a', paramB: 2, paramC: true });
            expect(http.get).toHaveBeenCalled();
        });

        it('adds the current SID to the request URL', () => {
            http.get = jasmine.createSpy('HttpClient.get').and.callFake(
                (url: string, options: HttpRequestOptions) => {
                    expect(options.params.get('sid')).toBe(TEST_SID.toString());
                    return NEVER;
                });
            apiBase.get(TEST_URL);
            expect(http.get).toHaveBeenCalled();
        });

        it('adds a timestamp to the request URL to prevent caching', () => {
            http.get = jasmine.createSpy('HttpClient.get').and.callFake(
                (url: string, options: HttpRequestOptions) => {
                    expect(options.params.get('gcms_ts')).toMatch(/\d+/);
                    return NEVER;
                });
            apiBase.get(TEST_URL);
            expect(http.get).toHaveBeenCalled();
        });

        it('succeeds when the server returns a responseCode "OK"', (done: DoneFn) => {
            apiBase.get(TEST_URL).subscribe(() => done(), err => done.fail(err));
            const req = expectOneRequest(httpTestingController, TEST_URL, 'GET');
            respondTo(req, {body: {responseInfo: {responseCode: 'OK'}}});
        });

        it('returns the result as a JavaScript object', () => {
            let response: any;
            apiBase.get(TEST_URL).subscribe(r => response = r);
            const req = expectOneRequest(httpTestingController, TEST_URL, 'GET');
            respondTo(req, {
                body: {
                    a: 1,
                    b: 'two',
                    c: [],
                    responseInfo: {
                        responseCode: 'OK',
                    },
                },
            });

            expect(response).toEqual({
                a: 1,
                b: 'two',
                c: [],
                responseInfo: {
                    responseCode: 'OK',
                },
            });
        });

        describe('returns an Observable that throws an ApiError when', () => {

            it('the response has a responseCode ≠ "OK"', () => {
                let error: ApiError;
                const sub = apiBase.get(TEST_URL).subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'GET');
                respondTo(req, {
                    body: { responseInfo: { responseCode: 'FAILURE' } },
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.reason).toBe('failed');
                expect(error.request.method).toBe('GET');
                expect(error.request.url).toBe(TEST_URL);
                expect(error.response).toEqual({ responseInfo: { responseCode: 'FAILURE' } });
            });

            xit('the response has no responseCode', () => {
                let error: ApiError;
                const sub = apiBase.get(TEST_URL).subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'GET');
                respondTo(req, {
                    body: { 'no response code': true } as any,
                });
                sub.unsubscribe();
                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.reason).toBe('failed');
            });

            it('the response is not valid JSON', () => {
                let error: ApiError;
                const sub = apiBase.get(TEST_URL).subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'GET');

                // The HttpTestingController passes the body of the flush() argument directly to
                // the HttpClient subscriber (so there is no parsing). Thus we only simulate
                // the parsing error, which would occur in the HttpClient.
                const errorEvent = new ErrorEvent('Http failure during parsing', { error: new SyntaxError('Unexpected end of JSON input') });
                req.error(errorEvent);
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.reason).toBe('http');
                expect((<any> error.originalError).error).toEqual(errorEvent);
            });

            it('an HTTP 404 error is returned', () => {
                let error: ApiError;
                const sub = apiBase.get(TEST_URL).subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'GET');
                respondTo(req, {
                    status: 404,
                    body: { responseInfo: { responseCode: 'OK' }, 'but server has 404': true },
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(404);
                expect(error.reason).toBe('http');
            });

            it('an HTTP 500 error is returned', () => {
                let error: ApiError;
                const sub = apiBase.get(TEST_URL).subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'GET');
                respondTo(req, {
                    status: 500,
                    body: { responseInfo: { responseCode: 'OK' }, 'but server has 500': true },
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(500);
                expect(error.reason).toBe('http');
            });

            it('an HTML error page is returned', () => {
                let error: ApiError;
                const sub = apiBase.get(TEST_URL).subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'GET');
                respondTo(req, {
                    status: 502,
                    headers: new HttpHeaders({
                        [HTTP_HEADER_CONTENT_TYPE]: 'text/html',
                    }),
                    body: '<html><body><h1>502: Bad Gateway</h1></body></html>',
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(502);
                expect(error.reason).toBe('http');
                expect(error.message).toBe('Server returned an unexpected error (HTTP 502)');
            });

            it('a network error occurs', () => {
                let error: ApiError;
                const sub = apiBase.get(TEST_URL).subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'GET');
                req.error(new ErrorEvent('NetworkError', { message: 'Simulated network error' }));
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(0);
                expect(error.reason).toBe('http');
                expect(error.message).toBe('Failed to connect to the server.');
            });

        });

        describe('error messages', () => {

            describe('in responses with status code 200', () => {

                it('the user-facing messages are displayed if the server provides them', () => {
                    let error: ApiError;
                    const sub = apiBase.get(TEST_URL).subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'GET');
                    respondTo(req, {
                        body: {
                            messages: [
                                { message: 'No such page found:<br/> /GCN5 Demo/News/testFileName', type: 'CRITICAL', timestamp: 1582124908184 },
                                { message: 'This info message should not be displayed to the user.', type: 'INFO', timestamp: 1582124908184 },
                                { message: 'Second Error message.', type: 'CRITICAL', timestamp: 1582124908184 },
                            ],
                            responseInfo: { responseCode: 'NOTFOUND', responseMessage: 'Not found error:<br/> /GCN5Demo/News/bdfb' },
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(200);
                    expect(error.reason).toBe('failed');
                    expect(error.message).toEqual('No such page found:<br/> /GCN5 Demo/News/testFileName\nSecond Error message.');
                });

                it('the response message is displayed otherwise', () => {
                    let error: ApiError;
                    const sub = apiBase.get(TEST_URL).subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'GET');
                    respondTo(req, {
                        body: {
                            responseInfo: {responseCode: 'NOTFOUND', responseMessage: 'Did not find a user with given credentials'},
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(200);
                    expect(error.reason).toBe('failed');
                    expect(error.message).toEqual('Did not find a user with given credentials');
                });

            });

            describe('in responses with an error status code', () => {

                it('the user-facing messages are displayed if the server provides them', () => {
                    let error: ApiError;
                    const sub = apiBase.get(TEST_URL).subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'GET');
                    respondTo(req, {
                        status: 404,
                        body: {
                            messages: [
                                { message: 'No such page found:<br/> /GCN5 Demo/News/testFileName', type: 'CRITICAL', timestamp: 1582124908184 },
                                { message: 'This info message should not be displayed to the user.', type: 'INFO', timestamp: 1582124908184 },
                                { message: 'Second Error message.', type: 'CRITICAL', timestamp: 1582124908184 },
                            ],
                            responseInfo: { responseCode: 'NOTFOUND', responseMessage: 'Not found error:<br/> /GCN5Demo/News/bdfb' },
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(404);
                    expect(error.reason).toBe('http');
                    expect(error.message).toEqual('No such page found:<br/> /GCN5 Demo/News/testFileName\nSecond Error message.');
                });

                it('the response message is displayed otherwise', () => {
                    let error: ApiError;
                    const sub = apiBase.get(TEST_URL).subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'GET');
                    respondTo(req, {
                        status: 400,
                        body: {
                            responseInfo: {responseCode: 'NOTFOUND', responseMessage: 'Did not find a user with given credentials'},
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(400);
                    expect(error.reason).toBe('http');
                    expect(error.message).toEqual('Did not find a user with given credentials');
                });

            });

        });

    });

    describe('post()', () => {

        it('forwards the passed url and parameters to Http.post', () => {
            http.post = jasmine.createSpy('HttpClient.post').and.callFake(
                (url: string, body: any, options: HttpRequestOptions) => {
                    expect(url).toEqual(API_BASE_URL + '/' + TEST_URL);
                    expect(options.params.get('paramA')).toBe('a');
                    expect(options.params.get('paramB')).toBe('2');
                    expect(options.params.get('paramC')).toBe('true');
                    expect((options.headers as HttpHeaders).get(HTTP_HEADER_CONTENT_TYPE)).toBe(CONTENT_TYPE_JSON);
                    expect(body).toBe('post body');

                    return NEVER;
                });

            apiBase.post(TEST_URL, 'post body', { paramA: 'a', paramB: 2, paramC: true });
            expect(http.post).toHaveBeenCalled();
        });

        it('adds the current SID to the request URL', () => {
            http.post = jasmine.createSpy('HttpClient.post').and.callFake(
                (url: string, body: any, options: HttpRequestOptions) => {
                    expect(options.params.get('sid')).toBe(TEST_SID.toString());
                    return NEVER;
                });
            apiBase.post(TEST_URL, 'body');
            expect(http.post).toHaveBeenCalled();
        });

        it('does not add a timestamp to the request URL', () => {
            http.post = jasmine.createSpy('HttpClient.post').and.callFake(
                (url: string, body: any, options: HttpRequestOptions) => {
                    expect(options.params.get('gcms_ts')).toBeNull();
                    return NEVER;
                });
            apiBase.post(TEST_URL, 'body');
            expect(http.post).toHaveBeenCalled();
        });

        it('encodes the passed body as JSON', () => {
            http.post = jasmine.createSpy('HttpClient.post').and.callFake(
                (url: string, body: any, options: HttpRequestOptions) => {
                    expect(body).toBe('{"color":"red","size":14}');
                    return NEVER;
                });
            apiBase.post(TEST_URL, { color: 'red', size: 14 });
            expect(http.post).toHaveBeenCalled();
        });

        it('does not re-encode the passed body as JSON if it is a string', () => {
            http.post = jasmine.createSpy('HttpClient.post').and.callFake(
                (url: string, body: any, options: HttpRequestOptions) => {
                    expect(body).toBe('{ "color": "red", "size": 14 }');
                    expect(body).not.toBe('{"color":"red","size":14}');
                    expect(body).not.toEqual({ color: 'red', size: 14 });

                    return NEVER;
                });
            apiBase.post(TEST_URL, '{ "color": "red", "size": 14 }');
            expect(http.post).toHaveBeenCalled();
        });

        it('succeeds when the server returns a responseCode "OK"', (done: DoneFn) => {
            apiBase.post(TEST_URL, 'body').subscribe(() => done(), err => done.fail(err));
            const req = expectOneRequest(httpTestingController, TEST_URL, 'POST');
            respondTo(req, {body: {responseInfo: {responseCode: 'OK'}}});
        });

        it('parses the result as JSON', () => {
            let response: any;
            apiBase.post(TEST_URL, 'body').subscribe(r => response = r);
            const req = expectOneRequest(httpTestingController, TEST_URL, 'POST');
            respondTo(req, {
                body: {
                    a: 1,
                    b: 'two',
                    c: [],
                    responseInfo: {
                        responseCode: 'OK',
                    },
                },
            });

            expect(response).toEqual({
                a: 1,
                b: 'two',
                c: [],
                responseInfo: {
                    responseCode: 'OK',
                },
            });
        });

        describe('returns an Observable that throws an ApiError when', () => {

            it('the response has a responseCode ≠ "OK"', () => {
                let error: ApiError;
                const sub = apiBase.post(TEST_URL, 'body').subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'POST');
                respondTo(req, {body: {responseInfo: {responseCode: 'FAILURE'}}});
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.reason).toBe('failed');
                expect(error.request.method).toBe('POST');
                expect(error.request.url).toBe('some/url');
                expect(error.response).toEqual({ responseInfo: { responseCode: 'FAILURE' } });
            });

            xit('the response has no responseCode', () => {
                let error: ApiError;
                const sub = apiBase.post(TEST_URL, 'body').subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'POST');
                respondTo(req, { body: { 'no response code' : true } as any });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.reason).toBe('failed');
            });

            it('the response is not valid JSON', () => {
                let error: ApiError;
                const sub = apiBase.post(TEST_URL, 'body').subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'POST');

                // The HttpTestingController passes the body of the flush() argument directly to
                // the HttpClient subscriber (so there is no parsing). Thus we only simulate
                // the parsing error, which would occur in the HttpClient.
                const errorEvent = new ErrorEvent('Http failure during parsing', { error: new SyntaxError('Unexpected end of JSON input') });
                req.error(errorEvent);
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.reason).toBe('http');
                expect((<any> error.originalError).error).toEqual(errorEvent);
            });

            it('an HTTP 404 error is returned', () => {
                let error: ApiError;
                const sub = apiBase.post(TEST_URL, 'body').subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'POST');
                respondTo(req, {
                    status: 404, statusText: 'File not Found',
                    body: { responseInfo: { responseCode: 'OK' }, 'but server has 404': true },
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(404);
                expect(error.reason).toBe('http');
            });

            it('an HTTP 500 error is returned', () => {
                let error: ApiError;
                const sub = apiBase.post(TEST_URL, 'body').subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'POST');
                respondTo(req, {
                    status: 500,
                    body: { responseInfo: { responseCode: 'OK' }, 'but server has 500': true },
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(500);
                expect(error.reason).toBe('http');
            });

            it('an HTML error page is returned', () => {
                let error: ApiError;
                const sub = apiBase.post(TEST_URL, 'body').subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'POST');
                respondTo(req, {
                    status: 502,
                    headers: new HttpHeaders({
                        [HTTP_HEADER_CONTENT_TYPE]: 'text/html',
                    }),
                    body: '<html><body><h1>502: Bad Gateway</h1></body></html>',
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(502);
                expect(error.reason).toBe('http');
                expect(error.message).toBe('Server returned an unexpected error (HTTP 502)');
            });

            it('a network error occurs', () => {
                let error: ApiError;
                const sub = apiBase.post(TEST_URL, 'body').subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'POST');
                req.error(new ErrorEvent('NetworkError', { message: 'Simulated network error' }));
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(0);
                expect(error.reason).toBe('http');
                expect(error.message).toBe('Failed to connect to the server.');
            });

        });

        describe('error messages', () => {

            describe('in responses with status code 200', () => {

                it('the user-facing messages are displayed if the server provides them', () => {
                    let error: ApiError;
                    const sub = apiBase.post(TEST_URL, 'body').subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'POST');
                    respondTo(req, {
                        body: {
                            messages: [
                                { message: 'A page with Nice URL exists:<br/> /GCN5 Demo/News/testFileName', type: 'CRITICAL', timestamp: 1582124908184 },
                                { message: 'This info message should not be displayed to the user.', type: 'INFO', timestamp: 1582124908184 },
                                { message: 'Second Error message.', type: 'CRITICAL', timestamp: 1582124908184 },
                            ],
                            responseInfo: { responseCode: 'INVALIDDATA', responseMessage: 'Error, page exists:<br/> /GCN5Demo/News/bdfb' },
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(200);
                    expect(error.reason).toBe('invalid_data');
                    expect(error.message).toEqual('A page with Nice URL exists:<br/> /GCN5 Demo/News/testFileName\nSecond Error message.');
                });

                it('the response message is displayed otherwise', () => {
                    let error: ApiError;
                    const sub = apiBase.post(TEST_URL, 'body').subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'POST');
                    respondTo(req, {
                        body: {
                            responseInfo: {responseCode: 'NOTFOUND', responseMessage: 'Did not find a user with given credentials'},
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(200);
                    expect(error.reason).toBe('failed');
                    expect(error.message).toEqual('Did not find a user with given credentials');
                });

            });

            describe('in responses with an error status code', () => {

                it('the user-facing messages are displayed if the server provides them', () => {
                    let error: ApiError;
                    const sub = apiBase.post(TEST_URL, 'body').subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'POST');
                    respondTo(req, {
                        status: 400,
                        body: {
                            messages: [
                                { message: 'A page with Nice URL exists:<br/> /GCN5 Demo/News/testFileName', type: 'CRITICAL', timestamp: 1582124908184 },
                                { message: 'This info message should not be displayed to the user.', type: 'INFO', timestamp: 1582124908184 },
                                { message: 'Second Error message.', type: 'CRITICAL', timestamp: 1582124908184 },
                            ],
                            responseInfo: { responseCode: 'INVALIDDATA', responseMessage: 'Error, page exists:<br/> /GCN5Demo/News/bdfb' },
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(400);
                    expect(error.reason).toBe('http');
                    expect(error.message).toEqual('A page with Nice URL exists:<br/> /GCN5 Demo/News/testFileName\nSecond Error message.');
                });

                it('the response message is displayed otherwise', () => {
                    let error: ApiError;
                    const sub = apiBase.post(TEST_URL, 'body').subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'POST');
                    respondTo(req, {
                        status: 400,
                        body: {
                            responseInfo: {responseCode: 'NOTFOUND', responseMessage: 'Did not find a user with given credentials'},
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(400);
                    expect(error.reason).toBe('http');
                    expect(error.message).toEqual('Did not find a user with given credentials');
                });

            });

        });

    });

    describe('put()', () => {

        it('forwards the passed url and parameters to Http.put', () => {
            http.put = jasmine.createSpy('HttpClient.put').and.callFake(
                (url: string, body: any, options: HttpRequestOptions) => {
                    expect(url).toEqual(API_BASE_URL + '/' + TEST_URL);
                    expect(options.params.get('paramA')).toBe('a');
                    expect(options.params.get('paramB')).toBe('2');
                    expect(options.params.get('paramC')).toBe('true');
                    expect((options.headers as HttpHeaders).get(HTTP_HEADER_CONTENT_TYPE)).toBe(CONTENT_TYPE_JSON);
                    expect(body).toBe('put body');

                    return NEVER;
                });

            apiBase.put(TEST_URL, 'put body', { paramA: 'a', paramB: 2, paramC: true });
            expect(http.put).toHaveBeenCalled();
        });

        it('adds the current SID to the request URL', () => {
            http.put = jasmine.createSpy('HttpClient.put').and.callFake(
                (url: string, body: any, options: HttpRequestOptions) => {
                    expect(options.params.get('sid')).toBe(TEST_SID.toString());
                    return NEVER;
                });
            apiBase.put(TEST_URL, 'body');
            expect(http.put).toHaveBeenCalled();
        });

        it('does not add a timestamp to the request URL', () => {
            http.put = jasmine.createSpy('HttpClient.put').and.callFake(
                (url: string, body: any, options: HttpRequestOptions) => {
                    expect(options.params.get('gcms_ts')).toBeNull();
                    return NEVER;
                });
            apiBase.put(TEST_URL, 'body');
            expect(http.put).toHaveBeenCalled();
        });

        it('encodes the passed body as JSON', () => {
            http.put = jasmine.createSpy('HttpClient.put').and.callFake(
                (url: string, body: any, options: HttpRequestOptions) => {
                    expect(body).toBe('{"color":"red","size":14}');
                    return NEVER;
                });
            apiBase.put(TEST_URL, { color: 'red', size: 14 });
            expect(http.put).toHaveBeenCalled();
        });

        it('does not re-encode the passed body as JSON if it is a string', () => {
            http.put = jasmine.createSpy('HttpClient.put').and.callFake(
                (url: string, body: any, options: HttpRequestOptions) => {
                    expect(body).toBe('{ "color": "red", "size": 14 }');
                    expect(body).not.toBe('{"color":"red","size":14}');
                    expect(body).not.toEqual({ color: 'red', size: 14 });

                    return NEVER;
                });
            apiBase.put(TEST_URL, '{ "color": "red", "size": 14 }');
            expect(http.put).toHaveBeenCalled();
        });

        it('succeeds when the server returns a responseCode "OK"', (done: DoneFn) => {
            apiBase.put(TEST_URL, 'body').subscribe(() => done(), err => done.fail(err));
            const req = expectOneRequest(httpTestingController, TEST_URL, 'PUT');
            respondTo(req, {body: {responseInfo: {responseCode: 'OK'}}});
        });

        it('parses the result as JSON', () => {
            let response: any;
            apiBase.put(TEST_URL, 'body').subscribe(r => response = r);
            const req = expectOneRequest(httpTestingController, TEST_URL, 'PUT');
            respondTo(req, {
                body: {
                    a: 1,
                    b: 'two',
                    c: [],
                    responseInfo: {
                        responseCode: 'OK',
                    },
                },
            });

            expect(response).toEqual({
                a: 1,
                b: 'two',
                c: [],
                responseInfo: {
                    responseCode: 'OK',
                },
            });
        });

        describe('returns an Observable that throws an ApiError when', () => {

            it('the response has a responseCode ≠ "OK"', () => {
                let error: ApiError;
                const sub = apiBase.put(TEST_URL, 'body').subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'PUT');
                respondTo(req, {body: {responseInfo: {responseCode: 'FAILURE'}}});
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.reason).toBe('failed');
                expect(error.request.method).toBe('PUT');
                expect(error.request.url).toBe('some/url');
                expect(error.response).toEqual({ responseInfo: { responseCode: 'FAILURE' } });
            });

            xit('the response has no responseCode', () => {
                let error: ApiError;
                const sub = apiBase.put(TEST_URL, 'body').subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'PUT');
                respondTo(req, { body: { 'no response code' : true } as any });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.reason).toBe('failed');
            });

            it('the response is not valid JSON', () => {
                let error: ApiError;
                const sub = apiBase.put(TEST_URL, 'body').subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'PUT');

                // The HttpTestingController passes the body of the flush() argument directly to
                // the HttpClient subscriber (so there is no parsing). Thus we only simulate
                // the parsing error, which would occur in the HttpClient.
                const errorEvent = new ErrorEvent('Http failure during parsing', { error: new SyntaxError('Unexpected end of JSON input') });
                req.error(errorEvent);
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.reason).toBe('http');
                expect((<any> error.originalError).error).toEqual(errorEvent);
            });

            it('an HTTP 404 error is returned', () => {
                let error: ApiError;
                const sub = apiBase.put(TEST_URL, 'body').subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'PUT');
                respondTo(req, {
                    status: 404, statusText: 'File not Found',
                    body: { responseInfo: { responseCode: 'OK' }, 'but server has 404': true },
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(404);
                expect(error.reason).toBe('http');
            });

            it('an HTTP 500 error is returned', () => {
                let error: ApiError;
                const sub = apiBase.put(TEST_URL, 'body').subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'PUT');
                respondTo(req, {
                    status: 500,
                    body: { responseInfo: { responseCode: 'OK' }, 'but server has 500': true },
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(500);
                expect(error.reason).toBe('http');
            });

            it('an HTML error page is returned', () => {
                let error: ApiError;
                const sub = apiBase.put(TEST_URL, 'body').subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'PUT');
                respondTo(req, {
                    status: 502,
                    headers: new HttpHeaders({
                        [HTTP_HEADER_CONTENT_TYPE]: 'text/html',
                    }),
                    body: '<html><body><h1>502: Bad Gateway</h1></body></html>',
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(502);
                expect(error.reason).toBe('http');
                expect(error.message).toBe('Server returned an unexpected error (HTTP 502)');
            });

            it('a network error occurs', () => {
                let error: ApiError;
                const sub = apiBase.put(TEST_URL, 'body').subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'PUT');
                req.error(new ErrorEvent('NetworkError', { message: 'Simulated network error' }));
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(0);
                expect(error.reason).toBe('http');
                expect(error.message).toBe('Failed to connect to the server.');
            });

        });

        describe('error messages', () => {

            describe('in responses with status code 201', () => {

                it('the user-facing messages are displayed if the server provides them', () => {
                    let error: ApiError;
                    const sub = apiBase.put(TEST_URL, 'body').subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'PUT');
                    respondTo(req, {
                        status: 201,
                        body: {
                            messages: [
                                { message: 'A page with Nice URL exists:<br/> /GCN5 Demo/News/testFileName', type: 'CRITICAL', timestamp: 1582124908184 },
                                { message: 'This info message should not be displayed to the user.', type: 'INFO', timestamp: 1582124908184 },
                                { message: 'Second Error message.', type: 'CRITICAL', timestamp: 1582124908184 },
                            ],
                            responseInfo: { responseCode: 'INVALIDDATA', responseMessage: 'Error, page exists:<br/> /GCN5Demo/News/bdfb' },
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(201);
                    expect(error.reason).toBe('invalid_data');
                    expect(error.message).toEqual('A page with Nice URL exists:<br/> /GCN5 Demo/News/testFileName\nSecond Error message.');
                });

                it('the response message is displayed otherwise', () => {
                    let error: ApiError;
                    const sub = apiBase.put(TEST_URL, 'body').subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'PUT');
                    respondTo(req, {
                        status: 201,
                        body: {
                            responseInfo: {responseCode: 'NOTFOUND', responseMessage: 'Did not find a user with given credentials'},
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(201);
                    expect(error.reason).toBe('failed');
                    expect(error.message).toEqual('Did not find a user with given credentials');
                });

            });

            describe('in responses with an error status code', () => {

                it('the user-facing messages are displayed if the server provides them', () => {
                    let error: ApiError;
                    const sub = apiBase.put(TEST_URL, 'body').subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'PUT');
                    respondTo(req, {
                        status: 400,
                        body: {
                            messages: [
                                { message: 'A page with Nice URL exists:<br/> /GCN5 Demo/News/testFileName', type: 'CRITICAL', timestamp: 1582124908184 },
                                { message: 'This info message should not be displayed to the user.', type: 'INFO', timestamp: 1582124908184 },
                                { message: 'Second Error message.', type: 'CRITICAL', timestamp: 1582124908184 },
                            ],
                            responseInfo: { responseCode: 'INVALIDDATA', responseMessage: 'Error, page exists:<br/> /GCN5Demo/News/bdfb' },
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(400);
                    expect(error.reason).toBe('http');
                    expect(error.message).toEqual('A page with Nice URL exists:<br/> /GCN5 Demo/News/testFileName\nSecond Error message.');
                });

                it('the response message is displayed otherwise', () => {
                    let error: ApiError;
                    const sub = apiBase.put(TEST_URL, 'body').subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'PUT');
                    respondTo(req, {
                        status: 400,
                        body: {
                            responseInfo: {responseCode: 'NOTFOUND', responseMessage: 'Did not find a user with given credentials'},
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(400);
                    expect(error.reason).toBe('http');
                    expect(error.message).toEqual('Did not find a user with given credentials');
                });

            });

        });

    });

    describe('delete()', () => {

        it('forwards the passed url and parameters to Http.delete', () => {
            http.delete = jasmine.createSpy('HttpClient.delete').and.callFake(
                (url: string, options: HttpRequestOptions) => {
                    expect(url).toEqual(API_BASE_URL + '/' + TEST_URL);
                    expect(options.params.get('paramA')).toBe('a');
                    expect(options.params.get('paramB')).toBe('2');
                    expect(options.params.get('paramC')).toBe('true');
                    expect((options.headers as HttpHeaders).get(HTTP_HEADER_CONTENT_TYPE)).toBe(CONTENT_TYPE_JSON);

                    return NEVER;
                });

            apiBase.delete(TEST_URL, { paramA: 'a', paramB: 2, paramC: true });
            expect(http.delete).toHaveBeenCalled();
        });

        it('adds the current SID to the request URL', () => {
            http.delete = jasmine.createSpy('HttpClient.delete').and.callFake(
                (url: string, options: HttpRequestOptions) => {
                    expect(options.params.get('sid')).toBe(TEST_SID.toString());
                    return NEVER;
                });
            apiBase.delete(TEST_URL);
            expect(http.delete).toHaveBeenCalled();
        });

        it('does not add a timestamp to the request URL', () => {
            http.delete = jasmine.createSpy('HttpClient.delete').and.callFake(
                (url: string, options: HttpRequestOptions) => {
                    expect(options.params.get('gcms_ts')).toBeNull();
                    return NEVER;
                });
            apiBase.delete(TEST_URL);
            expect(http.delete).toHaveBeenCalled();
        });

        it('succeeds when the server returns a statusCode 204', (done: DoneFn) => {
            apiBase.delete(TEST_URL).subscribe(
                res => {
                    expect(res).toBeUndefined('A 204 DELETE response should not have a body.');
                    done();
                },
                done.fail,
            );
            const req = expectOneRequest(httpTestingController, TEST_URL, 'DELETE');
            respondTo(req, {
                status: 204,
                body: {responseInfo: {responseCode: 'OK'}},
            });
        });

        it('returns the result as a JavaScript object if the response has the statusCode 200 and the responseCode "OK"', () => {
            let response: any;
            apiBase.delete(TEST_URL).subscribe(r => response = r);
            const req = expectOneRequest(httpTestingController, TEST_URL, 'DELETE');
            respondTo(req, {
                status: 200,
                body: {
                    a: 1,
                    b: 'two',
                    c: [],
                    responseInfo: {
                        responseCode: 'OK',
                    },
                },
            });

            expect(response).toEqual({
                a: 1,
                b: 'two',
                c: [],
                responseInfo: {
                    responseCode: 'OK',
                },
            });
        });

        describe('returns an Observable that throws an ApiError when', () => {

            it('the response with statusCode 200 has a responseCode ≠ "OK"', () => {
                let error: ApiError;
                const sub = apiBase.delete(TEST_URL).subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'DELETE');
                respondTo(req, {
                    body: { responseInfo: { responseCode: 'FAILURE' } },
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.reason).toBe('failed');
                expect(error.request.method).toBe('DELETE');
                expect(error.request.url).toBe(TEST_URL);
                expect(error.response).toEqual({ responseInfo: { responseCode: 'FAILURE' } });
            });

            xit('the response with statusCode 200 has no responseCode', () => {
                let error: ApiError;
                const sub = apiBase.delete(TEST_URL).subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'DELETE');
                respondTo(req, {
                    body: { 'no response code': true } as any,
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.reason).toBe('failed');
            });

            it('the response with statusCode 200 is not valid JSON', () => {
                let error: ApiError;
                const sub = apiBase.delete(TEST_URL).subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'DELETE');

                // The HttpTestingController passes the body of the flush() argument directly to
                // the HttpClient subscriber (so there is no parsing). Thus we only simulate
                // the parsing error, which would occur in the HttpClient.
                const errorEvent = new ErrorEvent('Http failure during parsing', { error: new SyntaxError('Unexpected end of JSON input') });
                req.error(errorEvent);
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.reason).toBe('http');
                expect((<any> error.originalError).error).toEqual(errorEvent);
            });

            it('an HTTP 404 error is returned', () => {
                let error: ApiError;
                const sub = apiBase.delete(TEST_URL).subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'DELETE');
                respondTo(req, {
                    status: 404,
                    body: { responseInfo: { responseCode: 'OK' }, 'but server has 404': true },
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(404);
                expect(error.reason).toBe('http');
            });

            it('an HTTP 500 error is returned', () => {
                let error: ApiError;
                const sub = apiBase.delete(TEST_URL).subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'DELETE');
                respondTo(req, {
                    status: 500,
                    body: { responseInfo: { responseCode: 'OK' }, 'but server has 500': true },
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(500);
                expect(error.reason).toBe('http');
            });

            it('an HTML error page is returned', () => {
                let error: ApiError;
                const sub = apiBase.delete(TEST_URL).subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'DELETE');
                respondTo(req, {
                    status: 502,
                    headers: new HttpHeaders({
                        [HTTP_HEADER_CONTENT_TYPE]: 'text/html',
                    }),
                    body: '<html><body><h1>502: Bad Gateway</h1></body></html>',
                });
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(502);
                expect(error.reason).toBe('http');
                expect(error.message).toBe('Server returned an unexpected error (HTTP 502)');
            });

            it('a network error occurs', () => {
                let error: ApiError;
                const sub = apiBase.delete(TEST_URL).subscribe({ error: e => error = e });
                const req = expectOneRequest(httpTestingController, TEST_URL, 'DELETE');
                req.error(new ErrorEvent('NetworkError', { message: 'Simulated network error' }));
                sub.unsubscribe();

                expect(error).toEqual(jasmine.any(ApiError));
                expect(error.statusCode).toBe(0);
                expect(error.reason).toBe('http');
                expect(error.message).toBe('Failed to connect to the server.');
            });

        });

        describe('error messages', () => {

            describe('in responses with status code 200', () => {

                it('the user-facing messages are displayed if the server provides them', () => {
                    let error: ApiError;
                    const sub = apiBase.delete(TEST_URL).subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'DELETE');
                    respondTo(req, {
                        body: {
                            messages: [
                                { message: 'No permissions on page:<br/> /GCN5 Demo/News/testFileName', type: 'CRITICAL', timestamp: 1582124908184 },
                                { message: 'This info message should not be displayed to the user.', type: 'INFO', timestamp: 1582124908184 },
                                { message: 'Second Error message.', type: 'CRITICAL', timestamp: 1582124908184 },
                            ],
                            responseInfo: { responseCode: 'INVALIDDATA', responseMessage: 'Permissions error:<br/> /GCN5Demo/News/bdfb' },
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(200);
                    expect(error.reason).toBe('invalid_data');
                    expect(error.message).toEqual('No permissions on page:<br/> /GCN5 Demo/News/testFileName\nSecond Error message.');
                });

                it('the response message is displayed otherwise', () => {
                    let error: ApiError;
                    const sub = apiBase.delete(TEST_URL).subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'DELETE');
                    respondTo(req, {
                        body: {
                            responseInfo: {responseCode: 'NOTFOUND', responseMessage: 'Did not find a user with given credentials'},
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(200);
                    expect(error.reason).toBe('failed');
                    expect(error.message).toEqual('Did not find a user with given credentials');
                });

            });

            describe('in responses with an error status code', () => {

                it('the user-facing messages are displayed if the server provides them', () => {
                    let error: ApiError;
                    const sub = apiBase.delete(TEST_URL).subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'DELETE');
                    respondTo(req, {
                        status: 400,
                        body: {
                            messages: [
                                { message: 'No permissions on page:<br/> /GCN5 Demo/News/testFileName', type: 'CRITICAL', timestamp: 1582124908184 },
                                { message: 'This info message should not be displayed to the user.', type: 'INFO', timestamp: 1582124908184 },
                                { message: 'Second Error message.', type: 'CRITICAL', timestamp: 1582124908184 },
                            ],
                            responseInfo: { responseCode: 'INVALIDDATA', responseMessage: 'Permissions error:<br/> /GCN5Demo/News/bdfb' },
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(400);
                    expect(error.reason).toBe('http');
                    expect(error.message).toEqual('No permissions on page:<br/> /GCN5 Demo/News/testFileName\nSecond Error message.');
                });

                it('the response message is displayed otherwise', () => {
                    let error: ApiError;
                    const sub = apiBase.delete(TEST_URL).subscribe({ error: e => error = e });
                    const req = expectOneRequest(httpTestingController, TEST_URL, 'DELETE');
                    respondTo(req, {
                        status: 400,
                        body: {
                            responseInfo: {responseCode: 'NOTFOUND', responseMessage: 'Did not find a user with given credentials'},
                        },
                    });
                    sub.unsubscribe();

                    expect(error.statusCode).toBe(400);
                    expect(error.reason).toBe('http');
                    expect(error.message).toEqual('Did not find a user with given credentials');
                });

            });

        });

    });

    describe('upload()', () => {

        const mockFiles = [
            { name: 'file1.txt' },
            { name: 'file2.png' },
            { name: 'file3.jpg' },
        ] as any[];

        it('creates a new FileUploader with the passed options', () => {
            const uploader = apiBase.upload([], 'file/create', {
                fileField: 'fileToUpload',
                fileNameField: 'uploadName',
                params: {
                    parentFolder: 15,
                },
            }) as any as MockFileUploader;

            expect(uploader.setOptions).toHaveBeenCalledWith({
                url: API_BASE_URL + '/file/create',
                fileField: 'fileToUpload',
                fileNameField: 'uploadName',
                parameters: jasmine.objectContaining({
                    parentFolder: 15,
                }),
            });
        });

        it('adds the SID to the passed url parameters', () => {
            const uploader = apiBase.upload([], 'file/create', { fileField: 'files' });

            expect(uploader.setOptions).toHaveBeenCalledWith(jasmine.objectContaining({
                parameters: jasmine.objectContaining({
                    sid: TEST_SID,
                }),
            }));
        });

        it('adds all files to the uploader via upload()', () => {
            const uploader = apiBase.upload(mockFiles, 'file/create', { fileField: 'files' });
            const firstArgs = (uploader.upload as jasmine.Spy).calls.allArgs().map(args => args[0]);

            expect(uploader.upload).toHaveBeenCalledTimes(mockFiles.length);
            expect(firstArgs).toEqual(mockFiles);
        });

    });

    describe('stringifyPagingSortOptions()', () => {

        it('works for a single option', () => {
            const sortOptions: PagingSortOption<User> = {
                sortOrder: PagingSortOrder.Asc,
                attribute: 'firstName',
            };
            const result = stringifyPagingSortOptions<User>(sortOptions);
            expect(result).toEqual('+firstName');
        });

        it('works for a multiple options', () => {
            const sortOptions: PagingSortOption<User>[] = [
                {
                    sortOrder: PagingSortOrder.Asc,
                    attribute: 'firstName',
                },
                {
                    sortOrder: PagingSortOrder.Desc,
                    attribute: 'email',
                },
            ];
            const result = stringifyPagingSortOptions<User>(sortOptions);
            expect(result).toEqual('+firstName,-email');
        });

        it('works without a sortOrder', () => {
            const sortOptions: PagingSortOption<User> = {
                attribute: 'firstName',
            };
            const result = stringifyPagingSortOptions<User>(sortOptions);
            expect(result).toEqual('firstName');
        });

    });

});

export class MockErrorHandler {
    catch = jasmine.createSpy('ErrorHandler.catch');
}

class MockFileUploader {
    setOptions = jasmine.createSpy('setOptions');
    upload = jasmine.createSpy('upload');
}

class MockUploaderFactory {
    lastUploader: MockFileUploader;
    create(): MockFileUploader {
        return this.lastUploader = new MockFileUploader();
    }
}

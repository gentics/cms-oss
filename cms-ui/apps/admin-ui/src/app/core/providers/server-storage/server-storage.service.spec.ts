import { HttpClient, HttpHeaders } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ResponseCode, UserDataResponse } from '@gentics/cms-models';
import { ApiBase, FileUploaderFactory, GcmsApi } from '@gentics/cms-rest-clients-angular';
import { expectOneRequest, MockResponseInfo, respondTo } from '@gentics/cms-rest-clients-angular/testing';
import { NEVER, of } from 'rxjs';
import { API_BASE_URL } from '../../../common';
import { AppStateService } from '../../../state';
import { assembleTestAppStateImports, TestAppState } from '../../../state/utils/test-app-state';
import { MockErrorHandler } from '../error-handler/error-handler.mock';
import { ServerStorageService } from './server-storage.service';

const testData = { test: 'data', x: 9 };

describe('ServerStorage', () => {

    let appState: TestAppState;
    let http: HttpClient;
    let httpTestingController: HttpTestingController;
    let api: GcmsApi;
    let serverStorage: ServerStorageService;
    let errorHandler: MockErrorHandler;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
                HttpClientTestingModule,
            ],
            providers: [
                { provide: AppStateService, useClass: TestAppState },
            ],
        });
        appState = TestBed.get(AppStateService);
        appState.mockState({
            auth: {
                sid: 4711,
            },
        });
        http = TestBed.get(HttpClient);
        httpTestingController = TestBed.get(HttpTestingController);
        errorHandler = new MockErrorHandler();
        const sid$ = appState.select(state => state.auth.sid);
        const apiBase = new ApiBase(http, {} as any as FileUploaderFactory, API_BASE_URL, sid$, errorHandler as any);
        api = new GcmsApi(apiBase);
        serverStorage = new ServerStorageService(api);
    });

    it('gets created ok', () => {
        expect(serverStorage).toBeDefined();
    });

    describe('getAll()', () => {

        it('requests all user data from the API if not marked as unsupported', () => {
            api.userData.getAllKeys = jasmine.createSpy('getAllKeys')
                .and.returnValue(of({ data: { key1: 'value1', key2: 1234 } }));

            expect(serverStorage.supported).toBe('unknown');

            let result: any;
            serverStorage.getAll().subscribe(all => result = all);

            expect(api.userData.getAllKeys).toHaveBeenCalled();
            expect(result).toEqual({ key1: 'value1', key2: 1234 });
        });

        it('marks user storage as supported if the request succeeds', () => {
            api.userData.getAllKeys = jasmine.createSpy('getAllKeys')
                .and.returnValue(of({ data: { } }));

            expect(serverStorage.supported).toBe('unknown');
            serverStorage.getAll().subscribe(() => {});
            expect(serverStorage.supported).toBe(true);
        });

        it('marks user storage as unsupported if the server responds with a HTTP 500', () => {
            expect(serverStorage.supported).toBe('unknown');
            let result: any;
            serverStorage.getAll().subscribe(all => result = all);
            const req = expectOneRequest(httpTestingController, 'user/me/data', 'GET');
            respondTo(req, createApiNotImplementedResponse());
            expect(result).toEqual({});
            expect(serverStorage.supported).toBe(false);
        });

        it('does nothing if the CMS version has no server storage', () => {
            api.userData.getAllKeys = jasmine.createSpy('getAllKeys').and.returnValue(NEVER);
            serverStorage.supported$.next(false);

            let result: any;
            serverStorage.getAll().subscribe(all => result = all);

            expect(result).toEqual({});
            expect(api.userData.getAllKeys).not.toHaveBeenCalled();
        });

    });

    describe('get()', () => {

        it('requests a specific user data key from the API if not marked as unsupported', () => {
            api.userData.getKey = jasmine.createSpy('getKey')
                .and.returnValue(of({ data: testData }));

            expect(serverStorage.supported).toBe('unknown');

            let userData: any;
            serverStorage.get('testData')
                .subscribe(storedData => { userData = storedData; });

            expect(api.userData.getKey).toHaveBeenCalledWith('testData');
            expect(userData).toEqual(testData);
        });

        it('marks user storage as supported if the request succeeds', () => {
            api.userData.getKey = jasmine.createSpy('getKey')
                .and.returnValue(of({ data: { } }));

            expect(serverStorage.supported).toBe('unknown');
            serverStorage.get('testData').subscribe(() => {});
            expect(serverStorage.supported).toBe(true);
        });

        it('marks user storage as unsupported if the server responds with a HTTP 500', () => {
            expect(serverStorage.supported).toBe('unknown');
            let userData: any;
            serverStorage.get('testData')
                .subscribe(storedData => { userData = storedData; });
            const req = expectOneRequest(httpTestingController, 'user/me/data/testData', 'GET');
            respondTo(req, createApiNotImplementedResponse());
            expect(userData).toBeNull();
            expect(serverStorage.supported).toBe(false);
        });

        it('does nothing if the CMS version has no server storage', () => {
            api.userData.getKey = jasmine.createSpy('getKey').and.returnValue(NEVER);
            serverStorage.supported$.next(false);

            let userData: any;
            serverStorage.get('testData')
                .subscribe(storedData => { userData = storedData; });
            expect(userData).toBeNull();
            expect(api.userData.getKey).not.toHaveBeenCalled();
        });

    });

    describe('set()', () => {

        it('saves specific user data to the API if not marked as unsupported', fakeAsync(() => {
            api.userData.setKey = jasmine.createSpy('setKey')
                .and.returnValue(of({ responseInfo: { responseCode: 'OK' } }));

            expect(serverStorage.supported).toBe('unknown');

            let userData: any;
            serverStorage.set('testData', testData)
                .then(result => userData = result);
            tick();

            expect(api.userData.setKey).toHaveBeenCalledWith('testData', testData);
            expect(userData).toEqual(testData);
        }));

        it('marks user storage as supported if the request succeeds', fakeAsync(() => {
            expect(serverStorage.supported).toBe('unknown');
            serverStorage.set('testData', testData);
            const req = expectOneRequest(httpTestingController, 'user/me/data/testData', 'POST');
            respondTo(req, {body: {responseInfo: {responseCode: ResponseCode.OK}}});
            tick();
            expect(serverStorage.supported).toBe(true);
        }));

        it('marks user storage as unsupported if the server responds with a HTTP 500', fakeAsync(() => {
            expect(serverStorage.supported).toBe('unknown');
            let userData: any;
            serverStorage.set('testData', testData)
                .then(result => { userData = result; });
            const req = expectOneRequest(httpTestingController, 'user/me/data/testData', 'POST');
            respondTo(req, createApiNotImplementedResponse());
            tick();
            expect(userData).toEqual(testData);
            expect(serverStorage.supported).toBe(false);
        }));

        // TODO: Check if this test is obsolete in Angular final.
        it('marks user storage as unsupported if the request throws as expected', fakeAsync(() => {
            expect(serverStorage.supported).toBe('unknown');

            let userData: any;
            let thrownError: any;
            serverStorage.set('testData', testData)
                .then(result => userData = result)
                .catch(err => thrownError = err);
            const req = expectOneRequest(httpTestingController, 'user/me/data/testData', 'POST');
            respondTo(req, createApiNotImplementedResponse());
            tick();

            expect(thrownError).toBeUndefined();
            expect(userData).toEqual(testData);
            expect(serverStorage.supported).toBe(false);
        }));

        it('does nothing and returns the passed data if the CMS version has no server storage', fakeAsync(() => {
            api.userData.setKey = jasmine.createSpy('setKey').and.returnValue(NEVER);
            serverStorage.supported$.next(false);

            let userData: any;
            serverStorage.set('testData', testData)
                .then(result => userData = result);
            tick();
            expect(userData).toEqual(testData);
            expect(api.userData.setKey).not.toHaveBeenCalled();
        }));

    });
});

// Returns a response like that sent by the server if the /user/me/data API is not implemented
function createApiNotImplementedResponse(): MockResponseInfo<UserDataResponse> {
    return {
        status: 500,
        statusText: 'Internal Server Error',
        headers: new HttpHeaders({
            'Content-Type': 'application/json',
        }),
        body: {
            data: null,
            messages: [{
                timestamp: Date.now() - 300,
                type: 'CRITICAL',
            }],
            responseInfo: {
                responseCode: ResponseCode.FAILURE,
            },
        },
    };
}

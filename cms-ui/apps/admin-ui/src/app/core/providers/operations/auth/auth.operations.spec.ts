import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import {
    ChangePasswordError,
    ChangePasswordStart,
    ChangePasswordSuccess,
    LoginError,
    LoginStart,
    LoginSuccess,
    LogoutError,
    LogoutStart,
    LogoutSuccess,
    ValidateError,
    ValidateStart,
    ValidateSuccess,
} from '@gentics/cms-components/auth';
import { LoginResponse, Raw, Response, ResponseCode, User, UserUpdateResponse, ValidateSidResponse } from '@gentics/cms-models';
import { getExampleFolderData } from '@gentics/cms-models/testing';
import { GCMSRestClientRequestError } from '@gentics/cms-rest-client';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { ApiError } from '@gentics/cms-rest-clients-angular';
import { ActionType, ofActionDispatched } from '@ngxs/store';
import { of as observableOf } from 'rxjs';
import { delay, map, takeUntil } from 'rxjs/operators';
import { ObservableStopper } from '../../../../common/utils/observable-stopper/observable-stopper';
import { AppStateService } from '../../../../state';
import { assembleTestAppStateImports, TestAppState } from '../../../../state/utils/test-app-state';
import { EditorUiLocalStorageService } from '../../editor-ui-local-storage/editor-ui-local-storage.service';
import { MockErrorHandler } from '../../error-handler/error-handler.mock';
import { ErrorHandler } from '../../error-handler/error-handler.service';
import { I18nNotificationService } from '../../i18n-notification/i18n-notification.service';
import { MockI18nNotificationService } from '../../i18n-notification/i18n-notification.service.mock';
import { AuthOperations } from './auth.operations';

class MockRouter {
    navigateByUrl = jasmine.createSpy('navigateByUrl').and.stub();
}

class MockEditorUiLocalStorage {
    getSid = jasmine.createSpy('getSid').and.returnValue(SID);
    setSid = jasmine.createSpy('setSid').and.stub();
}

const SID = 1234;
const MOCK_USER: User<Raw> = getExampleFolderData().editor;
Object.freeze(MOCK_USER);
const ERROR_MSG = 'Auth Error';

describe('AuthOperations', () => {

    let client: GCMSTestRestClientService;
    let authOps: AuthOperations;
    let editorUiLocalStorage: MockEditorUiLocalStorage;
    let errorHandler: MockErrorHandler;
    let state: TestAppState;
    let stopper: ObservableStopper;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
            ],
            providers: [
                AuthOperations,
                TestAppState,
                { provide: AppStateService, useExisting: TestAppState },
                MockEditorUiLocalStorage,
                { provide: EditorUiLocalStorageService, useExisting: MockEditorUiLocalStorage },
                MockErrorHandler,
                { provide: ErrorHandler, useExisting: MockErrorHandler },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                { provide: I18nNotificationService, useClass: MockI18nNotificationService },
                { provide: Router, useClass: MockRouter },
            ],
        }).compileComponents();

        client = TestBed.inject(GCMSRestClientService) as any;
        authOps = TestBed.inject(AuthOperations);
        editorUiLocalStorage = TestBed.inject(MockEditorUiLocalStorage);
        errorHandler = TestBed.inject(MockErrorHandler);
        state = TestBed.inject(TestAppState);
        stopper = new ObservableStopper();
    });

    afterEach(() => {
        stopper.stop();
    });

    describe('validateSessionId()', () => {

        let validateStartDispatched: boolean;
        let validateSuccessDispatched: boolean;
        let validateErrorDispatched: boolean;

        beforeEach(() => {
            validateStartDispatched = false;
            validateSuccessDispatched = false;
            validateErrorDispatched = false;

            state.trackActions().pipe(
                ofActionDispatched(ValidateStart as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe(() => validateStartDispatched = true);

            state.trackActions().pipe(
                ofActionDispatched(ValidateSuccess as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe((action: ValidateSuccess) => {
                validateSuccessDispatched = true;
                expect(action.sid).toBe(SID);
                expect(action.user).toEqual(MOCK_USER);
            });

            state.trackActions().pipe(
                ofActionDispatched(ValidateError as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe((action: ValidateError) => {
                validateErrorDispatched = true;
                expect(action.errorMessage).toEqual(ERROR_MSG);
            });
        });

        it('works for a success response', fakeAsync(() => {
            editorUiLocalStorage.getSid.and.returnValue(null);
           spyOn(client.user, 'me').and.returnValue(
                observableOf<ValidateSidResponse>({
                    responseInfo: { responseCode: ResponseCode.OK },
                    user: MOCK_USER,
                }).pipe(delay(0)), // We use delay() to make the response asynchronous.
            );

            authOps.validateSessionId(SID);
            expect(validateStartDispatched).toBe(true);
            expect(client.user.me).toHaveBeenCalledWith({ sid: SID });

            tick();
            expect(validateSuccessDispatched).toBe(true);
            expect(validateErrorDispatched).toBe(false);
            // The SID was not yet in the editor local storage, so it should be added.
            expect(editorUiLocalStorage.setSid).toHaveBeenCalledWith(SID);
        }));

        it('works for an error response', fakeAsync(() => {
            spyOn(client.user, 'me').and.returnValue(
                observableOf(null).pipe(
                    delay(0),
                    map(() => { throw new GCMSRestClientRequestError(ERROR_MSG, null, 401); }),
                ),
            );

            authOps.validateSessionId(SID);
            expect(validateStartDispatched).toBe(true);
            expect(client.user.me).toHaveBeenCalledWith({ sid: SID });

            tick();
            expect(validateSuccessDispatched).toBe(false);
            expect(validateErrorDispatched).toBe(true);
        }));

    });

    it('validateSessionFromLocalStorage() works', fakeAsync(() => {
        const validateSpy = spyOn(authOps, 'validateSessionId').and.callThrough();
        spyOn(client.user, 'me').and.returnValue(
            observableOf<ValidateSidResponse>({
                responseInfo: { responseCode: ResponseCode.OK },
                user: MOCK_USER,
            }).pipe(delay(0)),
        );
        authOps.validateSessionFromLocalStorage();

        expect(editorUiLocalStorage.getSid).toHaveBeenCalledTimes(1);
        expect(validateSpy).toHaveBeenCalledWith(SID);
        editorUiLocalStorage.getSid.calls.reset();

        // The SID was already in the editor local storage, so it should not be set again.
        tick();
        expect(editorUiLocalStorage.getSid).toHaveBeenCalledTimes(1);
        expect(editorUiLocalStorage.setSid).not.toHaveBeenCalled();
    }));

    describe('login()', () => {

        let loginStartDispatched: boolean;
        let loginSuccessDispatched: boolean;
        let loginErrorDispatched: boolean;

        beforeEach(() => {
            loginStartDispatched = false;
            loginSuccessDispatched = false;
            loginErrorDispatched = false;

            state.trackActions().pipe(
                ofActionDispatched(LoginStart as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe(() => loginStartDispatched = true);

            state.trackActions().pipe(
                ofActionDispatched(LoginSuccess as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe((action: LoginSuccess) => {
                loginSuccessDispatched = true;
                expect(action.sid).toBe(SID);
                expect(action.user).toEqual(MOCK_USER);
            });

            state.trackActions().pipe(
                ofActionDispatched(LoginError as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe((action: LoginError) => {
                loginErrorDispatched = true;
                expect(action.errorMsg).toEqual(ERROR_MSG);
            });
        });

        it('login works for a success response', fakeAsync(() => {
            const router: MockRouter = TestBed.inject(Router) as any;
            spyOn(client.auth, 'login').and.returnValue(
                observableOf<LoginResponse>({
                    responseInfo: { responseCode: ResponseCode.OK },
                    sid: SID,
                    user: MOCK_USER,
                }).pipe(delay(0)),
            );

            authOps.login('admin', 'pwd', '/');
            expect(loginStartDispatched).toBe(true);
            expect(client.auth.login).toHaveBeenCalledWith({
                login: 'admin',
                password: 'pwd',
            });

            tick();
            expect(loginSuccessDispatched).toBe(true);
            expect(loginErrorDispatched).toBe(false);
            expect(editorUiLocalStorage.setSid).toHaveBeenCalledWith(SID);
            expect(router.navigateByUrl).toHaveBeenCalledWith('/');
        }));

        it('login works for an error response', fakeAsync(() => {
            const error = new GCMSRestClientRequestError(ERROR_MSG, null, 400);
            spyOn(client.auth, 'login').and.returnValue(
                observableOf(null).pipe(
                    delay(0),
                    map(() => { throw error; }),
                ),
            );

            authOps.login('admin', 'pwd', '/');
            expect(loginStartDispatched).toBe(true);
            expect(client.auth.login).toHaveBeenCalledWith({
                login: 'admin',
                password: 'pwd',
            });

            tick();
            expect(loginSuccessDispatched).toBe(false);
            expect(loginErrorDispatched).toBe(true);
            expect(errorHandler.catch).toHaveBeenCalledWith(error);
        }));

    });

    describe('logout()', () => {

        let logoutStartDispatched: boolean;
        let logoutSuccessDispatched: boolean;
        let logoutErrorDispatched: boolean;

        beforeEach(() => {
            logoutStartDispatched = false;
            logoutSuccessDispatched = false;
            logoutErrorDispatched = false;

            state.trackActions().pipe(
                ofActionDispatched(LogoutStart as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe(() => logoutStartDispatched = true);

            state.trackActions().pipe(
                ofActionDispatched(LogoutSuccess as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe(() => logoutSuccessDispatched = true);

            state.trackActions().pipe(
                ofActionDispatched(LogoutError as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe((action: LoginError) => {
                logoutErrorDispatched = true;
                expect(action.errorMsg).toEqual(ERROR_MSG);
            });
        });

        it('works for a success response', fakeAsync(() => {
            spyOn(client.auth, 'logout').and.returnValue(
                observableOf<Response>({
                    responseInfo: { responseCode: ResponseCode.OK },
                }).pipe(delay(0)),
            );

            authOps.logout(SID);
            expect(logoutStartDispatched).toBe(true);
            expect(client.auth.logout).toHaveBeenCalledWith(SID);

            tick();
            expect(logoutSuccessDispatched).toBe(true);
            expect(logoutErrorDispatched).toBe(false);
            expect(editorUiLocalStorage.setSid).toHaveBeenCalledWith(null);
        }));

        it('works for an error response', fakeAsync(() => {
            const error = new ApiError(ERROR_MSG, 'failed');
            spyOn(client.auth, 'logout').and.returnValue(
                observableOf(null).pipe(
                    delay(0),
                    map(() => { throw error; }),
                ),
            );

            authOps.logout(SID);
            expect(logoutStartDispatched).toBe(true);
            expect(client.auth.logout).toHaveBeenCalledWith(SID);

            tick();
            expect(logoutSuccessDispatched).toBe(false);
            expect(logoutErrorDispatched).toBe(true);
            expect(errorHandler.catch).toHaveBeenCalledWith(error);
        }));
    });

    describe('changePassword()', () => {

        let changePwdStartDispatched: boolean;
        let changePwdSuccessDispatched: boolean;
        let changePwdErrorDispatched: boolean;

        beforeEach(() => {
            changePwdStartDispatched = false;
            changePwdSuccessDispatched = false;
            changePwdErrorDispatched = false;

            state.trackActions().pipe(
                ofActionDispatched(ChangePasswordStart as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe(() => changePwdStartDispatched = true);

            state.trackActions().pipe(
                ofActionDispatched(ChangePasswordSuccess as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe(() => changePwdSuccessDispatched = true);

            state.trackActions().pipe(
                ofActionDispatched(ChangePasswordError as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe((action: LoginError) => {
                changePwdErrorDispatched = true;
                expect(action.errorMsg).toEqual(ERROR_MSG);
            });
        });

        it('works for a success response', fakeAsync(() => {
            spyOn(client.user, 'update').and.returnValue(
                observableOf<UserUpdateResponse>({
                    responseInfo: { responseCode: ResponseCode.OK },
                    user: MOCK_USER,
                    messages: [],
                }).pipe(delay(0)),
            );

            authOps.changePassword(MOCK_USER.id, 'newPwd');
            expect(changePwdStartDispatched).toBe(true);
            expect(client.user.update).toHaveBeenCalledWith(MOCK_USER.id, {
                password: 'newPwd',
            });

            tick();
            expect(changePwdSuccessDispatched).toBe(true);
            expect(changePwdErrorDispatched).toBe(false);
        }));

        it('works for an error response', fakeAsync(() => {
            const error = new GCMSRestClientRequestError(ERROR_MSG, null, 400);
            spyOn(client.user, 'update').and.returnValue(
                observableOf(null).pipe(
                    delay(0),
                    map(() => { throw error; }),
                ),
            );

            authOps.changePassword(MOCK_USER.id, 'newPwd');
            expect(changePwdStartDispatched).toBe(true);
            expect(client.user.update).toHaveBeenCalledWith(MOCK_USER.id, {
                password: 'newPwd',
            });

            tick();
            expect(changePwdSuccessDispatched).toBe(false);
            expect(changePwdErrorDispatched).toBe(true);
            expect(errorHandler.catch).toHaveBeenCalledWith(error);
        }));

    });

});

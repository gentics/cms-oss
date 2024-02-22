import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { GcmsTestData, LoginResponse, Raw, Response, ResponseCode, User, ValidateSidResponse } from '@gentics/cms-models';
import { ApiError, GcmsApi } from '@gentics/cms-rest-clients-angular';
import { ActionType, ofActionDispatched } from '@ngxs/store';
import { of as observableOf } from 'rxjs';
import { delay, map, takeUntil } from 'rxjs/operators';
import { ObservableStopper } from '../../../../common/utils/observable-stopper/observable-stopper';
import {
    AppStateService,
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
} from '../../../../state';
import { assembleTestAppStateImports, TestAppState } from '../../../../state/utils/test-app-state';
import { EditorUiLocalStorageService } from '../../editor-ui-local-storage/editor-ui-local-storage.service';
import { EntityManagerService } from '../../entity-manager';
import { MockEntityManagerService } from '../../entity-manager/entity-manager.service.mock';
import { MockErrorHandler } from '../../error-handler/error-handler.mock';
import { ErrorHandler } from '../../error-handler/error-handler.service';
import { I18nNotificationService } from '../../i18n-notification/i18n-notification.service';
import { MockI18nNotificationService } from '../../i18n-notification/i18n-notification.service.mock';
import { AuthOperations } from './auth.operations';

class MockGcmsApi {
    auth = {
        validate: jasmine.createSpy('validate').and.stub(),
        login: jasmine.createSpy('login').and.stub(),
        logout: jasmine.createSpy('logout').and.stub(),
        changePassword: jasmine.createSpy('changePassword').and.stub(),
    };
}

class MockRouter {
    navigateByUrl = jasmine.createSpy('navigateByUrl').and.stub();
}

class MockEditorUiLocalStorage {
    getSid = jasmine.createSpy('getSid').and.returnValue(SID);
    setSid = jasmine.createSpy('setSid').and.stub();
}

const SID = 1234;
const MOCK_USER: User<Raw> = GcmsTestData.getExampleFolderData().editor;
Object.freeze(MOCK_USER);
const ERROR_MSG = 'Auth Error';

describe('AuthOperations', () => {

    let api: MockGcmsApi;
    let authOps: AuthOperations;
    let editorUiLocalStorage: MockEditorUiLocalStorage;
    let entities: MockEntityManagerService;
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
                MockEntityManagerService,
                { provide: EntityManagerService, useExisting: MockEntityManagerService },
                MockErrorHandler,
                { provide: ErrorHandler, useExisting: MockErrorHandler },
                MockGcmsApi,
                { provide: GcmsApi, useExisting: MockGcmsApi },
                { provide: I18nNotificationService, useClass: MockI18nNotificationService },
                { provide: Router, useClass: MockRouter },
            ],
        }).compileComponents();

        entities = TestBed.inject(MockEntityManagerService);
        api = TestBed.inject(MockGcmsApi);
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
            api.auth.validate.and.returnValue(
                observableOf<ValidateSidResponse>({
                    responseInfo: { responseCode: ResponseCode.OK },
                    user: MOCK_USER,
                }).pipe(delay(0)), // We use delay() to make the response asynchronous.
            );

            authOps.validateSessionId(SID);
            expect(validateStartDispatched).toBe(true);
            expect(api.auth.validate).toHaveBeenCalledWith(SID);

            tick();
            expect(validateSuccessDispatched).toBe(true);
            expect(validateErrorDispatched).toBe(false);
            // The SID was not yet in the editor local storage, so it should be added.
            expect(editorUiLocalStorage.setSid).toHaveBeenCalledWith(SID);
            expect(entities.addEntity).toHaveBeenCalledWith('user', MOCK_USER);
        }));

        it('works for an error response', fakeAsync(() => {
            api.auth.validate.and.returnValue(
                observableOf(null).pipe(
                    delay(0),
                    map(() => { throw new ApiError(ERROR_MSG, 'auth'); }),
                ),
            );

            authOps.validateSessionId(SID);
            expect(validateStartDispatched).toBe(true);
            expect(api.auth.validate).toHaveBeenCalledWith(SID);

            tick();
            expect(validateSuccessDispatched).toBe(false);
            expect(validateErrorDispatched).toBe(true);
        }));

    });

    it('validateSessionFromLocalStorage() works', fakeAsync(() => {
        const validateSpy = spyOn(authOps, 'validateSessionId').and.callThrough();
        api.auth.validate.and.returnValue(
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
            const router = TestBed.get(Router) as MockRouter;
            api.auth.login.and.returnValue(
                observableOf<LoginResponse>({
                    responseInfo: { responseCode: ResponseCode.OK },
                    sid: SID,
                    user: MOCK_USER,
                }).pipe(delay(0)),
            );

            authOps.login('admin', 'pwd', '/');
            expect(loginStartDispatched).toBe(true);
            expect(api.auth.login).toHaveBeenCalledWith('admin', 'pwd');

            tick();
            expect(loginSuccessDispatched).toBe(true);
            expect(loginErrorDispatched).toBe(false);
            expect(editorUiLocalStorage.setSid).toHaveBeenCalledWith(SID);
            expect(router.navigateByUrl).toHaveBeenCalledWith('/');
            expect(entities.addEntity).toHaveBeenCalledWith('user', MOCK_USER);
        }));

        it('login works for an error response', fakeAsync(() => {
            const error = new ApiError(ERROR_MSG, 'auth');
            api.auth.login.and.returnValue(
                observableOf(null).pipe(
                    delay(0),
                    map(() => { throw error; }),
                ),
            );

            authOps.login('admin', 'pwd', '/');
            expect(loginStartDispatched).toBe(true);
            expect(api.auth.login).toHaveBeenCalledWith('admin', 'pwd');

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
            api.auth.logout.and.returnValue(
                observableOf<Response>({
                    responseInfo: { responseCode: ResponseCode.OK },
                }).pipe(delay(0)),
            );

            authOps.logout(SID);
            expect(logoutStartDispatched).toBe(true);
            expect(api.auth.logout).toHaveBeenCalledWith(SID);

            tick();
            expect(logoutSuccessDispatched).toBe(true);
            expect(logoutErrorDispatched).toBe(false);
            expect(editorUiLocalStorage.setSid).toHaveBeenCalledWith(null);
        }));

        it('works for an error response', fakeAsync(() => {
            const error = new ApiError(ERROR_MSG, 'failed');
            api.auth.logout.and.returnValue(
                observableOf(null).pipe(
                    delay(0),
                    map(() => { throw error; }),
                ),
            );

            authOps.logout(SID);
            expect(logoutStartDispatched).toBe(true);
            expect(api.auth.logout).toHaveBeenCalledWith(SID);

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
            api.auth.changePassword.and.returnValue(
                observableOf<Response>({
                    responseInfo: { responseCode: ResponseCode.OK },
                }).pipe(delay(0)),
            );

            authOps.changePassword(MOCK_USER.id, 'newPwd');
            expect(changePwdStartDispatched).toBe(true);
            expect(api.auth.changePassword).toHaveBeenCalledWith(MOCK_USER.id, 'newPwd');

            tick();
            expect(changePwdSuccessDispatched).toBe(true);
            expect(changePwdErrorDispatched).toBe(false);
        }));

        it('works for an error response', fakeAsync(() => {
            const error = new ApiError(ERROR_MSG, 'failed');
            api.auth.changePassword.and.returnValue(
                observableOf(null).pipe(
                    delay(0),
                    map(() => { throw error; }),
                ),
            );

            authOps.changePassword(MOCK_USER.id, 'newPwd');
            expect(changePwdStartDispatched).toBe(true);
            expect(api.auth.changePassword).toHaveBeenCalledWith(MOCK_USER.id, 'newPwd');

            tick();
            expect(changePwdSuccessDispatched).toBe(false);
            expect(changePwdErrorDispatched).toBe(true);
            expect(errorHandler.catch).toHaveBeenCalledWith(error);
        }));

    });

});

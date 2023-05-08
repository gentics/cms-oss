import { TestBed, waitForAsync } from '@angular/core/testing';
import { User } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';

import { AppStateService } from '../providers/app-state/app-state.service';
import { TestAppState, TEST_APP_STATE } from '../utils/test-app-state';
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
    ResetAuth,
    ValidateError,
    ValidateStart,
    ValidateSuccess,
} from './auth.actions';
import { AuthStateModel, AuthStateModule, INITIAL_AUTH_STATE } from './auth.state';

const USER_ID = 10;
const SID = 4711;

const MOCK_USER: User = {
    id: USER_ID,
} as User;

describe('AuthStateModule', () => {

    let appState: TestAppState;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot([AuthStateModule])],
            providers: [TEST_APP_STATE],
        }).compileComponents();
        appState = TestBed.get(AppStateService);
    }));

    it('sets the correct initial state', () => {
        appState.selectOnce(state => state.auth).subscribe(authState => {
            expect(authState).toEqual(INITIAL_AUTH_STATE);
        });
    });

    describe('login', () => {

        it('LoginStart works', () => {
            appState.dispatch(new LoginStart());
            appState.selectOnce(state => state.auth).subscribe(authState => {
                expect(authState).toEqual(jasmine.objectContaining<AuthStateModel>({
                    loggingIn: true,
                    isLoggedIn: false,
                    currentUserId: null,
                    sid: null,
                }));
            });
        });

        it('LoginSuccess works', () => {
            appState.mockState({
                auth: {
                    loggingIn: true,
                },
            });
            appState.dispatch(new LoginSuccess(SID, MOCK_USER));
            appState.selectOnce(state => state.auth).subscribe(authState => {
                expect(authState).toEqual(jasmine.objectContaining<AuthStateModel>({
                    loggingIn: false,
                    isLoggedIn: true,
                    currentUserId: USER_ID,
                    sid: SID,
                    lastError: '',
                }));
            });
        });

        it('LoginError works', () => {
            appState.mockState({
                auth: {
                    loggingIn: true,
                },
            });
            appState.dispatch(new LoginError('test'));
            appState.selectOnce(state => state.auth).subscribe(authState => {
                expect(authState).toEqual(jasmine.objectContaining<AuthStateModel>({
                    loggingIn: false,
                    isLoggedIn: false,
                    currentUserId: null,
                    sid: null,
                    lastError: 'test',
                }));
            });
        });

    });


    describe('logout', () => {

        beforeEach(() => {
            appState.mockState({
                auth: {
                    isLoggedIn: true,
                    currentUserId: USER_ID,
                    sid: SID,
                },
            });
        });

        it('LogoutStart works', () => {
            appState.dispatch(new LogoutStart());
            appState.selectOnce(state => state.auth).subscribe(authState => {
                expect(authState).toEqual(jasmine.objectContaining<AuthStateModel>({
                    loggingOut: true,
                    isLoggedIn: true,
                    currentUserId: USER_ID,
                    sid: SID,
                }));
            });
        });

        it('LogoutSuccess works', () => {
            appState.dispatch(new LogoutSuccess());
            appState.selectOnce(state => state.auth).subscribe(authState => {
                expect(authState).toEqual(jasmine.objectContaining<AuthStateModel>({
                    loggingOut: false,
                    isLoggedIn: false,
                    currentUserId: null,
                    sid: null,
                }));
            });
        });

        it('LogoutError works', () => {
            appState.dispatch(new LogoutError('test'));
            appState.selectOnce(state => state.auth).subscribe(authState => {
                expect(authState).toEqual(jasmine.objectContaining<AuthStateModel>({
                    loggingOut: false,
                    isLoggedIn: true,
                    currentUserId: USER_ID,
                    sid: SID,
                    lastError: 'test',
                }));
            });
        });

    });

    it('ResetAuth works', () => {
        appState.mockState({
            auth: {
                isLoggedIn: true,
                currentUserId: USER_ID,
                sid: SID,
            },
        });
        appState.dispatch(new ResetAuth());
        appState.selectOnce(state => state.auth).subscribe(authState => {
            expect(authState).toEqual(INITIAL_AUTH_STATE);
        });
    });

    describe('validate', () => {

        it('ValidateStart works', () => {
            appState.dispatch(new ValidateStart());
            appState.selectOnce(state => state.auth).subscribe(authState => {
                expect(authState).toEqual(jasmine.objectContaining<AuthStateModel>({
                    loggingIn: true,
                    isLoggedIn: false,
                    currentUserId: null,
                    sid: null,
                }));
            });
        });

        it('ValidateSuccess works', () => {
            appState.mockState({
                auth: {
                    loggingIn: true,
                },
            });
            appState.dispatch(new ValidateSuccess(SID, MOCK_USER));
            appState.selectOnce(state => state.auth).subscribe(authState => {
                expect(authState).toEqual(jasmine.objectContaining<AuthStateModel>({
                    loggingIn: false,
                    isLoggedIn: true,
                    currentUserId: USER_ID,
                    sid: SID,
                    lastError: '',
                }));
            });
        });

        it('ValidateError works', () => {
            appState.mockState({
                auth: {
                    loggingIn: true,
                },
            });
            appState.dispatch(new ValidateError('test'));
            appState.selectOnce(state => state.auth).subscribe(authState => {
                expect(authState).toEqual(jasmine.objectContaining<AuthStateModel>({
                    loggingIn: false,
                    isLoggedIn: false,
                    currentUserId: null,
                    sid: null,
                    lastError: 'test',
                }));
            });
        });

    });

    describe('change password', () => {

        it('ChangePasswordStart works', () => {
            appState.dispatch(new ChangePasswordStart());
            appState.selectOnce(state => state.auth).subscribe(authState => {
                expect(authState).toEqual(jasmine.objectContaining<AuthStateModel>({
                    changingPassword: true,
                }));
            });
        });

        it('ChangePasswordSuccess works', () => {
            appState.mockState({
                auth: {
                    changingPassword: true,
                },
            });
            appState.dispatch(new ChangePasswordSuccess());
            appState.selectOnce(state => state.auth).subscribe(authState => {
                expect(authState).toEqual(jasmine.objectContaining<AuthStateModel>({
                    changingPassword: false,
                    lastError: '',
                }));
            });
        });

        it('ChangePasswordError works', () => {
            appState.mockState({
                auth: {
                    changingPassword: true,
                },
            });
            appState.dispatch(new ChangePasswordError('test'));
            appState.selectOnce(state => state.auth).subscribe(authState => {
                expect(authState).toEqual(jasmine.objectContaining<AuthStateModel>({
                    changingPassword: false,
                    lastError: 'test',
                }));
            });
        });

    });

});

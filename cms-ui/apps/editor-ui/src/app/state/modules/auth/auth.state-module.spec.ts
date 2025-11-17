import { TestBed } from '@angular/core/testing';
import { NgxsModule } from '@ngxs/store';
import { normalize } from 'normalizr';
import { AuthState, userSchema } from '../../../common/models';
import { ApplicationStateService } from '../../providers';
import { TestApplicationState } from '../../test-application-state.mock';
import { AddEntitiesAction } from '../entity/entity.actions';
import { UpdateSearchFilterAction } from '../folder/folder.actions';
import { STATE_MODULES } from '../state-modules';
import {
    ChangePasswordAction,
    LoginErrorAction,
    LoginSuccessAction,
    LogoutErrorAction,
    LogoutSuccessAction,
    StartLoginAction,
    StartLogoutAction,
    StartValidationAction,
    UpdateIsAdminAction,
    ValidationErrorAction,
    ValidationSuccessAction,
} from './auth.actions';

describe('AuthStateModule', () => {

    let appState: TestApplicationState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        appState = TestBed.inject(ApplicationStateService) as any;
    });

    it('sets the correct initial state', () => {
        expect(appState.now.auth).toEqual({
            isAdmin: false,
            isLoggedIn: false,
            loggingIn: false,
            loggingOut: false,
            changingPassword: false,
            currentUserId: null,
            sid: null,
            lastError: '',
        } as AuthState);
    });

    it('setIsAdmin works for true', () => {
        appState.dispatch(new UpdateIsAdminAction(true));
        expect(appState.now.auth.isAdmin).toBe(true);
    });

    it('setIsAdmin works for false', () => {
        appState.mockState({
            auth: {
                isAdmin: true,
            },
        });
        appState.dispatch(new UpdateIsAdminAction(false));
        expect(appState.now.auth.isAdmin).toBe(false);
    });

    it('changePasswordStart works', () => {
        appState.dispatch(new ChangePasswordAction(true));
        expect(appState.now.auth.changingPassword).toBe(true);
    });

    it('changePasswordSuccess works', () => {
        appState.mockState({
            auth: {
                changingPassword: true,
            },
        });
        appState.dispatch(new ChangePasswordAction(false));
        expect(appState.now.auth.changingPassword).toBe(false);
    });

    it('changePasswordError works', () => {
        appState.mockState({
            auth: {
                changingPassword: true,
            },
        });
        appState.dispatch(new ChangePasswordAction(false, 'Error message'));
        expect(appState.now.auth.changingPassword).toBe(false);
        expect(appState.now.auth.lastError).toBe('Error message');
    });

    it('loginStart works', () => {
        appState.dispatch(new StartLoginAction());
        expect(appState.now.auth.loggingIn).toBe(true);
    });

    it('loginSuccess works', () => {
        appState.mockState({
            auth: {
                loggingIn: true,
            },
        });

        const exampleUser = {
            id: 1234,
            firstName: 'John',
            lastName: 'Doe',
            login: 'jdoe',
            email: 'j.doe@example.com',
            description: 'Example user',
        };
        const normalizedUser = normalize(exampleUser, userSchema);

        appState.dispatch(new LoginSuccessAction(9999, exampleUser));
        appState.dispatch(new AddEntitiesAction(normalizedUser));

        expect(appState.now.auth.loggingIn).toBe(false);
        expect(appState.now.auth.isLoggedIn).toBe(true);
        expect(appState.now.auth.currentUserId).toBe(exampleUser.id);
        expect(appState.now.auth.sid).toBe(9999);
        expect(appState.now.entities.user[exampleUser.id]).toEqual(jasmine.objectContaining(exampleUser));
    });

    it('loginError works', () => {
        appState.mockState({
            auth: {
                loggingIn: true,
            },
        });
        const errorMessage = 'Error message';
        appState.dispatch(new LoginErrorAction(errorMessage));
        expect(appState.now.auth.lastError).toBe(errorMessage);
        expect(appState.now.auth.loggingIn).toBe(false);
    });

    it('logoutStart works', () => {
        appState.dispatch(new StartLogoutAction());
        expect(appState.now.auth.loggingOut).toBe(true);
    });

    it('logoutSuccess works', () => {
        appState.mockState({
            auth: {
                currentUserId: 1234,
                isLoggedIn: true,
                loggingOut: true,
                sid: 9999,
            },
            folder: {
                searchFiltersVisible: true,
            },
        });
        appState.dispatch(new LogoutSuccessAction());
        appState.dispatch(new UpdateSearchFilterAction({
            changing: false,
            valid: false,
            visible: false,
        }));
        expect(appState.now.auth.currentUserId).toBe(null);
        expect(appState.now.auth.isLoggedIn).toBe(false);
        expect(appState.now.auth.loggingOut).toBe(false);
        expect(appState.now.auth.sid).toBe(null);
        expect(appState.now.folder.searchFiltersVisible).toBe(false);
    });

    it('logoutError works', () => {
        appState.mockState({
            auth: {
                loggingOut: true,
            },
        });
        const errorMessage = 'Error message';
        appState.dispatch(new LogoutErrorAction(errorMessage));
        expect(appState.now.auth.lastError).toBe(errorMessage);
        expect(appState.now.auth.loggingOut).toBe(false);
    });

    it('validateStart works', () => {
        appState.dispatch(new StartValidationAction());
        expect(appState.now.auth.loggingIn).toBe(true);
    });

    it('validateSuccess works', () => {
        appState.mockState({
            auth: {
                loggingIn: true,
            },
        });
        const user = {
            id: 1234,
            firstName: 'John',
            lastName: 'Doe',
            login: 'jdoe',
            email: 'j.doe@example.com',
            description: 'Example user',
        };
        const normalizedUser = normalize(user, userSchema);

        appState.dispatch(new ValidationSuccessAction(9999, user));
        appState.dispatch(new AddEntitiesAction(normalizedUser));

        expect(appState.now.auth.loggingIn).toBe(false);
        expect(appState.now.auth.isLoggedIn).toBe(true);
        expect(appState.now.auth.currentUserId).toBe(user.id);
        expect(appState.now.auth.sid).toBe(9999);
        expect(appState.now.entities.user[user.id]).toEqual(jasmine.objectContaining(user));
    });

    it('validateError works', () => {
        appState.mockState({
            auth: {
                loggingIn: true,
            },
        });
        const errorMessage = 'Error message';
        appState.dispatch(new ValidationErrorAction(errorMessage));
        expect(appState.now.auth.lastError).toBe(errorMessage);
        expect(appState.now.auth.loggingIn).toBe(false);
    });

});

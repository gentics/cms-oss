import { Injectable } from '@angular/core';
import { ActionDefinition } from '@gentics/cms-components';
import { State, StateContext } from '@ngxs/store';
import {
    AuthStateModel,
    ChangePasswordError,
    ChangePasswordStart,
    ChangePasswordSuccess,
    INITIAL_AUTH_STATE,
    KeycloakConnectionState,
    KeycloakLoadError,
    KeycloakLoadStart,
    KeycloakLoadSuccess,
    LoginError,
    LoginStart,
    LoginSuccess,
    LogoutError,
    LogoutStart,
    LogoutSuccess,
    ResetAuth,
    SingleSignOnSkipped,
    ValidateError,
    ValidateStart,
    ValidateSuccess
} from '../models';

@State({
    name: 'auth',
    defaults: INITIAL_AUTH_STATE,
})
@Injectable()
export class AuthStateModule {

    @ActionDefinition(LoginStart)
    loginStart(ctx: StateContext<AuthStateModel>): void {
        ctx.patchState({
            loggingIn: true,
        });
    }

    @ActionDefinition(LoginSuccess)
    loginSuccess(ctx: StateContext<AuthStateModel>, action: LoginSuccess): void {
        ctx.patchState({
            isLoggedIn: true,
            loggingIn: false,
            user: action.user,
            sid: action.sid,
            lastError: '',
        });
    }

    @ActionDefinition(LoginError)
    loginError(ctx: StateContext<AuthStateModel>, action: LoginError): void {
        ctx.patchState({
            loggingIn: false,
            lastError: action.errorMsg,
        });
    }

    @ActionDefinition(LogoutStart)
    logoutStart(ctx: StateContext<AuthStateModel>): void {
        ctx.patchState({
            loggingOut: true,
        });
    }

    @ActionDefinition(LogoutSuccess)
    logoutSuccess(ctx: StateContext<AuthStateModel>): void {
        ctx.patchState({
            loggingOut: false,
            isLoggedIn: false,
            user: null,
            sid: null,
        });
    }

    @ActionDefinition(LogoutError)
    logoutError(ctx: StateContext<AuthStateModel>, action: LogoutError): void {
        ctx.patchState({
            loggingOut: false,
            lastError: action.errorMsg,
        });
    }

    @ActionDefinition(ResetAuth)
    resetAuth(ctx: StateContext<AuthStateModel>): void {
        ctx.setState(INITIAL_AUTH_STATE);
    }

    @ActionDefinition(ValidateStart)
    validateStart(ctx: StateContext<AuthStateModel>): void {
        ctx.patchState({
            loggingIn: true,
        });
    }

    @ActionDefinition(ValidateSuccess)
    validateSuccess(ctx: StateContext<AuthStateModel>, action: ValidateSuccess): void {
        ctx.patchState({
            isLoggedIn: true,
            loggingIn: false,
            user: action.user,
            sid: action.sid,
            lastError: '',
        });
    }

    @ActionDefinition(ValidateError)
    validateError(ctx: StateContext<AuthStateModel>, action: ValidateError): void {
        ctx.patchState({
            loggingIn: false,
            lastError: action.errorMessage,
        });
    }

    @ActionDefinition(ChangePasswordStart)
    changePasswordStart(ctx: StateContext<AuthStateModel>): void {
        ctx.patchState({
            changingPassword: true,
        });
    }

    @ActionDefinition(ChangePasswordSuccess)
    changePasswordSuccess(ctx: StateContext<AuthStateModel>): void {
        ctx.patchState({
            changingPassword: false,
            lastError: '',
        });
    }

    @ActionDefinition(ChangePasswordError)
    changePasswordError(ctx: StateContext<AuthStateModel>, action: ChangePasswordError): void {
        ctx.patchState({
            changingPassword: false,
            lastError: action.errorMsg,
        });
    }

    @ActionDefinition(KeycloakLoadStart)
    handleKeycloakLoadStart(ctx: StateContext<AuthStateModel>): void {
        ctx.patchState({
            keycloakAvailable: null,
            keycloakError: null,
            showSingleSignOnButton: false,
        });
    }

    @ActionDefinition(KeycloakLoadSuccess)
    handleKeycloakLoadSuccess(ctx: StateContext<AuthStateModel>, action: KeycloakLoadSuccess): void {
        ctx.patchState({
            keycloakAvailable: action.available,
            keycloakError: null,
            showSingleSignOnButton: action.ssoButton,
        });
    }

    @ActionDefinition(KeycloakLoadError)
    handleKeycloakLoadError(ctx: StateContext<AuthStateModel>, action: KeycloakLoadError): void {
        let message: string;

        switch (action.con) {
            case KeycloakConnectionState.UNREACHABLE:
                message = 'shared.keycloak_not_available';
                break;
            case KeycloakConnectionState.INVALID_CONFIG:
                message = 'shared.keycloak_invalid_config';
                break;
            case KeycloakConnectionState.ERROR:
            default:
                message = 'shared.keycloak_unknown_error';
                break;
        }

        ctx.patchState({
            keycloakAvailable: false,
            keycloakError: message,
            showSingleSignOnButton: false,
        });
    }

    @ActionDefinition(SingleSignOnSkipped)
    handleSingleSignOnSkipped(ctx: StateContext<AuthStateModel>, action: SingleSignOnSkipped): void {
        ctx.patchState({
            ssoSkipped: action.skipped,
        });
    }
}

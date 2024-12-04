import { Injectable } from '@angular/core';
import { User } from '@gentics/cms-models';
import { StateContext } from '@ngxs/store';
import { ActionDefinition, AppStateBranch, defineInitialState } from '../utils/state-utils';
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

export interface AuthStateModel {
    isLoggedIn: boolean;
    loggingIn: boolean;
    loggingOut: boolean;
    changingPassword: boolean;
    currentUserId: number;
    currentUser: User;
    /** The GCMS session ID */
    sid: number;
    lastError: string;
}

export const INITIAL_AUTH_STATE = defineInitialState<AuthStateModel>({
    isLoggedIn: false,
    loggingIn: false,
    loggingOut: false,
    changingPassword: false,
    currentUserId: null,
    currentUser: null,
    sid: null,
    lastError: null,
});

@AppStateBranch<AuthStateModel>({
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
            currentUserId: action.user.id,
            currentUser: action.user,
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
            currentUserId: null,
            currentUser: null,
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
            currentUserId: action.user.id,
            currentUser: action.user,
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

}

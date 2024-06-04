import { Injectable } from '@angular/core';
import { StateContext } from '@ngxs/store';
import { iif, patch } from '@ngxs/store/operators';
import { AuthState } from '../../../common/models';
import { ApplicationStateService } from '../../providers/application-state/application-state.service';
import { ActionDefinition, AppStateBranch } from '../../state-utils';
import {
    AUTH_STATE_KEY,
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

const INITIAL_AUTH_STATE: AuthState = {
    isAdmin: false,
    isLoggedIn: false,
    loggingIn: false,
    loggingOut: false,
    changingPassword: false,
    currentUserId: null,
    sid: null,
    lastError: '',
};

@AppStateBranch<AuthState>({
    name: AUTH_STATE_KEY,
    defaults: INITIAL_AUTH_STATE,
    })
@Injectable()
export class AuthStateModule {

    constructor(
        protected appState: ApplicationStateService,
    ) {}

    @ActionDefinition(UpdateIsAdminAction)
    handleUpdateIsAdminAction(ctx: StateContext<AuthState>, action: UpdateIsAdminAction): void {
        ctx.patchState({
            isAdmin: action.isAdmin,
        });
    }

    @ActionDefinition(ChangePasswordAction)
    handleChangePasswordAction(ctx: StateContext<AuthState>, action: ChangePasswordAction): void {
        ctx.setState(patch({
            changingPassword: action.isChanging,
            lastError: iif(action.errorMessage !== undefined, action.errorMessage),
        }));
    }

    @ActionDefinition(StartLoginAction)
    handleStartLoginAction(ctx: StateContext<AuthState>, action: StartLoginAction): void {
        ctx.patchState({
            loggingIn: true,
        });
    }

    @ActionDefinition(LoginSuccessAction)
    handleLoginSuccessAction(ctx: StateContext<AuthState>, action: LoginSuccessAction): void {
        ctx.patchState({
            loggingIn: false,
            isLoggedIn: true,
            currentUserId: action.user.id,
            sid: action.sid,
            lastError: '',
        });
    }

    @ActionDefinition(LoginErrorAction)
    handleLoginErrorAction(ctx: StateContext<AuthState>, action: LoginErrorAction): void {
        ctx.patchState({
            isLoggedIn: false,
            loggingIn: false,
            lastError: action.message,
            currentUserId: null,
            sid: null,
        });
    }

    @ActionDefinition(StartLogoutAction)
    handleLogoutStartAction(ctx: StateContext<AuthState>, action: StartLogoutAction): void {
        ctx.patchState({
            loggingOut: true,
        });
    }

    @ActionDefinition(LogoutSuccessAction)
    handleLogoutSuccessAction(ctx: StateContext<AuthState>, action: LogoutSuccessAction): void {
        ctx.patchState({
            loggingOut: false,
            isLoggedIn: false,
            currentUserId: null,
            sid: null,
        });
    }

    @ActionDefinition(LogoutErrorAction)
    handleLogoutErrorAction(ctx: StateContext<AuthState>, action: LogoutErrorAction): void {
        ctx.patchState({
            loggingOut: false,
            lastError: action.message,
        });
    }

    @ActionDefinition(StartValidationAction)
    handleStartValidationAction(ctx: StateContext<AuthState>, action: StartValidationAction): void {
        ctx.patchState({
            loggingIn: true,
        });
    }

    @ActionDefinition(ValidationSuccessAction)
    handleValidationSuccessAction(ctx: StateContext<AuthState>, action: ValidationSuccessAction): void {
        ctx.patchState({
            loggingIn: false,
            isLoggedIn: true,
            currentUserId: action.user.id,
            sid: action.sid,
            lastError: '',
        });
    }

    @ActionDefinition(ValidationErrorAction)
    handleValidationErrorAction(ctx: StateContext<AuthState>, action: ValidationErrorAction): void {
        ctx.patchState({
            isLoggedIn: false,
            loggingIn: false,
            lastError: action.message,
            currentUserId: null,
            sid: null,
        });
    }
}

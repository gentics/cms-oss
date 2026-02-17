import { ActionDeclaration } from '@gentics/cms-components';
import { Raw, User } from '@gentics/cms-models';
import { deepFreeze } from '@gentics/ui-core';
import { KeycloakError } from './errors';

export const SKIP_KEYCLOAK_PARAMETER_NAME = 'skip-sso';
export const KEYCLOAK_ERROR_KEY = 'keycloakError'

export enum KeycloakConnectionState {
    CLEAN,
    INITIALIZING,
    AVAILABLE,
    CONNECTED,
    INVALID_CONFIG,
    UNREACHABLE,
    ERROR,
}

const MODULE_STATE = 'auth';

export interface AuthStateModel {
    /** If the user is logged in. */
    isLoggedIn: boolean;
    /** If the user is currently logging in */
    loggingIn: boolean;
    /** If the user is currently logging out */
    loggingOut: boolean;
    /** If the user is currently changing passwords */
    changingPassword: boolean;
    /** If the sso login was skipped by the user (query param) */
    ssoSkipped: boolean;
    /** The GCMS session ID */
    sid: number | null;
    /** The logged in user object (if logged in) */
    user: User<Raw> | null;
    /** The message of the last error that was encountered */
    lastError: string | null;
    /** The message of the last keycloak error that was encountered */
    keycloakError: string | null;
    /** If keycloak is available for login. `null` means not yet determined. */
    keycloakAvailable: boolean | null;
    /** If it should display the SSO login button. */
    showSingleSignOnButton: boolean;
}

export const INITIAL_AUTH_STATE = deepFreeze<AuthStateModel>({
    isLoggedIn: false,
    loggingIn: false,
    loggingOut: false,
    changingPassword: false,
    ssoSkipped: false,
    sid: null,
    lastError: null,
    user: null,
    keycloakError: null,
    keycloakAvailable: null,
    showSingleSignOnButton: false,
});

@ActionDeclaration(MODULE_STATE)
export class LoginStart {
    static readonly type = 'LoginStart';
}

@ActionDeclaration(MODULE_STATE)
export class LoginSuccess {
    static readonly type = 'LoginSuccess';
    constructor(public sid: number, public user: User) {}
}

@ActionDeclaration(MODULE_STATE)
export class LoginError {
    static readonly type = 'LoginError';
    constructor(public errorMsg: string) {}
}

@ActionDeclaration(MODULE_STATE)
export class LogoutStart {
    static readonly type = 'LogoutStart';
}

@ActionDeclaration(MODULE_STATE)
export class LogoutSuccess {
    static readonly type = 'LogoutSuccess';
}

@ActionDeclaration(MODULE_STATE)
export class LogoutError {
    static readonly type = 'LogoutError';
    constructor(public errorMsg: string) {}
}

@ActionDeclaration(MODULE_STATE)
export class ResetAuth {
    static readonly type = 'ResetAuth';
}

@ActionDeclaration(MODULE_STATE)
export class ValidateStart {
    static readonly type = 'ValidateStart';
}

@ActionDeclaration(MODULE_STATE)
export class ValidateSuccess {
    static readonly type = 'ValidateSuccess';
    constructor(public sid: number, public user: User) {}
}

@ActionDeclaration(MODULE_STATE)
export class ValidateError {
    static readonly type = 'ValidateError';
    constructor(public errorMessage: string) {}
}

@ActionDeclaration(MODULE_STATE)
export class ChangePasswordStart {
    static readonly type = 'ChangePasswordStart';
}

@ActionDeclaration(MODULE_STATE)
export class ChangePasswordSuccess {
    static readonly type = 'ChangePasswordSuccess';
}

@ActionDeclaration(MODULE_STATE)
export class ChangePasswordError {
    static readonly type = 'ChangePasswordError';
    constructor(public errorMsg: string) {}
}

@ActionDeclaration(MODULE_STATE)
export class SingleSignOnSkipped {
    static readonly type = 'SingleSignOnSkipped';
    constructor(public skipped: boolean) {}
}

@ActionDeclaration(MODULE_STATE)
export class KeycloakLoadStart {
    static readonly type = 'KeycloakLoadStart';
}

@ActionDeclaration(MODULE_STATE)
export class KeycloakLoadSuccess {
    static readonly type = 'KeycloakLoadSuccess';
    constructor(
        public available: boolean,
        public con: KeycloakConnectionState,
        public ssoButton: boolean,
    ) {}
}

@ActionDeclaration(MODULE_STATE)
export class KeycloakLoadError {
    static readonly type = 'KeycloakLoadError';
    constructor(
        public con: KeycloakConnectionState,
        public error: KeycloakError,
    ) {}
}

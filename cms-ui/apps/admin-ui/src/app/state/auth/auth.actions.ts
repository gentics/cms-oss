import { User } from '@gentics/cms-models';
import { AppState } from '../app-state';
import {  ActionDeclaration } from '../utils/state-utils';

const AUTH: keyof AppState = 'auth';

@ActionDeclaration(AUTH)
export class LoginStart {
    static readonly type = 'LoginStart';
}

@ActionDeclaration(AUTH)
export class LoginSuccess {
    static readonly type = 'LoginSuccess';
    constructor(public sid: number, public user: User) {}
}

@ActionDeclaration(AUTH)
export class LoginError {
    static readonly type = 'LoginError';
    constructor(public errorMsg: string) {}
}

@ActionDeclaration(AUTH)
export class LogoutStart {
    static readonly type = 'LogoutStart';
}

@ActionDeclaration(AUTH)
export class LogoutSuccess {
    static readonly type = 'LogoutSuccess';
}

@ActionDeclaration(AUTH)
export class LogoutError {
    static readonly type = 'LogoutError';
    constructor(public errorMsg: string) {}
}

@ActionDeclaration(AUTH)
export class ResetAuth {
    static readonly type = 'ResetAuth';
}

@ActionDeclaration(AUTH)
export class ValidateStart {
    static readonly type = 'ValidateStart';
}

@ActionDeclaration(AUTH)
export class ValidateSuccess {
    static readonly type = 'ValidateSuccess';
    constructor(public sid: number, public user: User) {}
}

@ActionDeclaration(AUTH)
export class ValidateError {
    static readonly type = 'ValidateError';
    constructor(public errorMessage: string) {}
}

@ActionDeclaration(AUTH)
export class ChangePasswordStart {
    static readonly type = 'ChangePasswordStart';
}

@ActionDeclaration(AUTH)
export class ChangePasswordSuccess {
    static readonly type = 'ChangePasswordSuccess';
}

@ActionDeclaration(AUTH)
export class ChangePasswordError {
    static readonly type = 'ChangePasswordError';
    constructor(public errorMsg: string) {}
}

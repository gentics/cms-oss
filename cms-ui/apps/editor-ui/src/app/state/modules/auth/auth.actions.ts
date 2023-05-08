import { User } from '@gentics/cms-models';
import { AppState } from '../../../common/models';
import { ActionDeclaration } from '../../state-utils';

export const AUTH_STATE_KEY: keyof AppState = 'auth';

@ActionDeclaration(AUTH_STATE_KEY)
export class UpdateIsAdminAction {
    constructor(
        public isAdmin: boolean,
    ) {}
}

@ActionDeclaration(AUTH_STATE_KEY)
export class ChangePasswordAction {
    constructor(
        public isChanging: boolean,
        public errorMessage?: string,
    ) {}
}

@ActionDeclaration(AUTH_STATE_KEY)
export class StartLoginAction {}

@ActionDeclaration(AUTH_STATE_KEY)
export class LoginSuccessAction {
    constructor(
        public sid: number,
        public user: User,
    ) {}
}

@ActionDeclaration(AUTH_STATE_KEY)
export class LoginErrorAction {
    constructor(
        public message: string,
    ) {}
}

@ActionDeclaration(AUTH_STATE_KEY)
export class StartLogoutAction {}

@ActionDeclaration(AUTH_STATE_KEY)
export class LogoutSuccessAction {}

@ActionDeclaration(AUTH_STATE_KEY)
export class LogoutErrorAction {
    constructor(
        public message: string,
    ) {}
}

@ActionDeclaration(AUTH_STATE_KEY)
export class StartValidationAction {}

@ActionDeclaration(AUTH_STATE_KEY)
export class ValidationSuccessAction {
    constructor(
        public sid: number,
        public user: User,
    ) {}
}

@ActionDeclaration(AUTH_STATE_KEY)
export class ValidationErrorAction {
    constructor(
        public message: string,
    ) {}
}

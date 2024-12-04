import { Raw, User } from '@gentics/cms-models';

export interface AuthState {
    isAdmin: boolean;
    isLoggedIn: boolean;
    loggingIn: boolean;
    loggingOut: boolean;
    changingPassword: boolean;
    currentUserId: number;
    currentUser: User<Raw>;
    // The GCMS session ID
    sid: number;
    lastError: string;
}

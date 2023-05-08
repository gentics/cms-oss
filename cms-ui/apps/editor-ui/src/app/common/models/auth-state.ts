export interface AuthState {
    isAdmin: boolean;
    isLoggedIn: boolean;
    loggingIn: boolean;
    loggingOut: boolean;
    changingPassword: boolean;
    currentUserId: number;
    // The GCMS session ID
    sid: number;
    lastError: string;
}

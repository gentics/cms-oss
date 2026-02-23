import { Injectable } from '@angular/core';
import { KeycloakConfiguration } from '@gentics/cms-models';
import { GCMSRestClientRequestError } from '@gentics/cms-rest-client';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { Store } from '@ngxs/store';
import Keycloak from 'keycloak-js';
import { Observable, throwError } from 'rxjs';
import {
    KeycloakConfigLoadError,
    KeycloakInitializationError,
    KeycloakInvalidConfigError,
    KeycloakUnreachableError,
} from '../../errors';
import {
    KeycloakConnectionState,
    KeycloakLoadError,
    KeycloakLoadStart,
    KeycloakLoadSuccess,
    SingleSignOnSkipped,
    SKIP_KEYCLOAK_PARAMETER_NAME,
} from '../../models';

/** The Keycloak global is exposed when loading the keycloak.js script from the Keycloak server */
let keycloak: Keycloak;

const RETURNED_FROM_LOGIN_BUTTON_PARAMETER_NAME = 'button-back';

const ARGS_TO_VERIFY: (keyof KeycloakConfiguration)[] = [
    'auth-server-url',
    'realm',
    'resource',
];

@Injectable({
    providedIn: 'root',
})
export class KeycloakService {

    private config: KeycloakConfiguration | null = null;
    private state: KeycloakConnectionState = KeycloakConnectionState.CLEAN;

    constructor(
        private client: GCMSRestClientService,
        private store: Store,
    ) {}

    /**
     * Initializes Keycloak.
     *
     * This static method should be invoked prior to bootstrapping the app, since it will trigger redirects which will
     * cause multiple app reloads/bootstraps and will also break routing.
     */
    checkKeycloakAuth(): Promise<boolean> {
        if (checkParameter(SKIP_KEYCLOAK_PARAMETER_NAME)) {
            this.store.dispatch(new SingleSignOnSkipped(true));
            // same value provided as via .catch when config was not found
            return Promise.resolve(false);
        }

        this.store.dispatch(new SingleSignOnSkipped(false));
        return this.checkKeycloakAuthOnLoad();
    }

    get showSSOButton(): boolean {
        return this.state === KeycloakConnectionState.CONNECTED
          && this.config.showSSOButton;
    }

    /**
     * If Keycloak has been detected and successfully used prior to bootstrapping the app,
     * but has no token, we can call this function to log in into Keycloak.
     *
     * This function will not perform a login, if there already is a token.
     * Beware, the function does not check whether the token is expired.
     */
    login(): void {
        if (!keycloak) {
            console.error('Keycloak has not been set up. Cannot attempt login.');
            return;
        }

        // Don't need to login if we already are
        if (keycloak.token) {
            return;
        }

        let parameters: string;
        if (location.search.length > 0) {
            parameters = `${location.search}&${RETURNED_FROM_LOGIN_BUTTON_PARAMETER_NAME}`;
        } else {
            parameters = `?${RETURNED_FROM_LOGIN_BUTTON_PARAMETER_NAME}`;
        }

        keycloak.login({
            redirectUri: `${location.origin}${location.pathname}${parameters}${location.hash}`,
        });
    }

    /**
     * If Keycloak has been detected and successfully used prior to bootstrapping the app,
     * we can then call the ssologin REST endpoint and attempt to procure an SID to log in
     * to the CMS.
     *
     * Beware, the function does not check whether the token is expired.
     */
    attemptCmsLogin(): Observable<string> {
        if (!!keycloak && keycloak.token) {
            return this.client.auth.ssoLogin(keycloak.token);
        } else {
            return throwError(() => new Error('Keycloak has not been set up. Cannot attempt SSO login.'));
        }
    }

    /**
     * If Keycloak is enabled, logout from keycloak
     */
    async logout(): Promise<void> {
        if (!keycloak) {
            return;
        }

        try {
            await keycloak.logout();
            console.log('Keycloak logout successful', keycloak);
        } catch (error) {
            console.error('Keycloak logout failed', error);
            throw error;
        }
    }

    /**
     * Starts the entire keycloak workflow if needed.
     * Loads the config, and will redirect to keycloak if SSO is needed/available.
     * @throws {KeycloakError} when an unexpected error occurs.
     * @returns If it was attempted to connect to keycloak.
     */
    async checkKeycloakAuthOnLoad(): Promise<boolean> {
        this.state = KeycloakConnectionState.INITIALIZING;
        this.store.dispatch(new KeycloakLoadStart());
        let keycloakUrl: URL;

        try {
            this.config = await this.client.keycloak.configuration().toPromise();
            this.config.showSSOButton = this.config.showSSOButton || false;

            // eslint-disable-next-line no-console
            console.info('Keycloak config found');
        } catch (err) {
            if (err instanceof GCMSRestClientRequestError) {
                if (err.responseCode === 404) {
                    // log info that the 404 network error can safely be ignored,
                    // otherwise end-users who look in the console may get confused
                    // about why KeyCloak is being mentioned if they don't use it
                    // eslint-disable-next-line no-console
                    console.info('A keycloak config file was not found. If you are not using keycloak for authentication,'
                      + ' this notice can safely be ignored.');
                    this.state = KeycloakConnectionState.CLEAN;
                    this.store.dispatch(new KeycloakLoadSuccess(false, this.state, false));
                    return false;
                }
            }

            this.state = KeycloakConnectionState.ERROR;
            const kErr = new KeycloakConfigLoadError('Keycloak endpoint unreachable or returned invalid response', {
                cause: err,
            });
            this.store.dispatch(new KeycloakLoadError(this.state, kErr));
            throw kErr;
        }

        const invalidArgs: (keyof KeycloakConfiguration)[] = [];
        for (const arg of ARGS_TO_VERIFY) {
            if (!this.config[arg]) {
                invalidArgs.push(arg);
            }
        }

        if (invalidArgs.length > 0) {
            this.state = KeycloakConnectionState.INVALID_CONFIG;
            const kErr = new KeycloakInvalidConfigError(`The keycloak configuration is missing following properties: ${invalidArgs.join(', ')}`);
            this.store.dispatch(new KeycloakLoadError(this.state, kErr));
            throw kErr;
        }

        try {
            // Trying to parse the URL
            const baseUrl = this.config['auth-server-url'].replace(/\/$/, '');
            keycloakUrl = new URL(`${baseUrl}/realms/${this.config.realm}/.well-known/openid-configuration`);
        } catch (err) {
            this.state = KeycloakConnectionState.INVALID_CONFIG;
            const kErr = new KeycloakInvalidConfigError('Could not parse "auth-server-url" as URL', {
                cause: err,
            });
            this.store.dispatch(new KeycloakLoadError(this.state, kErr));
            throw kErr;
        }

        try {
            // we try to load the well-known configuration endpoint for the realm just to see whether keycloak is available
            await fetch(keycloakUrl);
        } catch (configErr) {
            try {
                // If we can connect to the keycloak instance however, then something is off with the realm
                await fetch(this.config['auth-server-url']);
                this.state = KeycloakConnectionState.INVALID_CONFIG;
                const kErr = new KeycloakInvalidConfigError(`The configured realm "${this.config.realm}" is not available or misconfigured`, {
                    cause: configErr,
                });
                this.store.dispatch(new KeycloakLoadError(this.state, kErr));
                throw kErr;
            } catch (hostErr) {
                this.state = KeycloakConnectionState.UNREACHABLE;
                const kErr = new KeycloakUnreachableError('The keycloak instance is currently not available/reachable', {
                    cause: configErr,
                });
                this.store.dispatch(new KeycloakLoadError(this.state, kErr));
                throw kErr;
            }
        }

        // Keycloak is available
        this.state = KeycloakConnectionState.AVAILABLE;

        try {
            keycloak = new Keycloak('/rest/keycloak');
            await initKeycloak(keycloak, this.config.showSSOButton ? 'check-sso' : 'login-required');
            this.state = KeycloakConnectionState.CONNECTED;
            this.store.dispatch(new KeycloakLoadSuccess(true, this.state, this.config.showSSOButton));
            return true;
        } catch (err) {
            this.state = KeycloakConnectionState.ERROR;
            const kErr = new KeycloakInitializationError('Could not initialize Keycloak', {
                cause: err,
            });
            this.store.dispatch(new KeycloakLoadError(this.state, kErr));
            throw kErr;
        }
    }
}

/**
 * Initialize the Keycloak instance and return a promise.
 */
async function initKeycloak(keycloak: Keycloak, onLoad: 'check-sso' | 'login-required'): Promise<void> {
    try {
        await keycloak.init({
            onLoad: onLoad,
            responseMode: 'fragment',
            checkLoginIframe: false,
            enableLogging: true,
            useNonce: false,
        });
        console.log('Keycloak login successful', keycloak);
    } catch (error) {
        console.error('Keycloak auth failed', error);
        throw error;
    }
}

/**
 * Checks get parameters if keycloak login should be skipped
 */
function checkParameter(parameterToCheck: string): boolean {
    if (!location || !location.search || location.search.length < 1) {
        return false;
    }

    try {
        const parameters = new URLSearchParams(location.search);
        const found = parameters.has(parameterToCheck);

        if (found) {
            // eslint-disable-next-line no-console
            console.info(`Keycloak will be skipped since the parameter "${parameterToCheck}" was found.`);
        }

        return found;
    } catch (error) {
        // If parsing fails, then we ignore it
        return false;
    }
}

import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { Observable, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';

/** The Keycloak global is exposed when loading the keycloak.js script from the Keycloak server */
let keycloak: Keycloak;
let showSSOButton: boolean;

const NO_CONFIG_FOUND = 'Keycloak config file not found';
export const SKIP_KEYCLOAK_PARAMETER_NAME = 'skip-sso';
const RETURNED_FROM_LOGIN_BUTTON_PARAMETER_NAME = 'button-back';
const CUSTOMER_CONFIG_PATH = './../ui-conf/';
export const KEYCLOAK_ERROR_KEY = 'keycloakError'

/**
 * Enum for the ready state to not have magic numbers.
 * @see https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/readyState
 */
enum XMLHttpRequestState {
    UNSENT = 0,
    OPENED = 1,
    HEADERS_RECEIVED = 2,
    LOADING = 3,
    DONE = 4,
}

@Injectable()
export class KeycloakService {

    /** Keycloak support will be triggered if there is a keycloak.json file in the /config folder of the UI root. */
    static readonly keycloakConfigFile = CUSTOMER_CONFIG_PATH + 'keycloak.json';

    /** The app will not trigger redirect to Keycloak if ui-overrides.json has showSSOButton: true */
    static readonly uiOverridesConfigFile = CUSTOMER_CONFIG_PATH + 'ui-overrides.json';

    constructor(
        private client: GCMSRestClientService,
        private router: Router,
    ) {}

    /**
     * Initializes Keycloak.
     *
     * This static method should be invoked prior to bootstrapping the app, since it will trigger redirects which will
     * cause multiple app reloads/bootstraps and will also break routing.
     */
    checkKeycloakAuth(): Promise<any> {
        if (checkParameter(SKIP_KEYCLOAK_PARAMETER_NAME)) {
            // same value provided as via .catch when config was not found
            return Promise.resolve(undefined);
        }
        return loadJSON(KeycloakService.uiOverridesConfigFile)
            .then(uiOverrides => {
                console.info('UI-Overrides config found');
                if (uiOverrides.showSSOButton && !checkParameter(RETURNED_FROM_LOGIN_BUTTON_PARAMETER_NAME)) {
                    showSSOButton = true;
                    return this.checkKeycloakAuthOnLoad('check-sso');
                } else {
                    showSSOButton = false;
                    return this.checkKeycloakAuthOnLoad('login-required');
                }
            })
            .catch(error => {
                // log error only if the config was found (but could not be parsed)
                if (error !== NO_CONFIG_FOUND) {
                    console.error('Parsing UI-Overrides failed: ', error);
                }
                showSSOButton = false;
                return this.checkKeycloakAuthOnLoad('login-required');
            });
    }

    ssoSkipped(): boolean {
        return checkParameter(SKIP_KEYCLOAK_PARAMETER_NAME);
    }

    get keycloakEnabled(): boolean {
        return !!keycloak;
    }

    get showSSOButton(): boolean {
        return !!showSSOButton;
    }

    /**
     * If Keycloak has been detected and successfully used prior to bootstrapping the app,
     * but has no token, we can call this function to log in into Keycloak.
     *
     * This function will not perform a login, if there already is a token.
     * Beware, the function does not check whether the token is expired.
     */
    login(): void {
        if (!this.keycloakEnabled) {
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
        if (this.keycloakEnabled && keycloak.token) {
            return this.client.auth.ssoLogin(keycloak.token);
        } else {
            return throwError('Keycloak has not been set up. Cannot attempt SSO login.');
        }
    }

    /**
     * If Keycloak is enabled, logout from keycloak
     */
    async logout(): Promise<void> {
        if (!this.keycloakEnabled) {
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
     * Checks for the existence of a Keycloak config file, and if found runs the Keycloak authentication.
     */
    async checkKeycloakAuthOnLoad(onLoad: 'check-sso' | 'login-required'): Promise<any> {
        try {
            const keycloakConfig = await loadJSON(KeycloakService.keycloakConfigFile);
            console.info('Keycloak config found');

            // we try to load the well-known configuration endpoint for the realm just to see whether keycloak is available
            const keycloakUrl = keycloakConfig['auth-server-url'].replace(/\/$/, '') + '/realms/' + keycloakConfig['realm'] + '/.well-known/openid-configuration';
            await fetch(keycloakUrl);

            keycloak = new Keycloak(KeycloakService.keycloakConfigFile);
            return initKeycloak(keycloak, onLoad);
        } catch (error) {
            showSSOButton = false;

            if (error !== NO_CONFIG_FOUND) {
                this.router.navigate(['/login'], { state: {
                    [KEYCLOAK_ERROR_KEY]: 'shared.keycloak_not_available',
                }});
                console.log(error);
            } else {
                // log info that the 404 network error can safely be ignored,
                // otherwise end-users who look in the console may get confused
                // about why KeyCloak is being mentioned if they don't use it
                console.info('A keycloak config file was not found. If you are not using keycloak for authentication,' +
                   ' this notice can safely be ignored.');
            }
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
 * Load a URL and parse the contents as JSON.
 */
function loadJSON(url: string): Promise<any> {
    return fetch(url).then(response => {
        if (!response.ok) {
            if (response.status == 404) {
                return Promise.reject(NO_CONFIG_FOUND);
            } else {
                return Promise.reject(response.statusText);
            }
        } else {
            return response.json();
        }
    });
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
            console.info(`Keycloak will be skipped since the parameter "${parameterToCheck}" was found.`);
        }

        return found;
    } catch (error) {
        // If parsing fails, then we ignore it
        return false;
    }
}

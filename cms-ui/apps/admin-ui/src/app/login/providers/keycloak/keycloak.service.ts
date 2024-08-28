/* eslint-disable @typescript-eslint/no-unsafe-call */
/* eslint-disable no-console */
/* eslint-disable @typescript-eslint/no-use-before-define */
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { Observable, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { CUSTOMER_CONFIG_PATH } from '../../../common/config/config';
import { API_BASE_URL } from '../../../common/utils/base-urls/base-urls';

/** The Keycloak global is exposed when loading the keycloak.js script from the Keycloak server */
let keycloak: Keycloak;

let showSSOButton: boolean;

const NO_CONFIG_FOUND = 'Keycloak config file not found';
const SKIP_KEYCLOAK_PARAMETER_NAME = 'skip-sso';
const RETURNED_FROM_LOGIN_BUTTON_PARAMETER_NAME = 'button-back';

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
    static keycloakConfigFile = CUSTOMER_CONFIG_PATH + 'keycloak.json';

    /** The app will not trigger redirect to Keycloak if ui-overrides.json has showSSOButton: true */
    static uiOverridesConfigFile = CUSTOMER_CONFIG_PATH + 'ui-overrides.json';

    /**
     * Initializes Keycloak.
     *
     * This static method should be invoked prior to bootstrapping the app, since it will trigger redirects which will
     * cause multiple app reloads/bootstraps and will also break routing.
     */
    static checkKeycloakAuth(router: Router): Promise<any> {
        if (checkParameter(SKIP_KEYCLOAK_PARAMETER_NAME)) {
            // same value provided as via .catch when config was not found
            return Promise.resolve(undefined);
        }
        return loadJSON(KeycloakService.uiOverridesConfigFile)
            .then(uiOverrides => {
                console.info('UI-Overrides config found');
                if (uiOverrides.showSSOButton && !checkParameter(RETURNED_FROM_LOGIN_BUTTON_PARAMETER_NAME)) {
                    showSSOButton = true;
                    return checkKeycloakAuthOnLoad('check-sso', router);
                } else {
                    showSSOButton = false;
                    return checkKeycloakAuthOnLoad('login-required', router);
                }
            })
            .catch(() => {
                showSSOButton = false;
                return checkKeycloakAuthOnLoad('login-required', router);
            });
    }

    get keycloakEnabled(): boolean {
        return !!keycloak;
    }

    get showSSOButton(): boolean {
        return !!showSSOButton;
    }

    constructor(private http: HttpClient) {}

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
            const headers = new HttpHeaders({
                // eslint-disable-next-line @typescript-eslint/naming-convention, @typescript-eslint/restrict-template-expressions
                Authorization: `Bearer ${keycloak.token}`,
            });
            return this.http.get(`${API_BASE_URL}/auth/ssologin?ts=${Date.now()}`, { headers, responseType: 'text' });
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
}

/**
 * Checks for the existence of a Keycloak config file, and if found runs the Keycloak authentication.
 */
async function checkKeycloakAuthOnLoad(onLoad: 'check-sso' | 'login-required', router: Router): Promise<any> {
    try {
        const keycloakConfig = await loadJSON(KeycloakService.keycloakConfigFile);
        console.info('Keycloak config found');

        // Load the keycloak scripts from the keycloak instance.
        // Has to be done this way, sadly
        const keycloakUrl: string = keycloakConfig['auth-server-url'];
        await loadScripts([
            `${keycloakUrl}/js/keycloak.js`,
            `${keycloakUrl}/js/keycloak-authz.js`,
        ]);

        keycloak = new Keycloak(KeycloakService.keycloakConfigFile);
        return initKeycloak(keycloak, onLoad);
    } catch (error) {
        showSSOButton = false;

        if (error !== NO_CONFIG_FOUND) {
            console.error(error)

            router.navigate(['/login'], { state: {
                keycloakError: 'shared.keycloak_not_available',
            }});
        } else {
            // keycloak is not configured, thus we can continue without SSO
            router.navigate(['/login'], {
                queryParams: {
                    'skip-sso': 'true',
                },
            });

            console.log(error);
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
    return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.overrideMimeType('application/json');
        xhr.open('GET', url, true);
        xhr.onreadystatechange = () => {
            if (xhr.readyState !== XMLHttpRequestState.DONE) {
                return;
            }

            if (xhr.status === 200) {
                let json;
                try {
                    json = JSON.parse(xhr.responseText);
                    resolve(json);
                } catch (e) {
                    reject('Keycloak config file was found but could not be parsed.\n'
                        // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                        + `Error: "${e.message}"\n`
                        + `File contents: \n${xhr.responseText}`);
                }
                return;
            }

            if (xhr.status === 404) {
                // log info that the 404 network error can safely be ignored,
                // otherwise end-users who look in the console may get confused
                // about why KeyCloak is being mentioned if they don't use it
                console.info('A keycloak config file was not found. If you are not using keycloak for authentication,' +
                    ' this notice can safely be ignored.');
            }

            reject(NO_CONFIG_FOUND);
        };
        xhr.send();
    });
}

/**
 * Returns a promise which resolves when all the scripts have loaded.
 */
function loadScripts(sources: string[]): Promise<void[]> {
    return Promise.all(
        sources.map(src => loadScript(src)),
    );
}

/**
 * Loads a JavaScript file asynchronously.
 */
function loadScript(src: string): Promise<void> {
    return new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.onload = () => resolve();
        script.onerror = (error) => reject(error);
        script.src = src;

        document.head.appendChild(script);
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

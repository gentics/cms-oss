import { ServiceBase } from '@admin-ui/shared/providers/service-base/service.base';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import * as KC from 'keycloak-js';
import { Observable } from 'rxjs';

import { CUSTOMER_CONFIG_PATH } from '../../../common/config/config';
import { API_BASE_URL } from '../../../common/utils/base-urls/base-urls';

/** The Keycloak global is exposed when loading the keycloak.js script from the Keycloak server */
declare const Keycloak: (config?: string | {}) => KC.KeycloakInstance;
let keycloak: KC.KeycloakInstance;

let _showSSOButton: boolean;

const NO_CONFIG_FOUND = 'Keycloak config file not found';
const SKIP_KEYCLOAK_PARAMETER_NAME = 'skip-sso';
const RETURNED_FROM_LOGIN_BUTTON_PARAMETER_NAME = 'button-back';

@Injectable()
export class KeycloakService extends ServiceBase {

    /** Keycloak support will be triggered if there is a keycloak.json file in the /config folder of the UI root. */
    static keycloakConfigFile = CUSTOMER_CONFIG_PATH + 'keycloak.json';

    /** The app will not trigger redirect to Keycloak if ui-overrides.json has showSSOButton: true */
    static uiOverridesConfigFile = CUSTOMER_CONFIG_PATH + 'ui-overrides.json';

    /**
     * Checks for the existence of a Keycloak config file, and if found runs the Keycloak authentication.
     *
     * This static method should be invoked prior to bootstrapping the app, since it will trigger redirects which will
     * cause multiple app reloads/bootstraps and will also break routing.
     */
    static checkKeycloakAuth(): Promise<any> {
        if (checkParameter(SKIP_KEYCLOAK_PARAMETER_NAME)) {
            // same value provided as via .catch when config was not found
            return Promise.resolve(undefined);
        }
        return loadJSON(KeycloakService.uiOverridesConfigFile)
            .then(uiOverrides => {
                // tslint:disable-next-line: no-console (the NGXLogger is not available here)
                console.info(`UI-Overrides config found`);
                if (uiOverrides.showSSOButton && !checkParameter(RETURNED_FROM_LOGIN_BUTTON_PARAMETER_NAME)) {
                    _showSSOButton = true;
                    return checkKeycloakAuthOnLoad('check-sso');
                } else {
                    _showSSOButton = false;
                    return checkKeycloakAuthOnLoad('login-required');
                }
            })
            .catch(e => {
                _showSSOButton = false;
                return checkKeycloakAuthOnLoad('login-required');
            });
    }

    get keycloakEnabled(): boolean {
        return !!keycloak;
    }

    get showSSOButton(): boolean {
        return !!_showSSOButton;
    }

    constructor(private http: HttpClient) {
        super();
    }

    /**
     * If Keycloak has been detected and successfully used prior to bootstrapping the app,
     * but has no token, we can call this function to log in into Keycloak.
     *
     * This function will not perform a login, if there already is a token.
     * Beware, the function does not check whether the token is expired.
     */
    login(): void {
        if (this.keycloakEnabled) {
            if (!keycloak.token) {
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
        } else {
            console.error(`Keycloak has not been set up. Cannot attempt login.`);
        }

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
                Authorization: 'Bearer ' + keycloak.token,
            });
            return this.http.get(`${API_BASE_URL}/auth/ssologin?ts=${Date.now()}`, { headers, responseType: 'text' });
        } else {
            return Observable.throw(`Keycloak has not been set up. Cannot attempt SSO login.`);
        }
    }

    /**
     * If Keycloak is enabled, logout from keycloak
     */
    logout(): Promise<void> {
        if (this.keycloakEnabled) {
            return new Promise((resolve, reject) => {
                keycloak.logout().success(() => {
                    console.log(`Keycloak logout successful`, keycloak);
                    resolve();
                }).error((e: any) => {
                    reject('Keycloak logout failed ' + e);
                });
            });
        } else {
            return Promise.resolve();
        }
    }
}

/**
 * Checks for the existence of a Keycloak config file, and if found runs the Keycloak authentication.
 */
function checkKeycloakAuthOnLoad(onLoad: 'check-sso' | 'login-required'): Promise<any> {
    return loadJSON(KeycloakService.keycloakConfigFile)
            .then(keycloakConfig => {
                // tslint:disable-next-line: no-console (the NGXLogger is not available here)
                console.info(`Keycloak config found`);
                const keycloakUrl = keycloakConfig['auth-server-url'];
                return loadScripts([
                    `${keycloakUrl}/js/keycloak.js`,
                    `${keycloakUrl}/js/keycloak-authz.js`,
                ]);
            })
            .then(() => {
                keycloak = Keycloak(KeycloakService.keycloakConfigFile);
                return initKeycloak(keycloak, onLoad);
            })
            .catch(e => {
                // Since it is quite usual for no Keycloak.json file to be found in most installations,
                // we swallow this error. All other errors will be re-thrown.
                if (e !== NO_CONFIG_FOUND) {
                    throw e;
                }
            });
}


/**
 * Initialize the Keycloak instance and return a promise.
 */
function initKeycloak(kc: Keycloak.KeycloakInstance, onLoad: 'check-sso' | 'login-required'): Promise<void> {
    return new Promise((resolve, reject) => {
        kc.init({ onLoad: onLoad, responseMode: 'fragment', checkLoginIframe: false })
            .success(() => {
                // tslint:disable-next-line: no-console (the NGXLogger is not available here)
                console.log(`Keycloak login successful`, kc);
                resolve();
            })
            .error((e: any) => {
                reject('Keycloak auth failed ' + e);
            });
    });
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
            if (xhr.readyState === 4) {
                if (xhr.status === 200) {
                    let json;
                    try {
                        json = JSON.parse(xhr.responseText);
                        resolve(json);
                    } catch (e) {
                        reject(`Keycloak config file was found but could not be parsed.\n`
                            + `Error: "${e.message}"\n`
                            + `File contents: \n${xhr.responseText}`);
                    }
                } else {
                    if (xhr.status === 404) {
                        // log info that the 404 network error can safely be ignored,
                        // otherwise end-users who look in the console may get confused
                        // about why KeyCloak is being mentioned if they don't use it
                        // tslint:disable-next-line: no-console (the NGXLogger is not available here)
                        console.info(`A keycloak config file was not found. If you are not using keycloak for authentication,` +
                            ` this notice can safely be ignored.`);
                    }
                    reject(NO_CONFIG_FOUND);
                }
            }
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
        script.onerror = () => reject();
        script.src = src;

        document.head.appendChild(script);
    });
}

/**
 * Checks get parameters if keycloak login should be skipped
 */
function checkParameter(parameterToCheck: string): boolean {
    if (location && location.search && location.search.length > 0) {
        const parameters = location.search.substr(1).split('&');
        for (let parameter of parameters) {
            const splitParameter = parameter.split('=');
            // as soon as the parameter is found it is assumed that keycloak should be skipped, regardless of the value
            if (splitParameter.length >= 1 && decodeURIComponent(splitParameter[0]) === parameterToCheck) {
                // tslint:disable-next-line: no-console (the NGXLogger is not available here)
                console.info(`Keycloak will be skipped since the parameter ${parameterToCheck} was found.`);
                return true;
            }
        }
        return false;
    } else {
        return false;
    }
}

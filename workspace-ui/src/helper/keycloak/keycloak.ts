import Keycloak from 'keycloak-js';

import { type CmsKeycloakConfig, getCmsKeycloakConfig, ssoLoginToCms } from '@/services/cmsApiService/cmsApiService';

/**
 * Keycloak single sign-on, as the editor and admin UI do it (`cms-ui`, `KeycloakService` in
 * `libs/cms-components/auth`): the CMS offers its Keycloak settings at `GET /rest/keycloak`,
 * `keycloak-js` logs in at Keycloak, and `GET /rest/auth/ssologin` with the access token turns that
 * into a CMS session cookie.
 */

/** Skips single sign-on, for a login with user name and password (the editor's `skip-sso`). */
export const SKIP_SSO_PARAMETER = 'skip-sso';

// Added to the URL Keycloak returns to after the SSO button was used, so the button is not offered again.
const RETURNED_FROM_BUTTON_PARAMETER = 'button-back';

/** The outcome of `startSingleSignOn`. */
export interface SingleSignOnState {
    /** Whether the CMS has Keycloak set up. */
    available: boolean;
    /** Whether the login form offers SSO as a button (`keycloak.show_sso_button`). */
    showButton: boolean;
    /** Whether a CMS session was created from a Keycloak login. */
    loggedIn: boolean;
}

// The Keycloak client of this page load, once `startSingleSignOn` set it up.
let keycloak: Keycloak | undefined;

function hasParameter(name: string): boolean {
    return new URLSearchParams(window.location.search).has(name);
}

// A Keycloak client from the settings the CMS offers, without loading them a second time.
function createKeycloak(config: CmsKeycloakConfig): Keycloak {
    return new Keycloak({ url: config['auth-server-url'], realm: config.realm, clientId: config.resource });
}

/**
 * Sets up single sign-on and, when Keycloak has a login, creates the CMS session from it. Without
 * the SSO button (`keycloak.show_sso_button` off) Keycloak requires a login and redirects to its
 * login page right away, so the promise does not settle on this page. With the button it only
 * checks for an existing login. Resolves to `available: false` when the CMS has no Keycloak or the
 * URL has `skip-sso`. Throws when Keycloak is set up but cannot be used.
 */
export async function startSingleSignOn(): Promise<SingleSignOnState> {
    if (hasParameter(SKIP_SSO_PARAMETER)) {
        return { available: false, showButton: false, loggedIn: false };
    }

    const config = await getCmsKeycloakConfig();

    if (!config) {
        return { available: false, showButton: false, loggedIn: false };
    }

    const showButton = !!config.showSSOButton && !hasParameter(RETURNED_FROM_BUTTON_PARAMETER);

    keycloak = createKeycloak(config);

    await keycloak.init({
        onLoad: showButton ? 'check-sso' : 'login-required',
        responseMode: 'fragment',
        checkLoginIframe: false,
    });

    if (!keycloak.token) {
        return { available: true, showButton, loggedIn: false };
    }

    await ssoLoginToCms(keycloak.token);

    return { available: true, showButton, loggedIn: true };
}

/**
 * Redirects to the Keycloak login page (the SSO button). Keycloak returns to this page, with the
 * marker that keeps the button from being offered again.
 */
export async function loginWithKeycloak(): Promise<void> {
    if (!keycloak) {
        throw new Error('Keycloak is not set up.');
    }

    const params = new URLSearchParams(window.location.search);

    params.set(RETURNED_FROM_BUTTON_PARAMETER, '');

    const { origin, pathname, hash } = window.location;

    await keycloak.login({ redirectUri: `${origin}${pathname}?${params.toString()}${hash}` });
}

/**
 * After the CMS logout: logs out at Keycloak too, so a Keycloak login does not log the user straight
 * back in; Keycloak then returns to the app. Without a Keycloak login (no Keycloak, `skip-sso`, or a
 * check on this page load that found none) it loads the page again. Either way the page starts
 * fresh, so no state of the logged-out user stays in memory.
 */
export async function leaveAfterLogout(): Promise<void> {
    const redirectUri = `${window.location.origin}${window.location.pathname}`;

    if (keycloak) {
        if (keycloak.authenticated) {
            await keycloak.logout({ redirectUri });
        } else {
            window.location.reload();
        }

        return;
    }

    // Single sign-on did not run on this page load (the session existed already), so whether there
    // is a Keycloak login is unknown: log out at Keycloak whenever the CMS has it.
    const config = hasParameter(SKIP_SSO_PARAMETER) ? null : await getCmsKeycloakConfig();

    if (!config) {
        window.location.reload();

        return;
    }

    // Without `onLoad`, `init` only sets the client up and does not leave the page.
    const logoutClient = createKeycloak(config);

    await logoutClient.init({ responseMode: 'fragment', checkLoginIframe: false });
    await logoutClient.logout({ redirectUri });
}

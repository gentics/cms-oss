import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { getCmsKeycloakConfig, ssoLoginToCms } from '@/services/cmsApiService/cmsApiService';

import { loginWithKeycloak, startSingleSignOn } from './keycloak';

const keycloakMock = vi.hoisted(() => ({
    config: undefined as unknown,
    token: undefined as string | undefined,
    init: vi.fn<(options: Record<string, unknown>) => Promise<boolean>>(),
    login: vi.fn<(options: Record<string, unknown>) => Promise<void>>(),
    logout: vi.fn<(options: Record<string, unknown>) => Promise<void>>(),
}));

vi.mock('keycloak-js', () => ({
    default: class {
        constructor(config: unknown) {
            keycloakMock.config = config;
        }

        get token() {
            return keycloakMock.token;
        }

        get authenticated() {
            return !!keycloakMock.token;
        }

        init = keycloakMock.init;

        login = keycloakMock.login;

        logout = keycloakMock.logout;
    },
}));

vi.mock('@/services/cmsApiService/cmsApiService', () => ({
    getCmsKeycloakConfig: vi.fn(),
    ssoLoginToCms: vi.fn(),
}));

const config = { 'auth-server-url': 'https://sso.example.com', realm: 'cms', resource: 'cms-ui' };

function openAt(search: string) {
    window.history.replaceState(null, '', `/workspace/${search}#/sessions/s-1`);
}

describe('startSingleSignOn', () => {
    beforeEach(() => {
        openAt('');
        keycloakMock.token = undefined;
        keycloakMock.init.mockResolvedValue(true);
        vi.mocked(ssoLoginToCms).mockResolvedValue();
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('is not available when the CMS has no Keycloak', async () => {
        vi.mocked(getCmsKeycloakConfig).mockResolvedValue(null);

        await expect(startSingleSignOn()).resolves.toEqual({ available: false, showButton: false, loggedIn: false });
        expect(keycloakMock.init).not.toHaveBeenCalled();
    });

    it('is skipped with skip-sso in the URL', async () => {
        openAt('?skip-sso');

        await expect(startSingleSignOn()).resolves.toEqual({ available: false, showButton: false, loggedIn: false });
        expect(getCmsKeycloakConfig).not.toHaveBeenCalled();
    });

    it('requires a Keycloak login without the SSO button and turns its token into a CMS session', async () => {
        vi.mocked(getCmsKeycloakConfig).mockResolvedValue(config);
        keycloakMock.token = 'kc-token';

        await expect(startSingleSignOn()).resolves.toEqual({ available: true, showButton: false, loggedIn: true });
        expect(keycloakMock.config).toEqual({ url: 'https://sso.example.com', realm: 'cms', clientId: 'cms-ui' });
        expect(keycloakMock.init).toHaveBeenCalledWith(expect.objectContaining({ onLoad: 'login-required', responseMode: 'fragment', checkLoginIframe: false }));
        expect(ssoLoginToCms).toHaveBeenCalledWith('kc-token');
    });

    it('only checks for a Keycloak login with the SSO button', async () => {
        vi.mocked(getCmsKeycloakConfig).mockResolvedValue({ ...config, showSSOButton: true });

        await expect(startSingleSignOn()).resolves.toEqual({ available: true, showButton: true, loggedIn: false });
        expect(keycloakMock.init).toHaveBeenCalledWith(expect.objectContaining({ onLoad: 'check-sso' }));
        expect(ssoLoginToCms).not.toHaveBeenCalled();
    });

    it('does not offer the button again after returning from it', async () => {
        vi.mocked(getCmsKeycloakConfig).mockResolvedValue({ ...config, showSSOButton: true });
        openAt('?button-back=');

        await expect(startSingleSignOn()).resolves.toMatchObject({ showButton: false });
        expect(keycloakMock.init).toHaveBeenCalledWith(expect.objectContaining({ onLoad: 'login-required' }));
    });

    it('redirects to Keycloak from the button, back to the same route with the marker', async () => {
        vi.mocked(getCmsKeycloakConfig).mockResolvedValue({ ...config, showSSOButton: true });
        await startSingleSignOn();

        await loginWithKeycloak();

        expect(keycloakMock.login).toHaveBeenCalledWith({ redirectUri: `${window.location.origin}/workspace/?button-back=#/sessions/s-1` });
    });
});

describe('leaveAfterLogout', () => {
    // The Keycloak client of a page load lives in the module: each test starts with a fresh one.
    async function freshModule() {
        vi.resetModules();

        const cmsApiService = await import('@/services/cmsApiService/cmsApiService');
        const keycloak = await import('./keycloak');

        return { getConfig: vi.mocked(cmsApiService.getCmsKeycloakConfig), ...keycloak };
    }

    beforeEach(() => {
        openAt('');
        keycloakMock.token = undefined;
        keycloakMock.init.mockResolvedValue(true);
        keycloakMock.logout.mockResolvedValue();
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('logs out at Keycloak after a single sign-on on this page load', async () => {
        const fresh = await freshModule();

        fresh.getConfig.mockResolvedValue(config);
        keycloakMock.token = 'kc-token';
        await fresh.startSingleSignOn();

        await fresh.leaveAfterLogout();

        expect(keycloakMock.logout).toHaveBeenCalledWith({ redirectUri: `${window.location.origin}/workspace/` });
        expect(keycloakMock.init).toHaveBeenCalledTimes(1);
    });

    it('logs out at Keycloak when the session existed before this page load', async () => {
        const fresh = await freshModule();

        fresh.getConfig.mockResolvedValue(config);

        await fresh.leaveAfterLogout();

        expect(keycloakMock.config).toEqual({ url: 'https://sso.example.com', realm: 'cms', clientId: 'cms-ui' });
        // Without `onLoad`: no redirect to the Keycloak login before the logout.
        expect(keycloakMock.init).toHaveBeenCalledWith({ responseMode: 'fragment', checkLoginIframe: false });
        expect(keycloakMock.logout).toHaveBeenCalledWith({ redirectUri: `${window.location.origin}/workspace/` });
    });

    it('does not log out at Keycloak when a check on this page load found no Keycloak login', async () => {
        const fresh = await freshModule();

        fresh.getConfig.mockResolvedValue({ ...config, showSSOButton: true });
        await fresh.startSingleSignOn();

        await fresh.leaveAfterLogout();

        expect(keycloakMock.logout).not.toHaveBeenCalled();
        expect(fresh.getConfig).toHaveBeenCalledTimes(1);
    });

    it('does not log out at Keycloak when the CMS has none', async () => {
        const fresh = await freshModule();

        fresh.getConfig.mockResolvedValue(null);

        await fresh.leaveAfterLogout();

        expect(keycloakMock.init).not.toHaveBeenCalled();
        expect(keycloakMock.logout).not.toHaveBeenCalled();
    });

    it('does not ask the CMS for Keycloak with skip-sso in the URL', async () => {
        const fresh = await freshModule();

        openAt('?skip-sso');

        await fresh.leaveAfterLogout();

        expect(fresh.getConfig).not.toHaveBeenCalled();
        expect(keycloakMock.logout).not.toHaveBeenCalled();
    });
});

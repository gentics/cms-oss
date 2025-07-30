import { Locator, Page } from '@playwright/test';
import { DEFAULT_KEYCLOAK_URL, ENV_KEYCLOAK_URL, LoginInformation } from './common';
import { matchesPath } from './utils';

export function blockKeycloakConfig(page: Page): Promise<void> {
    return page.route('/ui-conf/keycloak.json', route => {
        return route.abort('failed');
    });
}

export function waitForKeycloakAuthPage(page: Page): Promise<void> {
    const kcUrl = process.env[ENV_KEYCLOAK_URL] || DEFAULT_KEYCLOAK_URL;
    const parsedUrl = new URL(kcUrl);

    return page.waitForURL(url =>
        url.host === parsedUrl.host
        && matchesPath(url, '/realms/*/protocol/openid-connect/auth'),
    );
}

export async function navigateToApp(page: Page, path: string = '', withSSO: boolean = false): Promise<void> {
    if (path.startsWith('/')) {
        path = path.substring(1);
    }

    const fullPath = `./${!withSSO ? '?skip-sso' : ''}#/${path}`;
    await page.goto(fullPath);
}

export async function loginWithForm(source: Page | Locator, loginData: LoginInformation): Promise<void> {
    await source.locator('gtx-input[formcontrolname="username"] input:not([disabled]), input[name="username"]')
        .first()
        .fill(loginData.username);
    await source.locator('gtx-input[formcontrolname="password"] input:not([disabled]), input[name="password"]')
        .first()
        .fill(loginData.password);
    await source.locator('button[type="submit"]:not([disabled]), input[type="submit"]:not([disabled])')
        .first()
        .click();
}

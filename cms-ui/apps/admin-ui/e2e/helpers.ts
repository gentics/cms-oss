import { ENV_BASE_URL, isCIEnvironment, matchesPath } from '@gentics/e2e-utils';
import { Locator, Page } from '@playwright/test';
import { AUTH, LoginData } from './common';

export async function navigateToApp(page: Page, path: string = '/', omitSkipSSO?: boolean): Promise<void> {
    const hasBasePathOverride = !!process.env[ENV_BASE_URL];
    const isCI = isCIEnvironment();

    const fullPath = `${isCI && !hasBasePathOverride ? '/admin' : ''}/${!omitSkipSSO ? '?skip-sso' : ''}#/${path}`;
    await page.goto(fullPath);
}

export async function loginWithForm(source: Page | Locator, login: (keyof typeof AUTH) | LoginData): Promise<void> {
    // Get auth data and login
    const loginData: LoginData = typeof login === 'string' ? AUTH[login] : login;
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

export async function login(page: Page, account: string, keycloak?: boolean): Promise<void> {
    const data = AUTH[account];

    await page.fill('input[type="text"]', data.username);
    await page.fill('input[type="password"]', data.password);
    await page.click(`${keycloak ? 'input' : 'button'}[type="submit"]`);
}

export async function navigateToModule(page: Page, moduleId: string): Promise<void> {
    // Click the module item
    const moduleItem = page.locator(`gtx-dashboard-item[data-id="${moduleId}"] > .item:not(.disabled)`).locator('..');
    await moduleItem.click();

    // Wait for the module content to be visible
    await page.locator('gtx-split-view-router-outlet .master-route-wrapper > *:not(router-outlet)').waitFor();
}

/**
 * Selects a tab in a tab group by its ID
 */
export async function selectTab(source: Page | Locator, tabId: string): Promise<void> {
    const tab = source.locator(`.tab-link[data-id="${tabId}"]`);
    await tab.waitFor({ state: 'visible' });
    await tab.click();
}

/**
 * Logs out from the mesh management interface
 */
export async function logoutMeshManagement(page: Page): Promise<void> {
    const req = page.waitForResponse(response =>
        response.ok() && matchesPath(response.url(), '/rest/contentrepositories/*/proxy/api/v2/auth/logout'),
    );
    await page.locator('.management-container .logout-button').click();
    await req;
}

/**
 * Clicks the CR login button and waits for management to be visible
 */
export async function loginWithCR(page: Page, shouldBeLoggedIn: boolean = true): Promise<void> {
    await page.locator('.cr-login-button').click();

    if (shouldBeLoggedIn) {
        await page.locator('.management-container').waitFor({ state: 'visible' });
    }
}

/**
 * Clicks a modal button by its action
 */
export async function clickModalAction(source: Page | Locator, action: 'confirm' | 'cancel'): Promise<void> {
    const selector = action === 'confirm' ? '.modal-footer [data-action="confirm"]' : '.modal-footer [data-action="cancel"]';
    await source.locator(selector).click();
}

export function findEntityTableActionButton(source: Page | Locator, action: string): Locator {
    return source.locator(`.entity-table-actions-bar .table-action-button[data-action="${action}"] button`);
}

import { AccessControlledType } from '@gentics/cms-models';
import { ENV_CI, ENV_PLAYWRIGHT_TEST_CONNECT, matchesPath } from '@gentics/e2e-utils';
import { Locator, Page } from '@playwright/test';
import { AUTH, LoginData } from './common';

export async function navigateToApp(page: Page, path: string = '/', omitSkipSSO?: boolean): Promise<void> {
    const needsBasePath = process.env[ENV_CI] || process.env[ENV_PLAYWRIGHT_TEST_CONNECT];
    const fullPath = `${needsBasePath ? '/admin' : ''}/${!omitSkipSSO ? '?skip-sso' : ''}#/${path}`;
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

export async function navigateToModule(page: Page, moduleId: string, perms?: AccessControlledType): Promise<void> {
    // Setup route handlers for feature and permission checks
    let featureCheckPromise;
    let permCheckPromise;

    if (perms) {
        // Wait for both feature and permission checks
        featureCheckPromise = page.waitForResponse(response =>
            response.url().includes('/rest/admin/features/') && response.ok(),
        );

        permCheckPromise = page.waitForResponse(response =>
            response.url().includes(`/rest/perm/${perms}`) && response.ok(),
        );
    } else {
        // Only wait for feature check
        featureCheckPromise = page.waitForResponse(response =>
            response.url().includes('/rest/admin/features/') && response.ok(),
        );
    }

    // Click the module item
    const moduleItem = page.locator(`gtx-dashboard-item[data-id="${moduleId}"]`);
    await moduleItem.click();

    // Wait for the responses
    await featureCheckPromise;
    if (perms && permCheckPromise) {
        await permCheckPromise;
    }

    // Wait for the module content to be visible
    await page.locator('gtx-split-view-router-outlet .master-route-wrapper > *:not(router-outlet)').waitFor();
}

/**
 * Selects a tab in a tab group by its ID
 */
export async function selectTab(source: Page | Locator, tabId: string): Promise<void> {
    await source.locator(`.tab-link[data-id="${tabId}"]`).click();
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
    const req = page.waitForResponse(response =>
        (shouldBeLoggedIn ? response.ok() : !response.ok())
            && response.request().method() === 'POST'
            && matchesPath(response.url(), '/rest/contentrepositories/*/proxylogin'),
    );
    await page.locator('.cr-login-button').click();
    await req;

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

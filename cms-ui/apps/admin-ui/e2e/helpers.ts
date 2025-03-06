import { Locator, Page } from '@playwright/test';
import { AccessControlledType } from '@gentics/cms-models';
import { AUTH } from './common';

export async function navigateToApp(page: Page, path: string = '/', withSSO?: boolean): Promise<void> {
    const fullPath = `/admin/${!withSSO ? '?skip-sso' : ''}#/${path}`;
    await page.goto(fullPath);
}

export async function loginWithForm(page: Page, login: keyof typeof AUTH): Promise<void> {
    // Get auth data and login
    const loginData = AUTH[login];
    await page.fill('gtx-input[formcontrolname="username"] input', loginData.username);
    await page.fill('gtx-input[formcontrolname="password"] input', loginData.password);
    await page.click('button[type="submit"]');
}

export async function navigateToModule(page: Page, moduleId: string, perms?: AccessControlledType): Promise<void> {
    // Setup route handlers for feature and permission checks
    let featureCheckPromise;
    let permCheckPromise;

    if (perms) {
        // Wait for both feature and permission checks
        featureCheckPromise = page.waitForResponse(response =>
            response.url().includes('/rest/admin/features/') && response.ok()
        );

        permCheckPromise = page.waitForResponse(response =>
            response.url().includes(`/rest/perm/${perms}`) && response.ok()
        );
    } else {
        // Only wait for feature check
        featureCheckPromise = page.waitForResponse(response =>
            response.url().includes('/rest/admin/features/') && response.ok()
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

export function findTableRow(source: Page | Locator, id: string | number): Locator {
    return source.locator(`gtx-table .data-row[data-id="${id}"]`);
}

export function findTableAction(source: Page | Locator, id: string): Locator {
    return source.locator(`.action-column .action-button[data-id="${id}"], .header-row .action-column .action-button[data-id="${id}"]`);
}


/**
 * Finds a table row that contains the specified text
 */
export async function findTableRowByText(source: Page | Locator, text: string): Promise<Locator> {
    return source.locator('gtx-table .grid-row').filter({ hasText: text });
}

/**
 * Finds a table row by its data-id attribute
 */
export async function findTableRowById(source: Page | Locator, id: string): Promise<Locator> {
    return source.locator(`gtx-table .data-row[data-id="${id}"]`);
}

/**
 * Clicks a button in a table row's action column
 */
export async function clickTableRowAction(row: Locator, actionId: string): Promise<void> {
    await row.locator(`[data-id="${actionId}"]`).click();
}

/**
 * Expands a trable row (tree-table row)
 */
export async function expandTrableRow(row: Locator): Promise<void> {
    await row.locator('.row-expansion-wrapper').click();
}

/**
 * Finds a trable row by its text content
 */
export async function findTrableRowByText(source: Page | Locator, text: string): Promise<Locator> {
    return source.locator('gtx-mesh-role-permissions-trable .data-row').filter({ hasText: text });
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
    await page.locator('.management-container .logout-button').click();
}

/**
 * Checks if the mesh management container is visible/logged in
 */
export async function isMeshManagementVisible(page: Page): Promise<boolean> {
    const container = page.locator('.management-container');
    return await container.isVisible();
}

/**
 * Clicks the CR login button and waits for management to be visible
 */
export async function loginWithCR(page: Page): Promise<void> {
    await page.locator('.cr-login-button').click();
    await page.locator('.management-container').waitFor({ state: 'visible' });
}

/**
 * Clicks a modal button by its action
 */
export async function clickModalAction(page: Page, action: 'confirm' | 'cancel'): Promise<void> {
    const selector = action === 'confirm' ? '.modal-footer [data-action="confirm"]' : '.modal-footer [data-action="cancel"]';
    await page.locator(selector).click();
}


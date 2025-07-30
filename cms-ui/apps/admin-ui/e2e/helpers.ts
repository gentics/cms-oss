import { matchesPath } from '@gentics/e2e-utils';
import { Locator, Page } from '@playwright/test';

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

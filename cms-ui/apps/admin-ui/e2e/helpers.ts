import { matchesPath } from '@gentics/e2e-utils';
import { Locator, Page } from '@playwright/test';

export async function navigateToModule(page: Page, moduleId: string): Promise<Locator> {
    // Click the module item
    const moduleItem = page.locator(`gtx-dashboard-item[data-id="${moduleId}"] > .item:not(.disabled)`).locator('..');
    await moduleItem.click();

    // Wait for the module content to be visible
    const splitOutlet = page.locator('gtx-split-view-router-outlet .master-route-wrapper > *:not(router-outlet)');
    const genericOutlet = page.locator('gtx-generic-router-outlet > *:not(router-outlet):not(gtx-generic-router-outlet)');

    const module = splitOutlet.or(genericOutlet);

    await module.waitFor({ state: 'visible' });

    return module;
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

export function findEntityTableActionButton(source: Page | Locator, action: string): Locator {
    return source.locator(`.entity-table-actions-bar .table-action-button[data-action="${action}"] button`);
}

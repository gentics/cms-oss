import { test, expect } from '@playwright/test';
import { EntityImporter } from '@gentics/e2e-utils';
import { login, initPage } from './helpers';
import { AUTH_ADMIN } from './common';

test.describe('No Nodes', () => {
    const IMPORTER = new EntityImporter();

    test.beforeEach(async ({ page, request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await initPage(page);
        await page.goto('/');
        await login(page, AUTH_ADMIN);
    });

    test('should display message when no nodes are present', async ({ page }) => {
        const noNodesMessage = page.locator('.no-nodes-message');
        await expect(noNodesMessage).toBeVisible();
        await expect(noNodesMessage).toHaveText('No nodes available.');
    });
});

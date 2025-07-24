import { test, expect } from '@playwright/test';
import { EntityImporter } from '@gentics/e2e-utils';
import { login, initPage, navigateToApp } from './helpers';
import { AUTH_ADMIN } from './common';

test.describe('No Nodes', () => {
    const IMPORTER = new EntityImporter();

    test.beforeEach(async ({ page, request, context }, testInfo) => {
        testInfo.setTimeout(120_000);
        await context.clearCookies();
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest(true);
        await initPage(page);
        await navigateToApp(page);
        await login(page, AUTH_ADMIN);
    });

    test('should display message when no nodes are present', async ({ page }) => {
        const noNodesMessage = page.locator('gtx-no-nodes');
        await expect(noNodesMessage).toBeVisible();
    });
});

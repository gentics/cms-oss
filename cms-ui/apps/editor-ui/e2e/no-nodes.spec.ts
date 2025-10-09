import { EntityImporter, navigateToApp, loginWithForm } from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';

test.describe.configure({ mode: 'serial' });
test.describe('No Nodes', () => {
    const IMPORTER = new EntityImporter();

    test.beforeEach(async ({ page, request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);

        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest(true);

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
    });

    test('should display message when no nodes are present', async ({ page }) => {
        const noNodesMessage = page.locator('gtx-no-nodes');
        await expect(noNodesMessage).toBeVisible();
    });
});

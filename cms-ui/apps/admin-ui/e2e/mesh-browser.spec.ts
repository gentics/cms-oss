import { test, expect } from '@playwright/test';
import { EntityImporter, TestSize } from '@gentics/e2e-utils';
import { AUTH_ADMIN } from './common';
import { loginWithForm, navigateToApp, navigateToModule } from './helpers';
import * as auth from './auth.json';

test.describe('Mesh Browser', () => {
    const IMPORTER = new EntityImporter();
    const CR_NAME = 'Mesh CR';

    test.beforeAll(async ({ request }, testInfo) => {
        testInfo.setTimeout(120_000);
        IMPORTER.setApiContext(request);
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach(async ({ page, request, context }, testInfo) => {
        testInfo.setTimeout(120_000);
        await context.clearCookies();
        // Reset importer client to avoid 401 errors
        IMPORTER.setApiContext(request);

        // Reset importer client to avoid 401 errors
        await IMPORTER.cleanupTest();
        await IMPORTER.syncPackages(TestSize.MINIMAL);

        await navigateToApp(page);
        await loginWithForm(page, AUTH_ADMIN);

        // Navigate to constructs module
        await navigateToModule(page, 'mesh-browser');
    });

    test.describe('Mesh Browser', () => {
        test('should have content repositories listed', async ({ page }) => {
            const rows = page.locator('gtx-table .grid-row');
            await rows.waitFor({ timeout: 60_000 });
            await expect(rows).toHaveCount(1);
        });

        test('should show login gate on click', async ({ page }) => {
            const row = page.locator('gtx-table .grid-row', { hasText: CR_NAME });
            await row.waitFor({ timeout: 60_000 });
            await row.click();
            await expect(page.locator('.login-gate-wrapper')).toBeVisible();
        });
    });

    test.describe.skip('Mesh Browser (authenticated)', () => {
        test.beforeEach(async ({ page }) => {
            // Click into the Mesh CR
            const row = page.locator('gtx-table .grid-row', { hasText: CR_NAME });
            await row.waitFor({ timeout: 60_000 });
            await row.click();

            // Fill in Mesh credentials and submit
            await page.locator('.login-form input[type="text"]').fill(auth.mesh.username);
            await page.locator('.login-form input[type="password"]').fill(auth.mesh.password);
            await page.locator('.login-form button[type="submit"]').click();

            // Now the schema list should appear
            await expect(page.locator('.schema-list-wrapper')).toBeVisible();
        });

        test('should list content', async ({ page }) => {
            await expect(page.locator('.schema-items')).toHaveCount(1);
        });

        test('should be able to navigate to node content', async ({ page }) => {
            const container = page
                .locator('.schema-items .schema-element [data-is-container="true"]')
                .first();
            await expect(container).toBeVisible();
            await container.click();
        });

        test('should be able to open detail view', async ({ page }) => {
            const element = page
                .locator('.schema-items .schema-element [data-is-container="false"]')
                .first();
            await expect(element).toBeVisible();
            await element.click();
            await expect(page.locator('gtx-mesh-browser-editor')).toBeVisible();
        });
    });
});

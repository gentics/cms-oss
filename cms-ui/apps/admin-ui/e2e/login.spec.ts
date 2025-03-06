import { Feature } from '@gentics/cms-models';
import { EntityImporter } from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH, AUTH_ADMIN, AUTH_KEYCLOAK } from './common';
import { loginWithForm, navigateToApp } from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('Login', () => {
    const IMPORTER = new EntityImporter();

    test.beforeEach(async ({ request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
    });

    test.describe('Without keycloak feature enabled', () => {
        test.beforeAll(async ({ request }) => {
            IMPORTER.setApiContext(request);
            await IMPORTER.setupFeatures({
                [Feature.KEYCLOAK]: false,
            });
        });

        test('should be able to login', async ({ page }) => {
            await navigateToApp(page);
            await loginWithForm(page, AUTH_ADMIN);

            // Verify successful login
            await expect(page.locator('gtx-dashboard')).toBeVisible();
        });
    });

    test.describe.skip('With keycloak feature enabled', () => {
        test.beforeAll(async ({ request }) => {
            IMPORTER.setApiContext(request);
            await IMPORTER.setupFeatures({
                [Feature.KEYCLOAK]: true,
            });
        });

        test('should be able to login (skip-sso)', async ({ page }) => {
            await navigateToApp(page);

            // Get auth data and login
            const auth = AUTH[AUTH_ADMIN];
            await page.fill('input[name="username"]', auth.username);
            await page.fill('input[name="password"]', auth.password);
            await page.click('button[type="submit"]');

            // Verify successful login
            await expect(page.locator('gtx-dashboard')).toBeVisible();
        });

        test('should be able to login (default without skip-sso)', async ({ page }) => {
            await navigateToApp(page, '/', true);

            // Get auth data and login via Keycloak
            const auth = AUTH[AUTH_KEYCLOAK];
            await page.fill('input[name="username"]', auth.username);
            await page.fill('input[name="password"]', auth.password);
            await page.click('button[type="submit"]');

            // Verify successful login
            await expect(page.locator('gtx-dashboard')).toBeVisible();
        });
    });
});

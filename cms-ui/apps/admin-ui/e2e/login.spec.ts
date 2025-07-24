import { Feature, Variant } from '@gentics/cms-models';
import { blockKeycloakConfig, EntityImporter, isVariant, matchesPath, waitForKeycloakAuthPage } from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH_ADMIN, AUTH_KEYCLOAK } from './common';
import { loginWithForm, navigateToApp } from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('Login', () => {
    const IMPORTER = new EntityImporter();

    test.beforeEach(async ({ request, context }, testInfo) => {
        testInfo.setTimeout(120_000);
        await context.clearCookies();
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
    });

    test.describe('Without keycloak feature enabled', () => {
        test.beforeEach(async ({ request }, testInfo) => {
            testInfo.setTimeout(120_000);
            IMPORTER.setApiContext(request);
            await IMPORTER.setupFeatures({
                [Feature.KEYCLOAK]: false,
            });
        });

        test('should be able to login', async ({ page }) => {
            await blockKeycloakConfig(page);
            await navigateToApp(page, '', true);
            await loginWithForm(page, AUTH_ADMIN);

            // Verify successful login
            await page.locator('gtx-dashboard').waitFor({ state: 'visible' });
        });
    });

    test.describe('With keycloak feature enabled', () => {
        test.skip(() => !isVariant(Variant.ENTERPRISE), 'Requires Enterpise features');

        test.beforeEach(async ({ request }, testInfo) => {
            testInfo.setTimeout(120_000);
            IMPORTER.setApiContext(request);
            await IMPORTER.setupFeatures({
                [Feature.KEYCLOAK]: true,
            });
        });

        test('should be able to login (skip-sso)', async ({ page }) => {
            await navigateToApp(page);
            await loginWithForm(page, AUTH_ADMIN);

            // Verify successful login
            await page.locator('gtx-dashboard').waitFor({ state: 'visible' });
        });

        test('should be able to login (default without skip-sso)', async ({ page }) => {
            await navigateToApp(page, '', true);
            await waitForKeycloakAuthPage(page);

            const ssoReq = page.waitForRequest(req => matchesPath(req.url(), '/rest/auth/ssologin'));

            await loginWithForm(page, AUTH_KEYCLOAK);

            // Verify SSO response
            const sso = await ssoReq;
            const ssoRes = await sso.response();
            expect(ssoRes.ok()).toBe(true);
            expect(await ssoRes.text()).not.toBe('NOTFOUND');

            // Verify successful login
            await page.locator('gtx-dashboard').waitFor({ state: 'visible' });
        });
    });
});

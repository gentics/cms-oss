import { test, expect } from '@playwright/test';
import { Feature, Variant } from '@gentics/cms-models';
import {
    EntityImporter,
    TestSize,
    isVariant,
} from '@gentics/e2e-utils';
import { AUTH_ADMIN, AUTH_KEYCLOAK } from './common';
import { AUTH, login, initPage } from './helpers';

test.describe('Login', () => {
    const IMPORTER = new EntityImporter();

    test.beforeEach(async ({ request, context }, testInfo) => {
        testInfo.setTimeout(120_000);
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.setupTest(TestSize.MINIMAL);
        await context.clearCookies();
    });

    test.describe('Without Keycloak feature enabled', () => {
        test.beforeAll(async ({ request }) => {
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
            await IMPORTER.setupFeatures({ [Feature.KEYCLOAK]: false });
            await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
        });

        test('should be able to login', async ({ page }) => {
            await initPage(page);
            await page.goto('/');
            await login(page, AUTH_ADMIN);
            await expect(page.locator('project-editor')).toBeVisible();
        });

        test('should skip login if already logged in', async ({ page }) => {
            // Perform login via API
            const authResponse = await page.request.post('/rest/auth/login', {
                headers: { 'Content-Type': 'application/json' },
                data: {
                    login: AUTH[AUTH_ADMIN].username,
                    password: AUTH[AUTH_ADMIN].password,
                },
            });

            const authData = await authResponse.json();
            const sid = authData.sid;

            // Set session ID in local storage
            await page.goto('/');
            await page.evaluate((sid) => {
                localStorage.setItem('GCMSUI_sid', sid);
            }, sid);

            // Reload the page to apply the session
            await page.reload();

            // Verify that the user is logged in
            await expect(page.locator('project-editor')).toBeVisible();
        });
    });

    test.describe('With Keycloak feature enabled', () => {
        test.skip(() => !isVariant(Variant.ENTERPRISE), 'With keycloak feature enabled');
        test.beforeAll(async ({ request }) => {
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
            await IMPORTER.setupFeatures({ [Feature.KEYCLOAK]: true });
            await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
        });

        test('should be able to login (skip-sso)', async ({ page }) => {
            await initPage(page);
            await page.goto('/');
            await login(page, AUTH_ADMIN);
            await expect(page.locator('project-editor')).toBeVisible();
        });

        test.skip('should be able to login (default without skip-sso)', async ({ page }) => {
            await initPage(page);
            await page.goto('/', { waitUntil: 'networkidle' });
            await login(page, AUTH_KEYCLOAK, true);
            await expect(page.locator('project-editor')).toBeVisible();
        });
    });
});

import { Feature, Variant } from '@gentics/cms-models';
import {
    createClient,
    EntityImporter,
    isVariant,
    loginWithForm,
    navigateToApp,
    TestSize,
} from '@gentics/e2e-utils';
import { test } from '@playwright/test';
import { AUTH } from './common';

test.describe.configure({ mode: 'serial' });
test.describe('Login', () => {
    const IMPORTER = new EntityImporter();

    test.beforeEach(async ({ request, context }) => {
        await context.clearCookies();
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
            await IMPORTER.setupClient();

            await IMPORTER.setupFeatures({ [Feature.KEYCLOAK]: false });
            await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
        });

        test('should be able to login', async ({ page }) => {
            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);

            await page.locator('project-editor').waitFor();
        });

        test('should skip login if already logged in', async ({ page }) => {
            await navigateToApp(page);

            // Verify we aren't logged in
            await page.locator('login').waitFor();

            // // Setup client for login
            const client = await createClient({
                context: page.request,
                connection: {
                    absolute: false,
                    basePath: '/rest',
                },
            });

            // Perform login via API
            const authData = await client.auth.login({
                login: AUTH.admin.username,
                password: AUTH.admin.password,
            }).send();

            const sid = authData.sid;

            // Set session ID in local storage
            await page.evaluate((sid) => {
                localStorage.setItem('GCMSUI_sid', `${sid}`);
            }, sid);

            // Reload the page to apply the session
            await page.reload();

            // Verify that the user is logged in
            await page.locator('project-editor').waitFor();
        });
    });

    test.describe('With Keycloak feature enabled', () => {
        test.skip(() => !isVariant(Variant.ENTERPRISE), 'Keycloak is an enterprise feature');

        test.beforeAll(async ({ request }) => {
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
            await IMPORTER.setupClient();

            await IMPORTER.setupFeatures({ [Feature.KEYCLOAK]: true });
            await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
        });

        test('should be able to login with SSO', async ({ page }) => {
            await navigateToApp(page);
            await loginWithForm(page, AUTH.keycloak);
            await page.locator('project-editor').waitFor();
        });

        test('should be able to skip SSO and login directly', async ({ page }) => {
            await navigateToApp(page, '', true);
            await loginWithForm(page, AUTH.admin);
            await page.locator('project-editor').waitFor();
        });
    });
});

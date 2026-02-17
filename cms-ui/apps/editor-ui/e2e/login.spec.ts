import { Feature, KeycloakConfiguration, Response, ResponseCode, Variant } from '@gentics/cms-models';
import {
    createClient,
    EntityImporter,
    isVariant,
    KEYCLOAK_LOGIN,
    loginWithForm,
    matchesUrl,
    navigateToApp,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';

test.describe('Login', () => {
    const IMPORTER = new EntityImporter();

    test.beforeEach(async ({ request, context }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await context.clearCookies();
            await IMPORTER.clearClient();
        });

        await test.step('Common Test Setup', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });
    });

    test.describe('Without Keycloak feature enabled', () => {
        test.beforeAll(async ({ request }) => {
            await test.step('Client Setup', async () => {
                IMPORTER.setApiContext(request);
                await IMPORTER.clearClient();
            });

            await test.step('Test Bootstrapping', async () => {
                await IMPORTER.cleanupTest();
                await IMPORTER.setupFeatures({ [Feature.KEYCLOAK]: false });
                await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
            });
        });

        test('should be able to login', async ({ page }) => {
            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);

            await expect(page.locator('project-editor')).toBeVisible();
        });

        test('should skip login if already logged in', async ({ page }) => {
            await navigateToApp(page);

            // Verify we aren't logged in
            await expect(page.locator('gtx-login')).toBeVisible();

            // Login
            await loginWithForm(page, AUTH.admin);
            await expect(page.locator('project-editor')).toBeVisible();

            // Verify that the user is still logged in
            await page.reload();
            await expect(page.locator('project-editor')).toBeVisible();
        });
    });

    test.describe('With Keycloak feature enabled', () => {
        test.skip(() => !isVariant(Variant.ENTERPRISE), 'Keycloak is an enterprise feature');

        test.beforeAll(async ({ request }) => {
            await test.step('Client Setup', async () => {
                IMPORTER.setApiContext(request);
                await IMPORTER.clearClient();
            });

            await test.step('Test Bootstrapping', async () => {
                await IMPORTER.cleanupTest();
                await IMPORTER.setupFeatures({ [Feature.KEYCLOAK]: true });
                await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
            });
        });

        test('should be able to login with SSO', async ({ page }) => {
            await navigateToApp(page);
            await loginWithForm(page, KEYCLOAK_LOGIN);
            await expect(page.locator('project-editor')).toBeVisible();
        });

        test('should be able to skip SSO and login directly', async ({ page }) => {
            await navigateToApp(page, '', true);
            await loginWithForm(page, AUTH.admin);
            await expect(page.locator('project-editor')).toBeVisible();
        });

        test('should handle keycloak config load error correctly', async ({ page }) => {
            await page.route(url => matchesUrl(url, '/rest/keycloak'), (route, req) => {
                if (req.method() !== 'GET') {
                    return route.continue();
                }
                return route.fulfill({
                    json: {
                        responseInfo: {
                            responseCode: ResponseCode.FAILURE,
                            responseMessage: 'Example error',
                        },
                    } as Response,
                    status: 500,
                });
            });

            await navigateToApp(page, '', true);

            const err = page.locator('gtx-login .keycloak-error');
            await expect(err).toBeVisible();
            await expect(err).toHaveAttribute('data-value', 'shared.keycloak_unknown_error');
        });

        test('should handle invalid keycloak config correctly', async ({ page }) => {
            await page.route(url => matchesUrl(url, '/rest/keycloak'), (route, req) => {
                if (req.method() !== 'GET') {
                    return route.continue();
                }
                return route.fulfill({
                    json: {
                        "auth-server-url": 'invalid-url $$$',
                        realm: 'example',
                        resource: 'test',
                        showSSOButton: false,
                    } as KeycloakConfiguration,
                    status: 200,
                });
            });

            await navigateToApp(page, '', true);

            const err = page.locator('gtx-login .keycloak-error');
            await expect(err).toBeVisible();
            await expect(err).toHaveAttribute('data-value', 'shared.keycloak_invalid_config');
        });

        test('should handle invalid keycloak resource correctly', async ({ page }) => {
            await page.route(url => matchesUrl(url, '/rest/keycloak'), async (route, req) => {
                if (req.method() !== 'GET') {
                    return route.continue();
                }

                const original = await route.fetch();
                const data = await original.json() as KeycloakConfiguration;

                return route.fulfill({
                    json: {
                        ...data,
                        resource: 'dummy',
                    },
                    status: 200,
                });
            });

            await navigateToApp(page, '', true);

            const err = page.locator('gtx-login .keycloak-error');
            await expect(err).toBeVisible();
            await expect(err).toHaveAttribute('data-value', 'shared.keycloak_unknown_error');
        });

        test('should handle unreachable keycloak correctly', async ({ page }) => {
            await page.route(url => matchesUrl(url, '/rest/keycloak'), (route, req) => {
                if (req.method() !== 'GET') {
                    return route.continue();
                }
                return route.fulfill({
                    json: {
                        "auth-server-url": 'http://nowhere.example.com',
                        realm: 'example',
                        resource: 'test',
                        showSSOButton: false,
                    } as KeycloakConfiguration,
                    status: 200,
                });
            });

            await navigateToApp(page, '', true);

            const err = page.locator('gtx-login .keycloak-error');
            await expect(err).toBeVisible();
            await expect(err).toHaveAttribute('data-value', 'shared.keycloak_not_available');
        });
    });
});

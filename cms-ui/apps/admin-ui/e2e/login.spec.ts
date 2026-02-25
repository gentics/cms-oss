import { AccessControlledType, Feature, GcmsPermission, KeycloakConfiguration, Response, ResponseCode, Variant } from '@gentics/cms-models';
import {
    EntityImporter,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    isVariant,
    KEYCLOAK_LOGIN,
    loginWithForm,
    matchesUrl,
    navigateToApp,
    NODE_MINIMAL,
    TestSize,
    UserImportData,
} from '@gentics/e2e-utils';
import { cloneWithSymbols } from '@gentics/ui-core/utils/clone-with-symbols';
import { expect, test } from '@playwright/test';

test.describe('Login', () => {
    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'auth';

    const TEST_GROUP_ROOT: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_root`,

        description: 'Auth: Root',
        name: `group_${NAMESPACE}`,
        permissions: [],
    };

    /**
     * IMPORTANT: This groups name must be the same as in the `keycloak.yml` config from the CMS,
     * Otherwise the sync/login doesn't work properly.
     */
    const TEST_GROUP_SSO: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_sso`,

        description: 'Auth: SSO',
        name: `group_${NAMESPACE}_sso`,
        parent: TEST_GROUP_ROOT,
        permissions: [],
    };

    const TEST_GROUP_EDITOR: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_editor`,

        description: 'Auth: Editor',
        name: `group_${NAMESPACE}_editor`,
        parent: TEST_GROUP_ROOT,
        permissions: [],
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_editor`,

        group: TEST_GROUP_EDITOR,

        email: 'something@example.com',
        firstName: 'Auth',
        lastName: 'Editor',
        login: `${NAMESPACE}_editor`,
        password: 'testauth312',
    };

    test.beforeAll(async ({ request }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
        });

        await test.step('Test Bootstrapping', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
        });
    });

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

    async function setupWithPermissions(withKeycloak: boolean): Promise<void> {
        await test.step('Specialized Test Setup', async () => {
            const ROOT_GROUP = cloneWithSymbols(TEST_GROUP_ROOT);
            ROOT_GROUP.permissions = [
                {
                    type: AccessControlledType.ADMIN,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                    ],
                },
            ];
            await IMPORTER.importData([
                ROOT_GROUP,
                TEST_GROUP_SSO,
                TEST_GROUP_EDITOR,
                TEST_USER,
            ]);
            await IMPORTER.setupFeatures({ [Feature.KEYCLOAK]: withKeycloak });
        });
    }

    test.describe('Without Keycloak feature enabled', () => {
        test.beforeEach(async () => {
            await setupWithPermissions(false);
        });

        test('should be able to login', async ({ page }) => {
            await navigateToApp(page);
            await loginWithForm(page, TEST_USER);

            await expect(page.locator('gtx-dashboard')).toBeVisible();
        });

        test('should skip login if already logged in', async ({ page }) => {
            await navigateToApp(page);

            // Verify we aren't logged in
            await expect(page.locator('gtx-login')).toBeVisible();

            // Login
            await loginWithForm(page, TEST_USER);
            await expect(page.locator('gtx-dashboard')).toBeVisible();

            // Verify that the user is still logged in
            await page.reload();
            await expect(page.locator('gtx-dashboard')).toBeVisible();
        });
    });

    test.describe('With Keycloak feature enabled', () => {
        test.skip(() => !isVariant(Variant.ENTERPRISE), 'Keycloak is an enterprise feature');

        test.beforeEach(async () => {
            await setupWithPermissions(true);
        });

        test('should be able to login with SSO', async ({ page }) => {
            await navigateToApp(page, '/', true);
            await loginWithForm(page, KEYCLOAK_LOGIN);
            await expect(page.locator('gtx-dashboard')).toBeVisible();
        });

        test('should be able to skip SSO and login directly', async ({ page }) => {
            await navigateToApp(page, '');
            await loginWithForm(page, TEST_USER);
            await expect(page.locator('gtx-dashboard')).toBeVisible();
        });

        test('login with button', async ({ page }) => {
            // Edit the config response to use the sso-button
            await page.route((url) => matchesUrl(url, '/rest/keycloak'), async (route, req) => {
                if (req.method() !== 'GET') {
                    return route.continue();
                }

                const original = await route.fetch();
                const data = await original.json() as KeycloakConfiguration;
                data.showSSOButton = true;

                return route.fulfill({
                    json: data,
                    status: 200,
                });
            });

            await navigateToApp(page, '', true);

            const ssoButton = page.locator('gtx-single-sign-on [data-action="sso-login"]');
            await expect(ssoButton).toBeVisible();
            await ssoButton.click();

            await loginWithForm(page, KEYCLOAK_LOGIN);
            await expect(page.locator('gtx-dashboard')).toBeVisible();
        });

        test('should handle keycloak config load error correctly', async ({ page }) => {
            await page.route((url) => matchesUrl(url, '/rest/keycloak'), (route, req) => {
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
            await page.route((url) => matchesUrl(url, '/rest/keycloak'), (route, req) => {
                if (req.method() !== 'GET') {
                    return route.continue();
                }
                return route.fulfill({
                    json: {
                        'auth-server-url': 'invalid-url $$$',
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

        test('should handle invalid keycloak realm correctly', async ({ page }) => {
            await page.route((url) => matchesUrl(url, '/rest/keycloak'), async (route, req) => {
                if (req.method() !== 'GET') {
                    return route.continue();
                }

                const original = await route.fetch();
                const data = await original.json() as KeycloakConfiguration;

                return route.fulfill({
                    json: {
                        ...data,
                        realm: 'dummy',
                    },
                    status: 200,
                });
            });

            await navigateToApp(page, '', true);

            const err = page.locator('gtx-login .keycloak-error');
            await expect(err).toBeVisible();
            await expect(err).toHaveAttribute('data-value', 'shared.keycloak_not_available');
        });

        test('should handle unreachable keycloak correctly', async ({ page }) => {
            await page.route((url) => matchesUrl(url, '/rest/keycloak'), (route, req) => {
                if (req.method() !== 'GET') {
                    return route.continue();
                }
                return route.fulfill({
                    json: {
                        'auth-server-url': 'http://nowhere.example.com',
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

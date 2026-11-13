import { AccessControlledType, Feature, GcmsPermission } from '@gentics/cms-models';
import { cloneWithSymbols } from '@gentics/common';
import {
    EntityImporter,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    loginWithForm,
    navigateToApp,
    NODE_MINIMAL,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';

test.describe('Set password with a link', {
    annotation: [{
        type: 'ticket',
        description: 'GPU-2776',
    }],
}, () => {
    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'setpw';
    const LOGIN = `${NAMESPACE}_user`;
    const PASSWORD = 'new.pw-123';

    const TEST_GROUP: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}`,

        description: 'Set Password: Editor',
        name: `group_${NAMESPACE}`,
        permissions: [],
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

        await test.step('Specialized Test Setup', async () => {
            const group = cloneWithSymbols(TEST_GROUP);
            group.permissions = [
                {
                    type: AccessControlledType.NODE,
                    instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                    subObjects: true,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                        { type: GcmsPermission.READ_ITEMS, value: true },
                    ],
                },
            ];
            await IMPORTER.importData([group]);
            await IMPORTER.setupFeatures({ [Feature.KEYCLOAK]: false });
        });
    });

    /**
     * Create the test user with a password link through the REST API, as the admin UI does
     * @returns the token for the link
     */
    async function createUserWithPasswordLink(): Promise<string> {
        const res = await IMPORTER.client.group.createUser(IMPORTER.get(TEST_GROUP).id, {
            login: LOGIN,
            firstName: 'Set',
            lastName: 'Password',
        }, { passwordLink: true }).send();
        expect(res.passwordToken).toBeTruthy();
        return res.passwordToken;
    }

    test('should set the password and log in with it', async ({ page }) => {
        const token = await createUserWithPasswordLink();
        await navigateToApp(page, `set-password?token=${encodeURIComponent(token)}`);

        const setPassword = page.locator('gtx-set-password');
        await expect(setPassword.locator('form')).toBeVisible();
        await setPassword.locator('gtx-input[formcontrolname="password1"] input').fill(PASSWORD);
        await setPassword.locator('gtx-input[formcontrolname="password2"] input').fill(PASSWORD);
        await setPassword.locator('[data-action="set-password"] button').click();

        await expect(page.locator('gtx-login')).toBeVisible();
        await loginWithForm(page, { username: LOGIN, password: PASSWORD });
        await expect(page.locator('project-editor')).toBeVisible();
    });

    test('should reject a link which was already used', async ({ page }) => {
        const token = await createUserWithPasswordLink();
        await IMPORTER.client.auth.setPassword({ token, password: PASSWORD }).send();

        await navigateToApp(page, `set-password?token=${encodeURIComponent(token)}`);

        const setPassword = page.locator('gtx-set-password');
        await expect(setPassword.locator('[data-context="invalid-token"]')).toBeVisible();
        await expect(setPassword.locator('form')).toHaveCount(0);
    });

    test('should reject an invalid link', async ({ page }) => {
        await navigateToApp(page, 'set-password?token=invalid');

        const setPassword = page.locator('gtx-set-password');
        await expect(setPassword.locator('[data-context="invalid-token"]')).toBeVisible();
        await expect(setPassword.locator('form')).toHaveCount(0);
    });

    test('should not submit a password violating the rule', async ({ page }) => {
        const token = await createUserWithPasswordLink();
        await navigateToApp(page, `set-password?token=${encodeURIComponent(token)}`);

        const setPassword = page.locator('gtx-set-password');
        await setPassword.locator('gtx-input[formcontrolname="password1"] input').fill('two words');
        await setPassword.locator('gtx-input[formcontrolname="password2"] input').fill('two words');

        await expect(setPassword.locator('[data-action="set-password"] button')).toBeDisabled();
    });
});

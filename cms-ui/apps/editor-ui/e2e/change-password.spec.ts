import { AccessControlledType, GcmsPermission, Response as CmsResponse, ResponseCode } from '@gentics/cms-models';
import {
    EntityImporter,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    createClient,
    loginWithForm,
    navigateToApp,
    NODE_MINIMAL,
    openSidebar,
    TestSize,
    UserImportData,
    waitForResponseFrom,
} from '@gentics/e2e-utils';
import { cloneWithSymbols } from '@gentics/ui-core/utils/clone-with-symbols';
import { GCMSRestClientRequestError } from '@gentics/cms-rest-client';
import { expect, Locator, Page, PlaywrightWorkerArgs, test } from '@playwright/test';

test.describe('Change password', {
    annotation: [{
        type: 'ticket',
        description: 'SUP-20364',
    }],
}, () => {
    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'chpwd';

    const OLD_PASSWORD = 'chpwdold123';
    const NEW_PASSWORD = 'chpwdnew456';

    const TEST_GROUP: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_editor`,

        description: 'Change Password: Editor',
        name: `group_${NAMESPACE}_editor`,
        permissions: [],
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_editor`,

        group: TEST_GROUP,

        email: 'chpwd@example.com',
        firstName: 'Change Password',
        lastName: 'Editor',
        login: `${NAMESPACE}_editor`,
        password: OLD_PASSWORD,
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
            // Only the permissions to see the node are granted, no admin permissions at all
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
            await IMPORTER.importData([group, TEST_USER]);
        });
    });

    test.afterEach(async ({ request }) => {
        // Always set the old password back (with the admin client), whatever the test result was:
        // The importer re-uses an existing user without resetting its password, so a changed password
        // would break every following test (and test run) which logs in with this user.
        await test.step('Restore old password', async () => {
            IMPORTER.setApiContext(request);
            await IMPORTER.setupClient();
            const user = IMPORTER.get(TEST_USER);
            if (user?.id != null) {
                await IMPORTER.client.user.update(user.id, { password: OLD_PASSWORD }).send();
            }
        });
    });

    async function loginAs(page: Page, password: string): Promise<void> {
        await navigateToApp(page);
        await expect(page.locator('gtx-login')).toBeVisible();
        await loginWithForm(page, { username: TEST_USER.login, password });
        await expect(page.locator('project-editor')).toBeVisible({ timeout: 15_000 });
    }

    async function openChangePasswordModal(page: Page): Promise<Locator> {
        const userMenu = await openSidebar(page);
        await userMenu.locator('.user-details .user-name gtx-dropdown-trigger gtx-button button').click();
        await page.locator('gtx-dropdown-item[overrideslot="changePasswordOption"]').click();

        const modal = page.locator('change-password-modal');
        await expect(modal).toBeVisible();
        return modal;
    }

    async function fillForm(modal: Locator, oldPassword: string, newPassword: string): Promise<void> {
        await modal.locator('gtx-input[formcontrolname="oldPassword"] input').fill(oldPassword);
        await modal.locator('gtx-input[formcontrolname="password1"] input').fill(newPassword);
        await modal.locator('gtx-input[formcontrolname="password2"] input').fill(newPassword);
    }

    /**
     * Check whether the test user can log in with the given password, using a separate request context,
     * so that neither the browser session nor the importer's admin session is affected.
     */
    async function canLogin(playwright: PlaywrightWorkerArgs['playwright'], password: string): Promise<boolean> {
        const context = await playwright.request.newContext();
        try {
            const client = await createClient({ context });
            await client.auth.login({ login: TEST_USER.login, password }).send();
            return true;
        } catch (err) {
            if (err instanceof GCMSRestClientRequestError) {
                return false;
            }
            throw err;
        } finally {
            await context.dispose();
        }
    }

    function submitButton(modal: Locator): Locator {
        return modal.locator('.modal-footer gtx-button[type="primary"] button');
    }

    test('should change the own password after entering the current password', async ({ page, playwright }) => {
        await loginAs(page, OLD_PASSWORD);
        const modal = await openChangePasswordModal(page);

        await fillForm(modal, OLD_PASSWORD, NEW_PASSWORD);

        const changeReq = waitForResponseFrom(page, 'POST', '/rest/user/password');
        await submitButton(modal).click();
        const res = await changeReq;
        expect(res.request().postDataJSON()).toEqual({
            oldPassword: OLD_PASSWORD,
            newPassword: NEW_PASSWORD,
        });
        expect(((await res.json()) as CmsResponse).responseInfo.responseCode).toEqual(ResponseCode.OK);

        await expect(modal).toBeHidden();

        await test.step('Verify the password was changed', async () => {
            expect(await canLogin(playwright, NEW_PASSWORD)).toBe(true);
            expect(await canLogin(playwright, OLD_PASSWORD)).toBe(false);
        });
    });

    test('should not change the password with a wrong current password', async ({ page, playwright }) => {
        await loginAs(page, OLD_PASSWORD);
        const modal = await openChangePasswordModal(page);

        await fillForm(modal, 'wrongpassword', NEW_PASSWORD);

        const changeReq = waitForResponseFrom(page, 'POST', '/rest/user/password', { skipStatus: true });
        await submitButton(modal).click();
        const res = await changeReq;
        // Literal value sent by the server; `ResponseCode.INVALID_DATA` in cms-models is misspelled ('INVLIDDATA')
        expect(((await res.json()) as CmsResponse).responseInfo.responseCode as string).toEqual('INVALIDDATA');

        // The dialog stays open, so the input can be corrected
        await expect(modal).toBeVisible();
        await modal.locator('.modal-footer gtx-button[type="secondary"] button').click();
        await expect(modal).toBeHidden();

        await test.step('Verify the password was not changed', async () => {
            expect(await canLogin(playwright, OLD_PASSWORD)).toBe(true);
            expect(await canLogin(playwright, NEW_PASSWORD)).toBe(false);
        });
    });

    test('should require the current password', async ({ page }) => {
        await loginAs(page, OLD_PASSWORD);
        const modal = await openChangePasswordModal(page);

        await modal.locator('gtx-input[formcontrolname="password1"] input').fill(NEW_PASSWORD);
        await modal.locator('gtx-input[formcontrolname="password2"] input').fill(NEW_PASSWORD);
        await expect(submitButton(modal)).toBeDisabled();

        await modal.locator('gtx-input[formcontrolname="oldPassword"] input').fill(OLD_PASSWORD);
        await expect(submitButton(modal)).toBeEnabled();
    });
});

import {
    clickModalAction,
    EntityImporter,
    findTableRowById,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    ImportPermissions,
    loginWithForm,
    navigateToApp,
    selectTableRow,
    TestSize,
    UserImportData,
    waitForResponseFrom,
} from '@gentics/e2e-utils';
import { cloneWithSymbols } from '@gentics/ui-core/utils/clone-with-symbols';
import test, { expect, Locator, Page } from '@playwright/test';
import { navigateToModule } from './helpers';
import { AccessControlledType, GcmsPermission, UserResponse } from '@gentics/cms-models';

test.describe('Users Module', () => {

    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'users';

    const TEST_GROUP_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_admin`,

        description: 'Users: Admin',
        name: `group_${NAMESPACE}_admin`,
        permissions: [],
    };

    const TEST_GROUP_DUMMY: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_dummy`,
        parent: TEST_GROUP_BASE,

        description: 'Users: Dummy',
        name: `group_${NAMESPACE}_dummy`,
        permissions: [],
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_admin`,

        group: TEST_GROUP_BASE,

        email: 'something@example.com',
        firstName: 'Users',
        lastName: 'Admin',
        login: `${NAMESPACE}_admin`,
        password: 'thisisapassword123',
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
            await IMPORTER.syncPackages(TestSize.MINIMAL);
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });
    });

    async function setupWithPermissions(page: Page, permissions: ImportPermissions[]): Promise<Locator> {
        await test.step('Test User Setup', async () => {
            const TEST_GROUP = cloneWithSymbols(TEST_GROUP_BASE);
            TEST_GROUP.permissions = permissions;

            await IMPORTER.importData([
                TEST_GROUP,
                TEST_GROUP_DUMMY,
                TEST_USER,
            ]);
        });

        let module: Locator;

        await test.step('Open Editor-UI', async () => {
            await navigateToApp(page);
            await loginWithForm(page, TEST_USER);
            module = await navigateToModule(page, 'users');
        });

        return module;
    }

    test('create button is disabled without permissions', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20173',
        }],
    }, async ({ page }) => {
        const module = await setupWithPermissions(page, [
            {
                type: AccessControlledType.ADMIN,
                perms: [
                    GcmsPermission.READ,
                ],
            },
            {
                type: AccessControlledType.USER_ADMIN,
                perms: [
                    GcmsPermission.READ,
                ],
            },
        ]);

        const table = module.locator('gtx-user-table');
        const createButton = table.locator('.entity-table-actions-bar [data-action="create"]');
        await expect(createButton).toBeVisible();
        await expect(createButton.locator('button')).toBeDisabled();
    });

    test('can create a new user with permissions', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20173',
        }],
    }, async ({ page }) => {
        const module = await setupWithPermissions(page, [
            {
                type: AccessControlledType.ADMIN,
                perms: [
                    GcmsPermission.READ,
                ],
            },
            {
                type: AccessControlledType.USER_ADMIN,
                perms: [
                    GcmsPermission.READ,
                    GcmsPermission.CREATE_USER,
                ],
            },
            {
                type: AccessControlledType.GROUP_ADMIN,
                perms: [
                    GcmsPermission.READ,
                    GcmsPermission.USER_ASSIGNMENT,
                ],
            },
        ]);

        const table = module.locator('gtx-user-table');
        let createdUserId: number;

        const createButton = table.locator('.entity-table-actions-bar [data-action="create"]');
        await expect(createButton).toBeVisible();
        await expect(createButton.locator('button')).toBeEnabled();
        await createButton.click();

        await test.step('Create the user', async () => {
            const modal = page.locator('gtx-create-user-modal');
            await expect(modal).toBeVisible();

            const userForm = modal.locator('gtx-tab .tab-content[data-id="1"] form');
            await userForm.locator('[formControlName="login"] input').fill('testuser123');
            await userForm.locator('[formControlName="firstName"] input').fill('Hello');
            await userForm.locator('[formControlName="lastName"] input').fill('World');
            await userForm.locator('[formControlName="password1"] input').fill('secretpassword123');
            await userForm.locator('[formControlName="password2"] input').fill('secretpassword123');

            await clickModalAction(modal, 'next');

            const groupsTable = modal.locator('gtx-tab .tab-content[data-id="2"] gtx-group-table');
            const DUMMY_GROUP = IMPORTER.get(TEST_GROUP_DUMMY);
            const dummyGroupRow = await findTableRowById(groupsTable, DUMMY_GROUP.id);
            await selectTableRow(dummyGroupRow);

            const createReq = waitForResponseFrom(page, 'PUT', `/rest/group/${DUMMY_GROUP.id}/users`);
            await clickModalAction(modal, 'confirm');
            const createRes = await createReq;
            createdUserId = ((await createRes.json()) as UserResponse).user.id;
        });

        const createUserRow = await findTableRowById(table, createdUserId);
        await expect(createUserRow).toBeVisible();
    });

    test('can create a new user who sets the password with a link', {
        annotation: [{
            type: 'ticket',
            description: 'GPU-2776',
        }],
    }, async ({ page }) => {
        const module = await setupWithPermissions(page, [
            {
                type: AccessControlledType.ADMIN,
                perms: [
                    GcmsPermission.READ,
                ],
            },
            {
                type: AccessControlledType.USER_ADMIN,
                perms: [
                    GcmsPermission.READ,
                    GcmsPermission.CREATE_USER,
                ],
            },
            {
                type: AccessControlledType.GROUP_ADMIN,
                perms: [
                    GcmsPermission.READ,
                    GcmsPermission.USER_ASSIGNMENT,
                ],
            },
        ]);

        const table = module.locator('gtx-user-table');
        await table.locator('.entity-table-actions-bar [data-action="create"]').click();

        const modal = page.locator('gtx-create-user-modal');
        let createdUserId: number;
        let passwordToken: string;

        await test.step('Create the user with a password link', async () => {
            await expect(modal).toBeVisible();

            const userForm = modal.locator('gtx-tab .tab-content[data-id="1"] form');
            await userForm.locator('[formControlName="login"] input').fill('linkuser123');
            await userForm.locator('[formControlName="firstName"] input').fill('Hello');
            await userForm.locator('[formControlName="lastName"] input').fill('Link');
            await userForm.locator('gtx-checkbox[formcontrolname="passwordLink"] label').click();
            await expect(userForm.locator('[formControlName="password1"] input')).toBeDisabled();
            await expect(userForm.locator('[formControlName="password2"] input')).toBeDisabled();

            await clickModalAction(modal, 'next');

            const groupsTable = modal.locator('gtx-tab .tab-content[data-id="2"] gtx-group-table');
            const DUMMY_GROUP = IMPORTER.get(TEST_GROUP_DUMMY);
            const dummyGroupRow = await findTableRowById(groupsTable, DUMMY_GROUP.id);
            await selectTableRow(dummyGroupRow);

            const createReq = waitForResponseFrom(page, 'PUT', `/rest/group/${DUMMY_GROUP.id}/users`);
            await clickModalAction(modal, 'confirm');
            const createRes = await createReq;

            const req = createRes.request();
            expect(new URL(req.url()).searchParams.get('passwordLink')).toBe('true');
            expect(req.postDataJSON()).not.toHaveProperty('password');

            const body = await createRes.json();
            createdUserId = body.user.id;
            passwordToken = body.passwordToken;
            expect(passwordToken).toBeTruthy();
        });

        await test.step('The link is shown', async () => {
            const link = modal.locator('[data-context="password-link"] gtx-copy-value .content');
            await expect(link).toContainText(`#/set-password?token=${encodeURIComponent(passwordToken)}`);
            await expect(link).toContainText('?skip-sso');

            await clickModalAction(modal, 'close');
            await expect(modal).toBeHidden();
        });

        const createUserRow = await findTableRowById(table, createdUserId);
        await expect(createUserRow).toBeVisible();
    });
});

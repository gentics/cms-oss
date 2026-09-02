import {
    createClient,
    EntityImporter,
    findNotification,
    findTableAction,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    ImportPermissions,
    loginWithForm,
    matchRequest,
    navigateToApp,
    openContext,
    TestSize,
    UserImportData,
} from '@gentics/e2e-utils';
import { expect, Locator, Page, test } from '@playwright/test';
import { cloneWithSymbols } from '@gentics/common';
import { AccessControlledType, GcmsPermission, LoginResponse } from '@gentics/cms-models';
import { dateShouldBeDisabled, setGtxDateFromXpath } from './helpers';

const API_MODAL = 'gtx-api-tokens-modal';
const API_CREATE_MODAL = 'gtx-api-tokens-create-modal';
const BASE_GROUP: GroupImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
    [IMPORT_ID]: 'apiTokenMaintenance_group_base',
    name: 'apiTokenMaintenance_base',
    description: 'Api-Token-Maintenance: Base',
    permissions: [],
};

const TEST_USER: UserImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_USER,
    [IMPORT_ID]: 'apiTokenMaintenance_user_test',

    email: 'test@example.com',
    firstName: 'Content-Maintenance',
    lastName: 'Test',
    group: BASE_GROUP,

    login: 'apiTokenMaintenance_test',
    password: 'foobar2026',
};

test.describe('Api Tokens', () => {

    const IMPORTER = new EntityImporter();
    let login: LoginResponse;

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

    test.beforeEach(async ({ page, request, context }) => {
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

        await navigateToApp(page);

        await setupWithPermissions(IMPORTER, login, page, [
            {
                type: AccessControlledType.ADMIN,
                perms: [
                    { type: GcmsPermission.SET_PERMISSION, value: true },
                ],
            },
        ]);
    });

    test('should show api modal', async ({ page }) => {
        await openApiTokenModal(page);

        const table = page.locator(API_MODAL).locator('gtx-manage-api-tokens-table');

        await expect(table).toBeVisible();
    });

    test('should show api creation modal', async ({ page }) => {
        await openApiTokenModal(page);

        await openApiTokenCreateModal(page);

        await expect(page.locator(API_CREATE_MODAL)).toBeVisible();
    });

    test('can save when form is valid', async ({ page }) => {
        await openApiTokenModal(page);

        await openApiTokenCreateModal(page);

        const form = page.locator(API_CREATE_MODAL).locator('form');

        await expect(form).toBeVisible();

        const submitBtn = page.locator(API_CREATE_MODAL).locator('.modal-footer').locator('[data-action="confirm"]');

        await expect(submitBtn.locator('button')).toBeDisabled();

        // fill only name, keep date empty
        const nameInput = form.locator('input[type="text"]');
        await nameInput.fill('Test');

        await expect(submitBtn.locator('button')).toBeEnabled();

        const dateInput = form.locator('gtx-date-time-picker');

        // date from yesterday should be disabled
        await dateShouldBeDisabled(page, form, 'xpath=preceding-sibling::button[1]');

        // fill with date from tomorrow
        await setGtxDateFromXpath(page, dateInput, 'xpath=following-sibling::button[1]');

        await expect(submitBtn.locator('button')).toBeEnabled();

        await submitBtn.click();

        await expect(page.locator('gtx-copy-token-modal')).toBeVisible();
        await expect(findNotification(page, 'api-token-create-success')).toBeVisible();
        await expect(page.locator('gtx-copy-token-modal').locator('.content')).toHaveText(/\S+/);
    });

    test('can delete Api Tokens', async ({ page }) => {
        await addEntries(login, page, 1);
        await openApiTokenModal(page);

        const table = page.locator(API_MODAL).locator('gtx-manage-api-tokens-table');

        await expect(table).toBeVisible();

        const row = table.locator('.data-row').first();

        const rowId = await row.getAttribute('data-id');

        const deleteButton = findTableAction(row, 'delete');

        await deleteButton.click();

        await expect(page.locator('[data-action="api-token-delete"]')).toBeVisible();

        await page.locator('[data-action="api-token-delete"]').click();

        await expect(findNotification(page, 'api-token-delete-success')).toBeVisible();

        await expect(table.locator(`.data-row[data-id="${rowId}"]`)).not.toBeAttached();
    });

    test('can delete multiple Api Tokens', async ({ page }) => {
        await addEntries(login, page, 2);
        await openApiTokenModal(page);

        const table = page.locator(API_MODAL).locator('gtx-manage-api-tokens-table');

        await expect(table).toBeVisible();

        const row = table.locator('.header-row');

        await row.locator('gtx-checkbox').click();

        await findTableAction(row, 'delete').click();

        await expect(page.locator('[data-action="api-token-delete"]')).toBeVisible();

        await page.locator('[data-action="api-token-delete"]').click();

        await expect(findNotification(page, 'api-token-delete-success')).toBeVisible();

        await expect(table.locator('.data-row')).toHaveCount(0);
    });

});

async function openApiTokenModal(page: Page) {
    await page.locator('gtx-user-menu .toggle-button').click();
    const menu = page.locator('gtx-user-menu');
    const dropdown = await openContext(menu.locator('.user-name gtx-dropdown-list'));
    await dropdown.locator('[data-action="manage-api-tokens"]').click();
}

async function openApiTokenCreateModal(page: Page) {
    const modalTable = page.locator(API_MODAL).locator('gtx-manage-api-tokens-table');
    const createNewBtn = modalTable.locator('[data-action="create"]');
    await createNewBtn.click();
}

async function addEntries(login: LoginResponse, page: Page, amount: number) {
    const client = await createClient({
        context: page.context().request,
        isPageContext: true,
    });

    client.sid = login?.sid ?? null;

    for (let i = 0; i < amount; i++) {
        await client.admin.addApiTokens({ name: `token ${i + 1}` }).send();
    }
}

async function setupWithPermissions(importer: EntityImporter, login: LoginResponse, page: Page, permissions: ImportPermissions[]): Promise<void> {
    await test.step('Test User Setup', async () => {
        const TEST_GROUP = cloneWithSymbols(BASE_GROUP);
        TEST_GROUP.permissions = permissions;

        await importer.importData([
            TEST_GROUP,
            TEST_USER,
        ]);
    });

    await test.step('Open ADMIN-UI', async () => {
        await navigateToApp(page);
        const loginReq = page.waitForResponse(matchRequest('POST', '/rest/auth/login'));
        await loginWithForm(page, TEST_USER);
        login = await (await loginReq).json();
    });
}

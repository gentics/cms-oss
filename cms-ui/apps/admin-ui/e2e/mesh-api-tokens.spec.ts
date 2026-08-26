/* eslint-disable @typescript-eslint/naming-convention */
import { AccessControlledType, ContentRepository, GcmsPermission, LoginResponse, Variant } from '@gentics/cms-models';
import { GCMSRestClientRequestError, RequestMethod } from '@gentics/cms-rest-client';
import {
    clickModalAction,
    clickTableRow,
    CONTENT_REPOSITORY_MESH,
    EntityImporter,
    expandTrableRow,
    findTableAction,
    findTableRowById,
    findTableRowByText,
    findTrableRowById,
    findTrableRowByText,
    FOLDER_A,
    FOLDER_B,
    isVariant,
    loginWithForm,
    matchRequest,
    MESH_SCHEMA_PREFIX,
    navigateToApp,
    NODE_MINIMAL,
    SCHEDULE_PUBLISHER,
    selectTab,
    selectTableRow,
    TestSize,
    MESH_LOGIN,
    findNotification,
    createClient,
    createMeshProxy,
    PlaywrightMeshDriver,
    GroupImportData,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_ID,
    UserImportData,
    IMPORT_TYPE_USER,
    ImportPermissions,
    createMeshClient,
} from '@gentics/e2e-utils';
import { UserUpdateRequest } from '@gentics/mesh-models';
import { expect, Locator, Page, test } from '@playwright/test';
import { AUTH } from './common';
import {
    findEntityTableActionButton,
    loginWithCR,
    logoutMeshManagement,
    navigateToModule,
    setGtxDateFromXpath,
} from './helpers';
import { MeshRestClient } from '@gentics/mesh-rest-client';
import { cloneWithSymbols } from '@gentics/common';

// Selector constants
const SELECTORS = {
    EDITOR: 'gtx-content-repository-editor',
    TABS: {
        CONTAINER: '.gtx-entity-detail > gtx-tabs',
        MANAGEMENT: 'gtx-mesh-management',
        TAGMAP: 'gtx-tag-map-entry-table',
    },
    // PROJECT: {
    //     TABLE: 'gtx-mesh-project-table',
    //     MODAL: 'gtx-mesh-project-modal',
    //     SCHEMA_PICKER: 'gtx-mesh-schema-picker .select-button',
    //     SCHEMA_MODAL: 'gtx-mesh-select-schema-modal',
    //     NAME_INPUT: 'gtx-input[formcontrolname="name"] input[type="text"]',
    // },
    // LOGIN_FORM: {
    //     USERNAME: 'gtx-input[formcontrolname="username"] input',
    //     PASSWORD: 'gtx-input[formcontrolname="password"] input',
    //     NEW_PASSWORD: 'gtx-input[formcontrolname="newPassword"] input',
    //     SUBMIT: 'button[type="submit"]:not([disabled])',
    // },
    // FORM: {
    //     PASSWORD_CHECKBOX: '.password-checkbox label',
    //     PASSWORD_INPUTS: '[data-control="password"] input',
    //     FORCE_PASSWORD: '[data-control="forcePasswordChange"] label',
    // },
    // TAGMAP: {
    //     CREATE_BUTTON: 'gtx-button[data-action=create] button',
    // },
} as const;

// const CLASS_ACTIVE = /active/;
// const CLASS_GRANTED = /granted/;

const BASE_GROUP: GroupImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
    [IMPORT_ID]: 'meshApiTokenMaintenance_group_base',
    name: 'meshApiTokenMaintenance_base',
    description: 'Mesh-Api-Token-Maintenance: Base',
    permissions: [],
};

const TEST_USER: UserImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_USER,
    [IMPORT_ID]: 'meshApiTokenMaintenance_user_test',

    email: 'test@example.com',
    firstName: 'Content-Maintenance',
    lastName: 'Test',
    group: BASE_GROUP,

    login: 'apiTokenMaintenance_test',
    password: 'foobar2026',
};

test.describe('Mesh User Api Token', () => {
    const IMPORTER = new EntityImporter({
        logImports: false,
    });

    let testCr: ContentRepository;
    let master: Locator;

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
        await context.clearCookies();
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();

        // Clean and setup test data
        await IMPORTER.cleanupTest();
        await IMPORTER.syncPackages(TestSize.MINIMAL);
        await IMPORTER.setupTest(TestSize.MINIMAL);

        testCr = IMPORTER.get(CONTENT_REPOSITORY_MESH);

        await navigateToApp(page);

        await setupWithPermissions(IMPORTER, login, page, [
            // General Admin-UI Permissions
            {
                type: AccessControlledType.ADMIN,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
            // Object Property Permissions
            {
                type: AccessControlledType.CONTENT_ADMIN,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
            {
                type: AccessControlledType.CONTENT_REPOSITORY_ADMIN,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.CREATE, value: true },
                    { type: GcmsPermission.UPDATE, value: true },
                ],
            },
            {
                type: AccessControlledType.SCHEDULER,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
        ]);

        await navigateToModule(page, 'content-repositories');

        master = page.locator('gtx-content-repository-master');
        await master.waitFor({ state: 'visible' });
    });

    test('should be possible to open api token list of user', {
        annotation: [{
            type: 'ticket',
            description: 'GPU-2653',
        }],
    },
    async ({ page }) => {
        const row = await findTableRowById(master, testCr.id);
        await openUserTokenModal(page, row);
    });

    test('should be possible to create new token for user', {
        annotation: [{
            type: 'ticket',
            description: 'GPU-2653',
        }],
    },
    async ({ page }) => {
        const row = await findTableRowById(master, testCr.id);
        const userTokenModal = await openUserTokenModal(page, row);

        await userTokenModal.locator('.button-event-wrapper button[data-action="primary"]').first().click();

        const createTokenModal = page.locator('gtx-create-mesh-user-token-modal');

        await expect(createTokenModal).toBeVisible();

        const submitBtn = createTokenModal.locator('.modal-footer').locator('[data-action="confirm"]');

        const form = createTokenModal.locator('form');

        // fill only name, keep date empty
        const nameInput = form.locator('input[type="text"]');
        await nameInput.fill('Test');

        await expect(submitBtn.locator('button')).toBeEnabled();

        // select date from yesterday
        await fillNewTokenFormEntry(page, createTokenModal, 'Test', 'xpath=preceding-sibling::td[1]');

        await expect(submitBtn.locator('button')).toBeDisabled();

        // select date from tomorrow
        await fillNewTokenFormEntry(page, createTokenModal, 'Test', 'xpath=following-sibling::td[2]');

        await expect(submitBtn.locator('button')).toBeEnabled();

        await submitBtn.click();

        await expect(page.locator('gtx-mesh-copy-token-modal')).toBeVisible();
        await expect(page.locator('gtx-mesh-copy-token-modal gtx-copy-value').locator('.content')).toHaveText(/\S+/);

        await page.locator('gtx-mesh-copy-token-modal button[data-action="primary"]').click();
        const table = page.locator('gtx-mesh-user-token-modal').locator('gtx-mesh-user-token-table');

        await expect(table.locator('div[data-id="name"]').filter({ hasText: 'Test' })).toBeVisible();
    });

    test('should be possible to delete mesh tokens from user', {
        annotation: [{
            type: 'ticket',
            description: 'GPU-2653',
        }],
    },
    async ({ page }) => {
        const row = await findTableRowById(master, testCr.id);
        const userTokenModal = await openUserTokenModal(page, row);

        await userTokenModal.locator('.button-event-wrapper button[data-action="primary"]').first().click();

        const createTokenModal = page.locator('gtx-create-mesh-user-token-modal');

        await expect(createTokenModal).toBeVisible();

        const submitBtn = createTokenModal.locator('.modal-footer').locator('[data-action="confirm"]');

        // create entry
        await fillNewTokenFormEntry(page, createTokenModal, 'Test 1', 'xpath=following-sibling::td[1]');

        await submitBtn.click();

        await expect(page.locator('gtx-mesh-copy-token-modal')).toBeVisible();

        await page.locator('gtx-mesh-copy-token-modal .modal-footer button[data-action="primary"]').first().click();

        // delete entry
        const table = page.locator('gtx-mesh-user-token-modal').locator('gtx-mesh-user-token-table');

        await expect(table).toBeVisible();

        const firstRow = table.locator('.data-row').first();

        const deleteButton = findTableAction(firstRow, 'delete');

        await deleteButton.click();

        await expect(page.locator('gtx-confirm-delete-modal')).toBeVisible();

        await page.locator('gtx-confirm-delete-modal gtx-button[data-action="confirm"]').click();

        await expect(firstRow).not.toBeAttached();
    });

    test('should be possible to delete multiple mesh tokens from user', {
        annotation: [{
            type: 'ticket',
            description: 'GPU-2653',
        }],
    },
    async ({ page }) => {
        const row = await findTableRowById(master, testCr.id);
        const userTokenModal = await openUserTokenModal(page, row);

        // create entries
        for (let i = 0; i < 2; i++) {
            await userTokenModal.locator('.button-event-wrapper button[data-action="primary"]').first().click();

            const createTokenModal = page.locator('gtx-create-mesh-user-token-modal');

            await expect(createTokenModal).toBeVisible();

            const submitBtn = createTokenModal.locator('.modal-footer').locator('[data-action="confirm"]');

            await fillNewTokenFormEntry(page, createTokenModal, `Test ${i}`, 'xpath=following-sibling::td[1]');

            await submitBtn.click();

            await expect(page.locator('gtx-mesh-copy-token-modal')).toBeVisible();

            await page.locator('gtx-mesh-copy-token-modal .modal-footer button[data-action="primary"]').first().click();
        }

        const table = page.locator('gtx-mesh-user-token-modal').locator('gtx-mesh-user-token-table');

        await expect(table).toBeVisible();

        const firstRow = table.locator('.header-row');

        await firstRow.locator('gtx-checkbox').click();

        await findTableAction(firstRow, 'delete').click();

        await expect(page.locator('gtx-confirm-delete-modal')).toBeVisible();

        await page.locator('gtx-confirm-delete-modal gtx-button[data-action="confirm"]').click();

        await expect(table.locator('.data-row')).toHaveCount(0);
    });
});

async function openUserTokenModal(page: Page, row: Locator) {
    await row.waitFor({ state: 'visible' });
    await clickTableRow(row);

    const tabs = page.locator(`${SELECTORS.EDITOR} ${SELECTORS.TABS.CONTAINER}`);
    await selectTab(tabs, 'management');

    const management = tabs.locator(SELECTORS.TABS.MANAGEMENT);
    await management.waitFor({ state: 'visible' });

    await management.locator('gtx-mesh-login-gate .login button[data-action="primary"]').last().click();
    await management.locator('gtx-mesh-login-gate form input[type="text"]').fill('admin');
    await management.locator('gtx-mesh-login-gate form input[type="password"]').fill('admin');
    await management.locator('gtx-mesh-login-gate form gtx-button.form-login-button').click();

    const managementTabs = management.locator('gtx-grouped-tabs');
    await selectTab(managementTabs, 'users', true);

    await expect(management.locator('gtx-mesh-user-table')).toBeVisible();

    await management.locator('[data-id="showApiToken"]').first().click();

    await expect(page.locator('gtx-mesh-user-token-modal')).toBeVisible();

    await cleanUpTokensIfNecessary(page);

    return page.locator('gtx-mesh-user-token-modal');
}

async function fillNewTokenFormEntry(page: Page, createTokenModal: Locator, name: string, dateXPath: string) {
    const form = createTokenModal.locator('form');

    // fill only name, keep date empty
    const nameInput = form.locator('input[type="text"]');
    await nameInput.fill(name);

    const dateInput = form.locator('gtx-date-time-picker');

    await setGtxDateFromXpath(page, dateInput, dateXPath);
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

async function cleanUpTokensIfNecessary(page: Page) {
    const table = page.locator('gtx-mesh-user-token-modal').locator('gtx-mesh-user-token-table');

    await expect(table).toBeVisible();

    if (await table.locator('.data-row').count() === 0) {
        return;
    }

    const firstRow = table.locator('.header-row');

    await firstRow.locator('gtx-checkbox').click();

    await findTableAction(firstRow, 'delete').click();

    await expect(page.locator('gtx-confirm-delete-modal')).toBeVisible();

    await page.locator('gtx-confirm-delete-modal gtx-button[data-action="confirm"]').click();

    await expect(table.locator('.data-row')).toHaveCount(0);
}

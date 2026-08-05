import {
    EntityImporter,
    loginWithForm,
    navigateToApp,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, Page, test } from '@playwright/test';
import { AUTH } from './common';

const API_MODAL = "gtx-api-tokens-modal";
const API_CREATE_MODAL = "gtx-api-tokens-create-modal";
const API_DELETE_MODAL = "gtx-api-tokens-delete-modal";

test.describe('Api Tokens', () => {

    const IMPORTER = new EntityImporter();

    test.beforeAll(async ({ request }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
        });

        await test.step('Test Bootstrapping', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
        });

        await clearEntries(IMPORTER);
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
        await loginWithForm(page, AUTH.admin);
    });

    test('should show api modal', async ({ page }) => {
        await openApiTokenModal(page);

        const table = page.locator(API_MODAL).locator("gtx-manage-api-tokens-table");

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

        const form = page.locator(API_CREATE_MODAL).locator("form");

        await expect(form).toBeVisible();

        const submitBtn = page.locator(API_CREATE_MODAL).locator(".modal-footer").locator("button.primary");

        await expect(submitBtn).toBeDisabled();

        // fill only name, keep date empty
        const nameInput = form.locator('input[type="text"]');
        await nameInput.fill("Test");

        await expect(submitBtn).not.toBeDisabled();

        const dateInput = form.locator('input[type="date"]');

        // fill with date from yesterday
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);

        await dateInput.fill(yesterday.toISOString().split('T')[0]);

        await expect(submitBtn).toBeDisabled();

        //fill with date from tomorrow
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        
        await dateInput.fill(tomorrow.toISOString().split('T')[0]);

        await expect(submitBtn).not.toBeDisabled();

        await submitBtn.click();

        await expect(page.locator(".success-message")).toBeVisible();
        await expect(page.locator(".success-message").locator(".content")).toHaveText(/\S+/);
    });

    test('can delete Api Tokens', async ({ page }) => {
        await openApiTokenModal(page);
        
        const table = page.locator(API_MODAL).locator("gtx-manage-api-tokens-table");

        await expect(table).toBeVisible();

        const row = table.locator(".data-row").first();

        const deleteButton = row.locator('[title="editor.tagtype_delete_label"]').first();

        await deleteButton.click();

        await expect(page.locator(API_DELETE_MODAL)).toBeVisible();

        await page.locator(API_DELETE_MODAL).locator("button.alert").click();

        await expect(row).not.toBeAttached();
    });

    test('can delete multiple Api Tokens', async ({ page }) => {
        await addEntries(IMPORTER);
        await openApiTokenModal(page);
        
        const table = page.locator(API_MODAL).locator("gtx-manage-api-tokens-table");

        await expect(table).toBeVisible();

        const row = table.locator(".header-row");

        await row.locator('gtx-checkbox').click();

        await row.locator('button.alert').click();

        await expect(page.locator(API_DELETE_MODAL)).toBeVisible();

        await page.locator(API_DELETE_MODAL).locator("button.alert").click();

        await expect(table.locator(".data-row")).toHaveCount(0);
    });
    
});

async function openApiTokenModal(page: Page) {
    await page.locator(".ng-trigger-toggleState").click();
    await page.locator("gtx-dropdown-list gtx-button").last().click();
    await page.locator('[overrideslot="manageApiTokensOption"]').click();
}

async function openApiTokenCreateModal(page: Page) {
    const modalTable = page.locator(API_MODAL).locator("gtx-manage-api-tokens-table");
    const createNewBtn = modalTable.locator(".entity-table-actions-bar button");
    await createNewBtn.click();
}

async function clearEntries(IMPORTER: EntityImporter) {
    const tokens = await IMPORTER.client.admin.getApiTokens().send();
    for(const item of tokens.items) {
        await IMPORTER.client.admin.deleteApiTokens(item.id).send();
    }
}

async function addEntries(IMPORTER: EntityImporter) {
    await IMPORTER.client.admin.addApiTokens({name: "token 1"}).send();
    await IMPORTER.client.admin.addApiTokens({name: "token 2"}).send();
}
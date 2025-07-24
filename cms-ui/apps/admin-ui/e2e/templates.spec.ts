import { test, expect } from '@playwright/test';
import { EntityImporter, TestSize, findTableRowById, selectTab, clickTableRow } from '@gentics/e2e-utils';
import { AUTH_ADMIN } from './common';
import { loginWithForm, navigateToApp, navigateToModule } from './helpers';

const IMPORTER = new EntityImporter();
const NODE_NAME = 'empty node';
const TEMPLATE_NAME = '[Test] Basic Template';
const LINK_TO_NODE_ACTION = 'linkToNode';
const LINK_TO_NODE_MODAL = 'gtx-assign-templates-to-nodes-modal';
const LINK_TO_FOLDER_ACTION = 'linkToFolder';
const LINK_TO_FOLDER_MODAL = 'gtx-assign-templates-to-folders-modal';

test.describe('Templates Module', () => {
    const IMPORTER = new EntityImporter();

    test.beforeAll(async ({ request }, testInfo) => {
        testInfo.setTimeout(120_000);
        IMPORTER.setApiContext(request);
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach(async ({ page, request, context }, testInfo) => {
        testInfo.setTimeout(120_000);
        await context.clearCookies();
        // Reset importer client to avoid 401 errors
        IMPORTER.setApiContext(request);

        // Clean and setup test data
        await IMPORTER.cleanupTest();
        await IMPORTER.syncPackages(TestSize.MINIMAL);

        // Navigate to the app and log in
        await navigateToApp(page);
        await loginWithForm(page, AUTH_ADMIN);

        // Navigate to the scheduler
        await navigateToModule(page, 'templates');

        // select our "empty node"
        const nodeTable = page.locator('gtx-table');
        await nodeTable
            .locator('.data-row:has(.data-column[data-id="name"])')
            .click();
    });

    test('should open node assignment modal for single template', async ({ page }) => {
        const tplRow = page
            .locator('gtx-table')
            .locator('.data-row');
        await tplRow
            .locator(`gtx-button[data-id="${LINK_TO_NODE_ACTION}"]`)
            .click();
        await expect(page.locator(LINK_TO_NODE_MODAL)).toBeVisible();
    });

    test('should open node assignment modal for template selection', async ({ page }) => {
        const tplRow = page
            .locator('gtx-table')
            .locator('.data-row');
        await tplRow
            .locator(`gtx-button[data-id="${LINK_TO_NODE_ACTION}"]`)
            .click();
        await expect(page.locator(LINK_TO_NODE_MODAL)).toBeVisible();
    });

    test('should open folder assignment modal for single template', async ({ page }) => {
        const tplRow = page
            .locator('gtx-table')
            .locator('.data-row');
        await tplRow
            .locator(`gtx-button[data-id="${LINK_TO_FOLDER_ACTION}"]`)
            .click();
        await expect(page.locator(LINK_TO_FOLDER_MODAL)).toBeVisible();
    });

    test('should open folder assignment modal for template selection', async ({ page }) => {
        const tplRow = page
            .locator('gtx-table')
            .locator('.data-row');
        await tplRow
            .locator(`gtx-button[data-id="${LINK_TO_FOLDER_ACTION}"]`)
            .click();
        await expect(page.locator(LINK_TO_FOLDER_MODAL)).toBeVisible();
    });
});

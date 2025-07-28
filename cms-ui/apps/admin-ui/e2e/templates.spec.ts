import { Template } from '@gentics/cms-models';
import {
    BASIC_TEMPLATE_ID,
    EntityImporter,
    TestSize,
    findTableRowById,
    loginWithForm,
    navigateToApp,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import { navigateToModule } from './helpers';

const NODE_NAME = 'empty node';
const TEMPLATE_NAME = '[Test] Basic Template';
const LINK_TO_NODE_ACTION = 'linkToNode';
const LINK_TO_NODE_MODAL = 'gtx-assign-templates-to-nodes-modal';
const LINK_TO_FOLDER_ACTION = 'linkToFolder';
const LINK_TO_FOLDER_MODAL = 'gtx-assign-templates-to-folders-modal';

test.describe.configure({ mode: 'serial' });
test.describe('Templates Module', () => {
    const IMPORTER = new EntityImporter();

    let testTemplate: Template;

    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach(async ({ page, request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();

        // Clean and setup test data
        await IMPORTER.cleanupTest();
        await IMPORTER.syncPackages(TestSize.MINIMAL);
        await IMPORTER.setupTest(TestSize.MINIMAL);
        testTemplate = IMPORTER.get(BASIC_TEMPLATE_ID as any) as any;

        // Navigate to the app and log in
        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);

        // Navigate to the scheduler
        await navigateToModule(page, 'templates');

        // select our "empty node"
        const nodeTable = page.locator('gtx-table');
        await nodeTable
            .locator('.data-row:has(.data-column[data-id="name"])')
            .first()
            .click();
    });

    test('should open node assignment modal for single template', async ({ page }) => {
        const tplRow = findTableRowById(page, testTemplate.id);
        await tplRow
            .locator(`gtx-button[data-id="${LINK_TO_NODE_ACTION}"]`)
            .click();
        await expect(page.locator(LINK_TO_NODE_MODAL)).toBeVisible();
    });

    test('should open node assignment modal for template selection', async ({ page }) => {
        const tplRow = findTableRowById(page, testTemplate.id);
        await tplRow
            .locator(`gtx-button[data-id="${LINK_TO_NODE_ACTION}"]`)
            .click();
        await expect(page.locator(LINK_TO_NODE_MODAL)).toBeVisible();
    });

    test('should open folder assignment modal for single template', async ({ page }) => {
        const tplRow = findTableRowById(page, testTemplate.id);
        await tplRow
            .locator(`gtx-button[data-id="${LINK_TO_FOLDER_ACTION}"]`)
            .click();
        await expect(page.locator(LINK_TO_FOLDER_MODAL)).toBeVisible();
    });

    test('should open folder assignment modal for template selection', async ({ page }) => {
        const tplRow = findTableRowById(page, testTemplate.id);
        await tplRow
            .locator(`gtx-button[data-id="${LINK_TO_FOLDER_ACTION}"]`)
            .click();
        await expect(page.locator(LINK_TO_FOLDER_MODAL)).toBeVisible();
    });
});

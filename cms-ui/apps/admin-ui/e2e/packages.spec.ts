import {ObjectProperty, Template} from '@gentics/cms-models';
import {
    BASIC_TEMPLATE_ID,
    EntityImporter,
    TestSize,
    findTableRowById,
    loginWithForm,
    navigateToApp, OBJECT_PROPERTY_PAGE_COLOR, matchRequest, clickTableRow, selectTableRow,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import {navigateToModule, selectTab} from './helpers';

const NODE_NAME = 'empty node';
const TEMPLATE_NAME = '[Test] Basic Template';
const LINK_TO_NODE_ACTION = 'linkToNode';
const LINK_TO_NODE_MODAL = 'gtx-assign-templates-to-nodes-modal';
const LINK_TO_FOLDER_ACTION = 'linkToFolder';
const LINK_TO_FOLDER_MODAL = 'gtx-assign-templates-to-folders-modal';

test.describe.configure({ mode: 'serial' });
test.describe('Packages Module', () => {
    const IMPORTER = new EntityImporter();

    let objectProperty: ObjectProperty;

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
        objectProperty = IMPORTER.get(OBJECT_PROPERTY_PAGE_COLOR as any) as any;

        // Navigate to the app and log in
        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);

        // Navigate to the scheduler
        await navigateToModule(page, 'devtool-packages');
    });

    test('should show package', async ({ page }) => {
        const TEST_PACKAGE_NAME = 'Test Package';
        const master = page.locator('gtx-dev-tool-package-master');
        const masterTable = master.locator('gtx-dev-tool-package-table');

        await test.step('Create test package', async () => {
            await masterTable.locator('.entity-table-actions-bar [data-action="create"] button').click();

            const modal = page.locator('gtx-create-dev-tool-package-modal');
            const form  = modal.locator('.modal-content form');
            const input = form.locator('[formcontrolname="name"] input');

            await input.fill(TEST_PACKAGE_NAME);
            await modal.locator('.modal-footer [data-action="confirm"] button').click();
        });

        await test.step('Assign object property to test package', async () => {
            const row = findTableRowById(masterTable, TEST_PACKAGE_NAME);

            await clickTableRow(row);

            const packageEditor = page.locator('gtx-dev-tool-package-editor');

            await selectTab(page, 'object-properties');

            const tabContents = packageEditor.locator('.is-active');
            const selectObjectPropertyTable = tabContents.locator('gtx-object-property-table');

            await selectObjectPropertyTable.locator('.entity-table-actions-bar [data-action="assign"] button').click();

            const assignModal = page.locator('gtx-assign-entity-to-package-modal');
            const objectPropertyRow = findTableRowById(assignModal, objectProperty.id);

            await selectTableRow(objectPropertyRow);
            await assignModal.locator('gtx-button[data-action="confirm"]').click();

            const assignedObjectPropertyRow = findTableRowById(selectObjectPropertyTable, objectProperty.id);

            await expect(assignedObjectPropertyRow).toBeVisible();
        });
    });
});

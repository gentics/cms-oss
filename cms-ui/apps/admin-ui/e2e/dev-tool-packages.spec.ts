import { ObjectProperty } from '@gentics/cms-models';
import {
    EntityImporter,
    OBJECT_PROPERTY_PAGE_COLOR,
    TestSize,
    clickTableRow,
    findTableRowById,
    loginWithForm,
    navigateToApp,
    selectTab,
    selectTableRow,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import { navigateToModule } from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('Dev-Tool Packages Module', () => {
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

    test('should be possible to add a object-property to package', async ({ page }) => {
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
            const packageRow = findTableRowById(masterTable, TEST_PACKAGE_NAME);
            await clickTableRow(packageRow);

            const packageEditor = page.locator('gtx-dev-tool-package-editor');
            await selectTab(page, 'object-properties');
            const tabContents = packageEditor.locator('.is-active');

            const packagePropertiesTable = tabContents.locator('gtx-object-property-table');
            await packagePropertiesTable.locator('.entity-table-actions-bar [data-action="assign"] button').click();

            const assignModal = page.locator('gtx-assign-entity-to-package-modal');
            const objectPropertyRow = findTableRowById(assignModal, objectProperty.id);
            await selectTableRow(objectPropertyRow);
            await assignModal.locator('.modal-footer [data-action="confirm"] button').click();

            const assignedObjectPropertyRow = findTableRowById(packagePropertiesTable, objectProperty.id);
            await expect(assignedObjectPropertyRow).toBeVisible();
        });
    });
});

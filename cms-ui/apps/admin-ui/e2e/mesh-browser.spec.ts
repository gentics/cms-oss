import {
    EntityImporter,
    findTableRowByText,
    loginWithForm,
    navigateToApp,
    pageOne,
    schedulePublisher,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import { navigateToModule } from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('Mesh Browser', () => {
    const IMPORTER = new EntityImporter();
    const CR_NAME = 'Mesh CR';

    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach(async ({ page, request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();

        await IMPORTER.cleanupTest();
        await IMPORTER.syncPackages(TestSize.MINIMAL);

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);

        // Navigate to constructs module
        await navigateToModule(page, 'mesh-browser');
    });

    test.describe('Mesh Browser', () => {
        test('should have content repositories listed', async ({ page }) => {
            const rows = page.locator('gtx-table .grid-row.data-row');
            await rows.waitFor();
            await expect(rows).toHaveCount(1);
        });

        test('should show login gate on click', async ({ page }) => {
            const row = findTableRowByText(page, CR_NAME);
            await row.waitFor();
            await row.click();
            await expect(page.locator('.login-gate-wrapper')).toBeVisible();
        });
    });

    test.describe('Mesh Browser (authenticated)', () => {

        test.beforeEach(async ({ page }) => {
            // Setup Data which should be published to mesh in order to be visible in the mesh-browser
            await IMPORTER.setupTest(TestSize.MINIMAL);
            await IMPORTER.client.page.publish(IMPORTER.get(pageOne).id, { alllang: true }).send();

            // Import and execute the publisher to have the content be published
            await IMPORTER.importData([
                schedulePublisher,
            ]);
            await IMPORTER.executeSchedule(schedulePublisher);

            // Click into the Mesh CR
            const row = findTableRowByText(page, CR_NAME);
            await row.waitFor();
            await row.click();

            // Fill in Mesh credentials and submit
            await page.locator('.login-form input[type="text"]').fill(AUTH.mesh.username);
            await page.locator('.login-form input[type="password"]').fill(AUTH.mesh.password);
            await page.locator('.login-form button[type="submit"]').click();

            // Now the schema list should appear
            page.locator('.schema-list-wrapper').waitFor();
            // await expect().toBeVisible();
        });

        test('should list content', async ({ page }) => {
            const elements = page.locator('.schema-items .schema-element');
            await elements.waitFor();
            await expect(elements).toHaveCount(1);
        });

        test('should be able to navigate to node content', async ({ page }) => {
            const container = page
                .locator('.schema-items .schema-element [data-is-container="true"]')
                .first();
            await expect(container).toBeVisible();
            await container.click();
        });

        test('should be able to open detail view', async ({ page }) => {
            const folders = page.locator('gtx-mesh-browser-schema-items[data-id="example_folder"]');

            // Navigate into the first folder
            await folders.locator('.schema-content .schema-element .title').click();

            // Find the published page and click it to open the editor
            const contents = page.locator('gtx-mesh-browser-schema-items[data-id="example_content"]');
            const element = contents.locator('.schema-content .schema-element');
            await element.waitFor();
            await element.locator('.title').click();

            // Wait for the editor to open up
            const editor = page.locator('gtx-mesh-browser-editor');
            await editor.waitFor();
        });
    });
});

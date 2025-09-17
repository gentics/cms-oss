import {
    EntityImporter,
    findTableRowByText,
    LANGUAGE_DE,
    LANGUAGE_EN,
    loginWithForm,
    navigateToApp,
    openContext,
    pageOne,
    pageOneDE,
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

        test('should be able to open detail view in a current language', async ({ page }) => {
            // Select the language
            const languageSelector = page.locator('.dropdown-language');
            const languageSelectorContext = await openContext(languageSelector);
            await languageSelectorContext.locator(`[data-id="${LANGUAGE_DE}"]`).click();

            const folders = page.locator('gtx-mesh-browser-schema-items[data-id="example_folder"]');

            // Navigate into the first folder
            await folders.locator('.schema-content .schema-element .title').click();

            // Find the published page and click it to open the editor
            const contents = page.locator('gtx-mesh-browser-schema-items[data-id="example_content"]');
            const element = contents.locator('.schema-content .schema-element').first();
            await element.locator('.title').click();

            // Wait for the editor to open up
            const editor = page.locator('gtx-mesh-browser-editor');

            const langIndicatorPage = editor.locator('.language-indicator');
            await expect(langIndicatorPage).toHaveText(LANGUAGE_DE);
        });

        test('should be able to open detail view in an alternative language', async ({ page }) => {
            // Select the language
            const languageSelector = page.locator('.dropdown-language');
            const languageSelectorContext = await openContext(languageSelector);
            await languageSelectorContext.locator(`[data-id="${LANGUAGE_EN}"]`).click();

            const folders = page.locator('gtx-mesh-browser-schema-items[data-id="example_folder"]');

            // Navigate into the first folder
            await folders.locator('.schema-content .schema-element .title').click();

            // Find the published page and click it to open the editor
            const contents = page.locator('gtx-mesh-browser-schema-items[data-id="example_content"]');
            const element = contents.locator('.schema-content .schema-element').first();
            await element.locator('.title').click();

            // Wait for the editor to open up
            const editor = page.locator('gtx-mesh-browser-editor');

            const langIndicatorPage = editor.locator('.language-indicator');
            await expect(langIndicatorPage).toHaveText(LANGUAGE_EN);
        });
    });


    test.describe('Mesh Browser multilingual (authenticated)', () => {

        test.beforeEach(async ({ page }) => {
            // Setup Data which should be published to mesh in order to be visible in the mesh-browser
            await IMPORTER.setupTest(TestSize.MINIMAL);
            await IMPORTER.client.page.publish(IMPORTER.get(pageOneDE).id, { alllang: true }).send();

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
            await page.locator('.schema-list-wrapper').waitFor();
            // await expect().toBeVisible();
        });

        test('should be able to open detail view in a different language', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-18877',
            }],
        }, async ({ page }) => {
            const languageSelector = page.locator('.dropdown-language');

            const folders = page.locator('gtx-mesh-browser-schema-items[data-id="example_folder"]');

            // Navigate into the first folder
            await folders.locator('.schema-content .schema-element .title').click();

            // Select german first
            let languageSelectorContext = await openContext(languageSelector);
            await languageSelectorContext.locator(`[data-id="${LANGUAGE_DE}"]`).click();

            // Find the published page and click it to open the editor
            const contents = page.locator('gtx-mesh-browser-schema-items[data-id="example_content"]');
            const element = contents.locator('.schema-content .schema-element').first();
            await element.locator('.title').click();

            // Wait for the editor to open up
            const editor = page.locator('gtx-mesh-browser-editor');

            const langIndicatorPage = editor.locator('gtx-entity-detail-header .language-indicator');
            await expect(langIndicatorPage).toHaveText(LANGUAGE_DE);

            // Close the editor
            await editor.locator('gtx-entity-detail-header .gtx-cancel-button button').click();

            // Select english language
            languageSelectorContext = await openContext(languageSelector);
            await languageSelectorContext.locator(`[data-id="${LANGUAGE_EN}"]`).click();

            // Open the page in english now
            await element.locator('.title').click();
            // Should be in correct language
            await expect(langIndicatorPage).toHaveText(LANGUAGE_EN);
        });
    });
});

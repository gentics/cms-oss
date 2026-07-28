import { ContentRepository } from '@gentics/cms-models';
import {
    BASIC_TEMPLATE_ID,
    CONTENT_REPOSITORY_MESH,
    EntityImporter,
    FILE_ONE,
    findTableRowById,
    FIXTURE_FILE_PDF1,
    FIXTURE_IMAGE_JPEG1,
    FOLDER_A,
    IMAGE_ONE,
    IMPORT_ID,
    IMPORT_TYPE,
    ITEM_TYPE_PAGE,
    LANGUAGE_DE,
    LANGUAGE_EN,
    loginWithForm,
    matchRequest,
    MESH_LOGIN,
    MESH_SCHEMA_BIN_CONTENT,
    MESH_SCHEMA_CONTENT,
    MESH_SCHEMA_FOLDER,
    navigateToApp,
    NODE_MINIMAL,
    openContext,
    PAGE_ONE,
    PAGE_ONE_DE,
    PageImportData,
    SCHEDULE_PUBLISHER,
    TestSize,
} from '@gentics/e2e-utils';
import { GraphQLRequest } from '@gentics/mesh-models';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import { navigateToModule } from './helpers';

test.describe('Mesh Browser', () => {

    const IMPORTER = new EntityImporter();
    let testCr: ContentRepository;

    test.beforeAll('Test bootstrapping', async ({ request }) => {
        IMPORTER.setApiContext(request);
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach('General Test setup', async ({ request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();

        await IMPORTER.cleanupTest();
        await IMPORTER.syncPackages(TestSize.MINIMAL);
        await IMPORTER.setupBinaryFiles({
            [IMAGE_ONE[IMPORT_ID]]: FIXTURE_IMAGE_JPEG1,
            [FILE_ONE[IMPORT_ID]]: FIXTURE_FILE_PDF1,
        });
        await IMPORTER.setupTest(TestSize.MINIMAL);
        testCr = IMPORTER.get(CONTENT_REPOSITORY_MESH);
        // Make sure that mesh is properly setup
        await IMPORTER.client.contentRepository.repairStructure(testCr.id).send();
    });

    test.describe('Overview', () => {
        test.beforeEach(async ({ page }) => {
            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);

            // Navigate to constructs module
            await navigateToModule(page, 'mesh-browser');
        });

        test('should have content repositories listed', async ({ page }) => {
            const rows = page.locator('gtx-table .grid-row.data-row');
            await rows.waitFor();
            await expect(rows).toHaveCount(1);
        });

        test('should show login gate on click', async ({ page }) => {
            const row = await findTableRowById(page, testCr.id);
            await row.waitFor();
            await row.click();
            await expect(page.locator('.login-gate-wrapper')).toBeVisible();
        });
    });

    test.describe('Browsing', () => {
        test.slow();

        // Simple "empty" page in german, as we need to publish this page
        // in order for mesh to create an additional language.
        const PAGE_GERMAN: PageImportData = {
            [IMPORT_TYPE]: ITEM_TYPE_PAGE,
            [IMPORT_ID]: 'germanMeshPage1',
            folderId: FOLDER_A[IMPORT_ID],
            nodeId: NODE_MINIMAL[IMPORT_ID],
            pageName: 'xSomethingGerman',
            templateId: BASIC_TEMPLATE_ID,
            language: LANGUAGE_DE,
        };

        test.beforeEach(async ({ page }) => {
            // Import publisher and our datq
            await IMPORTER.importData([
                SCHEDULE_PUBLISHER,
                PAGE_GERMAN,
            ]);

            // Publish the pages
            await IMPORTER.client.page.publish(IMPORTER.get(PAGE_ONE).id, { alllang: true }).send();
            await IMPORTER.client.page.publish(IMPORTER.get(PAGE_GERMAN).id, { alllang: true }).send();
            await IMPORTER.executeSchedule(SCHEDULE_PUBLISHER, 3);
            // await waitForPublishDone(page, IMPORTER.client);

            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);

            // Navigate to constructs module
            await navigateToModule(page, 'mesh-browser');

            // Click into the Mesh CR
            const row = await findTableRowById(page, testCr.id);
            await row.waitFor();
            await row.click();

            // Fill in Mesh credentials and submit
            await loginWithForm(page.locator('.login-form'), MESH_LOGIN);

            // Now the schema list should appear
            page.locator('.schema-list-wrapper').waitFor();
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

        test('should be able to open detail view in the current language', async ({ page }) => {
            // Select the language
            const languageSelector = page.locator('.dropdown-language');
            const languageSelectorContext = await openContext(languageSelector);
            await languageSelectorContext.locator(`[data-id="${LANGUAGE_EN}"]`).click();

            const folders = page.locator(`gtx-mesh-browser-schema-items[data-id="${MESH_SCHEMA_FOLDER}"]`);

            // Navigate into the first folder
            await folders.locator('.schema-content .schema-element .title').click();

            // Find the published page and click it to open the editor
            const contents = page.locator(`gtx-mesh-browser-schema-items[data-id="${MESH_SCHEMA_CONTENT}"]`);
            const element = contents.locator('.schema-content .schema-element').filter({
                hasText: IMPORTER.get(PAGE_ONE).name,
            });
            await element.locator('.title').click();

            // Wait for the editor to open up
            const editor = page.locator('gtx-mesh-browser-editor');

            const langIndicatorPage = editor.locator('.language-indicator');
            await expect(langIndicatorPage).toHaveText(LANGUAGE_EN);
        });

        test('should be able to open detail view in an alternative language', async ({ page }) => {
            // Select the language
            const languageSelector = page.locator('.dropdown-language');
            const languageSelectorContext = await openContext(languageSelector);
            await languageSelectorContext.locator(`[data-id="${LANGUAGE_DE}"]`).click();

            const folders = page.locator(`gtx-mesh-browser-schema-items[data-id="${MESH_SCHEMA_FOLDER}"]`);

            // Navigate into the first folder
            await folders.locator('.schema-content .schema-element .title').click();

            // Find the published page and click it to open the editor
            const contents = page.locator(`gtx-mesh-browser-schema-items[data-id="${MESH_SCHEMA_CONTENT}"]`);
            const element = contents.locator('.schema-content .schema-element').filter({
                hasText: IMPORTER.get(PAGE_ONE).name,
            });
            await element.locator('.title').click();

            // Wait for the editor to open up
            const editor = page.locator('gtx-mesh-browser-editor');

            const langIndicatorPage = editor.locator('.language-indicator');
            await expect(langIndicatorPage).toHaveText(LANGUAGE_EN);
        });
    });

    test.describe('Browsing details', () => {
        test.beforeEach('Details setup', async ({ page }) => {
            test.slow();
            await IMPORTER.importData([
                FILE_ONE,
                IMAGE_ONE,
                SCHEDULE_PUBLISHER,
            ], TestSize.MINIMAL);

            await IMPORTER.executeSchedule(SCHEDULE_PUBLISHER);

            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);

            // Navigate to constructs module
            await navigateToModule(page, 'mesh-browser');

            // Click into the Mesh CR
            const row = await findTableRowById(page, testCr.id);
            await row.click();

            // Wait for folders to be loaded
            const folderLoad = page.waitForResponse((request) => {
                if (!matchRequest('POST', '/rest/contentrepositories/*/proxy/api/v2/*/graphql')(request)) {
                    return false;
                }
                const gqlReq: GraphQLRequest = request.request().postDataJSON();
                return gqlReq.variables?.schemaName === MESH_SCHEMA_FOLDER;
            }, { timeout: 5_000 });

            // Fill in Mesh credentials and submit
            await loginWithForm(page.locator('.login-form'), MESH_LOGIN);

            // Now the schema list should appear
            await folderLoad;

            await page.locator('.schema-list-wrapper').waitFor();
        });

        test('should be able to view binary data link in a detail view', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-19256',
            }],
        }, async ({ page }) => {
            const folders = page.locator(`gtx-mesh-browser-schema-items[data-id="${MESH_SCHEMA_FOLDER}"]`);
            await folders.locator('.schema-content .schema-element .title').click();

            const binaries = page.locator(`gtx-mesh-browser-schema-items[data-id="${MESH_SCHEMA_BIN_CONTENT}"]`);
            await binaries.locator('.schema-content .schema-element .title').first().click();

            const editor = page.locator('gtx-mesh-browser-editor');
            await editor.waitFor();
            const binaryContent = editor.locator('.grid-content[data-id="binarycontent"]');

            await expect(binaryContent.locator('a')).toHaveAttribute('href');
        });
    });

    test.describe('Browsing multilingual', () => {
        test.slow();

        test.beforeEach('Multilingual setup', async ({ page }) => {
            // Setup Data which should be published to mesh in order to be visible in the mesh-browser
            await IMPORTER.importData([
                PAGE_ONE_DE,
                SCHEDULE_PUBLISHER,
            ], TestSize.MINIMAL);
            await IMPORTER.client.page.publish(IMPORTER.get(PAGE_ONE).id, { alllang: true }).send();

            // Import and execute the publisher to have the content be published
            await IMPORTER.executeSchedule(SCHEDULE_PUBLISHER);

            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);

            // Navigate to constructs module
            await navigateToModule(page, 'mesh-browser');

            // Click into the Mesh CR
            const row = await findTableRowById(page, testCr.id);
            await row.waitFor();
            await row.click();

            // Wait for folders to be loaded
            const folderLoad = page.waitForResponse((request) => {
                if (!matchRequest('POST', '/rest/contentrepositories/*/proxy/api/v2/*/graphql')(request)) {
                    return false;
                }
                const gqlReq: GraphQLRequest = request.request().postDataJSON();
                return gqlReq.variables?.schemaName === MESH_SCHEMA_FOLDER;
            }, { timeout: 5_000 });

            // Fill in Mesh credentials and submit
            await loginWithForm(page.locator('.login-form'), MESH_LOGIN);

            // Now the schema list should appear
            await folderLoad;

            await page.locator('.schema-list-wrapper').waitFor();
        });

        test('should be able to open detail view in a different language', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-18877',
            }],
        }, async ({ page }) => {
            const languageSelector = page.locator('.dropdown-language');
            const folders = page.locator(`gtx-mesh-browser-schema-items[data-id="${MESH_SCHEMA_FOLDER}"]`);

            // Navigate into the first folder
            await folders.locator('.schema-content .schema-element .title').click();

            // Select german first
            let languageSelectorContext = await openContext(languageSelector);
            await languageSelectorContext.locator(`[data-id="${LANGUAGE_DE}"]`).click();

            // Find the published page and click it to open the editor
            const contents = page.locator(`gtx-mesh-browser-schema-items[data-id="${MESH_SCHEMA_CONTENT}"]`);
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

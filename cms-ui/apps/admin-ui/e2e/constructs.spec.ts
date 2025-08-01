import { NodeMultiLinkRequest, NodePageLanguageCode, NodeUrlMode } from '@gentics/cms-models';
import {
    EntityImporter,
    findTableAction,
    findTableRowById,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_NODE,
    LANGUAGE_DE,
    LANGUAGE_EN,
    loginWithForm,
    navigateToApp,
    NodeImportData,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import { navigateToModule } from './helpers';

const EXAMPLE_NODE_ONE: NodeImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_NODE,
    [IMPORT_ID]: 'constructExampleNodeOne',

    node: {
        name: 'Construct Example Node #1',
        host: 'http://construct01.localhost',
        hostProperty: '',
        publishDir: '',
        binaryPublishDir: '',
        pubDirSegment: true,
        publishImageVariants: false,
        publishFs: false,
        publishFsPages: false,
        publishFsFiles: false,
        publishContentMap: false,
        publishContentMapPages: false,
        publishContentMapFiles: false,
        publishContentMapFolders: false,
        urlRenderWayPages: NodeUrlMode.AUTOMATIC,
        urlRenderWayFiles: NodeUrlMode.AUTOMATIC,
        omitPageExtension: false,
        pageLanguageCode: NodePageLanguageCode.FILENAME,
        meshPreviewUrlProperty: '',
    },
    description: 'Test Node',
    languages: [LANGUAGE_DE],
    templates: [],
};

const EXAMPLE_NODE_TWO: NodeImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_NODE,
    [IMPORT_ID]: 'constructExampleNodeTwo',

    node: {
        name: 'Construct Example Node #2',
        host: 'http://construct02.localhost',
        hostProperty: '',
        publishDir: '',
        binaryPublishDir: '',
        pubDirSegment: true,
        publishImageVariants: false,
        publishFs: false,
        publishFsPages: false,
        publishFsFiles: false,
        publishContentMap: false,
        publishContentMapPages: false,
        publishContentMapFiles: false,
        publishContentMapFolders: false,
        urlRenderWayPages: NodeUrlMode.AUTOMATIC,
        urlRenderWayFiles: NodeUrlMode.AUTOMATIC,
        omitPageExtension: false,
        pageLanguageCode: NodePageLanguageCode.FILENAME,
        meshPreviewUrlProperty: '',
    },
    description: 'Test Node',
    languages: [LANGUAGE_EN],
    templates: [],
};

test.describe.configure({ mode: 'serial' });
test.describe('Constructs Module', () => {
    const IMPORTER = new EntityImporter();
    const TEST_CONSTRUCT_ID = '13';

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
        await IMPORTER.importData([
            EXAMPLE_NODE_ONE,
            EXAMPLE_NODE_TWO,
        ], TestSize.NONE);

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);

        // Navigate to constructs module
        await navigateToModule(page, 'constructs');
    });

    test.describe('Constructs', () => {
        test('should properly remove and assign the constructs to the node', async ({ page }) => {
            const row = findTableRowById(page, TEST_CONSTRUCT_ID)
            await findTableAction(row, 'assignConstructToNodes').click();

            // Wait for modal and find the node table
            const modal = page.locator('gtx-assign-constructs-to-nodes-modal');
            await modal.waitFor();

            // Select the first node
            const nodeTable = modal.locator('.modal-content gtx-table');
            const firstNodeRow = nodeTable.locator('.data-row:not(.selected)').first();
            const nodeId = await firstNodeRow.getAttribute('data-id');
            await firstNodeRow.locator('.select-column gtx-checkbox label').click();

            // Setup route handler for the link request
            const linking = page.route('/rest/construct/link/nodes', (route) => {
                const request = route.request();
                const linkRequestBody: NodeMultiLinkRequest = JSON.parse(request.postData());

                // Verify the request
                expect(linkRequestBody.ids).toEqual([parseInt(nodeId!, 10)]);

                return route.continue();
            });

            // Block unexpected unlink requests
            await page.route('/rest/construct/unlink/nodes', () => {
                throw new Error('Invalid Request to "/rest/construct/unlink/nodes" has been sent!');
            });

            // Confirm the assignment
            await modal.locator('.modal-footer gtx-button[data-action="confirm"]').click();

            // Await the request
            await linking;
        });
    });
});

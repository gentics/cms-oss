import { MOVE_DOWN_ACTION, MOVE_TO_TOP_ACTION, MOVE_UP_ACTION } from '../src/app/common/models/tables';
import {
    Construct,
    ConstructCategoryListResponse,
    ConstructCategorySortRequest,
    NodeMultiLinkRequest,
    NodePageLanguageCode,
    NodeUrlMode,
} from '@gentics/cms-models';
import {
    CONSTRUCT_TEST_SELECT_COLOR,
    ConstructCategoryImportData,
    EntityImporter,
    findTableAction,
    findTableRowById,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_NODE,
    LANGUAGE_DE,
    LANGUAGE_EN,
    loginWithForm,
    matchRequest,
    navigateToApp,
    NodeImportData,
    selectTab,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, Locator, test } from '@playwright/test';
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
    let testConstruct: Construct;

    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.describe('Constructs', () => {
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

            testConstruct = IMPORTER.get(CONSTRUCT_TEST_SELECT_COLOR);

            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);

            // Navigate to constructs module
            await navigateToModule(page, 'constructs');
        });

        test('should properly remove and assign the constructs to the node', async ({ page }) => {
            const row = findTableRowById(page, testConstruct.id);
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
                expect(linkRequestBody.ids).toEqual([parseInt(nodeId, 10)]);

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

    test.describe('Categories', () => {
        function createCategoryData(id: number): ConstructCategoryImportData {
            return {
                [IMPORT_TYPE]: 'construct-category',
                [IMPORT_ID]: `construct-category_${id}`,
                nameI18n: {
                    de: `Test Category ${id}`,
                    en: `Test Category ${id}`,
                },
            };
        }

        async function rowShouldHaveOrder(table: Locator, row: Locator, index: number): Promise<void> {
            // Text should be correct
            await expect(row.locator('.data-column[data-id="sortorder"]')).toHaveText(new RegExp(`\\s*${index + 1}\\s*`));
            // Actual position in the list should be correct
            // TODO: Actual position check, but all methods so far don't work, as changes to the
            // order or child-elements isn't properly getting updated in lookups. While they *do* correctly
            // change, all lookups will result in wrong results and therefore broken tests.
        }

        // One core, one from the devtools
        const DEFAULT_CATEGORIES_COUNT = 2;

        const CATEGORIES = [...Array(20).keys()]
            .map((idx) => createCategoryData(idx + 1));

        test.beforeEach(async ({ page, request, context }) => {
            await context.clearCookies();
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();

            // Clean and setup test data
            await IMPORTER.cleanupTest();
            await IMPORTER.syncPackages(TestSize.MINIMAL);
            await IMPORTER.importData(CATEGORIES);

            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);

            // Navigate to constructs module
            await navigateToModule(page, 'constructs');
            await selectTab(page.locator('gtx-construct-module-master > gtx-tabs'), 'categories');
        });

        test('should be possible to re-order the categories', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-18765',
            }],
        }, async ({ page }) => {
            const REORDER_UP_FROM_INDEX = 7;
            const REORDER_UP_TO_INDEX = 4;
            const REORDER_DOWN_FROM_INDEX = 10;
            const REORDER_DOWN_TO_INDEX = 15;
            const REORDER_TOP_INDEX = 19;

            const categoryLoad = page.waitForResponse(matchRequest('GET', '/rest/construct/category'));
            const master = page.locator('gtx-construct-category-master');
            const masterTable = master.locator('gtx-construct-category-table');
            await masterTable.locator('.entity-table-actions-bar [data-action="reorder"] button').click();

            const categoryResponse = await categoryLoad;
            const LOADED_CATEGORIES = (await categoryResponse.json() as ConstructCategoryListResponse).items;

            const REORDER_UP_ELEMENT = LOADED_CATEGORIES[REORDER_UP_FROM_INDEX];
            const REORDER_DOWN_ELEMENT = LOADED_CATEGORIES[REORDER_DOWN_FROM_INDEX];
            const REORDER_TOP_ELEMENT = LOADED_CATEGORIES[REORDER_TOP_INDEX];

            const modal = page.locator('gtx-construct-category-sort-modal');
            const sortTable = modal.locator('gtx-construct-category-table');

            // All categories should be listed
            await expect(sortTable.locator('.data-row')).toHaveCount(DEFAULT_CATEGORIES_COUNT + CATEGORIES.length);

            // Reorder the elements

            const upRow = sortTable.locator(`.data-row[data-id="${REORDER_UP_ELEMENT.id}"]`);
            await test.step('Move element up', async () => {
                for (let i = 0; i < REORDER_UP_FROM_INDEX - REORDER_UP_TO_INDEX; i++) {
                    await findTableAction(upRow, MOVE_UP_ACTION).click();
                }
                await rowShouldHaveOrder(masterTable, upRow, REORDER_UP_TO_INDEX);
            });

            const downRow = sortTable.locator(`.data-row[data-id="${REORDER_DOWN_ELEMENT.id}"]`);
            await test.step('Move element down', async () => {
                for (let i = 0; i < REORDER_DOWN_TO_INDEX - REORDER_DOWN_FROM_INDEX; i++) {
                    await findTableAction(downRow, MOVE_DOWN_ACTION).click();
                }
                await rowShouldHaveOrder(masterTable, downRow, REORDER_DOWN_TO_INDEX);
            });

            const topRow = sortTable.locator(`.data-row[data-id="${REORDER_TOP_ELEMENT.id}"]`);
            await test.step('Move element to top', async () => {
                await findTableAction(topRow, MOVE_TO_TOP_ACTION).click();
                await rowShouldHaveOrder(masterTable, topRow, 0);
            });

            // Other rows should have moved down now
            await rowShouldHaveOrder(masterTable, upRow, REORDER_UP_TO_INDEX + 1);
            await rowShouldHaveOrder(masterTable, downRow, REORDER_DOWN_TO_INDEX + 1);

            // Validate the request

            const EXPECTED_ORDER = [
                REORDER_TOP_ELEMENT,
                ...LOADED_CATEGORIES.slice(0, REORDER_UP_TO_INDEX),
                REORDER_UP_ELEMENT,
                ...LOADED_CATEGORIES.slice(REORDER_UP_TO_INDEX, REORDER_UP_FROM_INDEX),
                // UP START position
                ...LOADED_CATEGORIES.slice(REORDER_UP_FROM_INDEX + 1, REORDER_DOWN_FROM_INDEX),
                // DOWN START position
                ...LOADED_CATEGORIES.slice(REORDER_DOWN_FROM_INDEX + 1, REORDER_DOWN_TO_INDEX + 1),
                REORDER_DOWN_ELEMENT,
                ...LOADED_CATEGORIES.slice(REORDER_DOWN_TO_INDEX + 1, REORDER_TOP_INDEX),
                // TOP START position
                ...LOADED_CATEGORIES.slice(REORDER_TOP_INDEX + 1),
            ].map((cat) => `${cat.id}`);
            expect(EXPECTED_ORDER).toHaveLength(DEFAULT_CATEGORIES_COUNT + CATEGORIES.length);

            const updateReq = page.waitForRequest(matchRequest('POST', '/rest/construct/category/sortorder'));
            await modal.locator('.modal-footer [data-action="confirm"] button').click();
            const updateBody = (await updateReq).postDataJSON() as ConstructCategorySortRequest;

            expect(updateBody.ids).toEqual(EXPECTED_ORDER);
        });
    });
});

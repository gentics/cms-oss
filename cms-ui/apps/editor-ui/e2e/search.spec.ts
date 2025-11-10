import { Feature, Variant } from '@gentics/cms-models';
import {
    BASIC_TEMPLATE_ID,
    EntityImporter,
    FolderImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    isVariant,
    ITEM_TYPE_FOLDER,
    ITEM_TYPE_PAGE,
    loginWithForm,
    matchRequest,
    navigateToApp,
    NODE_MINIMAL,
    PageImportData,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, Locator, Page, test } from '@playwright/test';
import { AUTH } from './common';
import { addSearchChip, findItem, findList, selectNode, setChipOperator, setDateChipValue, setStringChipValue } from './helpers';

const FOLDER_TEST_ONE: FolderImportData = {
    [IMPORT_TYPE]: ITEM_TYPE_FOLDER,
    [IMPORT_ID]: 'folderTestLongName1',

    motherId: NODE_MINIMAL[IMPORT_ID],
    nodeId: NODE_MINIMAL[IMPORT_ID],

    name: 'A really unreasonably and nonsensical long folder name for testing purposes',
};
const FOLDER_TEST_TWO: FolderImportData = {
    [IMPORT_TYPE]: ITEM_TYPE_FOLDER,
    [IMPORT_ID]: 'folderTestLongName2',

    motherId: FOLDER_TEST_ONE[IMPORT_ID],
    nodeId: NODE_MINIMAL[IMPORT_ID],

    name: 'Again a really unreasonably long folder name that has even more text for testing purposes',
};
const FOLDER_TEST_THREE: FolderImportData = {
    [IMPORT_TYPE]: ITEM_TYPE_FOLDER,
    [IMPORT_ID]: 'folderTestLongName3',

    motherId: FOLDER_TEST_TWO[IMPORT_ID],
    nodeId: NODE_MINIMAL[IMPORT_ID],

    name: 'Another exceptionally long, boring, but yet important folder name for testing purposes',
};
const FOLDER_TEST_FOUR: FolderImportData = {
    [IMPORT_TYPE]: ITEM_TYPE_FOLDER,
    [IMPORT_ID]: 'folderTestLongName4',

    motherId: FOLDER_TEST_THREE[IMPORT_ID],
    nodeId: NODE_MINIMAL[IMPORT_ID],

    name: 'The last very long folder name for testing purposes as there should be enough examples by now',
};
const PAGE_TEST_LONG: PageImportData = {
    [IMPORT_TYPE]: ITEM_TYPE_PAGE,
    [IMPORT_ID]: 'pageVeryLongName',

    folderId: FOLDER_TEST_FOUR[IMPORT_ID],
    nodeId: NODE_MINIMAL[IMPORT_ID],

    pageName: 'Finally a page with an even longer and more unreasonable name for testing purposes',
    templateId: BASIC_TEMPLATE_ID,
};

test.describe('Search', () => {
    const IMPORTER = new EntityImporter();

    let searchBar: Locator;
    let searchInput: Locator;
    let searchButton: Locator;

    test.beforeAll(async ({ request }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
        });

        await test.step('Test Bootstrapping', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
        });
    });

    test.beforeEach(async ({ request, context }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await context.clearCookies();
            await IMPORTER.clearClient();
        });

        await test.step('Common Test Setup', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.setupTest(TestSize.MINIMAL);
            await IMPORTER.importData([
                FOLDER_TEST_ONE,
                FOLDER_TEST_TWO,
                FOLDER_TEST_THREE,
                FOLDER_TEST_FOUR,
                PAGE_TEST_LONG,
            ]);
        });
    });

    function setupBasicLocators(page: Page): void {
        searchBar = page.locator('gtx-top-bar .search-container chip-search-bar');
        searchInput = searchBar.locator('input.gtx-chipsearchbar-value');
        searchButton = searchBar.locator('.gtx-chipsearchbar-button-container .gtx-chipsearchbar-button gtx-button[data-action="search"] button');
    }

    // -------------------------------
    // via Gentics CMS (Elasticsearch disabled)
    // -------------------------------
    test.describe('via Gentics CMS', () => {
        test.beforeEach(async ({ page }) => {
            await IMPORTER.setupFeatures({
                [Feature.ELASTICSEARCH]: false,
            });

            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);
            setupBasicLocators(page);
        });

        test('searches for the requested term with search button', async ({ page }) => {
            const SEARCH_TERM = 'test';

            await searchInput.fill(SEARCH_TERM);

            const searchReq = page.waitForRequest(matchRequest('GET', '/rest/folder/getPages/*', {
                params: {
                    search: SEARCH_TERM,
                },
            }));

            await searchButton.click();

            await searchReq;
        });

        test('searches for the requested term with Enter', async ({ page }) => {
            const SEARCH_TERM = 'test';

            await searchInput.fill(SEARCH_TERM);

            const searchReq = page.waitForRequest(matchRequest('GET', '/rest/folder/getPages/*', {
                params: {
                    search: SEARCH_TERM,
                },
            }));

            await searchInput.press('Enter');

            await searchReq;
        });

        test('searches a string chip as expected', async ({ page }) => {
            // Matches Cypress: chip "filename" with value "one" becomes query param filename=*one*
            const CHIP_NAME = 'filename';
            const CHIP_VALUE = 'one';

            await test.step('Add search chip', async () => {
                const chip = await addSearchChip(searchBar, CHIP_NAME);
                await setStringChipValue(chip, CHIP_VALUE);
            });

            const searchReq = page.waitForRequest(matchRequest('GET', '/rest/folder/getPages/*', {
                params: {
                    [CHIP_NAME]: `%${CHIP_VALUE}%`,
                },
            }));

            await searchButton.click();

            await searchReq;
        });

        test('searches a date chip as expected (created AFTER)', async ({ page }) => {
            const CHIP_NAME = 'created';
            const chipDate = new Date();
            chipDate.setFullYear(chipDate.getFullYear(), chipDate.getMonth(), chipDate.getDate());
            const TIME = Math.floor(chipDate.getTime() / 1_000);

            await test.step('Add search chip', async () => {
                const chip = await addSearchChip(searchBar, CHIP_NAME);
                await setChipOperator(chip, 'AFTER');
                await setDateChipValue(chip, chipDate);
            });

            const searchReq = page.waitForRequest(matchRequest('GET', '/rest/folder/getPages/*'));

            await searchButton.click();

            const req = await searchReq;
            const url = new URL(req.url());
            const timeStamp = parseInt(url.searchParams.get(`${CHIP_NAME}since`) || '', 10);
            expect([0, 3_600]).toContainEqual(Math.abs(TIME - timeStamp));
        });

        test('search result breadcrumbs should be displayed correctly', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-19012',
            }],
        }, async ({ page }) => {
            const SEARCH_TERM = 'test';
            const SEARCH_ITEM = IMPORTER.get(PAGE_TEST_LONG);

            await searchInput.fill(SEARCH_TERM);
            const searchReq = page.waitForResponse(matchRequest('GET', '/rest/folder/getPages/*'));
            await searchButton.click();
            await searchReq;

            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, SEARCH_ITEM.id);
            const breadcrumbs = item.locator('item-breadcrumbs .item-breadcrumbs');
            const size = await breadcrumbs.evaluate(el => el.getBoundingClientRect());

            // Should be a max of 44px, i.E. two lines + a bit of buffer
            expect(size.height).toBeLessThanOrEqual(46);
        });
    });

    // -------------------------------
    // via ElasticSearch (Enterprise)
    // -------------------------------
    test.describe('via ElasticSearch', () => {
        test.skip(() => !isVariant(Variant.ENTERPRISE), 'Requires Enterpise features');

        test.beforeEach(async ({ page }) => {
            await IMPORTER.setupFeatures({
                [Feature.ELASTICSEARCH]: true,
            });

            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);
            setupBasicLocators(page);
        });

        test('searches for the requested term with search button', async ({ page }) => {
            const SEARCH_TERM = 'test';

            await searchInput.fill(SEARCH_TERM);

            const searchReq = page.waitForRequest(matchRequest('POST', '/rest/elastic/page/_search'));

            await searchButton.click();

            const req = await searchReq;
            const body = req.postDataJSON();

            expect(body.query.bool).toEqual({
                must: [
                    {
                        bool: {
                            should: [
                                {
                                    multi_match: {
                                        fields: ['name^2', 'path', 'description', 'content'],
                                        query: SEARCH_TERM,
                                    },
                                },
                                { wildcard: { niceUrl: { value: `*${SEARCH_TERM}*` } } },
                                { wildcard: { filename: { value: `*${SEARCH_TERM}*`, boost: 2 } } },
                                { wildcard: { 'name.raw': { value: `*${SEARCH_TERM}*`, boost: 2 } } },
                            ],
                        },
                    },
                ],
            });
        });

        test('searches for the requested term with Enter', async ({ page }) => {
            const SEARCH_TERM = 'test';

            await searchInput.fill(SEARCH_TERM);

            const searchReq = page.waitForRequest(matchRequest('POST', '/rest/elastic/page/_search'));

            await searchInput.press('Enter');

            const req = await searchReq;
            const body = req.postDataJSON();

            expect(body.query.bool).toEqual({
                must: [
                    {
                        bool: {
                            should: [
                                {
                                    multi_match: {
                                        fields: ['name^2', 'path', 'description', 'content'],
                                        query: SEARCH_TERM,
                                    },
                                },
                                { wildcard: { niceUrl: { value: `*${SEARCH_TERM}*` } } },
                                { wildcard: { filename: { value: `*${SEARCH_TERM}*`, boost: 2 } } },
                                { wildcard: { 'name.raw': { value: `*${SEARCH_TERM}*`, boost: 2 } } },
                            ],
                        },
                    },
                ],
            });
        });

        test('searches a string chip as expected', async ({ page }) => {
            const CHIP_NAME = 'filename';
            const CHIP_VALUE = 'one';

            await test.step('Add search chip', async () => {
                const chip = await addSearchChip(searchBar, CHIP_NAME);
                await setStringChipValue(chip, CHIP_VALUE);
            });

            const searchReq = page.waitForRequest(matchRequest('POST', '/rest/elastic/page/_search'));

            await searchButton.click();

            const req = await searchReq;
            const body = req.postDataJSON();

            expect(body.query.bool).toEqual({
                must: [{
                    wildcard: {
                        [CHIP_NAME]: {
                            value: `*${CHIP_VALUE}*`,
                        },
                    },
                }],
            });
        });

        test('searches a date chip as expected', async ({ page }) => {
            const CHIP_NAME = 'created';
            const chipDate = new Date();
            chipDate.setFullYear(chipDate.getFullYear(), chipDate.getMonth(), chipDate.getDate());

            await test.step('Add search chip', async () => {
                const chip = await addSearchChip(searchBar, CHIP_NAME);
                await setChipOperator(chip, 'AFTER');
                await setDateChipValue(chip, chipDate);
            });

            const searchReq = page.waitForRequest(matchRequest('POST', '/rest/elastic/page/_search'));

            await searchButton.click();

            const req = await searchReq;
            const body = req.postDataJSON();

            expect(body.query.bool).toEqual({
                must: [
                    {
                        range: {
                            [CHIP_NAME]: {
                                format: 'yyyy-MM-dd',
                                gte: chipDate.toISOString().substring(0, 10),
                            },
                        },
                    },
                ],
            });
        });
    });
});

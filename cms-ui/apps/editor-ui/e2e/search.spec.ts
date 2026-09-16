import { Feature, Response as CMSResponse, Variant } from '@gentics/cms-models';
import {
    BASIC_TEMPLATE_ID,
    dismissNotifications,
    EntityImporter,
    FolderImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    isVariant,
    ITEM_TYPE_FOLDER,
    ITEM_TYPE_PAGE, LANGUAGE_EN,
    loginWithForm,
    matchRequest,
    navigateToApp,
    NODE_MINIMAL,
    PageImportData,
    TestSize, wait, waitForResponseFrom,
} from '@gentics/e2e-utils';
import { expect, Locator, Page, test } from '@playwright/test';
import { AUTH } from './common';
import {
    addSearchChip,
    expectItemPublished,
    findItem,
    findList,
    itemAction,
    selectNode,
    setChipOperator,
    setDateChipValue,
    setStringChipValue,
} from './helpers';

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

    language: LANGUAGE_EN,
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
        });

        await test.step('Specialized Test Setup', async () => {
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
            await selectNode(page, IMPORTER.get(NODE_MINIMAL).id);
            setupBasicLocators(page);
        });

        test('searches for the requested term with search button', async ({ page }) => {
            const SEARCH_TERM = 'test';

            await searchInput.fill(SEARCH_TERM);

            const searchReq = waitForResponseFrom(page, 'GET', '/rest/folder/getPages/*', {
                params: {
                    search: SEARCH_TERM,
                },
            });

            await dismissNotifications(page);
            await searchButton.click();

            await searchReq;
        });

        test('searches for the requested term with Enter', async ({ page }) => {
            const SEARCH_TERM = 'test';

            await searchInput.fill(SEARCH_TERM);
            // Wait for angular to handle the data in the background
            await wait(1_000);

            const searchReq = waitForResponseFrom(page, 'GET', '/rest/folder/getPages/*', {
                params: {
                    search: SEARCH_TERM,
                },
            });

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

            const searchReq = waitForResponseFrom(page, 'GET', '/rest/folder/getPages/*', {
                params: {
                    [CHIP_NAME]: `%${CHIP_VALUE}%`,
                },
            });

            await dismissNotifications(page);
            await searchButton.click();

            await searchReq;
        });

        test('searches a date chip as expected (created AFTER)', async ({ page }) => {
            const CHIP_NAME = 'created';

            // `now` is an absolute instant, so its epoch (`TIME`) is correct regardless of
            // timezone. The date-picker widget however works with wall-clock digits
            // (year/month/day/hour/...), and the browser context's timezone is pinned to
            // `Europe/Vienna` (see playwright.config.ts), so it interprets whatever digits we
            // feed it as Vienna local time. To make the widget reconstruct the same instant as
            // `now`, we have to read those digits as they'd appear in Vienna - not in the test
            // runner host's own timezone, which may not match and previously caused this to be
            // flaky/wrong headless.
            const now = new Date();
            const TIME = Math.floor(now.getTime() / 1_000);

            const viennaParts = new Intl.DateTimeFormat('en-GB', {
                timeZone: 'Europe/Vienna',
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hourCycle: 'h23',
            }).formatToParts(now).reduce((parts, part) => {
                parts[part.type] = part.value;
                return parts;
            }, {} as Record<string, string>);

            const chipDate = new Date(
                parseInt(viennaParts.year, 10),
                parseInt(viennaParts.month, 10) - 1,
                parseInt(viennaParts.day, 10),
                parseInt(viennaParts.hour, 10),
                parseInt(viennaParts.minute, 10),
                parseInt(viennaParts.second, 10),
            );

            await test.step('Add search chip', async () => {
                const chip = await addSearchChip(searchBar, CHIP_NAME);
                await setChipOperator(chip, 'AFTER');
                await setDateChipValue(chip, chipDate);
            });

            const searchReq = waitForResponseFrom(page, 'GET', '/rest/folder/getPages/*');

            await dismissNotifications(page);
            await searchButton.click();

            const req = await searchReq;
            const url = new URL(req.request().url());
            const timeStamp = parseInt(url.searchParams.get(`${CHIP_NAME}since`) || '', 10);
            // A tiny (<=1s) jitter is expected: the widget re-derives its value from its own
            // clock as we interact with it, between when we captured `now` and when the value
            // actually gets submitted. This is unrelated to the timezone handling above - it's
            // just normal UI-interaction timing, so a wide tolerance would hide real regressions.
            expect(Math.abs(timeStamp - TIME)).toBeLessThanOrEqual(1);
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
            const searchReq = waitForResponseFrom(page, 'GET', '/rest/folder/getPages/*');
            await dismissNotifications(page);
            await searchButton.click();
            await searchReq;

            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, SEARCH_ITEM.id);
            const breadcrumbs = item.locator('item-breadcrumbs .item-breadcrumbs');
            const size = await breadcrumbs.evaluate((el) => el.getBoundingClientRect());

            // Should be a max of 44px, i.E. two lines + a bit of buffer
            expect(size.height).toBeLessThanOrEqual(46);
        });

        test('should handle actions on search results correctly', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-19117',
            }],
        }, async ({ page }) => {
            const SEARCH_TERM = 'test';
            const SEARCH_ITEM = IMPORTER.get(PAGE_TEST_LONG);

            await searchInput.fill(SEARCH_TERM);
            const searchReq = waitForResponseFrom(page, 'GET', '/rest/folder/getPages/*');
            await dismissNotifications(page);
            await searchButton.click();
            await searchReq;

            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, SEARCH_ITEM.id);

            const publishReq = waitForResponseFrom(page, 'POST', `/rest/page/publish/${SEARCH_ITEM.id}`, {
                skipStatus: true,
            });
            await itemAction(item, 'publish');
            const publishRes = await publishReq;
            const publishBody = await publishRes.json() as CMSResponse;

            const toasts = page.locator('gtx-toast');

            await page.waitForTimeout(500); // Allow for notifications to spawn
            await expect(toasts).toHaveCount(1);
            await expect(toasts.locator('.message')).toContainText(publishBody.messages[0].message);

            await expectItemPublished(item);
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
            await selectNode(page, IMPORTER.get(NODE_MINIMAL).id);
            setupBasicLocators(page);
        });

        test('searches for the requested term with search button', async ({ page }) => {
            const SEARCH_TERM = 'test';

            await searchInput.fill(SEARCH_TERM);

            const searchReq = waitForResponseFrom(page, 'POST', '/rest/elastic/page/_search');

            await dismissNotifications(page);
            await searchButton.click();

            const req = await searchReq;
            const body = req.request().postDataJSON();

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
            // Wait for angular to handle the data in the background
            await wait(1_000);

            const searchReq = waitForResponseFrom(page, 'POST', '/rest/elastic/page/_search');

            await searchInput.press('Enter');

            const req = await searchReq;
            const body = req.request().postDataJSON();

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

            const searchReq = waitForResponseFrom(page, 'POST', '/rest/elastic/page/_search');

            await dismissNotifications(page);
            await searchButton.click();

            const req = await searchReq;
            const body = req.request().postDataJSON();

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

            const searchReq = waitForResponseFrom(page, 'POST', '/rest/elastic/page/_search');

            await dismissNotifications(page);
            await searchButton.click();

            const req = await searchReq;
            const body = req.request().postDataJSON();

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

import { Feature, Variant } from '@gentics/cms-models';
import {
    EntityImporter,
    isVariant,
    loginWithForm,
    matchRequest,
    navigateToApp,
    NODE_MINIMAL,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, Locator, Page, test } from '@playwright/test';
import { AUTH } from './common';
import { addSearchChip, selectNode, setChipOperator, setDateChipValue, setStringChipValue } from './helpers';

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
            const TIME = Math.floor(chipDate.getTime() / 1000);

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
            expect(Math.abs(TIME - timeStamp)).toBeLessThanOrEqual(1000);
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

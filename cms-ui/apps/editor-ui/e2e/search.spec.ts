import { Feature, Variant } from '@gentics/cms-models';
import { test, expect } from '@playwright/test';
import {
    EntityImporter,
    TestSize,
    loginWithForm,
    minimalNode,
    navigateToApp,
    isVariant,
} from '@gentics/e2e-utils';
import { AUTH } from './common';
import { selectNode } from './helpers';

// Optional: if you gate ES tests by env/variant, keep a flag:
const IS_ENTERPRISE =
    process.env.GCN_VARIANT === 'enterprise' ||
    isVariant?.(Variant.ENTERPRISE) === true;

test.describe('Search', () => {
    const IMPORTER = new EntityImporter();

    const selectors = {
        searchBar: 'gtx-top-bar .search-container chip-search-bar',
        searchInput: 'input.gtx-chipsearchbar-value',
        searchButton:
            '.gtx-chipsearchbar-button-container .gtx-chipsearchbar-button gtx-button[data-action="search"]',
    searchChipButton:
            '.gtx-chipsearchbar-button-container .gtx-chipsearchbar-button gtx-button button[data-action="primary"]',
    };

    // --- Global bootstrap like the Cypress 'before' ---
    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    // --- Per test setup like Cypress 'beforeEach' ---
    test.beforeEach(async ({ page, request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.setupTest(TestSize.MINIMAL);
    });

    // -------------------------------
    // via Gentics CMS (Elasticsearch disabled)
    // -------------------------------
    test.describe('via Gentics CMS', () => {
        test.beforeAll(async ({ request }) => {
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
            await IMPORTER.cleanupTest();
            await IMPORTER.setupFeatures({
                [Feature.ELASTICSEARCH]: false,
            });
        });

        test.beforeEach(async ({ page }) => {
            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, IMPORTER.get(minimalNode)!.id);
        });

        test('searches for the requested term with search button', async ({ page }) => {
            const SEARCH_TERM = 'test';

            const waitReq = page.waitForRequest((req) => {
                if (!req.url().includes('/rest/folder/getPages/')) return false;
                const url = new URL(req.url());
                return url.searchParams.get('search') === SEARCH_TERM;
            });

            await page.locator(selectors.searchBar).locator(selectors.searchInput).fill(SEARCH_TERM);
            await page.locator(selectors.searchBar).locator(selectors.searchButton).click();

            await waitReq; // parity with cy.wait(alias)
        });

        test('searches for the requested term with Enter', async ({ page }) => {
            const SEARCH_TERM = 'test';

            const waitReq = page.waitForRequest((req) => {
                if (!req.url().includes('/rest/folder/getPages/')) return false;
                const url = new URL(req.url());
                return url.searchParams.get('search') === SEARCH_TERM;
            });

            await page.locator(selectors.searchBar).locator(selectors.searchInput).fill(SEARCH_TERM);
            await page.locator(selectors.searchBar).locator(selectors.searchInput).press('Enter');

            await waitReq;
        });

        test('searches a string chip as expected', async ({ page }) => {
            // Matches Cypress: chip "filename" with value "one" becomes query param filename=*one*
            const CHIP_NAME = 'filename';
            const CHIP_VALUE = 'one';
            const CHIP_OPERATOR = '';

            const waitReq = page.waitForRequest((req) => {
                if (!req.url().includes('/rest/folder/getPages/')) return false;
                const url = new URL(req.url());
                return url.searchParams.get(CHIP_NAME) === `%${CHIP_VALUE}%`;
            });

            // If you have chip helpers, replace this inline sequence with them
            const bar = page.locator(selectors.searchBar);
            // Open chip editor
            await bar.getByRole('button', { name: 'filter_list'}).click();
            // Choose the chip
            await page.locator(`.custom-content-menu .custom-content-menu-button[data-value="${CHIP_NAME}"]`).click();
            // Set the chip value
            await bar.locator('input.gtx-chip-input-value-inner-string').fill(CHIP_VALUE);
            await bar.locator(selectors.searchButton).click();

            await waitReq;
        });

        test('searches a date chip as expected (created AFTER)', async ({ page }) => {
            const CHIP_NAME = 'created';
            const chipDate = new Date();
            chipDate.setFullYear(chipDate.getFullYear(), chipDate.getMonth(), chipDate.getDate());
            const TIME = Math.floor(chipDate.getTime() / 1000);

            const waitReq = page.waitForRequest((req) => {
                if (!req.url().includes('/rest/folder/getPages/')) return false;
                const url = new URL(req.url());
                const ts = parseInt(url.searchParams.get(`${CHIP_NAME}since`) || '', 10);
                if (!Number.isInteger(ts)) return false;
                const diff = Math.abs(TIME - ts);
                return diff <= 1000;
            });

            const bar = page.locator(selectors.searchBar);
            // As above: prefer a helper if you have one. Here we emulate a typed chip:
            // "created>YYYY-MM-DD" is app-dependent; adjust to your actual chip UI.
            // Open chip editor
            await bar.getByRole('button', { name: 'filter_list'}).click();
            // Choose the chip
            await page.locator(`.custom-content-menu .custom-content-menu-button[data-value="${CHIP_NAME}"]`).click();
	    // Choose the chip operator
	    await bar.getByRole('button', { name: 'am arrow_drop_down' }).click();
	    await page.locator('.custom-content-menu .custom-content-menu-button[data-value="AFTER"]').click();
            // Set the chip value
            await bar.locator('.gtx-chip-input-value-inner-date').click();
            await page.locator('gtx-date-time-picker-modal gtx-button[data-action="confirm"]').click();
            await bar.locator(selectors.searchButton).click();

            await waitReq;
        });
    });

    // -------------------------------
    // via ElasticSearch (Enterprise)
    // -------------------------------
    const es = IS_ENTERPRISE ? test.describe : test.describe.skip;

    es('via ElasticSearch', () => {
        test.beforeAll(async ({ request }) => {
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
            await IMPORTER.cleanupTest();
            await IMPORTER.setupFeatures({
                [Feature.ELASTICSEARCH]: true,
            });
        });

        test.beforeEach(async ({ page }) => {
            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, IMPORTER.get(minimalNode)!.id);
        });

        test('searches for the requested term with search button', async ({ page }) => {
            const SEARCH_TERM = 'test';

            const waitReq = page.waitForRequest((req) => {
                if (!(req.url().includes('/rest/elastic/page/_search') && req.method() === 'POST')) return false;
                try {
                    const body = req.postDataJSON();
                    const expected = {
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
                    };
                    return JSON.stringify(body.query?.bool) === JSON.stringify(expected);
                } catch {
                    return false;
                }
            });

            await page.locator(selectors.searchBar).locator(selectors.searchInput).fill(SEARCH_TERM);
            await page.locator(selectors.searchBar).locator(selectors.searchButton).click();

            await waitReq;
        });

        test('searches for the requested term with Enter', async ({ page }) => {
            const SEARCH_TERM = 'test';

            const waitReq = page.waitForRequest((req) => {
                if (!(req.url().includes('/rest/elastic/page/_search') && req.method() === 'POST')) return false;
                try {
                    const body = req.postDataJSON();
                    const expected = {
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
                    };
                    return JSON.stringify(body.query?.bool) === JSON.stringify(expected);
                } catch {
                    return false;
                }
            });

            await page.locator(selectors.searchBar).locator(selectors.searchInput).fill(SEARCH_TERM);
            await page.locator(selectors.searchBar).locator(selectors.searchInput).press('Enter');

            await waitReq;
        });

        test('searches a string chip as expected', async ({ page }) => {
            const CHIP_NAME = 'filename';
            const CHIP_VALUE = 'one';
            const CHIP_OPERATOR = '';

            const waitReq = page.waitForRequest((req) => {
                if (!(req.url().includes('/rest/elastic/page/_search') && req.method() === 'POST')) return false;
                try {
                    const body = req.postDataJSON();
                    const expected = {
                        must: [{ wildcard: { [CHIP_NAME]: { value: `*${CHIP_VALUE}*` } } }],
                    };
                    return JSON.stringify(body.query?.bool) === JSON.stringify(expected);
                } catch {
                    return false;
                }
            });

            // If you have chip helpers, replace this inline sequence with them
            const bar = page.locator(selectors.searchBar);
            // Open chip editor
            await bar.getByRole('button', { name: 'filter_list'}).click();
            // Choose the chip
            await page.locator(`.custom-content-menu .custom-content-menu-button[data-value="${CHIP_NAME}"]`).click();
            // Set the chip value
            await bar.locator('input.gtx-chip-input-value-inner-string').fill(CHIP_VALUE);
            await bar.locator(selectors.searchButton).click();

            await waitReq;
        });

        test('searches a date chip as expected', async ({ page }) => {
            const CHIP_NAME = 'created';
            const chipDate = new Date();
            chipDate.setFullYear(chipDate.getFullYear(), chipDate.getMonth(), chipDate.getDate());

            const waitReq = page.waitForRequest((req) => {
                if (!(req.url().includes('/rest/elastic/page/_search') && req.method() === 'POST')) return false;
                try {
                    const body = req.postDataJSON();
                    const expected = {
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
                    };
                    return JSON.stringify(body.query?.bool) === JSON.stringify(expected);
                } catch {
                    return false;
                }
            });
            const bar = page.locator(selectors.searchBar);
            // Open chip editor
            await bar.getByRole('button', { name: 'filter_list'}).click();
            // Choose the chip
            await page.locator(`.custom-content-menu .custom-content-menu-button[data-value="${CHIP_NAME}"]`).click();
	    // Choose the chip operator
	    await bar.getByRole('button', { name: 'am arrow_drop_down' }).click();
	    await page.locator('.custom-content-menu .custom-content-menu-button[data-value="AFTER"]').click();
            // Set the chip value
            await bar.locator('.gtx-chip-input-value-inner-date').click();
            await page.locator('gtx-date-time-picker-modal gtx-button[data-action="confirm"]').click();
            await bar.locator(selectors.searchButton).click();

            await waitReq;
        });
    });
});

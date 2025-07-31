import { PageCopyResponse } from '@gentics/cms-models';
import {
    EntityImporter,
    fullNode,
    loginWithForm,
    matchRequest,
    navigateToApp,
    pageTwentyfour,
    pickSelectValue,
    setupUserDataRerouting,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, Locator, test } from '@playwright/test';
import { AUTH } from './common';
import { findItem, findList, itemAction, selectNode } from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('Page Management', () => {

    const IMPORTER = new EntityImporter();

    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);

        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.bootstrapSuite(TestSize.FULL);
    });

    test.beforeEach(async ({ page, request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);

        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.setupTest(TestSize.FULL);

        // Pagination data may be loaded from the server stored user-data
        // This will simply return empty data for the user
        await setupUserDataRerouting(page);

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(fullNode)!.id);
    });

    test('should be possible to change the pagination display size', async ({ page }) => {
        const list = findList(page, 'page');

        async function waitForListItems(): Promise<Locator> {
            // Wait for initial items to be loaded
            const body = list.locator('.list-body');
            await body.waitFor({ state: 'visible' });
            return list.locator('item-list-row');
        }

        let items = await waitForListItems();
        const pageSize = list.locator('gtx-page-size-selector gtx-select');

        // Default is 10, and full set size has more, so we should have a full page visible
        await expect(items).toHaveCount(10);

        // Update the page size to be higher to show all items
        await pickSelectValue(pageSize, '25');
        items = await waitForListItems();

        // Total are 30 items, but page size still too low
        await expect(items).toHaveCount(25);

        // Update the page size to be higher to show all items
        await pickSelectValue(pageSize, '100');
        items = await waitForListItems();

        // Now all items should be displayed
        await expect(items).toHaveCount(30);
    });

    test('should reload the page correctly when copying a page', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-18792',
        }],
    }, async ({ page }) => {
        const PAGE_TO_COPY = IMPORTER.get(pageTwentyfour)!;
        const list = findList(page, 'page');

        async function waitForListItems(): Promise<Locator> {
            // Wait for initial items to be loaded
            const body = list.locator('.list-body');
            await body.waitFor({ state: 'visible' });
            return list.locator('item-list-row');
        }

        await waitForListItems();
        // Update the page size to be higher to show all items
        const pageSize = list.locator('gtx-page-size-selector gtx-select');
        await pickSelectValue(pageSize, '100');
        await waitForListItems();

        // Copy the page
        await itemAction(findItem(list, PAGE_TO_COPY.id), 'copy');
        const copyReq = page.waitForResponse(matchRequest('POST', '/rest/page/copy'));
        await page.click('repository-browser .modal-footer [data-action="confirm"]');
        const res = await copyReq;

        // Find the newly copied page
        const data: PageCopyResponse = await res.json();
        await findItem(list, data.pages[0].id).waitFor();

        // Verify that the pagination hasn't been reset and is still displaying all items
        expect(list.locator('item-list-row')).toHaveCount(31);
    });
});

import { Node, PageCopyResponse } from '@gentics/cms-models';
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
import { expect, Locator, Page, test } from '@playwright/test';
import { AUTH } from './common';
import { findItem, findList, itemAction, selectNode } from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe.only('List Loading', () => {

    const IMPORTER = new EntityImporter();
    let ACTIVE_NODE: Node<Raw>;

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
        ACTIVE_NODE = IMPORTER.get(fullNode);

        // Pagination data may be loaded from the server stored user-data
        // This will simply return empty data for the user
        await setupUserDataRerouting(page);

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(fullNode)!.id);
    });

    async function waitForListItems(page: Page, list: Locator, actor?: () => Promise<any>): Promise<Locator> {
        let req: Promise<any> = Promise.resolve();
        if (actor) {
            req = page.waitForResponse(matchRequest('GET', `/rest/folder/getPages/${ACTIVE_NODE.folderId}`));
            await actor();
        }

        await req;
        // Wait for initial items to be loaded
        const body = list.locator('.list-body');
        await body.waitFor({ state: 'visible' });
        return list.locator('item-list-row');
    }

    test('should be possible to change the pagination display size', async ({ page }) => {
        const list = findList(page, 'page');

        let items = await waitForListItems(page, list);
        const pageSize = list.locator('gtx-page-size-selector gtx-select');

        // Default is 10, and full set size has more, so we should have a full page visible
        await expect(items).toHaveCount(10);

        // Update the page size to be higher to show all items
        items = await waitForListItems(page, list, async () => {
            await pickSelectValue(pageSize, '25');
        });

        // Total are 30 items, but page size still too low
        await expect(items).toHaveCount(25);

        // Update the page size to be higher to show all items
        items = await waitForListItems(page, list, async () => {
            await pickSelectValue(pageSize, '100');
        });

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

        await waitForListItems(page, list);
        // Update the page size to be higher to show all items
        const pageSize = list.locator('gtx-page-size-selector gtx-select');

        await waitForListItems(page, list, async () => {
            await pickSelectValue(pageSize, '100');
        });

        // Copy the page
        await itemAction(findItem(list, PAGE_TO_COPY.id), 'copy');
        const copyReq = page.waitForResponse(matchRequest('POST', '/rest/page/copy'));
        const listReq = page.waitForResponse(matchRequest('GET', `/rest/folder/getPages/${PAGE_TO_COPY.folderId}`));
        await page.click('repository-browser .modal-footer [data-action="confirm"]');

        const [copyRes, listRes] = await Promise.all([
            copyReq,
            listReq,
        ]);

        // Expect to request 100 items, since we changed the pagination
        const listUrl = new URL(listRes.request().url());
        expect(listUrl.searchParams.get('maxItems')).toBe('100');

        // Find the newly copied page
        const copyResData: PageCopyResponse = await copyRes.json();
        await findItem(list, copyResData.pages[0].id).waitFor();

        // Verify that the pagination hasn't been reset and is still displaying all items
        expect(list.locator('item-list-row')).toHaveCount(31, { timeout: 10_000 });
    });
});

import { AccessControlledType, GcmsPermission, Node, PageCopyResponse, User } from '@gentics/cms-models';
import {
    createClient,
    EntityImporter,
    FOLDER_T,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    ITEM_TYPE_PAGE,
    LoginInformation,
    loginWithForm,
    matchRequest,
    navigateToApp,
    NODE_FULL,
    PAGE_FOUR,
    PAGE_TWENTYFOUR,
    pickSelectValue,
    setupUserDataRerouting,
    TestSize,
    UserImportData,
} from '@gentics/e2e-utils';
import { expect, Locator, Page, test } from '@playwright/test';
import { AUTH } from './common';
import { findItem, findList, itemAction, selectNode } from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('List Loading', () => {

    const IMPORTER = new EntityImporter();
    let activeNode: Node;

    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);

        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.bootstrapSuite(TestSize.FULL);
    });

    test.beforeEach(async ({ request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);

        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.setupTest(TestSize.FULL);
        activeNode = IMPORTER.get(NODE_FULL);
    });

    test.describe('User Settings', () => {
        test.beforeEach(async ({ page }) => {
            // Pagination data may be loaded from the server stored user-data
            // This will simply return empty data for the user
            await setupUserDataRerouting(page);

            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, IMPORTER.get(NODE_FULL)!.id);
        });

        async function waitForListItems(page: Page, list: Locator, actor?: () => Promise<any>): Promise<Locator> {
            let listType = await list.getAttribute('data-item-type');
            listType = listType[0].toUpperCase() + listType.slice(1);

            let req: Promise<any> = Promise.resolve();
            if (actor) {
                req = page.waitForResponse(matchRequest('GET', `/rest/folder/get${listType}s/${activeNode.folderId}`));
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
            const PAGE_TO_COPY = IMPORTER.get(PAGE_TWENTYFOUR)!;
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
            await page.click('repository-browser .modal-footer [data-action="confirm"] button');

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
            await expect(list.locator('item-list-row')).toHaveCount(31, { timeout: 10_000 });
        });

        test('should load the saved page-size on reload', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-18850',
            }],
        }, async ({ page }) => {
            const FOLDER_TO_FIND = IMPORTER.get(FOLDER_T);

            const list = findList(page, 'folder');

            await waitForListItems(page, list);

            // Shouldn't be here yet
            await expect(findItem(list, FOLDER_TO_FIND.id)).not.toBeAttached();

            // Update the page size to 100
            const pageSize = list.locator('gtx-page-size-selector gtx-select');
            await waitForListItems(page, list, async () => {
                await pickSelectValue(pageSize, '100');
            });

            // Now it should be visible
            await expect(findItem(list, FOLDER_TO_FIND.id)).toBeAttached();

            // Reload the whole app and verify that the state has properly restored and the
            // folder is still visible.
            page.reload();
            await waitForListItems(page, list);
            await expect(findItem(list, FOLDER_TO_FIND.id)).toBeAttached();
        });
    });

    test.describe('Missing User Information', () => {

        test.beforeEach(async ({ page }) => {
            // Making sure we always have a pagination of 100, so we can verify that
            // the rendering is done properly
            await setupUserDataRerouting(page, () => ({
                pageItemsPerPage: 100,
            }));
        });

        async function createTempUser(handler: (user: User, auth: LoginInformation) => Promise<void>): Promise<void> {
            const TEST_GROUP: GroupImportData = {
                [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
                [IMPORT_ID]: 'group_list-loading_temp',

                description: 'List Loading: Temp',
                name: 'list-loading_temp',

                permissions: [
                    {
                        type: AccessControlledType.NODE,
                        instanceId: `${activeNode.folderId}`,
                        subObjects: true,
                        perms: [
                            { type: GcmsPermission.READ, value: true },
                            { type: GcmsPermission.READ_ITEMS, value: true },
                            { type: GcmsPermission.UPDATE_ITEMS, value: true },
                            { type: GcmsPermission.READ_TEMPLATES, value: true },
                            { type: GcmsPermission.PUBLISH_PAGES, value: true },
                        ],
                    },
                ],
            };

            const TEST_USER: UserImportData = {
                [IMPORT_TYPE]: IMPORT_TYPE_USER,
                [IMPORT_ID]: 'user_list-loading_temp',
                login: 'list-loading_temp',
                password: 'something',
                email: 'mail@example.com',
                firstName: 'List Loading',
                lastName: 'Temp',
                group: TEST_GROUP,
            };

            await IMPORTER.importData([TEST_GROUP, TEST_USER]);
            const user = IMPORTER.get(TEST_USER);

            await handler(user, {
                username: TEST_USER.login,
                password: TEST_USER.password,
            });

            await IMPORTER.client.user.delete(user.id).send();
        }

        test('should load Pages correctly with missing publisher', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-19014',
            }],
        }, async ({ page, context }) => {
            const TEST_PAGE = IMPORTER.get(PAGE_FOUR);

            // Publish the page now as temp user which will be deleted
            await createTempUser(async (user, auth) => {
                const client = await createClient({
                    context: context.request,
                    autoLogin: auth,
                });
                await client.page.publish(TEST_PAGE.id, {
                    alllang: true,
                }).send();
            });

            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, activeNode.id);

            const list = findList(page, ITEM_TYPE_PAGE);
            await expect(list.locator('item-list-row')).toHaveCount(30, { timeout: 10_000 });
            const items = await list.locator('item-list-row').all();
            for (const item of items) {
                await expect(item).toBeVisible();
            }
        });

        test('should load Pages correctly with missing future publisher', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-19014',
            }],
        }, async ({ page, context }) => {
            const TEST_PAGE = IMPORTER.get(PAGE_FOUR);

            // Publish the page now as temp user which will be deleted
            await createTempUser(async (user, auth) => {
                const client = await createClient({
                    context: context.request,
                    autoLogin: auth,
                });
                await client.page.publish(TEST_PAGE.id, {
                    alllang: false,
                    // Now + 2h
                    at: (new Date().getTime() / 1000) + 7_200,
                    keepVersion: false,
                }).send();
            });

            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, activeNode.id);

            const list = findList(page, ITEM_TYPE_PAGE);
            await expect(list.locator('item-list-row')).toHaveCount(30, { timeout: 10_000 });
            const items = await list.locator('item-list-row').all();
            for (const item of items) {
                await expect(item).toBeVisible();
            }
        });
    });
});

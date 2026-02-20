import { TAB_ID_CONSTRUCTS } from '@gentics/cms-integration-api-models';
import {
    AccessControlledType,
    BackgroundJobResponse,
    Page as CMSPage,
    Feature,
    GcmsPermission,
    LocalizationType,
    Node,
    NodeFeature,
    NodePageLanguageCode,
    NodeUrlMode,
    PageLocalizeRequest,
    PageResponse,
    ResponseCode,
    Variant,
} from '@gentics/cms-models';
import {
    BASIC_TEMPLATE_ID,
    clickModalAction,
    EntityImporter,
    findTableAction,
    findTableRowById,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_NODE,
    IMPORT_TYPE_USER,
    isVariant,
    ITEM_TYPE_PAGE,
    loginWithForm,
    matchesUrl,
    matchRequest,
    navigateToApp,
    NODE_FULL,
    NodeImportData,
    openContext,
    PAGE_EIGHT,
    PAGE_ELEVEN,
    PAGE_FIVE,
    PAGE_FOUR,
    PAGE_NINE,
    pickSelectValue,
    TestSize,
    UserImportData,
    wait,
    waitForResponseFrom,
} from '@gentics/e2e-utils';
import { cloneWithSymbols } from '@gentics/ui-core/utils/clone-with-symbols';
import { expect, Locator, test } from '@playwright/test';
import { AUTH } from './common';
import { findItem, findList, getAlohaIFrame, getEditorToolbarContext, itemAction, selectNode, setupHelperWindowFunctions } from './helpers';

test.describe('Multichannelling', () => {
    test.skip(() => !isVariant(Variant.ENTERPRISE), 'Requires Enterpise features');

    const IMPORTER = new EntityImporter();

    const CHANNEL_IMPORT_DATA: NodeImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_NODE,
        [IMPORT_ID]: 'channelNode',

        node: {
            name: 'Channel',
            masterId: NODE_FULL[IMPORT_ID],
            publishDir: '',
            binaryPublishDir: '',
            pubDirSegment: true,
            publishImageVariants: false,
            host: 'channel.localhost',
            publishFs: false,
            publishFsPages: false,
            publishFsFiles: false,
            publishContentMap: true,
            publishContentMapPages: true,
            publishContentMapFiles: true,
            publishContentMapFolders: true,
            urlRenderWayPages: NodeUrlMode.AUTOMATIC,
            urlRenderWayFiles: NodeUrlMode.AUTOMATIC,
            omitPageExtension: false,
            pageLanguageCode: NodePageLanguageCode.FILENAME,
            meshPreviewUrlProperty: '',
        },
        description: 'channel of "full"',

        languages: [],
        templates: [],
    };

    const NAMESPACE = 'multichannelling';

    const TEST_GROUP_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_base`,

        name: `${NAMESPACE}_base`,
        description: 'Multichannelling: Base',
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_base`,

        email: 'test@example.com',
        firstName: 'Multichannelling',
        lastName: 'User',
        login: `${NAMESPACE}_user`,
        password: 'multichannellingtest123',

        group: TEST_GROUP_BASE,
    };

    let masterNode: Node;
    let channelNode: Node;

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
        await IMPORTER.setupFeatures({
            [Feature.MULTICHANNELLING]: true,
        });
        await IMPORTER.setupTest(TestSize.FULL);
        await IMPORTER.syncTag(BASIC_TEMPLATE_ID, 'content');
        await IMPORTER.importData([CHANNEL_IMPORT_DATA]);

        masterNode = IMPORTER.get(NODE_FULL);
        channelNode = IMPORTER.get(CHANNEL_IMPORT_DATA);

        const TEST_GROUP = cloneWithSymbols(TEST_GROUP_BASE);
        TEST_GROUP.permissions = [
            {
                type: AccessControlledType.NODE,
                instanceId: `${masterNode.folderId}`,
                perms: [
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.PUBLISH_PAGES, value: true },
                    { type: GcmsPermission.DELETE_ITEMS, value: true },
                ],
            },
        ];

        await IMPORTER.importData([TEST_GROUP, TEST_USER]);
    });

    test.describe('Edit Mode', () => {
        let editor: Locator;

        test.beforeEach(async ({ page }) => {
            await setupHelperWindowFunctions(page);
            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, IMPORTER.get(NODE_FULL).id);

            // Setup page for editing
            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, IMPORTER.get(PAGE_FIVE).id);
            await itemAction(item, 'edit');

            // Wait for editor to be ready
            const iframe = await getAlohaIFrame(page);
            editor = iframe.locator('main .GENTICS_tagname_content[contenteditable="true"]');
            await editor.waitFor({ timeout: 60_000 });
        });

        test('should handle node IDs for overview items using sticky channels', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-18873',
            }],
        }, async ({ page }) => {
            const MASTER_PAGE = IMPORTER.get(PAGE_EIGHT);
            const CHANNEL_PAGE = IMPORTER.get(PAGE_ELEVEN);
            const EXTRA_CHANNEL_PAGE = IMPORTER.get(PAGE_NINE);

            let editButton: Locator;

            await test.step('Tag Setup', async () => {
                // Select correct editor tab
                const tabs = page.locator('content-frame gtx-page-editor-tabs');
                await tabs.locator(`[data-id="${TAB_ID_CONSTRUCTS}"]`).click();

                // Focus the editable and clear the content
                await editor.click();
                await editor.clear();

                // Insert the overview tag
                const category = page.locator('content-frame .editor-toolbar gtx-construct-controls .construct-category[data-id="2"]');
                const categoryContent = await openContext(category);
                await categoryContent.locator('.construct-element[data-keyword="test_overview"]').click();

                // Edit the tag
                const tag = editor.locator('.aloha-block');
                editButton = tag.locator('.gcn-construct-button-edit');
                await editButton.click();
            });

            let modal: Locator;
            let overview: Locator;

            await test.step('Overview Setup', async () => {
                modal = page.locator('gtx-tag-editor-modal');
                overview = modal.locator('overview-tag-property-editor');

                // Setup Overview
                await pickSelectValue(overview.locator('gtx-select[data-control="listType"]'), 'PAGE');
                await pickSelectValue(overview.locator('gtx-select[data-control="selectType"]'), 'MANUAL');
                await overview.locator('gtx-button[data-action="browse-items"] button').click();
            });

            // Repo Browser
            await test.step('Repository Browser', async () => {
                const repoBrowser = page.locator('repository-browser');

                // Select the master page
                await repoBrowser.locator(`repository-browser-list[data-type="page"] [data-id="${MASTER_PAGE.id}"] .item-checkbox label`).click();

                // Switch to channel and select the channel page
                await selectNode(repoBrowser, channelNode.id);
                await repoBrowser.locator(`repository-browser-list[data-type="page"] [data-id="${CHANNEL_PAGE.id}"] .item-checkbox label`).click();

                // Close repo-browser and the modal
                await repoBrowser.locator('.modal-footer [data-action="confirm"] button').click();
                await modal.locator('.footer [data-action="confirm"] button').click();
            });

            await test.step('Validate Overview loading', async () => {
                // Open Overview again
                await editButton.click();

                const masterItem = overview.locator(`.overview-items .overview-item[data-type="page"][data-id="${MASTER_PAGE.id}"]`);
                const channelItem = overview.locator(`.overview-items .overview-item[data-type="page"][data-id="${CHANNEL_PAGE.id}"]`);

                await expect(masterItem).toHaveAttribute('data-node-id', `${IMPORTER.get(NODE_FULL).id}`);
                await expect(masterItem.locator('.item-path')).toHaveText(`${MASTER_PAGE.path}${MASTER_PAGE.name}`);

                await expect(channelItem).toHaveAttribute('data-node-id', `${channelNode.id}`);
                await expect(channelItem.locator('.item-path')).toHaveText(`/${channelNode.name}/${CHANNEL_PAGE.name}`);
            });

            // Select another channel item
            await test.step('Repository Browser round two', async () => {
                await overview.locator('gtx-button[data-action="browse-items"] button').click();
                const repoBrowser = page.locator('repository-browser');

                // Switch to channel and select the channel page
                await selectNode(repoBrowser, channelNode.id);
                await repoBrowser.locator(`repository-browser-list[data-type="page"] [data-id="${EXTRA_CHANNEL_PAGE.id}"] .item-checkbox label`).click();

                // Close repo-browser and the modal
                await repoBrowser.locator('.modal-footer [data-action="confirm"] button').click();
                await modal.locator('.footer [data-action="confirm"] button').click();
            });

            await test.step('Validate Overview loading round two', async () => {
                // Open Overview again
                await editButton.click();

                const masterItem = overview.locator(`.overview-items .overview-item[data-type="page"][data-id="${MASTER_PAGE.id}"]`);
                const channelItem = overview.locator(`.overview-items .overview-item[data-type="page"][data-id="${CHANNEL_PAGE.id}"]`);
                const extraItem = overview.locator(`.overview-items .overview-item[data-type="page"][data-id="${EXTRA_CHANNEL_PAGE.id}"]`);

                await expect(masterItem).toHaveAttribute('data-node-id', `${IMPORTER.get(NODE_FULL).id}`);
                await expect(masterItem.locator('.item-path')).toHaveText(`${MASTER_PAGE.path}${MASTER_PAGE.name}`);

                await expect(channelItem).toHaveAttribute('data-node-id', `${channelNode.id}`);
                await expect(channelItem.locator('.item-path')).toHaveText(`/${channelNode.name}/${CHANNEL_PAGE.name}`);

                await expect(extraItem).toHaveAttribute('data-node-id', `${channelNode.id}`);
                await expect(extraItem.locator('.item-path')).toHaveText(`/${channelNode.name}/${EXTRA_CHANNEL_PAGE.name}`);
            });
        });
    });

    test.describe('Publishing', () => {
        test.beforeEach(async ({ page }) => {
            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, channelNode.id);
        });

        test('should not show publish actions for inherited objects', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-19051',
            }],
        }, async ({ page }) => {
            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, IMPORTER.get(PAGE_FOUR).id);
            const context = await openContext(item.locator('[data-action="item-context"]'));

            await expect(context.locator('[data-action="publish"]')).toBeHidden();
            await expect(context.locator('[data-action="publish-variants"]')).toBeHidden();
        });
    });

    test.describe('Partial Localizing', () => {
        let testPage: CMSPage;

        test.beforeEach(async ({ page }) => {
            // Enable the feature
            await IMPORTER.client.node.activateFeature(IMPORTER.get(NODE_FULL).id, NodeFeature.PARTIAL_MULTICHANNELLING).send();
            await IMPORTER.client.node.activateFeature(channelNode.id, NodeFeature.PARTIAL_MULTICHANNELLING).send();

            testPage = IMPORTER.get(PAGE_FOUR);

            await navigateToApp(page);
            await loginWithForm(page, TEST_USER);
            await selectNode(page, channelNode.id);
        });

        test('should be possible to localize a page partially', {
            annotation: [{
                type: 'issue',
                description: 'GPU-2072',
            }],
        }, async ({ page }) => {
            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, testPage.id);
            await itemAction(item, 'localize');

            const localizeReq = page.waitForRequest(matchRequest('POST', `/rest/page/localize/${testPage.id}`));
            const loadReq = page.waitForResponse(matchRequest('GET', `/rest/page/load/${testPage.id}`));
            const modal = page.locator('gtx-modal-dialog');
            await clickModalAction(modal, 'partial');

            await test.step('Validate localize request', async () => {
                const req = await localizeReq;
                const reqData: PageLocalizeRequest = req.postDataJSON();
                expect(reqData.channelId).toEqual(channelNode.id);
                expect(reqData.localizationType).toEqual(LocalizationType.PARTIAL);
            });

            await test.step('Validate page load', async () => {
                const req = await loadReq;
                const res: PageResponse = await req.json();
                expect(res.page.localizationType).toEqual(LocalizationType.PARTIAL);
            });
        });

        test('should be able to manage tag-inheritance via tag-list', {
            annotation: [{
                type: 'ticket',
                description: 'GPU-2072',
            }],
        }, async ({ page }) => {
            let testTag = testPage.tags.title;

            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, testPage.id);
            await itemAction(item, 'localize');

            const modal = page.locator('gtx-modal-dialog');
            const pageLoadReq = page.waitForResponse(matchRequest('GET', `/rest/page/load/${testPage.id}`));
            await clickModalAction(modal, 'partial');
            const pageLoadRes = await pageLoadReq;
            testPage = (await pageLoadRes.json() as PageResponse).page;

            const editorCtx = getEditorToolbarContext(page);
            const ctx = await openContext(editorCtx);
            await ctx.locator('[data-action="edit-properties"]').click();

            const contentFrame = page.locator('content-frame');
            const editor = contentFrame.locator('combined-properties-editor');
            await editor.locator('.properties-tabs .tab-link[data-id="item-tag-list"]').click();

            const table = editor.locator('gtx-table');

            await test.step('Localize Tag', async () => {
                const titleRow = await findTableRowById(table, testTag.id);
                await expect(titleRow).toBeVisible();

                const localizeReq = page.waitForResponse(matchRequest('POST', `/rest/page/localize/${testPage.id}/tags/${testTag.name}`));
                const loadReq = page.waitForResponse(matchRequest('GET', `/rest/page/load/${testPage.id}`));

                await findTableAction(titleRow, 'localize-tag').click();

                await localizeReq;
                const loadRes = await loadReq;
                const resData: PageResponse = await loadRes.json();
                testTag = resData.page.tags[testTag.name];
                expect(testTag.inherited).toBe(false);
            });

            await test.step('Unlocalize Tag', async () => {
                const titleRow = await findTableRowById(table, testTag.id);
                await expect(titleRow).toBeVisible();

                const unlocalizeReq = page.waitForResponse(matchRequest('POST', `/rest/page/unlocalize/${testPage.id}/tags/${testTag.name}`));
                const loadReq = page.waitForResponse(matchRequest('GET', '/rest/page/load/*'));

                await findTableAction(titleRow, 'delete-tag-localization').click();

                await unlocalizeReq;
                const loadRes = await loadReq;
                const resData: PageResponse = await loadRes.json();
                expect(resData.page.tags[testTag.name].inherited).toBe(true);
            });
        });
    });

    test.describe('Localization', () => {
        test.beforeEach(async ({ page }) => {
            await navigateToApp(page);
            await loginWithForm(page, TEST_USER);
            await selectNode(page, channelNode.id);
        });

        test('should handle a localization correctly', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-19214',
            }],
        }, async ({ page }) => {
            const TEST_PAGE = IMPORTER.get(PAGE_FOUR);
            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, TEST_PAGE.id);

            const localizeReq = waitForResponseFrom(page, 'POST', `/rest/page/localize/${TEST_PAGE.id}`);
            await itemAction(item, 'localize');
            const localizeRes = await localizeReq;
            const localizeBody = await localizeRes.json() as BackgroundJobResponse;

            await wait(500);
            const notifications = page.locator('gtx-toast');
            expect(await notifications.all()).toHaveLength(1);
            await expect(notifications.locator('.message')).toContainText(localizeBody.messages[0].message);

            await expect(item.locator('.item-primary .localized-icon')).not.toBeVisible();
        });

        test('should handle a background localization correctly', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-19214',
            }],
        }, async ({ page }) => {
            const TEST_PAGE = IMPORTER.get(PAGE_FOUR);
            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, TEST_PAGE.id);
            const backgroundMessage = "Your job 'mocked' needs longer to finish. It is now running in background. You will be informed when it is finished.";

            await page.route(url => matchesUrl(url, `/rest/page/localize/${TEST_PAGE.id}`), async (route, req) => {
                if (req.method() !== 'POST') {
                    return route.continue();
                }

                // Wait a bit, and then return with a mocked background job
                await wait(2_000);
                return route.fulfill({
                    status: 200,
                    json: {
                        messages: [{
                            type: 'INFO',
                            timestamp: new Date().getTime(),
                            message: backgroundMessage,
                        }],
                        responseInfo: {
                            responseCode: ResponseCode.OK,
                            responseMessage: backgroundMessage,
                        },
                        inBackground: true,
                    } as BackgroundJobResponse,
                });
            });

            const localizeReq = waitForResponseFrom(page, 'POST', `/rest/page/localize/${TEST_PAGE.id}`);
            await itemAction(item, 'localize');
            await localizeReq;

            await wait(500);
            const notifications = page.locator('gtx-toast');
            expect(await notifications.all()).toHaveLength(1);
            await expect(notifications.locator('.message')).toContainText(backgroundMessage);

            await expect(item.locator('.item-primary .inherited-icon')).toBeVisible();
        });
    });
});

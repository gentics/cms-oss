import { TAB_ID_CONSTRUCTS } from '@gentics/cms-integration-api-models';
import { Feature, MultiUnlocalizeRequest, Node, NodePageLanguageCode, NodeUrlMode, Page, PageListResponse, Variant } from '@gentics/cms-models';
import {
    BASIC_TEMPLATE_ID,
    EntityImporter,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_NODE,
    isVariant,
    ITEM_TYPE_PAGE,
    loginWithForm,
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
} from '@gentics/e2e-utils';
import { expect, Locator, test } from '@playwright/test';
import { AUTH } from './common';
import {
    expectItemInherited,
    expectItemLocalized,
    findItem,
    findList,
    getAlohaIFrame,
    itemAction,
    selectNode,
    setupHelperWindowFunctions,
} from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('Multichannelling', () => {
    test.skip(() => !isVariant(Variant.ENTERPRISE), 'Requires Enterpise features');

    const IMPORTER = new EntityImporter();
    const CHANNEL_IMPORT_DATA: NodeImportData = {

        [IMPORT_TYPE]: IMPORT_TYPE_NODE,
        [IMPORT_ID]: 'channelNode',

        node: {
            name : 'Channel',
            masterId: NODE_FULL[IMPORT_ID],
            publishDir : '',
            binaryPublishDir : '',
            pubDirSegment : true,
            publishImageVariants : false,
            host : 'channel.localhost',
            publishFs : false,
            publishFsPages : false,
            publishFsFiles : false,
            publishContentMap : true,
            publishContentMapPages : true,
            publishContentMapFiles : true,
            publishContentMapFolders : true,
            urlRenderWayPages: NodeUrlMode.AUTOMATIC,
            urlRenderWayFiles: NodeUrlMode.AUTOMATIC,
            omitPageExtension : false,
            pageLanguageCode : NodePageLanguageCode.FILENAME,
            meshPreviewUrlProperty : '',
        },
        description: 'channel of "full"',

        languages : [],
        templates: [],
    };
    let channelNode: Node;

    test.beforeAll(async ({request}) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
        });

        await test.step('Test Bootstrapping', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.bootstrapSuite(TestSize.FULL);
        });
    });

    test.beforeEach(async ({page, request, context}) => {
        await test.step('Client Setup', async () => {
            await context.clearCookies();
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
        });

        await test.step('Common Test Setup', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.setupFeatures({
                [Feature.MULTICHANNELLING]: true,
            });
            await IMPORTER.setupTest(TestSize.FULL);
        });

        await test.step('Specialized Test Setup', async () => {
            await IMPORTER.syncTag(BASIC_TEMPLATE_ID, 'content');
            await IMPORTER.importData([CHANNEL_IMPORT_DATA]);
            await setupHelperWindowFunctions(page);
        });

        channelNode = IMPORTER.get(CHANNEL_IMPORT_DATA);

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
    });

    test.describe('Edit Mode', () => {
        let editor: Locator;

        test.beforeEach(async ({ page }) => {
            // Setup page for editing
            await selectNode(page, IMPORTER.get(NODE_FULL).id);
            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, IMPORTER.get(PAGE_FIVE).id);
            await itemAction(item, 'edit');

            // Wait for editor to be ready
            const iframe = await getAlohaIFrame(page);
            editor = iframe.locator('main .GENTICS_tagname_content[contenteditable="true"]');
            await editor.waitFor({timeout: 60_000});
        });

        test('should handle node IDs for overview items using sticky channels', {
            annotation: [{
                type: 'issue',
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
            await test.step('Repository Browser', async() => {
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
            await test.step('Repository Browser round two', async() => {
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

    test.describe('List', () => {
        test.beforeEach(async ({page}) => {
            await selectNode(page, channelNode.id);
        });

        test('should not show publish actions for inherited objects', {
            annotation: [{
                type: 'issue',
                description: 'SUP-19051',
            }],
        }, async ({ page }) => {
            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, IMPORTER.get(PAGE_FOUR).id);
            const context = await openContext(item.locator('[data-action="item-context"]'));

            await expect(context.locator('[data-action="publish"]')).toBeHidden();
            await expect(context.locator('[data-action="publish-variants"]')).toBeHidden();
        });

        test('Localizing and delocalizing should update the list correctly', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-19201',
            }],
        }, async ({ page }) => {
            const TEST_PAGE = IMPORTER.get(PAGE_FOUR);
            const list = findList(page, ITEM_TYPE_PAGE);
            const row = findItem(list, TEST_PAGE.id);

            let channelPage: Page;
            let channelRow: Locator;

            await test.step('Localize a page', async () => {
                const localizeReq = page.waitForResponse(matchRequest('POST', `/rest/page/localize/${TEST_PAGE.id}`));

                await expectItemInherited(row);
                await itemAction(row, 'localize');
                await localizeReq;
            });

            await test.step('Verify localization', async () => {
                const loadReq = page.waitForResponse(matchRequest('GET', `/rest/folder/getPages/${channelNode.folderId}`));
                const loadRes = await (await loadReq).json() as PageListResponse;
                channelPage = loadRes.pages.find(loadedPage => {
                    return loadedPage.channelSetId === TEST_PAGE.channelSetId
                        && loadedPage.contentSetId === TEST_PAGE.contentSetId;
                });
                channelRow = findItem(list, channelPage.id);

                await expectItemLocalized(channelRow);
            });

            await test.step('Delocalizing the page', async () => {
                await itemAction(channelRow, 'delete-localized');

                const modal = page.locator('multi-delete-modal-modal');
                const delocalizeReq = page.waitForResponse(matchRequest('POST', '/rest/page/unlocalize'));
                await modal.locator('.modal-footer [data-action="confirm"] button').click();
                const delocalizeData = (await delocalizeReq).request().postDataJSON() as MultiUnlocalizeRequest;
                expect(delocalizeData.ids).toEqual([channelPage.id]);
                await page.waitForResponse(matchRequest('GET', `/rest/folder/getPages/${channelNode.folderId}`));

                await expectItemInherited(row);
            });
        });
    });
});

import {
    BASIC_TEMPLATE_ID,
    EntityImporter,
    fullNode, IMPORT_ID, IMPORT_TYPE, IMPORT_TYPE_NODE,
    ITEM_TYPE_PAGE, LANGUAGE_DE, LANGUAGE_EN,
    loginWithForm,
    navigateToApp, NodeImportData, openContext, pageFullOne,
    pageOne, pickSelectValue,
    TestSize,
} from '@gentics/e2e-utils';
import {Locator, test} from '@playwright/test';
import {AUTH,} from './common';
import {findItem, findList, getAlohaIFrame, itemAction, selectNode, setupHelperWindowFunctions,} from './helpers';
import {NodePageLanguageCode, NodeUrlMode, Node, Feature} from "@gentics/cms-models";
import {TAB_ID_CONSTRUCTS} from "@gentics/cms-integration-api-models";

test.describe.configure({ mode: 'serial' });
test.describe('Multichannelling', () => {
    // Mark this suite as slow - Because it is
    test.slow();

    const IMPORTER = new EntityImporter();
    const CHANNEL_IMPORT_DATA: NodeImportData = {

        [IMPORT_TYPE]: IMPORT_TYPE_NODE,
        [IMPORT_ID]: 'channelNode',

        node: {
            name : 'Channel',
            masterId: fullNode[IMPORT_ID],
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
        IMPORTER.setApiContext(request);

        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.bootstrapSuite(TestSize.FULL);
    });

    test.beforeEach(async ({page, request, context}) => {
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

        channelNode = IMPORTER.get(CHANNEL_IMPORT_DATA)!;

        await setupHelperWindowFunctions(page);
        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, channelNode.id);
    });

    test.describe('Edit Mode', () => {
        let editor: Locator;

        test.beforeEach(async ({page}) => {
            // Setup page for editing
            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, IMPORTER.get(pageFullOne).id);
            await itemAction(item, 'edit');

            // Wait for editor to be ready
            const iframe = await getAlohaIFrame(page);
            editor = iframe.locator('main [contenteditable="true"]');
            await editor.waitFor({timeout: 60_000});
        });

        test('should handle node IDs for overview items using sticky channels', {
            annotation: [
                {
                    type: 'issue',
                    description: 'SUP-18873'
                }
            ]
        }, async ({ page }) => {
            const tabs = page.locator('content-frame gtx-page-editor-tabs');
            await tabs.locator(`[data-id="${TAB_ID_CONSTRUCTS}"]`).click();

            await editor.click();
            await editor.clear();

            const category = page.locator('content-frame .editor-toolbar gtx-construct-controls .construct-category[data-id="2"]');
            const categoryContent = await openContext(category);

            await categoryContent.locator('.construct-element[data-keyword="test_overview"]').click();

            const tag = editor.locator('.aloha-block');
            const editButton = tag.locator('.gcn-construct-button-edit');

            await editButton.click();

            const modal = page.locator('gtx-tag-editor-modal');
            const overview = modal.locator('overview-tag-property-editor');

            await pickSelectValue(overview.locator('gtx-select[data-control="listType"]'), 'PAGE');
            await pickSelectValue(overview.locator('gtx-select[data-control="selectType"]'), 'MANUAL');
        });
    });
});

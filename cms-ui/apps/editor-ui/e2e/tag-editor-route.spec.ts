import { TagEditorChangeMessage } from "@gentics/cms-integration-api-models";
import { AccessControlledType, Construct, GcmsPermission, Node, OverviewTagPartProperty, Template } from "@gentics/cms-models";
import {
    BASIC_TEMPLATE_ID,
    clickModalAction,
    CONSTRUCT_TEST_OVERVIEW,
    createClient,
    EntityImporter,
    FOLDER_A,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    loginWithForm,
    navigateToApp,
    NODE_MINIMAL,
    pickSelectValue,
    TestSize,
    UserImportData,
    wait
} from "@gentics/e2e-utils";
import { expect, test } from "@playwright/test";
import { findRepoBrowserItem, findRepoBrowserList, selectItem, selectNode } from "./helpers";

test.describe('Tag Editor Route', () => {

    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'tageditor-route';
    let NODE: Node;

    const TEST_GROUP_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_base`,

        name: `${NAMESPACE}_base`,
        description: 'Tag Edtior Route: Base',
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_base`,

        email: 'test@example.com',
        firstName: 'Tag Editor Route',
        lastName: 'User',
        login: `${NAMESPACE}_user`,
        password: 'hellotageditor',

        group: TEST_GROUP_BASE,
    };

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
            await IMPORTER.syncPackages(TestSize.MINIMAL);
        });

        NODE = IMPORTER.get(NODE_MINIMAL);

        await test.step('Test User Setup', async () => {
            // We have to assemble the group permissions here, because we
            // need the imported data refs.
            // Create a copy, so we have a clean base object
            const TEST_GROUP = {...TEST_GROUP_BASE};

            TEST_GROUP.permissions = [
                {
                    type: AccessControlledType.NODE,
                    instanceId: `${NODE.folderId}`,
                    subObjects: true,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                        { type: GcmsPermission.READ_ITEMS, value: true },
                        { type: GcmsPermission.UPDATE_ITEMS, value: true },
                        { type: GcmsPermission.PUBLISH_PAGES, value: true },
                        { type: GcmsPermission.READ_TEMPLATES, value: true },
                        { type: GcmsPermission.UPDATE_TEMPLATES, value: true },
                    ],
                },
                {
                    type: AccessControlledType.CONTENT,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                    ],
                },
            ];

            await IMPORTER.importData([
                TEST_GROUP,
                TEST_USER,
            ]);
        });
    });

    test('template overview-tag', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19640',
        }]
    }, async ({ page }) => {
        const TEMPLATE: Template = IMPORTER.get(BASIC_TEMPLATE_ID);
        const CONSTRUCT: Construct = IMPORTER.get(CONSTRUCT_TEST_OVERVIEW);
        const FOLDER = IMPORTER.get(FOLDER_A);
        const OVERVIEW_TAG_NAME = 'foobar123';

        await test.step('Test Template Setup', async () => {
            // Add a new tag to the template for our testing purpose
            await IMPORTER.client.template.update(TEMPLATE.id, {
                unlock: true,
                template: {
                    templateTags: {
                        ...TEMPLATE.templateTags,
                        [OVERVIEW_TAG_NAME]: {
                            name: OVERVIEW_TAG_NAME,
                            active: true,
                            constructId: CONSTRUCT.id,
                            editableInPage: false,
                            mandatory: false,
                        } as any,
                    },
                },
            }).send();
        });

        await navigateToApp(page, `/tag-editor/${NODE.id}/template/${TEMPLATE.id}/${OVERVIEW_TAG_NAME}`);
        await loginWithForm(page, TEST_USER);

        await expect(page.locator('tag-editor-host')).toBeAttached();

        const messages: TagEditorChangeMessage[] = [];
        page.exposeFunction('_logMessage', (msg: TagEditorChangeMessage) => {
            messages.push(msg);
        })

        page.evaluate(() => {
            window.addEventListener('message', (event: MessageEvent<TagEditorChangeMessage>) => {
                // Ignore invalid messages
                if (
                    event.data == null
                    || typeof event.data !== 'object'
                    || event.data.type !== 'tag-editor-change'
                ) {
                    return;
                }

                const { type: _, ...msg } = event.data;
                (window as any)._logMessage(msg);
            });
        });

        const editor = page.locator('tag-editor-host overview-tag-property-editor');

        await test.step('Set settings for overview', async () => {
            await pickSelectValue(editor.locator('[data-control="listType"]'), 'FOLDER');

            // Validate that the message has been sent
            await wait(100);
            expect(messages).toHaveLength(1);
            let props = messages[0].tag.properties.overview as OverviewTagPartProperty;
            expect(props.overview.listType).toEqual('FOLDER');
            expect(props.overview.selectType).toEqual('UNDEFINED');

            await pickSelectValue(editor.locator('[data-control="selectType"]'), 'MANUAL');

            // Validate that the message has been sent
            await wait(100);
            expect(messages).toHaveLength(2);
            props = messages[1].tag.properties.overview as OverviewTagPartProperty;
            expect(props.overview.listType).toEqual('FOLDER');
            expect(props.overview.selectType).toEqual('MANUAL');
        });

        await test.step('Select item in Repo-Browser', async () => {
            await editor.locator('[data-action="browse-items"]').click();
            const repoBrowser = page.locator('repository-browser');

            await selectNode(repoBrowser.locator('repository-browser-breadcrumb'), NODE.id);
            const folderRepoList = findRepoBrowserList(repoBrowser, 'folder');
            const folderItem = findRepoBrowserItem(folderRepoList, FOLDER.id);
            await selectItem(folderItem);

            await clickModalAction(repoBrowser, 'confirm');
            await repoBrowser.waitFor({ state: 'detached' });
        });

        // Folder should be correctly selected
        const overviewItem = editor.locator('.overview-items .overview-item').first();
        await expect(overviewItem).toBeAttached();
        await expect(overviewItem).toHaveAttribute('data-type', 'folder');
        await expect(overviewItem).toHaveAttribute('data-id', `${FOLDER.id}`);
        await expect(overviewItem).toHaveAttribute('data-node-id', `${NODE.id}`);

        // Validate the last event as well now
        expect(messages).toHaveLength(3);
        const overviewProps = messages[2].tag.properties.overview as OverviewTagPartProperty;
        expect(overviewProps.overview.listType).toEqual('FOLDER');
        expect(overviewProps.overview.selectType).toEqual('MANUAL');
        expect(overviewProps.overview.selectedNodeItemIds).toHaveLength(1);
        expect(overviewProps.overview.selectedNodeItemIds[0]).toEqual({
            objectId: FOLDER.id,
            nodeId: NODE.id,
        });
    });
});

import { TagEditorChangeMessage } from "@gentics/cms-integration-api-models";
import {
    AccessControlledType,
    CmsFormTagPartProperty,
    Construct,
    Form,
    GcmsPermission,
    Node,
    NodeFeature,
    OverviewTagPartProperty,
    TagPropertyType,
    Template,
    Variant,
} from "@gentics/cms-models";
import {
    BASIC_TEMPLATE_ID,
    clickModalAction,
    CONSTRUCT_TEST_FORM,
    CONSTRUCT_TEST_OVERVIEW,
    createClient,
    EntityImporter,
    FOLDER_A,
    FORM_ONE,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    isVariant,
    ITEM_TYPE_FORM,
    loginWithForm,
    navigateToApp,
    NODE_MINIMAL,
    pickSelectValue,
    TestSize,
    UserImportData,
    wait
} from "@gentics/e2e-utils";
import { expect, Page, test } from "@playwright/test";
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
                        { type: GcmsPermission.VIEW_FORM, value: true },
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

    /**
     * Collects all `tag-editor-change` messages which the tag-editor route posts to the window.
     */
    async function collectTagEditorMessages(page: Page): Promise<TagEditorChangeMessage[]> {
        const messages: TagEditorChangeMessage[] = [];
        await page.exposeFunction('logTagEditorMessage', (msg: TagEditorChangeMessage) => {
            messages.push(msg);
        });

        await page.evaluate(() => {
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
                (window as any).logTagEditorMessage(msg);
            });
        });

        return messages;
    }

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

    test.describe('form-tag', () => {
        test.skip(() => !isVariant(Variant.ENTERPRISE), 'Requires Enterpise features');

        const FORM_TYPE_GENERIC = 'generic';
        const FORM_TAG_NAME = 'formTag';
        const FORM_PART_KEYWORD = 'form';

        let FORM: Form;
        let TEMPLATE: Template;

        test.beforeEach(async () => {
            await test.step('Form Setup', async () => {
                await IMPORTER.setupFeatures(TestSize.MINIMAL, {
                    [NodeFeature.FORMS]: true,
                });
                await IMPORTER.client.form.assignConfiguration(FORM_TYPE_GENERIC, NODE.id).send();
                await IMPORTER.importData([FORM_ONE]);
                FORM = IMPORTER.get(FORM_ONE);
            });

            await test.step('Template with empty Form-Tag Setup', async () => {
                const FORM_CONSTRUCT: Construct = IMPORTER.get(CONSTRUCT_TEST_FORM);
                TEMPLATE = IMPORTER.get(BASIC_TEMPLATE_ID);

                // Add a new form-tag to the template for our testing purpose
                await IMPORTER.client.template.update(TEMPLATE.id, {
                    unlock: true,
                    template: {
                        templateTags: {
                            ...TEMPLATE.templateTags,
                            [FORM_TAG_NAME]: {
                                name: FORM_TAG_NAME,
                                active: true,
                                constructId: FORM_CONSTRUCT.id,
                                editableInPage: false,
                                mandatory: false,
                            } as any,
                        },
                    },
                }).send();
            });
        });

        test('should only allow picking forms from the current node', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-20309',
            }],
        }, async ({ page }) => {
            await navigateToApp(page, `/tag-editor/${NODE.id}/template/${TEMPLATE.id}/${FORM_TAG_NAME}`);
            await loginWithForm(page, TEST_USER);

            await expect(page.locator('tag-editor-host')).toBeAttached();
            const messages = await collectTagEditorMessages(page);

            const editor = page.locator('tag-editor-host form-tag-property-editor');
            const browseBox = editor.locator('gtx-browse-box');
            await expect(browseBox.locator('[data-action="browse"]')).toBeEnabled();
            await expect(browseBox.locator('.display-value')).toHaveAttribute('data-value', '');

            const repoBrowser = page.locator('repository-browser');

            await test.step('Repo-Browser is restricted to the current node', async () => {
                await browseBox.locator('[data-action="browse"]').click();
                await expect(repoBrowser).toBeVisible();

                const breadcrumb = repoBrowser.locator('repository-browser-breadcrumb');
                await expect(breadcrumb.locator('gtx-breadcrumbs')).toBeVisible();
                // The node-selector must not be available, as the user should not be able to switch the node
                await expect(breadcrumb.locator('node-selector')).not.toBeAttached();
                // Breadcrumbs should start in the node of the template
                await expect(breadcrumb.locator('gtx-breadcrumbs')).toContainText(NODE.name);
            });

            await test.step('Select form of the current node', async () => {
                const formList = findRepoBrowserList(repoBrowser, ITEM_TYPE_FORM);
                const formItem = findRepoBrowserItem(formList, FORM.id);
                await expect(formItem).toBeVisible();
                await selectItem(formItem);

                await clickModalAction(repoBrowser, 'confirm');
                await repoBrowser.waitFor({ state: 'detached' });
            });

            await test.step('Form is correctly selected', async () => {
                await expect(browseBox.locator('.display-value')).toHaveText(FORM.name);
                await expect(browseBox.locator('.display-value')).toHaveAttribute('data-value', new RegExp(`^form:${FORM.id}(:\\d+)?$`));

                await expect.poll(() => messages.length).toBeGreaterThanOrEqual(1);
                const lastMessage = messages[messages.length - 1];
                const formProp = lastMessage.tag.properties[FORM_PART_KEYWORD] as CmsFormTagPartProperty;
                expect(formProp.type).toEqual(TagPropertyType.CMSFORM);
                expect(formProp.formId).toEqual(FORM.id);
            });
        });
    });
});

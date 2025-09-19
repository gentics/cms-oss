import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import {
    EntityImporter,
    findNotification,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    loginWithForm,
    logout,
    matchRequest,
    minimalNode,
    navigateToApp,
    openSidebar,
    pageOne,
    rootGroup,
    selectTab,
    TestSize,
    UserImportData,
} from '@gentics/e2e-utils';
import { randomId } from '@gentics/ui-core/utils/random-id';
import test from '@playwright/test';
import { editorAction, findItem, findList, itemAction, openToolOrAction, selectNode } from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('Publish Queue', () => {

    const IMPORTER = new EntityImporter();

    const TEST_GROUP_EDITOR_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: 'group_pubqueue_editor',

        parentId: rootGroup[IMPORT_ID],

        description: 'PubQueue: Editor',
        name: 'pubqueue_editor',
        permissions: [],
    };

    const TEST_GROUP_ASSIGNER_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: 'group_pubqueue_assigner',

        parentId: rootGroup[IMPORT_ID],

        description: 'PubQueue: Assigner',
        name: 'pubqueue_assigner',
        permissions: [],
    };

    const TEST_GROUP_PUBLISHER_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: 'group_pubqueue_publisher',

        parentId: rootGroup[IMPORT_ID],

        description: 'PubQueue: Publisher',
        name: 'pubqueue_publisher',
        permissions: [],
    };

    const TEST_USER_EDITOR: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: 'user_pubqueue_editor',

        groupId: TEST_GROUP_EDITOR_BASE[IMPORT_ID],

        email: 'something@example.com',
        firstName: 'PubQueue',
        lastName: 'Editor',
        login: 'pubqueue_editor',
        password: 'test',
    };

    const TEST_USER_ASSIGNER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: 'user_pubqueue_assigner',

        groupId: TEST_GROUP_ASSIGNER_BASE[IMPORT_ID],
        // Has to be in a higher or same group as others to see them
        // Therefore we put them into the root group, so they can see
        // the publisher user.
        extraGroups: [
            rootGroup[IMPORT_ID],
        ],

        email: 'something@example.com',
        firstName: 'PubQueue',
        lastName: 'Assigner',
        login: 'pubqueue_assigner',
        password: 'test',
    };

    const TEST_USER_PUBLISHER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: 'user_pubqueue_publisher',

        groupId: TEST_GROUP_PUBLISHER_BASE[IMPORT_ID],

        email: 'something@example.com',
        firstName: 'PubQueue',
        lastName: 'Publisher',
        login: 'pubqueue_publisher',
        password: 'test',
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
        });

        await test.step('Specialized Test Setup', async () => {
            // We have to assemble the group permissions here, because we
            // need the imported data refs.
            const NODE = IMPORTER.get(minimalNode);
            // Create a copy, so we have a clean base object
            const TEST_GROUP_EDITOR = {...TEST_GROUP_EDITOR_BASE};
            const TEST_GROUP_PUBLISHER = {...TEST_GROUP_PUBLISHER_BASE};
            const TEST_GROUP_ASSIGNER = {...TEST_GROUP_ASSIGNER_BASE};

            TEST_GROUP_EDITOR.permissions = [
                {
                    type: AccessControlledType.INBOX,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                    ],
                },
                {
                    type: AccessControlledType.NODE,
                    instanceId: `${NODE.folderId}`,
                    // subObjects: true,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                        { type: GcmsPermission.READ_ITEMS, value: true },
                        { type: GcmsPermission.UPDATE_ITEMS, value: true },
                        // FIXME: SUP-19039, should be removed once fixed.
                        { type: GcmsPermission.READ_TEMPLATES, value: true },
                    ],
                },
            ];

            TEST_GROUP_ASSIGNER.permissions = [
                {
                    type: AccessControlledType.PUBLISH_QUEUE,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                    ],
                },
                {
                    type: AccessControlledType.ADMIN,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                    ],
                },
                {
                    type: AccessControlledType.USER_ADMIN,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                    ],
                },
                {
                    type: AccessControlledType.NODE,
                    instanceId: `${NODE.folderId}`,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                        { type: GcmsPermission.READ_ITEMS, value: true },
                        { type: GcmsPermission.UPDATE_ITEMS, value: true },
                        { type: GcmsPermission.PUBLISH_PAGES, value: true },
                    ],
                },
            ];

            TEST_GROUP_PUBLISHER.permissions = [
                {
                    type: AccessControlledType.PUBLISH_QUEUE,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                    ],
                },
                {
                    type: AccessControlledType.NODE,
                    instanceId: `${NODE.folderId}`,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                        { type: GcmsPermission.UPDATE_FOLDER, value: true },
                        { type: GcmsPermission.READ_ITEMS, value: true },
                        { type: GcmsPermission.UPDATE_ITEMS, value: true },
                        { type: GcmsPermission.PUBLISH_PAGES, value: true },
                    ],
                },
            ];

            await IMPORTER.importData([
                rootGroup,
                TEST_GROUP_EDITOR,
                TEST_GROUP_ASSIGNER,
                TEST_GROUP_PUBLISHER,
                TEST_USER_EDITOR,
                TEST_USER_ASSIGNER,
                TEST_USER_PUBLISHER,
            ]);
        });
    });

    test('should put page in publish queue, assign it back', async ({ page }) => {
        test.slow();

        const PAGE_TO_PUBLISH = IMPORTER.get(pageOne);
        const TEST_MESSAGE = `Publish ${randomId()}`;
        const EDITOR_USER = IMPORTER.get(TEST_USER_EDITOR);

        await navigateToApp(page, '/');

        await test.step('Edit and Publish as editor user', async () => {
            await loginWithForm(page, {
                username: TEST_USER_EDITOR.login,
                password: TEST_USER_EDITOR.password,
            });
            await selectNode(page, IMPORTER.get(minimalNode).id);

            // Open the properties
            const pageList = findList(page, 'page');
            const pageRow = findItem(pageList, PAGE_TO_PUBLISH.id);
            await itemAction(pageRow, 'properties');

            // Update the page
            const form = page.locator('content-frame combined-properties-editor .properties-content gtx-page-properties');
            await form.locator('[formcontrolname="name"] input').fill('Some other page name');
            await editorAction(page, 'save');

            // Get the notification and click to publish the page
            const publishNotif = findNotification(page, `page-save-success-with-publish:${PAGE_TO_PUBLISH.id}`);
            await publishNotif.locator('.action button').click();

            // Logout for the next user
            await logout(page);
        });

        await test.step('Assign the review back to editor user', async () => {
            await loginWithForm(page, {
                username: TEST_USER_ASSIGNER.login,
                password: TEST_USER_ASSIGNER.password,
            });

            await openToolOrAction(page, 'publish-queue');
            const publishQueueModal = page.locator('publish-queue');
            // Select the first/only element
            await publishQueueModal.locator('publish-queue-list .row-checkbox label').click();

            const userLoad = page.waitForResponse(matchRequest('GET', '/rest/user'));
            await publishQueueModal.locator('.modal-footer [data-action="assign"]').click();
            await userLoad;

            const assignModal = page.locator('gtx-assign-page-modal');
            await assignModal.locator('.modal-content [data-control="message"] textarea').fill(TEST_MESSAGE);
            await assignModal.locator(`.modal-content .user-list .user-entry[data-id="${EDITOR_USER.id}"] gtx-checkbox label`).click();
            await assignModal.locator('.modal-footer [data-action="confirm"] button').click();

            await logout(page);
        });

        await test.step('Confirm editor got review message', async () => {
            // The UI polls every 30 seconds, so we wait a bit longer in case we missed the cycle
            const messageLoad = page.waitForResponse(matchRequest('GET', '/rest/msg/list'), {
                timeout: 40_000,
            });
            await loginWithForm(page, {
                username: TEST_USER_EDITOR.login,
                password: TEST_USER_EDITOR.password,
            });

            const userMenu = await openSidebar(page);
            const tabs = userMenu.locator('gtx-tabs');
            const messagesTab = await selectTab(tabs, 'messages');
            await messageLoad;

            const receivedMessage = messagesTab.locator('.unread-messages .message').filter({
                hasText: TEST_MESSAGE,
            });
            // Should be rendered in the messages list
            await receivedMessage.waitFor({ state: 'visible' });
        });
    });

});
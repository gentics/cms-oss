import { AccessControlledType, GcmsPermission, Page as CmsPage } from '@gentics/cms-models';
import {
    clickNotificationAction,
    EntityImporter,
    findContextContent,
    findNotification,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    ImportPermissions,
    ITEM_TYPE_PAGE,
    loginWithForm,
    matchRequest,
    navigateToApp,
    NODE_MINIMAL,
    PAGE_ONE,
    TestSize,
    UserImportData,
} from '@gentics/e2e-utils';
import { cloneWithSymbols } from '@gentics/ui-core/utils/clone-with-symbols';
import { expect, Locator, Page, Response, test } from '@playwright/test';
import {
    closeObjectPropertyEditor,
    editorAction,
    expectItemOffline,
    expectItemPublished,
    findItem,
    findList,
    itemAction,
    openObjectPropertyEditor,
    selectNode,
} from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('Page Management', () => {

    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'pagemngt';

    const TEST_GROUP_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_test`,

        description: 'Page-Management: Test Group',
        name: `${NAMESPACE}_test`,
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_test`,

        email: 'page-management@example.com',
        firstName: 'Page Management',
        lastName: 'User',
        login: `${NAMESPACE}_user`,
        password: 'somethingsomething',

        group: TEST_GROUP_BASE,
    };

    const NEW_PAGE_NAME = 'Hello World';
    const CHANGE_PAGE_NAME = 'Foo bar change';
    const OBJECT_PROPERTY = 'test_color';
    const TEST_CATEGORY_ID = 2;
    const COLOR_ID = 2;

    // eslint-disable-next-line @typescript-eslint/naming-convention
    let TEST_PAGE: CmsPage;

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
            TEST_PAGE = IMPORTER.get(PAGE_ONE);
        });
    });

    async function setupWithPermissions(page: Page, permissions: ImportPermissions[]): Promise<void> {
        await test.step('Test User Setup', async () => {
            const TEST_GROUP = cloneWithSymbols(TEST_GROUP_BASE);
            TEST_GROUP.permissions = permissions;

            await IMPORTER.importData([
                TEST_GROUP,
                TEST_USER,
            ]);
        });

        await test.step('Open Editor-UI', async () => {
            await navigateToApp(page);
            await loginWithForm(page, TEST_USER);
            await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);
        });
    }

    test('should be possible to create a new page', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                    { type: GcmsPermission.READ_TEMPLATES, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        let createReq: Promise<Response>;

        await test.step('Create a new Page', async () => {
            await list.locator('.header-controls [data-action="create-new-item"] button').click();
            const modal = page.locator('create-page-modal');
            const form = modal.locator('gtx-page-properties');
            await form.locator('[formcontrolname="name"] input').fill(NEW_PAGE_NAME);
            createReq = page.waitForResponse(matchRequest('POST', '/rest/page/create'));
            await modal.locator('.modal-footer [data-action="confirm"] button').click();
        });

        const response = await createReq;
        const responseBody = await response.json();
        const pageId = responseBody.page.id;

        // The new page should now be displayed in the list
        await expect(findItem(list, pageId)).toBeVisible();
    });

    test('should be possible to edit the page properties', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, TEST_PAGE.id);

        await expect(item.locator('.item-name .item-name-only')).toHaveText(TEST_PAGE.name);

        await test.step('Update the page name', async () => {
            await itemAction(item, 'properties');
            const form = page.locator('content-frame combined-properties-editor .properties-content gtx-page-properties');
            await form.locator('[formcontrolname="name"] input').fill(CHANGE_PAGE_NAME);
            await page.waitForTimeout(500); // Have to wait for internals to propagate

            await editorAction(page, 'save');
        });

        await expect(item.locator('.item-name .item-name-only')).toHaveText(CHANGE_PAGE_NAME);
    });

    test('should be possible to publish the page after saving properties', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-18802',
        }],
    }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.PUBLISH_PAGES, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, TEST_PAGE.id);
        let form: Locator;

        // expect the page to be offline
        await expectItemOffline(item);

        await test.step('Update the page', async () => {
            await itemAction(item, 'properties');
            form = page.locator('content-frame combined-properties-editor .properties-content gtx-page-properties');
            await form.locator('[formcontrolname="name"] input').fill(CHANGE_PAGE_NAME);
            await editorAction(page, 'save');
        });

        await test.step('Publish the page with toast action', async () => {
            const publishNotification = findNotification(page, `page-save-success-with-publish:${TEST_PAGE.id}`);
            const publishReq = page.waitForResponse(matchRequest('POST', `/rest/page/publish/${TEST_PAGE.id}`));
            await clickNotificationAction(publishNotification);
            await publishReq;
            await expect(form).toBeHidden();
        });

        // page should be published now
        await expectItemPublished(item);
    });

    test('should be possible to edit the page object-properties', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                ],
            },
            {
                type: AccessControlledType.OBJECT_PROPERTY_TYPE,
                instanceId: '10007',
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.UPDATE, value: true },
                ],
            },
        ]);

        let list = findList(page, ITEM_TYPE_PAGE);
        let item = findItem(list, TEST_PAGE.id);

        await test.step('Update Page Object-Properties', async () => {
            await itemAction(item, 'properties');
            await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY);
            await page.locator('gentics-tag-editor select-tag-property-editor gtx-select gtx-dropdown-trigger').click();
            await page.locator(`gtx-dropdown-content li.select-option[data-id="${COLOR_ID}"]`).click();
            await editorAction(page, 'save');
        });

        // Reopen the editor to reload fresh values
        await test.step('Validate the changes are correct after reloading', async () => {
            await closeObjectPropertyEditor(page);
            list = findList(page, ITEM_TYPE_PAGE);
            item = findItem(list, TEST_PAGE.id);
            await itemAction(item, 'properties');
            await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY);
            await expect(page.locator('gentics-tag-editor select-tag-property-editor gtx-select gtx-dropdown-trigger .view-value')).toHaveAttribute('data-value', `${COLOR_ID}`);
        });
    });

    test('should be possible to open the context-menu in the page-properties', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-18791',
        }],
    }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, TEST_PAGE.id);

        await itemAction(item, 'properties');
        await editorAction(page, 'editor-context');
        const content = findContextContent(page, 'item-editor');

        await expect(content).toBeAttached();
    });
});

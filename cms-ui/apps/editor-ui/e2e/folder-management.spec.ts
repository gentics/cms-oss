import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import {
    EntityImporter,
    FOLDER_A,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    ImportPermissions,
    ITEM_TYPE_FOLDER,
    loginWithForm,
    navigateToApp,
    NODE_MINIMAL,
    TestSize,
    UserImportData
} from '@gentics/e2e-utils';
import { cloneWithSymbols } from '@gentics/ui-core/utils/clone-with-symbols';
import { expect, Page, test } from '@playwright/test';
import {
    closeObjectPropertyEditor,
    editorAction,
    findItem,
    findList,
    itemAction,
    openObjectPropertyEditor,
    selectNode,
} from './helpers';

test.describe('Folder Management', () => {

    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'foldermngt';

    const NEW_FOLDER_NAME = 'Hello World';
    const NEW_FOLDER_PATH = 'example';
    const CHANGE_FOLDER_NAME = 'Foo bar change';
    const TEST_CATEGORY_ID = 2;
    const OBJECT_PROPERTY = 'test_color';
    const COLOR_ID = 2;

    const TEST_GROUP_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_editor`,

        description: 'Folder Management: Editor',
        name: `group_${NAMESPACE}_editor`,
        permissions: [],
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_editor`,

        group: TEST_GROUP_BASE,

        email: 'something@example.com',
        firstName: 'FolderManagement',
        lastName: 'Editor',
        login: `${NAMESPACE}_editor`,
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
            await IMPORTER.syncPackages(TestSize.MINIMAL);
            await IMPORTER.setupTest(TestSize.MINIMAL);
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

    test('should be possible to create a new folder', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL)!.folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.CREATE, value: true },
                ],
            }
        ]);

        const list = findList(page, ITEM_TYPE_FOLDER);
        await list.locator('.header-controls [data-action="create-new-item"]').click({ force: true });

        const modal = page.locator('create-folder-modal');
        const form = modal.locator('gtx-folder-properties');

        await form.locator('[formcontrolname="name"] input').fill(NEW_FOLDER_NAME);
        await form.locator('[formcontrolname="publishDir"] input').fill(NEW_FOLDER_PATH);

        const [response] = await Promise.all([
            page.waitForResponse(resp => resp.url().includes('/rest/folder/create') && resp.status() === 200),
            modal.locator('.modal-footer [data-action="confirm"]').click({ force: true }),
        ]);

        const responseBody = await response.json();
        const folderId = responseBody.folder.id;

        await expect(findItem(list, folderId)).toBeVisible();
    });

    test('should be possible to edit the folder properties', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL)!.folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_FOLDER, value: true },
                ],
            }
        ]);

        const folder = IMPORTER.get(FOLDER_A)!;
        const list = findList(page, ITEM_TYPE_FOLDER);
        const item = findItem(list, folder.id);

        await expect(item.locator('.item-name .item-name-only')).toHaveText(folder.name);

        await itemAction(item, 'properties');

        const form = page.locator('content-frame combined-properties-editor .properties-content gtx-folder-properties');
        await form.locator('[formcontrolname="name"] input').fill(CHANGE_FOLDER_NAME);
        await page.waitForTimeout(500); // Have to wait for internals to propagate

        await editorAction(page, 'save');

        await expect(item.locator('.item-name .item-name-only')).toHaveText(CHANGE_FOLDER_NAME);
    });

    test('should not be possible to edit the folder properties without permissions', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19638',
        }],
    }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL)!.folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            }
        ]);

        const folder = IMPORTER.get(FOLDER_A)!;
        const list = findList(page, ITEM_TYPE_FOLDER);
        const item = findItem(list, folder.id);
        await itemAction(item, 'properties');

        const form = page.locator('content-frame combined-properties-editor .properties-content gtx-folder-properties');
        await expect(form.locator('[formcontrolname="name"] input')).toBeDisabled();
        await expect(form.locator('[formcontrolname="description"] input')).toBeDisabled();
    });

    test('should be possible to edit the folder object-properties', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL)!.folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_FOLDER, value: true },
                ],
            },
            {
                type: AccessControlledType.OBJECT_PROPERTY_TYPE,
                instanceId: '10002',
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.UPDATE, value: true },
                ],
            },
        ]);

        const folder = IMPORTER.get(FOLDER_A)!;
        let list = findList(page, ITEM_TYPE_FOLDER);
        let item = findItem(list, folder.id);

        await itemAction(item, 'properties');

        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY);
        await page.locator('gentics-tag-editor select-tag-property-editor gtx-select gtx-dropdown-trigger').click();
        await page.locator(`gtx-dropdown-content li.select-option[data-id="${COLOR_ID}"]`).click();

        await editorAction(page, 'save');

        // Reopen the editor to reload fresh values
        await closeObjectPropertyEditor(page);
        list = findList(page, ITEM_TYPE_FOLDER);
        item = findItem(list, folder.id);
        await itemAction(item, 'properties');
        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY);
        await expect(page.locator('gentics-tag-editor select-tag-property-editor gtx-select gtx-dropdown-trigger .view-value')).toHaveAttribute('data-value', `${COLOR_ID}`);
    });

    test('should not be possible to edit the folder object-properties, if no permissions set', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19186',
        }],
    }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL)!.folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                ],
            },
            {
                type: AccessControlledType.OBJECT_PROPERTY_TYPE,
                instanceId: '10002',
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
        ]);

        const folder = IMPORTER.get(FOLDER_A)!;
        const list = findList(page, ITEM_TYPE_FOLDER);
        const item = findItem(list, folder.id);

        await test.step('Folder-Properties are readonly', async() => {
            await itemAction(item, 'properties');
            await expect(page.locator('content-frame gtx-editor-toolbar [data-action="save"] gtx-button button[type=button]'))
                .toHaveAttribute('disabled');
            const form = page.locator('content-frame gtx-properties-editor gtx-folder-properties');
            await expect(form.locator('gtx-input[formcontrolname="publishDir"] input'))
                .toHaveAttribute('disabled');
            await expect(form.locator('gtx-i18n-input[formcontrolname="publishDirI18n"] input'))
                .toHaveAttribute('disabled');
        });

        await test.step('Object-Properties are readonly', async () => {
            await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY);
            await expect(page.locator('gentics-tag-editor select-tag-property-editor gtx-select gtx-dropdown-trigger .view-value.select-input'))
                .toHaveAttribute('disabled');
        });
    });
});

import {
    EntityImporter,
    ITEM_TYPE_FOLDER,
    TestSize,
    folderA,
    loginWithForm,
    minimalNode,
    navigateToApp,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import {
    closeObjectPropertyEditor,
    editorAction,
    findItem,
    findList,
    itemAction,
    openObjectPropertyEditor,
    selectNode,
} from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('Folder Management', () => {
    const IMPORTER = new EntityImporter();
    const NEW_FOLDER_NAME = 'Hello World';
    const NEW_FOLDER_PATH = 'example';
    const CHANGE_FOLDER_NAME = 'Foo bar change';
    const TEST_CATEGORY_ID = 2;
    const OBJECT_PROPERTY = 'test_color';
    const COLOR_ID = 2;

    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);

        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach(async ({ page, request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);

        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.setupTest(TestSize.MINIMAL);

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);
    });

    test('should be possible to create a new folder', async ({ page }) => {
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
        const folder = IMPORTER.get(folderA)!;
        const list = findList(page, ITEM_TYPE_FOLDER);
        const item = findItem(list, folder.id);

        await expect(item.locator('.item-name .item-name-only')).toHaveText(folder.name);

        await itemAction(item, 'properties');

        const form = page.locator('content-frame combined-properties-editor .properties-content gtx-folder-properties');

        await form.locator('[formcontrolname="name"] input').fill(CHANGE_FOLDER_NAME);

        await editorAction(page, 'save');

        await expect(item.locator('.item-name .item-name-only')).toHaveText(CHANGE_FOLDER_NAME);
    });

    test('should be possible to edit the folder object-properties', async ({ page }) => {
        const folder = IMPORTER.get(folderA)!;
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
});

import { test, expect } from '@playwright/test';
import {
    EntityImporter,
    TestSize,
    ITEM_TYPE_PAGE,
    folderA,
    minimalNode,
    pageOne,
} from '@gentics/e2e-utils';
import {
    login,
    selectNode,
    findList,
    findItem,
    itemAction,
    initPage,
    editorAction,
    openObjectPropertyEditor,
} from './helpers';
import { AUTH_ADMIN } from './common';

test.describe('Page Management', () => {
    const IMPORTER = new EntityImporter();
    const NEW_PAGE_NAME = 'Hello World';
    const CHANGE_PAGE_NAME = 'Foo bar change';
    const OBJECT_PROPERTY = 'test_color';
    const TEST_CATEGORY_ID = 2;
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
        await initPage(page);
        await page.goto('/');
        await login(page, AUTH_ADMIN);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);
    });

    test('should be possible to create a new page', async ({ page }) => {
        const list = findList(page, ITEM_TYPE_PAGE);
        await list.locator('.header-controls [data-action="create-new-item"]').click({ force: true });

        const modal = page.locator('create-page-modal');
        const form = modal.locator('gtx-page-properties');

        await form.locator('[formcontrolname="name"] input').fill(NEW_PAGE_NAME);

        const [response] = await Promise.all([
            page.waitForResponse(resp => resp.url().includes('/rest/page/create') && resp.status() === 200),
            modal.locator('.modal-footer [data-action="confirm"]').click({ force: true }),
        ]);

        const responseBody = await response.json();
        const pageId = responseBody.page.id;

        await expect(findItem(list, pageId)).toBeVisible();
    });

    test('should be possible to edit the page properties', async ({ page }) => {
        const PAGE = IMPORTER.get(pageOne)!;
        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, PAGE.id);

        await expect(item.locator('.item-name .item-name-only')).toHaveText(PAGE.name);

        await itemAction(item, 'properties');

        const form = page.locator('content-frame combined-properties-editor .properties-content gtx-page-properties');

        await form.locator('[formcontrolname="name"] input').fill(CHANGE_PAGE_NAME);

        await editorAction(page, 'save');

        await expect(item.locator('.item-name .item-name-only')).toHaveText(CHANGE_PAGE_NAME);
    });

    test('should be possible to edit the page object-properties', async ({ page }) => {
        const PAGE = IMPORTER.get(pageOne)!;
        let list = findList(page, ITEM_TYPE_PAGE);
        let item = findItem(list, PAGE.id);

        await itemAction(item, 'properties');

        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY);
        await page.locator('gentics-tag-editor select-tag-property-editor gtx-select gtx-dropdown-trigger').click();
        await page.locator(`gtx-dropdown-content li.select-option[data-id="${COLOR_ID}"]`).click();

        await editorAction(page, 'save');

        // Reopen the editor to reload fresh values
        await page.locator('content-frame gtx-editor-toolbar gtx-button.close-button').click();
        list = findList(page, ITEM_TYPE_PAGE);
        item = findItem(list, PAGE.id);
        await itemAction(item, 'properties');
        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY);
        await expect(page.locator('gentics-tag-editor select-tag-property-editor gtx-select gtx-dropdown-trigger .view-value')).toHaveAttribute('data-value', `${COLOR_ID}`);
    });
});

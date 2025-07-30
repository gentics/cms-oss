import {
    EntityImporter,
    ITEM_TYPE_PAGE,
    TestSize,
    loginWithForm,
    minimalNode,
    navigateToApp,
    pageOne,
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

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);
    });

    test('should be possible to create a new page', async ({ page }) => {
        const list = findList(page, ITEM_TYPE_PAGE);
        await list.locator('.header-controls [data-action="create-new-item"] button').click();

        const modal = page.locator('create-page-modal');
        const form = modal.locator('gtx-page-properties');

        await form.locator('[formcontrolname="name"] input').fill(NEW_PAGE_NAME);

        const [response] = await Promise.all([
            page.waitForResponse(resp => resp.url().includes('/rest/page/create') && resp.status() === 200),
            modal.locator('.modal-footer [data-action="confirm"] button').click(),
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
        await closeObjectPropertyEditor(page);
        list = findList(page, ITEM_TYPE_PAGE);
        item = findItem(list, PAGE.id);
        await itemAction(item, 'properties');
        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY);
        await expect(page.locator('gentics-tag-editor select-tag-property-editor gtx-select gtx-dropdown-trigger .view-value')).toHaveAttribute('data-value', `${COLOR_ID}`);
    });

    test('should be possible to open the context-menu in the page-properties', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-18791',
        }],
    }, async ({ page }) => {
        const PAGE = IMPORTER.get(pageOne)!;
        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, PAGE.id);

        await itemAction(item, 'properties');
        await editorAction(page, 'editor-context');
        const content = findContextContent(page, 'item-editor');

        await expect(content).toBeAttached();
    });
});

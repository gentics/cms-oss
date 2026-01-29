import {
    EntityImporter,
    ITEM_TYPE_FILE,
    ITEM_TYPE_IMAGE,
    TestSize,
    loginWithForm,
    NODE_MINIMAL,
    navigateToApp,
    pickSelectValue,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH, FIXTURE_TEST_FILE_TXT_1, FIXTURE_TEST_IMAGE_JPG_1, FIXTURE_TEST_IMAGE_JPG_2 } from './common';
import {
    closeObjectPropertyEditor,
    editorAction,
    findImage,
    findItem,
    findList,
    itemAction,
    openObjectPropertyEditor,
    selectNode,
    uploadFiles,
} from './helpers';

test.describe('Media Management', () => {
    const IMPORTER = new EntityImporter();

    const TEST_CATEGORY_ID = 2;
    const OBJECT_PROPERTY_COLOR = 'test_color';
    const DEFAULT_CATEGORY_ID = 1;
    const OBJECT_PROPERTY_COPYRIGHT = 'copyright';
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
        await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);
    });

    test('should be possible to create a new file and edit the object-properties', async ({ page }) => {
        // Upload file and wait for response
        const uploadedFiles = await uploadFiles(page, ITEM_TYPE_FILE, [FIXTURE_TEST_FILE_TXT_1]);
        const FILE = uploadedFiles[FIXTURE_TEST_FILE_TXT_1];

        // Open properties
        let list = findList(page, ITEM_TYPE_FILE);
        let item = findItem(list, FILE.id);
        await itemAction(item, 'properties');

        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY_COLOR);
        const colorSelect = page.locator('gentics-tag-editor select-tag-property-editor gtx-select');
        await pickSelectValue(colorSelect, `${COLOR_ID}`);

        await editorAction(page, 'save');

        // Reopen the editor to reload fresh values
        await closeObjectPropertyEditor(page);
        list = findList(page, ITEM_TYPE_FILE);
        item = findItem(list, FILE.id);
        await itemAction(item, 'properties');
        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY_COLOR);
        await expect(colorSelect.locator('gtx-dropdown-trigger .view-value')).toHaveAttribute('data-value', `${COLOR_ID}`);
    });

    test('should be possible to create a new image and edit the object-properties', async ({ page }) => {
        // Upload image and wait for response
        const uploadedFiles = await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_TEST_IMAGE_JPG_1]);
        const IMAGE = uploadedFiles[FIXTURE_TEST_IMAGE_JPG_1];

        // Open properties
        let list = findList(page, ITEM_TYPE_IMAGE);
        let item = await findImage(list, IMAGE.id);
        await itemAction(item, 'properties');

        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY_COLOR);
        const colorSelect = page.locator('gentics-tag-editor select-tag-property-editor gtx-select');
        await pickSelectValue(colorSelect, `${COLOR_ID}`);

        await editorAction(page, 'save');

        // Reopen the editor to reload fresh values
        await closeObjectPropertyEditor(page);
        list = findList(page, ITEM_TYPE_IMAGE);
        item = findItem(list, IMAGE.id);
        await itemAction(item, 'properties');
        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY_COLOR);
        await expect(colorSelect.locator('gtx-dropdown-trigger .view-value')).toHaveAttribute('data-value', `${COLOR_ID}`);
    });

    test('should display the updated value even after switching object-properties', async ({ page }) => {
        // Upload image and wait for response
        const uploadedFiles = await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_TEST_IMAGE_JPG_2]);
        const IMAGE = uploadedFiles[FIXTURE_TEST_IMAGE_JPG_2];

        // Open properties
        let list = findList(page, ITEM_TYPE_IMAGE);
        let item = await findImage(list, IMAGE.id);
        await itemAction(item, 'properties');

        // Edit object property
        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY_COLOR);
        const colorSelect = page.locator('gentics-tag-editor select-tag-property-editor gtx-select');
        await pickSelectValue(colorSelect, `${COLOR_ID}`);

        await editorAction(page, 'save');
        // Reopen the editor to reload fresh values
        await closeObjectPropertyEditor(page);
        list = findList(page, ITEM_TYPE_IMAGE);
        item = findItem(list, IMAGE.id);
        await itemAction(item, 'properties');
        // Switch to another property and back
        await openObjectPropertyEditor(page, DEFAULT_CATEGORY_ID, OBJECT_PROPERTY_COPYRIGHT);
        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY_COLOR);

        // Verify the value is still selected
        const selectedValue = await colorSelect.locator('.view-value')
            .getAttribute('data-value');
        expect(selectedValue).toBe(String(COLOR_ID));
    });
});

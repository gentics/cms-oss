import {
    EntityImporter,
    ITEM_TYPE_FILE,
    ITEM_TYPE_IMAGE,
    TestSize,
    loginWithForm,
    NODE_MINIMAL,
    navigateToApp,
    pickSelectValue,
    FIXTURE_IMAGE_JPEG1,
    FIXTURE_IMAGE_JPEG2,
    FIXTURE_FILE_TXT1,
    waitForResponseFrom,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
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
        const uploadedFiles = await uploadFiles(page, ITEM_TYPE_FILE, [FIXTURE_FILE_TXT1]);
        const FILE = uploadedFiles[FIXTURE_FILE_TXT1.fixturePath];

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
        const uploadedFiles = await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_IMAGE_JPEG1]);
        const IMAGE = uploadedFiles[FIXTURE_IMAGE_JPEG1.fixturePath];

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
        const uploadedFiles = await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_IMAGE_JPEG2]);
        const IMAGE = uploadedFiles[FIXTURE_IMAGE_JPEG2.fixturePath];

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

    test('should be able to replace an existing image', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19525',
        }],
    }, async ({ page }) => {
        // Upload image and wait for response
        const uploadedFiles = await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_IMAGE_JPEG1]);
        const IMAGE = uploadedFiles[FIXTURE_IMAGE_JPEG1.fixturePath];

        // Open properties
        let list = findList(page, ITEM_TYPE_IMAGE);
        let item = await findImage(list, IMAGE.id);
        await itemAction(item, 'properties');

        // Verify current state and upload a replacement image
        const preview = page.locator('content-frame gtx-file-preview');
        const thumbnail = preview.locator('.image-preview .thumbnail');

        // Wait for the image to be visible/loaded
        await thumbnail.locator('.previewed-image img').waitFor({ state: 'visible' });
        const imageRect = await thumbnail.locator('.previewed-image img').evaluate((el) => el.getBoundingClientRect());
        const details = preview.locator('.image-details');
        const dimensions = await details.locator('.dimensions').textContent();
        const fileSize = await details.locator('.filesize').textContent();

        const replaceFilePicker = preview.locator('[data-action="replace"] input[type="file"]');
        const replaceReq = waitForResponseFrom(page, 'POST', `/rest/file/save/${IMAGE.id}`, {
            timeout: 20_000,
        });
        await replaceFilePicker.setInputFiles(FIXTURE_IMAGE_JPEG2.fixturePath);
        await replaceReq;

        await expect(details.locator('.dimensions')).not.toHaveText(dimensions);
        await expect(details.locator('.filesize')).not.toHaveText(fileSize);
        const newRect = await thumbnail.locator('.previewed-image img').evaluate((el) => el.getBoundingClientRect());

        expect(`${imageRect.width}x${imageRect.height}`).not.toEqual(`${newRect.width}x${newRect.height}`);
    });
});

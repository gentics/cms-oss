import { test, expect } from '@playwright/test';
import { EntityImporter, ITEM_TYPE_FILE, ITEM_TYPE_IMAGE, TestSize, minimalNode } from '@gentics/e2e-utils';
import { FileSaveRequest, ImageSaveRequest, SelectTagPartProperty } from '@gentics/cms-models';
import { AUTH_ADMIN, FIXTURE_TEST_FILE_TXT_1, FIXTURE_TEST_IMAGE_JPG_1, FIXTURE_TEST_IMAGE_JPG_2 } from './common';
import { login, selectNode, uploadFiles, findList, findItem, itemAction, openObjectPropertyEditor, editorAction, selectOption } from './helpers';

test.describe.configure({ mode: 'serial' });
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

        // Navigate and login
        await page.goto('/', { waitUntil: 'networkidle', timeout: 60_000 });
        await login(page, AUTH_ADMIN);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);
    });

    test('should be possible to create a new file and edit the object-properties', async ({ page }) => {
        // Upload file and wait for response
        const uploadedFiles = await uploadFiles(page, ITEM_TYPE_FILE, [FIXTURE_TEST_FILE_TXT_1]);
        const FILE = uploadedFiles[FIXTURE_TEST_FILE_TXT_1];

        // Open properties
        await findList(page, ITEM_TYPE_FILE)
            .then(list => findItem(list, FILE.id))
            .then(item => itemAction(item, 'properties'));

        // Edit object property
        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY_COLOR);
        await selectOption(page.locator('gentics-tag-editor select-tag-property-editor gtx-select'), COLOR_ID);

        // Save and verify request
        const saveRequest = page.waitForRequest(request =>
            request.method() === 'POST' &&
            request.url().includes('/rest/file/save/'),
        );

        await editorAction(page, 'save');

        const request = await saveRequest;
        const requestBody = await request.postDataJSON() as FileSaveRequest;
        const tag = requestBody.file.tags?.[`object.${OBJECT_PROPERTY_COLOR}`];
        const options = (tag?.properties['select'] as SelectTagPartProperty).selectedOptions;

        expect(options).toHaveLength(1);
        expect(options![0].id).toBe(COLOR_ID);
    });

    test('should be possible to create a new image and edit the object-properties', async ({ page }) => {
        // Upload image and wait for response
        const uploadedFiles = await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_TEST_IMAGE_JPG_1]);
        const IMAGE = uploadedFiles[FIXTURE_TEST_IMAGE_JPG_1];

        // Open properties
        await findList(page, ITEM_TYPE_IMAGE)
            .then(list => findItem(list, IMAGE.id))
            .then(item => itemAction(item, 'properties'));

        // Edit object property
        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY_COLOR);
        await selectOption(page.locator('gentics-tag-editor select-tag-property-editor gtx-select'), COLOR_ID);

        // Save and verify request
        const saveRequest = page.waitForRequest(request =>
            request.method() === 'POST' &&
            request.url().includes('/rest/image/save/'),
        );

        await editorAction(page, 'save');

        const request = await saveRequest;
        const requestBody = await request.postDataJSON() as ImageSaveRequest;
        const tag = requestBody.image.tags?.[`object.${OBJECT_PROPERTY_COLOR}`];
        const options = (tag?.properties['select'] as SelectTagPartProperty).selectedOptions;

        expect(options).toHaveLength(1);
        expect(options![0].id).toBe(COLOR_ID);
    });

    test('should display the updated value even after switching object-properties', async ({ page }) => {
        // Upload image and wait for response
        const uploadedFiles = await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_TEST_IMAGE_JPG_2]);
        const IMAGE = uploadedFiles[FIXTURE_TEST_IMAGE_JPG_2];

        // Open properties
        await findList(page, ITEM_TYPE_IMAGE)
            .then(list => findItem(list, IMAGE.id))
            .then(item => itemAction(item, 'properties'));

        // Edit object property
        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY_COLOR);
        await selectOption(page.locator('gentics-tag-editor select-tag-property-editor gtx-select'), COLOR_ID);

        // Save changes
        const saveRequest = page.waitForRequest(request =>
            request.method() === 'POST' &&
            request.url().includes('/rest/image/save/'),
        );

        await editorAction(page, 'save');
        await saveRequest;

        // Wait for state updates
        await page.waitForTimeout(2000);

        // Switch to another property and back
        await openObjectPropertyEditor(page, DEFAULT_CATEGORY_ID, OBJECT_PROPERTY_COPYRIGHT);
        await page.waitForTimeout(1000);
        await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY_COLOR);

        // Verify the value is still selected
        const selectedValue = await page.locator('gentics-tag-editor select-tag-property-editor gtx-select .view-value')
            .getAttribute('data-value');
        expect(selectedValue).toBe(String(COLOR_ID));
    });
});

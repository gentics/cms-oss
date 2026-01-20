import {
    EntityImporter,
    ITEM_TYPE_FILE,
    ITEM_TYPE_IMAGE,
    loginWithForm,
    NODE_MINIMAL,
    navigateToApp,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import {
    AUTH,
    FIXTURE_TEST_FILE_PDF_1,
    FIXTURE_TEST_FILE_TXT_1,
    FIXTURE_TEST_FILE_TXT_2,
    FIXTURE_TEST_IMAGE_JPG_2,
    FIXTURE_TEST_IMAGE_PNG_1,
    FIXTURE_TEST_IMAGE_PNG_2,
} from './common';
import {
    findList,
    selectNode,
    uploadFiles,
} from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('Media Upload', () => {
    const IMPORTER = new EntityImporter();

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

    test('should be possible to upload a regular text file', async ({ page }) => {
        const list = findList(page, ITEM_TYPE_FILE);
        await uploadFiles(page, ITEM_TYPE_FILE, [FIXTURE_TEST_FILE_TXT_1]);
        await expect(list.locator('.list-body item-list-row, .list-body masonry-item')).toHaveCount(1);
    });

    test('should be possible to upload an image file', async ({ page }) => {
        const list = findList(page, ITEM_TYPE_IMAGE);
        await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_TEST_IMAGE_JPG_2]);
        await expect(list.locator('.list-body item-list-row, .list-body masonry-item')).toHaveCount(1);
    });

    test('should be possible to upload a regular text file with drag-and-drop', async ({ page }) => {
        const list = findList(page, ITEM_TYPE_FILE);
        await uploadFiles(page, ITEM_TYPE_FILE, [FIXTURE_TEST_FILE_TXT_1], { dragAndDrop: true });
        await expect(list.locator('.list-body item-list-row, .list-body masonry-item')).toHaveCount(1);
    });

    test('should be possible to upload an image file with drag-and-drop', async ({ page }) => {
        const list = findList(page, ITEM_TYPE_IMAGE);
        await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_TEST_IMAGE_JPG_2], { dragAndDrop: true });
        await expect(list.locator('.list-body item-list-row, .list-body masonry-item')).toHaveCount(1);
    });

    test('should be possible to upload multiple text files', async ({ page }) => {
        const list = findList(page, ITEM_TYPE_FILE);
        await uploadFiles(page, ITEM_TYPE_FILE, [FIXTURE_TEST_FILE_TXT_2, FIXTURE_TEST_FILE_PDF_1]);
        await expect(list.locator('.list-body item-list-row, .list-body masonry-item')).toHaveCount(2);
    });

    test('should be possible to upload multiple image files', async ({ page }) => {
        const list = findList(page, ITEM_TYPE_IMAGE);
        await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_TEST_IMAGE_PNG_1, FIXTURE_TEST_IMAGE_PNG_2]);
        await expect(list.locator('.list-body item-list-row, .list-body masonry-item')).toHaveCount(2);
    });
});

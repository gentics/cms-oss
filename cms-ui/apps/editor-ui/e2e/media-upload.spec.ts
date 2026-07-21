import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import {
    EntityImporter,
    FIXTURE_FILE_PDF1,
    FIXTURE_FILE_TXT1,
    FIXTURE_FILE_TXT2,
    FIXTURE_IMAGE_JPEG2,
    FIXTURE_IMAGE_PNG1,
    FIXTURE_IMAGE_PNG2,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    ImportPermissions,
    ITEM_TYPE_FILE,
    ITEM_TYPE_IMAGE,
    loginWithForm,
    navigateToApp,
    NODE_MINIMAL,
    setupUserDataRerouting,
    TestSize,
    uploadFileFromInput,
    UserImportData,
    waitForResponseFrom,
} from '@gentics/e2e-utils';
import { cloneWithSymbols } from '@gentics/ui-core/utils/clone-with-symbols';
import { expect, Page, test } from '@playwright/test';
import {
    findItem,
    findList,
    itemAction,
    selectNode,
    uploadFiles,
} from './helpers';

test.describe('Media Upload', () => {
    const NAMESPACE = 'mediaupload';
    const IMPORTER = new EntityImporter();

    const TEST_GROUP_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_editor`,

        description: 'Media Management: Editor',
        name: `group_${NAMESPACE}_editor`,
        permissions: [],
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_editor`,

        group: TEST_GROUP_BASE,

        email: 'something@example.com',
        firstName: 'MediaManagement',
        lastName: 'Editor',
        login: `${NAMESPACE}_editor`,
        password: 'testmedia',
    };

    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);

        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach(async ({ request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);

        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.setupTest(TestSize.MINIMAL);
    });

    async function setupWithPermissions(page: Page, permissions: ImportPermissions[]): Promise<void> {
        await setupUserDataRerouting(page);

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
            await selectNode(page, IMPORTER.get(NODE_MINIMAL).id);
        });
    }

    test('should be possible to upload a regular text file', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_FILE);
        await uploadFiles(page, ITEM_TYPE_FILE, [FIXTURE_FILE_TXT1]);
        await expect(list.locator('.list-body item-list-row, .list-body masonry-item')).toHaveCount(1);
    });

    test('should be possible to upload an image file', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_IMAGE);
        await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_IMAGE_JPEG2]);
        await expect(list.locator('.list-body item-list-row, .list-body masonry-item')).toHaveCount(1);
    });

    test('should be possible to upload a regular text file with drag-and-drop', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_FILE);
        await uploadFiles(page, ITEM_TYPE_FILE, [FIXTURE_FILE_TXT1], { dragAndDrop: true });
        await expect(list.locator('.list-body item-list-row, .list-body masonry-item')).toHaveCount(1);
    });

    test('should be possible to upload an image file with drag-and-drop', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_IMAGE);
        await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_IMAGE_JPEG2], { dragAndDrop: true });
        await expect(list.locator('.list-body item-list-row, .list-body masonry-item')).toHaveCount(1);
    });

    test('should be possible to upload multiple text files', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_FILE);
        await uploadFiles(page, ITEM_TYPE_FILE, [FIXTURE_FILE_TXT2, FIXTURE_FILE_PDF1]);
        await expect(list.locator('.list-body item-list-row, .list-body masonry-item')).toHaveCount(2);
    });

    test('should be possible to upload multiple image files', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_IMAGE);
        const files = await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_IMAGE_PNG1, FIXTURE_IMAGE_PNG2]);
        await expect(list.locator('.list-body item-list-row, .list-body masonry-item')).toHaveCount(2);
        expect(files[FIXTURE_IMAGE_PNG1.fixturePath]).not.toEqual(files[FIXTURE_IMAGE_PNG2.fixturePath]);
    });

    test('replacing a file with a new binary', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19702',
        }],
    }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_FILE);
        const files = await uploadFiles(page, ITEM_TYPE_FILE, [FIXTURE_FILE_TXT1]);
        const fileObj = files[FIXTURE_FILE_TXT1.fixturePath];
        await expect(list.locator('.list-body item-list-row, .list-body masonry-item')).toHaveCount(1);
        const item = findItem(list, fileObj.id);
        await itemAction(item, 'properties');

        const fileInput = page.locator('content-frame gtx-file-preview gtx-file-picker[data-action="replace"] gtx-button button');

        const uploadReq = waitForResponseFrom(page, 'POST', `/rest/file/save/${fileObj.id}`);
        await uploadFileFromInput(page, fileInput, [FIXTURE_FILE_TXT2.fixturePath]);
        await uploadReq;
    });

    test('replacing a image with a new binary', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19702',
        }],
    }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_IMAGE);
        const files = await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_IMAGE_PNG1]);
        const fileObj = files[FIXTURE_IMAGE_PNG1.fixturePath];
        await expect(list.locator('.list-body item-list-row, .list-body masonry-item')).toHaveCount(1);
        const item = findItem(list, fileObj.id);
        await itemAction(item, 'properties');

        const fileInput = page.locator('content-frame gtx-file-preview gtx-file-picker[data-action="replace"] gtx-button button');

        const uploadReq = waitForResponseFrom(page, 'POST', `/rest/file/save/${fileObj.id}`);
        await uploadFileFromInput(page, fileInput, [FIXTURE_IMAGE_JPEG2.fixturePath]);
        await uploadReq;
    });
});

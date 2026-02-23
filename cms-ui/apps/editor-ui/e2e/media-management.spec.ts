import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import {
    EntityImporter,
    FILE_ONE,
    FIXTURE_FILE_TXT1,
    FIXTURE_IMAGE_JPEG1,
    FIXTURE_IMAGE_JPEG2,
    GroupImportData,
    IMAGE_ONE,
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
    pickSelectValue,
    TestSize,
    UserImportData,
    waitForResponseFrom,
} from '@gentics/e2e-utils';
import { cloneWithSymbols } from '@gentics/ui-core/utils/clone-with-symbols';
import { expect, Page, test } from '@playwright/test';
import {
    closeObjectPropertyEditor,
    editorAction,
    findImage,
    findItem,
    findList,
    itemAction,
    openFilePropertiesTab,
    openObjectPropertyEditor,
    selectNode,
    uploadFiles,
} from './helpers';

test.describe('Media Management', () => {
    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'mediamngt';

    const TEST_CATEGORY_ID = 2;
    const OBJECT_PROPERTY_COLOR = 'test_color';
    const DEFAULT_CATEGORY_ID = 1;
    const OBJECT_PROPERTY_COPYRIGHT = 'copyright';
    const COLOR_ID = 2;

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
            await IMPORTER.syncPackages(TestSize.MINIMAL);
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

    test('should not be possible to edit the file properties without permissions', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19638',
        }],
    }, async ({ page }) => {
        await IMPORTER.setupBinaryFiles({
            [FILE_ONE[IMPORT_ID]]: FIXTURE_FILE_TXT1,
        });
        await IMPORTER.importData([FILE_ONE]);

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL)!.folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                ],
            }
        ]);

        const fileEntity = IMPORTER.get(FILE_ONE)!;
        const list = findList(page, ITEM_TYPE_FILE);
        const item = findItem(list, fileEntity.id);

        await itemAction(item, 'properties');
        await openFilePropertiesTab(page);

        const form = page.locator('content-frame combined-properties-editor .properties-content gtx-file-properties');
        await expect(form.locator('[formcontrolname="name"] input')).toBeDisabled();
        await expect(form.locator('[formcontrolname="description"] input')).toBeDisabled();
    });

    test('should not be possible to edit the image properties without permissions', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19638',
        }],
    }, async ({ page }) => {
        await IMPORTER.setupBinaryFiles({
            [IMAGE_ONE[IMPORT_ID]]: FIXTURE_IMAGE_JPEG1,
        });
        await IMPORTER.importData([IMAGE_ONE]);

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL)!.folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                ],
            }
        ]);

        const imgEntity = IMPORTER.get(IMAGE_ONE)!;
        const list = findList(page, ITEM_TYPE_IMAGE);
        const item = await findImage(list, imgEntity.id);

        await itemAction(item, 'properties');
        await openFilePropertiesTab(page);

        const form = page.locator('content-frame combined-properties-editor .properties-content gtx-file-properties');
        await expect(form.locator('[formcontrolname="name"] input')).toBeDisabled();
        await expect(form.locator('[formcontrolname="description"] input')).toBeDisabled();
    });

    test('should be possible to create a new file and edit the object-properties', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL)!.folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                ],
            },
            {
                type: AccessControlledType.OBJECT_PROPERTY_TYPE,
                instanceId: '10008',
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.UPDATE, value: true },
                ],
            },
        ]);

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
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL)!.folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                ],
            },
            {
                type: AccessControlledType.OBJECT_PROPERTY_TYPE,
                instanceId: '10011',
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.UPDATE, value: true },
                ],
            },
        ]);

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
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL)!.folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                ],
            },
            {
                type: AccessControlledType.OBJECT_PROPERTY_TYPE,
                instanceId: '10011',
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.UPDATE, value: true },
                ],
            },
        ]);

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
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL)!.folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                ],
            },
        ]);

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

import { AccessControlledType, Feature, GcmsPermission, NodeFeature, SelectTagPartProperty } from '@gentics/cms-models';
import {
    clickModalAction,
    dismissNotifications,
    EntityImporter,
    FILE_ONE,
    FIXTURE_FILE_PDF1,
    FIXTURE_FILE_TXT1,
    FIXTURE_IMAGE_JPEG1,
    FIXTURE_IMAGE_JPEG2,
    FIXTURE_IMAGE_JPEG3,
    FixtureFile,
    findNotification,
    getFileName,
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
    openContext,
    pickSelectValue,
    setupUserDataRerouting,
    TestSize,
    UserImportData,
    waitForResponseFrom,
} from '@gentics/e2e-utils';
import { cloneWithSymbols } from '@gentics/common';
import { expect, Locator, Page, test } from '@playwright/test';
import {
    closeObjectPropertyEditor,
    editorAction,
    ensureObjectPropertyGroupExpanded,
    findImage,
    findItem,
    findList,
    itemAction,
    openFilePropertiesTab,
    openObjectPropertyEditor,
    selectNode,
    toggleDisplayAllCheckbox,
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

    /**
     * Uploads the fixture as the specified type, and opens the properties of it.
     * @returns The uploaded entity and the file-preview element.
     */
    async function setupForReplace(
        page: Page,
        type: 'file' | 'image',
        fixture: FixtureFile,
    ): Promise<{ entity: { id: number; name: string }; preview: Locator }> {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                ],
            },
        ]);

        const uploadedFiles = await uploadFiles(page, type, [fixture]);
        const entity = uploadedFiles[fixture.fixturePath];

        const list = findList(page, type);
        const item = type === ITEM_TYPE_IMAGE
            ? await findImage(list, entity.id)
            : findItem(list, entity.id);
        await itemAction(item, 'properties');

        const preview = page.locator('content-frame gtx-file-preview');
        await preview.waitFor({ state: 'visible' });

        // Remove the notifications from the upload, to not interfere with the replace notifications
        await dismissNotifications(page);
        await expect(findNotification(page)).toHaveCount(0);

        return { entity, preview };
    }

    /**
     * Selects the fixture in the replace file-picker and waits for the upload to finish.
     */
    async function replaceWith(page: Page, preview: Locator, id: number, fixture: FixtureFile): Promise<void> {
        const replaceReq = waitForResponseFrom(page, 'POST', `/rest/file/save/${id}`, {
            timeout: 20_000,
        });
        await preview.locator('[data-action="replace"] input[type="file"]').setInputFiles(fixture.fixturePath);
        await replaceReq;
    }

    async function setKeepFileName(preview: Locator, keep: boolean): Promise<void> {
        const checkbox = preview.locator('.keep-filename gtx-checkbox');
        const input = checkbox.locator('input[type="checkbox"]');
        if ((await input.isChecked()) !== keep) {
            await checkbox.locator('label').click();
        }
        if (keep) {
            await expect(input).toBeChecked();
        } else {
            await expect(input).not.toBeChecked();
        }
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
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                ],
            },
        ]);

        const fileEntity = IMPORTER.get(FILE_ONE);
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
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                ],
            },
        ]);

        const imgEntity = IMPORTER.get(IMAGE_ONE);
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
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
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
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
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
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
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
        const selectedValue = colorSelect.locator('.view-value')
            ;
        await expect(selectedValue).toHaveAttribute('data-value', String(COLOR_ID));
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
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
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
        const list = findList(page, ITEM_TYPE_IMAGE);
        const item = await findImage(list, IMAGE.id);
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

    test('image usage information is clickable in grid view', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19872',
        }],
    }, async ({ page }) => {
        // Import a test image
        await IMPORTER.setupBinaryFiles({
            [IMAGE_ONE[IMPORT_ID]]: FIXTURE_IMAGE_JPEG2,
        });
        await IMPORTER.importData([IMAGE_ONE]);
        const IMAGE = IMPORTER.get(IMAGE_ONE);

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_IMAGE);

        await test.step('Setup image list display', async () => {
            const listOptions = await openContext(list.locator('item-list-header [data-action="open-list-context"]'));
            await listOptions.locator('[data-action="toggle-display-type"]').click();
        });

        await test.step('Setup display fields', async () => {
            const listOptions = await openContext(list.locator('item-list-header [data-action="open-list-context"]'));
            await listOptions.locator('[data-action="select-display-fields"]').click();
            const modal = page.locator('display-field-selector');
            await modal.locator('gtx-contents-list-item[data-id="usage"] label').click();
            await clickModalAction(modal, 'confirm');
        });

        const item = await findImage(list, IMAGE.id);
        await item.locator('list-item-details detail-chip.usage').click();

        await expect(page.locator('gtx-usage-modal')).toBeVisible();
    });

    test('image usage information is clickable in list view', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19872',
        }],
    }, async ({ page }) => {
        // Import a test image
        await IMPORTER.setupBinaryFiles({
            [IMAGE_ONE[IMPORT_ID]]: FIXTURE_IMAGE_JPEG2,
        });
        await IMPORTER.importData([IMAGE_ONE]);
        const IMAGE = IMPORTER.get(IMAGE_ONE);

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_IMAGE);

        await test.step('Setup display fields', async () => {
            const listOptions = await openContext(list.locator('item-list-header [data-action="open-list-context"]'));
            await listOptions.locator('[data-action="select-display-fields"]').click();
            const modal = page.locator('display-field-selector');
            await modal.locator('gtx-contents-list-item[data-id="usage"] label').click();
            await clickModalAction(modal, 'confirm');
        });

        const item = await findImage(list, IMAGE.id);
        await item.locator('list-item-details detail-chip.usage').click();

        await expect(page.locator('gtx-usage-modal')).toBeVisible();
    });

    test('should be possible to toggle display of deleted items', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20081',
        }],
    }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.UPDATE, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_FOLDER, value: true },
                ],
            },
        ]);

        const files = findList(page, ITEM_TYPE_FILE);
        const images = findList(page, ITEM_TYPE_IMAGE);
        await toggleDisplayAllCheckbox(page, files, 'toggle-wastebin', false);
        await toggleDisplayAllCheckbox(page, files, 'toggle-wastebin', true);
        await toggleDisplayAllCheckbox(page, images, 'toggle-wastebin', false);
        await toggleDisplayAllCheckbox(page, images, 'toggle-wastebin', true);
    });

    test('should show a warning when replacing a file with a different file extension', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20269',
        }],
    }, async ({ page }) => {
        const { entity, preview } = await setupForReplace(page, ITEM_TYPE_FILE, FIXTURE_FILE_TXT1);
        await setKeepFileName(preview, true);

        await replaceWith(page, preview, entity.id, FIXTURE_FILE_PDF1);

        const warning = findNotification(page, `file-replace-type-changed:${entity.id}`);
        await expect(warning).toBeVisible();
        await expect(warning.locator('.message')).toContainText(entity.name);
        await expect(warning.locator('.message')).toContainText(getFileName(FIXTURE_FILE_PDF1));
    });

    test('should not show a warning when replacing an image with an equivalent file extension', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20269',
        }],
    }, async ({ page }) => {
        const { entity, preview } = await setupForReplace(page, ITEM_TYPE_IMAGE, FIXTURE_IMAGE_JPEG1);
        await setKeepFileName(preview, true);

        // `.jpg` -> `.jpeg` should be considered the same extension
        await replaceWith(page, preview, entity.id, FIXTURE_IMAGE_JPEG3);

        // The warning would be shown before the success notification, so once the success is visible
        // we can safely check that no warning has been shown.
        await expect(findNotification(page, `file-replace-success:${entity.id}`)).toBeVisible();
        await expect(findNotification(page, `file-replace-type-changed:${entity.id}`)).toHaveCount(0);
        await expect(findNotification(page, `file-replace-error:${entity.id}`)).toHaveCount(0);
    });

    test('should switch to the image view when replacing a file with an image', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20269',
        }],
    }, async ({ page }) => {
        const { entity, preview } = await setupForReplace(page, ITEM_TYPE_FILE, FIXTURE_FILE_TXT1);
        await setKeepFileName(preview, false);

        await expect(page).toHaveURL(new RegExp(`detail:node/\\d+/file/${entity.id}/`));
        await expect(preview.locator('.preview-pane.file')).toBeVisible();

        await replaceWith(page, preview, entity.id, FIXTURE_IMAGE_JPEG1);

        await expect(page).toHaveURL(new RegExp(`detail:node/\\d+/image/${entity.id}/editProperties`));
        await expect(preview.locator('.preview-pane.image')).toBeVisible();

        await expect(findItem(findList(page, ITEM_TYPE_FILE), entity.id)).toHaveCount(0);
        await expect(findItem(findList(page, ITEM_TYPE_IMAGE), entity.id)).toBeVisible();
    });

    test('should switch to the file view when replacing an image with a file', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20269',
        }],
    }, async ({ page }) => {
        const { entity, preview } = await setupForReplace(page, ITEM_TYPE_IMAGE, FIXTURE_IMAGE_JPEG1);
        await setKeepFileName(preview, false);

        await expect(page).toHaveURL(new RegExp(`detail:node/\\d+/image/${entity.id}/`));
        await expect(preview.locator('.preview-pane.image')).toBeVisible();

        await replaceWith(page, preview, entity.id, FIXTURE_FILE_TXT1);

        await expect(page).toHaveURL(new RegExp(`detail:node/\\d+/file/${entity.id}/editProperties`));
        await expect(preview.locator('.preview-pane.file')).toBeVisible();

        await expect(findItem(findList(page, ITEM_TYPE_IMAGE), entity.id)).toHaveCount(0);
        await expect(findItem(findList(page, ITEM_TYPE_FILE), entity.id)).toBeVisible();
    });

    test.describe('Upload Properties', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19822',
        }],
    }, () => {
        const OBJECT_PROPERTY_TYPE_FILE = '10008';
        const OBJECT_PROPERTY_TYPE_IMAGE = '10011';

        /**
         * Enables both upload features globally, but only activates the specified ones on the node.
         */
        async function setupWithUploadFeatures(page: Page, nodeFeatures: NodeFeature[]): Promise<void> {
            await test.step('Enable upload features', async () => {
                await IMPORTER.setupFeatures({
                    [Feature.UPLOAD_FILE_PROPERTIES]: true,
                    [Feature.UPLOAD_IMAGE_PROPERTIES]: true,
                });
                for (const feature of nodeFeatures) {
                    await IMPORTER.client.node.activateFeature(IMPORTER.get(NODE_MINIMAL).id, feature).send();
                }
            });

            await setupWithPermissions(page, [
                {
                    type: AccessControlledType.NODE,
                    instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                    subObjects: true,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                        { type: GcmsPermission.READ_ITEMS, value: true },
                        { type: GcmsPermission.UPDATE_ITEMS, value: true },
                        { type: GcmsPermission.CREATE_ITEMS, value: true },
                    ],
                },
                ...[OBJECT_PROPERTY_TYPE_FILE, OBJECT_PROPERTY_TYPE_IMAGE].map((instanceId) => ({
                    type: AccessControlledType.OBJECT_PROPERTY_TYPE,
                    instanceId,
                    subObjects: true,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                        { type: GcmsPermission.UPDATE, value: true },
                    ],
                })),
            ]);
        }

        /**
         * Edits the properties and the color object-property in the upload properties modal and saves it.
         */
        async function editInUploadModal(page: Page, type: 'file' | 'image', id: number, name: string, description: string): Promise<void> {
            const modal = page.locator('image-properties-modal');
            await expect(modal).toBeVisible();

            await test.step('Edit properties', async () => {
                const form = modal.locator('combined-properties-editor .properties-content gtx-file-properties');
                await form.locator('[formcontrolname="name"] input').fill(name);
                await form.locator('[formcontrolname="description"] input').fill(description);
            });

            await test.step('Edit object-property', async () => {
                const group = modal.locator(`combined-properties-editor .properties-tabs .tab-group[data-id="${TEST_CATEGORY_ID}"]`);
                await ensureObjectPropertyGroupExpanded(group);
                await group.locator(`.tab-link[data-id="object.${OBJECT_PROPERTY_COLOR}"]`).click();

                const colorSelect = modal.locator('gentics-tag-editor select-tag-property-editor gtx-select');
                await pickSelectValue(colorSelect, `${COLOR_ID}`);
            });

            await test.step('Save changes', async () => {
                const saveReq = waitForResponseFrom(page, 'POST', `/rest/${type}/save/${id}`);
                await clickModalAction(modal, 'confirm');
                await saveReq;
                await expect(modal).toHaveCount(0);
            });
        }

        async function findUploadedItem(page: Page, type: 'file' | 'image', id: number): Promise<Locator> {
            const list = findList(page, type);
            return type === ITEM_TYPE_IMAGE
                ? findImage(list, id)
                : findItem(list, id);
        }

        async function loadUploadedItem(type: 'file' | 'image', id: number) {
            return type === ITEM_TYPE_IMAGE
                ? (await IMPORTER.client.image.get(id).send()).image
                : (await IMPORTER.client.file.get(id).send()).file;
        }

        for (const { type, feature, fixture, extension } of [
            { type: ITEM_TYPE_FILE, feature: NodeFeature.UPLOAD_FILE_PROPERTIES, fixture: FIXTURE_FILE_TXT1, extension: 'txt' },
            { type: ITEM_TYPE_IMAGE, feature: NodeFeature.UPLOAD_IMAGE_PROPERTIES, fixture: FIXTURE_IMAGE_JPEG1, extension: 'jpg' },
        ] as const) {
            test(`should be possible to edit the properties and object-properties of an uploaded ${type} with "${feature}"`, async ({ page }) => {
                await setupWithUploadFeatures(page, [feature]);

                const uploadedFiles = await uploadFiles(page, type, [fixture]);
                const entity = uploadedFiles[fixture.fixturePath];

                const newName = `upload-properties-${type}.${extension}`;
                const newDescription = `Description of the uploaded ${type}`;
                await editInUploadModal(page, type, entity.id, newName, newDescription);

                await test.step('Verify saved values', async () => {
                    const loaded = await loadUploadedItem(type, entity.id);
                    expect(loaded.name).toEqual(newName);
                    expect(loaded.description).toEqual(newDescription);

                    const colorProp = loaded.tags[`object.${OBJECT_PROPERTY_COLOR}`];
                    expect(colorProp.active).toBe(true);
                    const select = Object.values(colorProp.properties)
                        .find((prop) => prop.type === 'SELECT') as SelectTagPartProperty;
                    expect(select.selectedOptions.map((opt) => opt.id)).toEqual([COLOR_ID]);
                });

                await test.step('Verify values in the properties editor', async () => {
                    const item = await findUploadedItem(page, type, entity.id);
                    await expect(item).toContainText(newName);
                    await itemAction(item, 'properties');

                    await openFilePropertiesTab(page);
                    const form = page.locator('content-frame combined-properties-editor .properties-content gtx-file-properties');
                    await expect(form.locator('[formcontrolname="name"] input')).toHaveValue(newName);
                    await expect(form.locator('[formcontrolname="description"] input')).toHaveValue(newDescription);

                    await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY_COLOR);
                    const colorSelect = page.locator('content-frame gentics-tag-editor select-tag-property-editor gtx-select');
                    await expect(colorSelect.locator('gtx-dropdown-trigger .view-value')).toHaveAttribute('data-value', `${COLOR_ID}`);
                });
            });
        }

        test('should not open the upload properties modal when the features are not activated for the node', async ({ page }) => {
            await setupWithUploadFeatures(page, []);

            const files = await uploadFiles(page, ITEM_TYPE_FILE, [FIXTURE_FILE_TXT1]);
            const images = await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_IMAGE_JPEG1]);
            const file = files[FIXTURE_FILE_TXT1.fixturePath];
            const image = images[FIXTURE_IMAGE_JPEG1.fixturePath];

            // An open modal would block the interaction with the lists
            await itemAction(findItem(findList(page, ITEM_TYPE_FILE), file.id), 'properties');
            await expect(page).toHaveURL(new RegExp(`detail:node/\\d+/file/${file.id}/`));
            await itemAction(await findImage(findList(page, ITEM_TYPE_IMAGE), image.id), 'properties');
            await expect(page).toHaveURL(new RegExp(`detail:node/\\d+/image/${image.id}/`));

            await expect(page.locator('image-properties-modal')).toHaveCount(0);
        });

        test('should only open the upload properties modal for files when only "upload_file_properties" is active', async ({ page }) => {
            await setupWithUploadFeatures(page, [NodeFeature.UPLOAD_FILE_PROPERTIES]);

            await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_IMAGE_JPEG1]);
            const uploaded = await uploadFiles(page, ITEM_TYPE_FILE, [FIXTURE_FILE_TXT1]);
            const entity = uploaded[FIXTURE_FILE_TXT1.fixturePath];

            // The modals are opened in upload order, so only the one for the second upload may be present
            const modal = page.locator('image-properties-modal');
            await expect(modal).toHaveCount(1);
            await expect(modal.locator('.modal-title')).toContainText(entity.name);
        });

        test('should only open the upload properties modal for images when only "upload_image_properties" is active', async ({ page }) => {
            await setupWithUploadFeatures(page, [NodeFeature.UPLOAD_IMAGE_PROPERTIES]);

            await uploadFiles(page, ITEM_TYPE_FILE, [FIXTURE_FILE_TXT1]);
            const uploaded = await uploadFiles(page, ITEM_TYPE_IMAGE, [FIXTURE_IMAGE_JPEG1]);
            const entity = uploaded[FIXTURE_IMAGE_JPEG1.fixturePath];

            // The modals are opened in upload order, so only the one for the second upload may be present
            const modal = page.locator('image-properties-modal');
            await expect(modal).toHaveCount(1);
            await expect(modal.locator('.modal-title')).toContainText(entity.name);
        });
    });
});

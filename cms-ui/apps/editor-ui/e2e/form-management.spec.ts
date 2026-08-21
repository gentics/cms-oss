import {
    AccessControlledType,
    FormSaveRequest,
    GcmsPermission,
    NodeFeature,
    Variant,
} from '@gentics/cms-models';
import { cloneWithSymbols } from '@gentics/common';
import {
    clickModalAction,
    clickNotificationAction,
    EntityImporter,
    findNotification,
    FORM_ONE,
    FORM_TWO,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    ImportPermissions,
    isVariant,
    ITEM_TYPE_FORM,
    LANGUAGE_DE,
    LANGUAGE_EN,
    loginWithForm,
    matchRequest,
    navigateToApp,
    NODE_MINIMAL,
    openContext,
    PAGE_ONE,
    pickSelectValue,
    TestSize,
    UserImportData,
    waitForResponseFrom,
} from '@gentics/e2e-utils';
import { expect, Page, test } from '@playwright/test';
import {
    editorAction,
    expectItemOffline,
    expectItemPublished,
    fgAddControl,
    fgFindEditSidebar,
    fgSelectElementTab,
    findItem,
    findList,
    itemAction,
    selectNode,
} from './helpers';

test.describe('Form Management', () => {
    test.skip(() => !isVariant(Variant.ENTERPRISE), 'Requires Enterpise features');

    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'formmngt';

    const NEW_FORM_NAME = 'Hello World';
    const CHANGE_FORM_NAME = 'Hello World again';
    const NEW_FORM_DESCRIPTION = 'This is an example text';

    const FORM_TYPE_GENERIC = 'generic';

    const TEST_GROUP_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_editor`,

        description: 'Form Management: Editor',
        name: `group_${NAMESPACE}_editor`,
        permissions: [],
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_editor`,

        group: TEST_GROUP_BASE,

        email: 'something@example.com',
        firstName: 'FormManagement',
        lastName: 'Editor',
        login: `${NAMESPACE}_editor`,
        password: 'testforms',
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
        });

        await test.step('Specialized Test Setup', async () => {
            await IMPORTER.setupFeatures(TestSize.MINIMAL, {
                [NodeFeature.FORMS]: true,
            });
            const nodeId = IMPORTER.get(NODE_MINIMAL).id;
            await IMPORTER.client.form.assignConfiguration(FORM_TYPE_GENERIC, nodeId).send();
            await IMPORTER.importData([FORM_ONE]);
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
            await selectNode(page, IMPORTER.get(NODE_MINIMAL).id);
        });
    }

    test('should be possible to create a new form', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.VIEW_FORM, value: true },
                    { type: GcmsPermission.CREATE_FORM, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_FORM);
        await list.locator('.header-controls [data-action="create-new-item"] button').click();

        const modal = page.locator('create-form-modal');
        const form = modal.locator('gtx-form-properties');

        await form.locator('[formcontrolname="name"] input').fill(NEW_FORM_NAME);
        await pickSelectValue(form.locator('[formcontrolname="formType"]'), FORM_TYPE_GENERIC);
        await form.locator('[formcontrolname="description"] input').fill(NEW_FORM_DESCRIPTION);
        await pickSelectValue(form.locator('[formcontrolname="languages"]'), [LANGUAGE_DE]);

        const formUpdate = page.waitForResponse(matchRequest('POST', '/rest/form'));

        await modal.locator('.modal-footer [data-action="confirm"] button').click();

        const response = await formUpdate;
        const responseBody = await response.json();
        const formId = responseBody.item.id;

        await expect(list.locator(`[data-id="${formId}"]`)).toBeVisible();
    });

    test('should not be possible to edit the form properties without permissions', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19638',
        }],
    }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.VIEW_FORM, value: true },
                ],
            },
        ]);

        const formEntity = IMPORTER.get(FORM_ONE);
        const list = findList(page, ITEM_TYPE_FORM);
        const item = findItem(list, formEntity.id);

        await itemAction(item, 'properties');

        const form = page.locator('content-frame combined-properties-editor .properties-content gtx-form-properties');
        await expect(form.locator('[formcontrolname="name"] input')).toBeDisabled();
        await expect(form.locator('[formcontrolname="description"] input')).toBeDisabled();
    });

    test('should load forms on initial navigation', async ({ page }) => {
        const EDITING_FORM = IMPORTER.get(FORM_ONE);

        const loadReq = page.waitForResponse(matchRequest('GET', '/rest/form', {
            params: {
                folderId: `${EDITING_FORM.folderId}`,
            },
        }));

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.VIEW_FORM, value: true },
                ],
            },
        ]);

        await loadReq;

        const list = findList(page, ITEM_TYPE_FORM);
        const item = findItem(list, EDITING_FORM.id);
        await expect(item).toBeVisible();
    });

    test('should be able to change success-page correctly', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-18694',
        }],
    }, async ({ page }) => {
        const SUCCESS_PAGE = IMPORTER.get(PAGE_ONE);
        const SUCCESS_FOLDER = IMPORTER.get(NODE_MINIMAL);
        const EDITING_FORM = IMPORTER.get(FORM_ONE);
        const SUCCESS_URL = 'https://gentics.com';

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.VIEW_FORM, value: true },
                    { type: GcmsPermission.UPDATE_FORM, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_FORM);
        const item = findItem(list, EDITING_FORM.id);
        await itemAction(item, 'properties');

        const form = page.locator('gtx-form-properties');
        const successPicker = form.locator('[data-action="pick-success-page"]');
        const breadcrumbs = successPicker.locator('.breadcrumbs');

        // Select page
        await test.step('Set success page', async () => {
            await successPicker.locator('[data-action="browse"]').click();
            const repoBrowser = page.locator('repository-browser');
            await repoBrowser.locator(`repository-browser-list[data-type="page"] [data-id="${SUCCESS_PAGE.id}"] .item-checkbox label`).click();
            await repoBrowser.locator('.modal-footer [data-action="confirm"] button').click();

            // Validate that the page has been selected
            await page.waitForTimeout(3_000);
            await expect(successPicker.locator('.display-value')).toHaveAttribute('data-value', `page:${SUCCESS_PAGE.id}:${SUCCESS_PAGE.masterNodeId}`);
            await expect(successPicker.locator('.display-value')).toContainText(SUCCESS_PAGE.name);
            await expect(breadcrumbs).toContainText(SUCCESS_FOLDER.name);
        });

        await test.step('Validate success page save', async () => {
            // Save and validate the request
            const saveReq = page.waitForResponse(matchRequest('PUT', `/rest/form/${EDITING_FORM.id}`));
            await editorAction(page, 'save');
            const saveRes = await saveReq;
            const saveData: FormSaveRequest = await saveRes.request().postDataJSON();

            // Saves the correct page
            expect(saveData.data.successPageId).toEqual(SUCCESS_PAGE.id);
            expect(saveData.data.successNodeId).toEqual(SUCCESS_PAGE.masterNodeId);
            // Clears potentially saved old URLs
            expect(saveData.data.successUrlI18n).toEqual({});
        });

        await test.step('Validate loading of set page', async () => {
            // Open the properties again and validate that the item has properly loaded the page
            const pageLoadReq = waitForResponseFrom(page, 'GET', `/rest/page/load/${SUCCESS_PAGE.id}`);
            await page.waitForTimeout(2_000);
            await editorAction(page, 'close');

            await itemAction(item, 'properties');
            await pageLoadReq;

            // Validate that the picker has the correct values loaded again
            await expect(successPicker.locator('.display-value')).toHaveAttribute('data-value', `page:${SUCCESS_PAGE.id}:${SUCCESS_PAGE.masterNodeId}`);
            await expect(successPicker.locator('.display-value')).toContainText(SUCCESS_PAGE.name);
            await expect(breadcrumbs).toContainText(SUCCESS_FOLDER.name);
        });

        await test.step('Set external success url', async () => {
            await page.waitForTimeout(500);

            // Change it to use a success url instead
            await form.locator('gtx-radio-button[data-id="success-url"] label').click();
            await form.locator('[formControlName="successUrlI18n"] input').fill(SUCCESS_URL);

            await page.waitForTimeout(500);

            // Save and validate the request
            const saveReq = page.waitForResponse(matchRequest('PUT', `/rest/form/${EDITING_FORM.id}`));
            await editorAction(page, 'save');
            const saveRes = await saveReq;
            const saveData = await saveRes.request().postDataJSON() as FormSaveRequest;

            // Saves the correct data for the URL
            expect(saveData.data.successUrlI18n[LANGUAGE_EN]).toEqual(SUCCESS_URL);
            // Clears the old selected page
            expect(saveData.data.successPageId).toEqual(0);
            expect(saveData.data.successNodeId).toEqual(0);
        });
    });

    test('should be possible to publish the form after saving properties', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-18802',
        }],
    }, async ({ page }) => {
        const EDITING_FORM = IMPORTER.get(FORM_ONE);

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.VIEW_FORM, value: true },
                    { type: GcmsPermission.UPDATE_FORM, value: true },
                    { type: GcmsPermission.PUBLISH_FORM, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_FORM);
        const item = findItem(list, EDITING_FORM.id);

        // expect the form to be offline
        await expectItemOffline(item);

        await test.step('Update Form', async () => {
            await itemAction(item, 'properties');

            const propertiesform = page.locator('content-frame combined-properties-editor .properties-content gtx-form-properties');
            await propertiesform.locator('[formcontrolname="name"] input').fill(CHANGE_FORM_NAME);

            await editorAction(page, 'save');
        });

        await test.step('Publish with notification action', async () => {
            const publishReq = waitForResponseFrom(page, 'PUT', `/rest/form/${EDITING_FORM.id}/online`, {
                params: {
                    at: '0',
                },
            });

            // toast with success notification should have the "publish" action
            const publishToast = findNotification(page, `form-save-success-with-publish:${EDITING_FORM.id}`);
            await clickNotificationAction(publishToast);

            await publishReq;

            // form should be published now
            await expectItemPublished(item);
        });
    });

    test('should display the label value as title', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19032',
        }],
    }, async ({ page }) => {
        const EDITING_FORM = IMPORTER.get(FORM_ONE);
        const LABEL_TEXT = 'Hello World';

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.VIEW_FORM, value: true },
                    { type: GcmsPermission.UPDATE_FORM, value: true },
                ],
            },
        ]);

        await test.step('Open Editor', async () => {
            const list = findList(page, ITEM_TYPE_FORM);
            const item = findItem(list, EDITING_FORM.id);
            await itemAction(item, 'edit');
        });

        await test.step('Edit Form', async () => {
            const grid = page.locator('content-frame gtx-form-grid');
            const el = await fgAddControl(grid, 'number');

            await expect(el).toBeVisible();
            await el.click();
            await expect(el).toContainClass('is-selected');

            const editSidebar = fgFindEditSidebar(grid);
            await expect(editSidebar).toBeVisible();

            const elLabel = el.locator('.element-container .element-title');
            const translationTab = await fgSelectElementTab(editSidebar, 'translations');
            const labelCtrl = translationTab.locator('[data-control="label"] input');

            await labelCtrl.fill(LABEL_TEXT);
            await expect(elLabel).toHaveText(LABEL_TEXT);
        });
    });

    test('should edit and save selectable-options correctly', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19335',
        }],
    }, async ({ page }) => {
        const EDITING_FORM = IMPORTER.get(FORM_ONE);
        const KEY_TEXT = 'Hello World';
        const VALUE_TEXT = 'Foo Bar Content!';

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.VIEW_FORM, value: true },
                    { type: GcmsPermission.UPDATE_FORM, value: true },
                ],
            },
        ]);

        await test.step('Open Editor', async () => {
            const list = findList(page, ITEM_TYPE_FORM);
            const item = findItem(list, EDITING_FORM.id);
            await itemAction(item, 'edit');
        });

        await test.step('Edit Form', async () => {
            const grid = page.locator('content-frame gtx-form-grid');
            const el = await fgAddControl(grid, 'catalog');

            await expect(el).toBeVisible();
            await el.click();
            await expect(el).toContainClass('is-selected');

            const editSidebar = fgFindEditSidebar(grid);
            await expect(editSidebar).toBeVisible();

            const definitionTab = await fgSelectElementTab(editSidebar, 'definition');
            const keyOptions = definitionTab.locator('[data-control="selectOptions"]');

            // Add a new option, fill it out, save
            await keyOptions.locator('[data-action="add-option"]').click();
            await keyOptions.locator('gtx-input input').fill(KEY_TEXT);

            // Open the translations tab, and edit the label text
            const translationTab = await fgSelectElementTab(editSidebar, 'translations');
            const valueOptions = translationTab.locator('[data-control="selectOptions"]');

            // Should have the key as default value set initially
            await expect(valueOptions.locator('input')).toHaveValue(KEY_TEXT);
            await valueOptions.locator('input').fill(VALUE_TEXT);
        });

        await test.step('Save and Validate', async () => {
            const saveReq = page.waitForResponse(matchRequest('PUT', '/rest/form/*'));
            await editorAction(page, 'save');
            const res = await saveReq;
            const req: FormSaveRequest = res.request().postDataJSON();

            const props = Object.entries(req.data.schema?.properties || {});
            expect(props).toHaveLength(1);

            const el = props[0][1];
            expect(el.formGridOptions.selectOptions).toEqual([{
                _defaulted: [], // Internal structure
                value: KEY_TEXT,
                label: {
                    [LANGUAGE_EN]: VALUE_TEXT,
                },
            }]);
        });
    });

    test('should be possible to delete a language variant of a form', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19642',
        }],
    }, async ({ page }) => {
        // import the language in two languages
        await IMPORTER.importData([FORM_TWO]);

        const DE_EN_FORM = IMPORTER.get(FORM_TWO);

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.VIEW_FORM, value: true },
                    { type: GcmsPermission.UPDATE_FORM, value: true },
                    { type: GcmsPermission.DELETE_FORM, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_FORM);

        await test.step('Delete english variant', async () => {
            const deEnFormItem = findItem(list, DE_EN_FORM.id);
            await itemAction(deEnFormItem, 'delete');

            // delete modal should be opened
            const modal = page.locator('multi-delete-modal-modal');
            await expect(modal).toBeVisible();

            // click the language selector
            await pickSelectValue(modal.locator('gtx-form-language-selector gtx-select'), ['de']);

            // prepare the expected requests
            const saveRequest = waitForResponseFrom(page, 'PUT', `/rest/form/${DE_EN_FORM.id}`);
            const loadListRequest = waitForResponseFrom(page, 'GET', '/rest/form');

            // click "delete"
            await clickModalAction(modal, 'confirm');

            // wait for the form to be saved and the list to be reloaded
            await saveRequest;
            await loadListRequest;

            // english should be deleted now
            const enLangIndicator = deEnFormItem.locator('.item-primary .language-indicator [data-action="page-language"][data-id="en"]');
            await expect(enLangIndicator).toBeHidden(); // Should be hidden, since when the language isn't available

            // german should still be here
            const deLangIndicator = deEnFormItem.locator('.item-primary .language-indicator [data-action="page-language"][data-id="de"]');
            await expect(deLangIndicator.locator('.indicator.indicator-untranslated')).not.toBeAttached();
        });
    });
});

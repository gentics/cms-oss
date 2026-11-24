import {
    AccessControlledType,
    FormSaveRequest,
    GcmsPermission,
    NodeFeature,
    TagPropertyType,
    Variant,
} from '@gentics/cms-models';
import { cloneWithSymbols } from '@gentics/common';
import {
    BASIC_TEMPLATE_ID,
    clickModalAction,
    clickNotificationAction,
    CONSTRUCT_TEST_FORM,
    EntityImporter,
    findNotification,
    FOLDER_A,
    FORM_ONE,
    FORM_TWO,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    ImportPermissions,
    isVariant,
    ITEM_TYPE_FOLDER,
    ITEM_TYPE_FORM,
    ITEM_TYPE_PAGE,
    LANGUAGE_DE,
    LANGUAGE_EN,
    loginWithForm,
    matchRequest,
    navigateToApp,
    NODE_FULL,
    NODE_MINIMAL,
    openContext,
    PAGE_ONE,
    PageImportData,
    pickSelectValue,
    TestSize,
    UserImportData,
    waitForResponseFrom,
} from '@gentics/e2e-utils';
import { expect, Locator, Page, test } from '@playwright/test';
import {
    editorAction,
    expectItemOffline,
    expectItemPublished,
    findItem,
    findList,
    findRepoBrowserItem,
    findRepoBrowserList,
    itemAction,
    selectItem,
    selectNode,
    toggleDisplayAllCheckbox,
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
            expect(saveData.data.successUrl).toEqual({});
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
            await form.locator('[formControlName="successUrl"] input').fill(SUCCESS_URL);

            await page.waitForTimeout(500);

            // Save and validate the request
            const saveReq = page.waitForResponse(matchRequest('PUT', `/rest/form/${EDITING_FORM.id}`));
            await editorAction(page, 'save');
            const saveRes = await saveReq;
            const saveData = await saveRes.request().postDataJSON() as FormSaveRequest;

            // Saves the correct data for the URL
            expect(saveData.data.successUrl[LANGUAGE_EN]).toEqual(SUCCESS_URL);
            // Clears the old selected page
            expect(saveData.data.successPageId).toEqual(0);
            expect(saveData.data.successNodeId).toEqual(0);
        });
    });

    test('should store an external success url as successUrl', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20415',
        }],
    }, async ({ page }) => {
        const EDITING_FORM = IMPORTER.get(FORM_ONE);
        const SUCCESS_URL = 'https://gentics.com/thank-you';

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

        await test.step('Set and save external success url', async () => {
            await form.locator('gtx-radio-button[data-id="success-url"] label').click();
            await form.locator('.confirmation-page-setting.lang-select gtx-i18n-input input').fill(SUCCESS_URL);

            await page.waitForTimeout(500);

            const saveReq = page.waitForResponse(matchRequest('PUT', `/rest/form/${EDITING_FORM.id}`));
            await editorAction(page, 'save');
            const saveRes = await saveReq;
            expect(saveRes.ok()).toBe(true);
        });

        await test.step('Validate stored form data', async () => {
            const loaded = await IMPORTER.client.form.get(EDITING_FORM.id).send();
            const data = loaded.item.data as unknown as Record<string, unknown>;

            expect(data['successUrl']).toEqual({ [LANGUAGE_EN]: SUCCESS_URL });
            expect(data['successUrlI18n']).toBeUndefined();
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

    test('should display the pages using the form in the usage modal', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20334',
        }],
    }, async ({ page }) => {
        const USED_FORM = IMPORTER.get(FORM_ONE);
        const FORM_CONSTRUCT = IMPORTER.get(CONSTRUCT_TEST_FORM);

        const USING_PAGE: PageImportData = {
            [IMPORT_TYPE]: ITEM_TYPE_PAGE,
            [IMPORT_ID]: `page_${NAMESPACE}_form_usage`,

            folderId: NODE_MINIMAL[IMPORT_ID],
            nodeId: NODE_MINIMAL[IMPORT_ID],
            templateId: BASIC_TEMPLATE_ID,

            pageName: 'Page using the form',
            fileName: 'page-using-the-form',
            language: LANGUAGE_EN,
            priority: 1,

            tags: {
                formUsage: {
                    id: null,
                    constructId: FORM_CONSTRUCT.id,
                    name: 'formUsage',
                    active: true,
                    type: 'CONTENTTAG',
                    properties: {
                        form: {
                            type: TagPropertyType.CMSFORM,
                            formId: USED_FORM.id,
                        },
                    },
                } as any,
            },
        };

        await test.step('Create page using the form', async () => {
            await IMPORTER.importData([USING_PAGE]);
        });

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.VIEW_FORM, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_FORM);

        await test.step('Setup display fields', async () => {
            const listOptions = await openContext(list.locator('gtx-form-list-header [data-action="open-list-context"]'));
            await listOptions.locator('[data-action="select-display-fields"]').click();
            const modal = page.locator('display-field-selector');
            await modal.locator('gtx-contents-list-item[data-id="usage"] label').click();
            await clickModalAction(modal, 'confirm');
        });

        const item = findItem(list, USED_FORM.id);
        const usageChip = item.locator('list-item-details detail-chip.usage');
        await expect(usageChip.locator('.usage-count')).toHaveText('1');

        const usageReq = waitForResponseFrom(page, 'GET', '/rest/form/usage/page');
        await usageChip.click();
        await usageReq;

        const usageModal = page.locator('gtx-usage-modal');
        await expect(usageModal).toBeVisible();
        await expect(usageModal.locator('.empty-message')).toBeHidden();
        await expect(usageModal.locator('gtx-item-usage-list .usage-item .item-name')).toHaveText([IMPORTER.get(USING_PAGE).name]);
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

        const list = findList(page, ITEM_TYPE_FORM);
        await toggleDisplayAllCheckbox(page, list, 'toggle-wastebin', false);
        await toggleDisplayAllCheckbox(page, list, 'toggle-wastebin', true);
    });

    test('should be possible to toggle display of all languages', {
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

        const list = findList(page, ITEM_TYPE_FORM);
        await toggleDisplayAllCheckbox(page, list, 'toggle-language-display', false);
        await toggleDisplayAllCheckbox(page, list, 'toggle-language-display', true);
    });

    test.describe('Moving', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20217',
        }],
    }, () => {
        const MOVE_WARNING_MODAL = 'gtx-move-forms-warning-modal';

        test.beforeEach(async ({ page }) => {
            // Setting up the content of two nodes takes longer than the default timeout
            test.slow();

            await test.step('Second Node Setup', async () => {
                // The forms are moved from the minimal node into the full node
                await IMPORTER.setupTest(TestSize.FULL);
                await IMPORTER.setupFeatures(TestSize.FULL, {
                    [NodeFeature.FORMS]: true,
                });
                await IMPORTER.client.form.assignConfiguration(FORM_TYPE_GENERIC, IMPORTER.get(NODE_FULL).id).send();
                await IMPORTER.importData([FORM_TWO]);
            });

            const formPermissions = [
                { type: GcmsPermission.READ, value: true },
                { type: GcmsPermission.READ_ITEMS, value: true },
                { type: GcmsPermission.VIEW_FORM, value: true },
                { type: GcmsPermission.CREATE_FORM, value: true },
                { type: GcmsPermission.UPDATE_FORM, value: true },
                { type: GcmsPermission.DELETE_FORM, value: true },
            ];
            await setupWithPermissions(page, [NODE_MINIMAL, NODE_FULL].map((node) => ({
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(node).folderId}`,
                subObjects: true,
                perms: formPermissions,
            })));
        });

        /**
         * Collects all requests which would move the given form, so that tests can verify
         * that no move has been triggered.
         */
        function trackMoveRequests(page: Page, formId: number): string[] {
            const requests: string[] = [];
            page.on('request', (req) => {
                if (matchRequest('PUT', new RegExp(`/rest/form/${formId}/folder/\\d+`))(req)) {
                    requests.push(req.url());
                }
            });
            return requests;
        }

        /** Triggers the move via the given action and selects the root folder of the other node in the repository browser. */
        async function moveToOtherNode(page: Page, action: () => Promise<void>): Promise<void> {
            await action();

            const repoBrowser = page.locator('repository-browser');
            await repoBrowser.waitFor();
            await selectNode(repoBrowser, IMPORTER.get(NODE_FULL).id);
            await clickModalAction(repoBrowser, 'confirm');
            await repoBrowser.waitFor({ state: 'detached' });
        }

        async function getFormFolderId(formId: number): Promise<number> {
            const res = await IMPORTER.client.form.get(formId).send();
            return res.item.folderId;
        }

        function findModalForms(modal: Locator): Locator {
            return modal.locator('.forms-to-move gtx-contents-list-item');
        }

        test('should ask for confirmation and not move the form when cancelled', async ({ page }) => {
            const FORM = IMPORTER.get(FORM_ONE);
            const moveRequests = trackMoveRequests(page, FORM.id);

            const list = findList(page, ITEM_TYPE_FORM);
            await moveToOtherNode(page, () => itemAction(findItem(list, FORM.id), 'move'));

            const modal = page.locator(MOVE_WARNING_MODAL);
            await expect(modal).toBeVisible();
            await expect(findModalForms(modal)).toHaveCount(1);
            await expect(modal.locator(`.forms-to-move gtx-contents-list-item[data-id="${FORM.id}"] .item-name`)).toHaveText(FORM.name);

            await clickModalAction(modal, 'cancel');
            await modal.waitFor({ state: 'detached' });

            expect(moveRequests).toHaveLength(0);
            expect(await getFormFolderId(FORM.id)).toEqual(FORM.folderId);
            await expect(findItem(list, FORM.id)).toBeVisible();
        });

        test('should move the form to the other node when confirmed', async ({ page }) => {
            const FORM = IMPORTER.get(FORM_ONE);
            const TARGET_NODE = IMPORTER.get(NODE_FULL);

            const list = findList(page, ITEM_TYPE_FORM);
            await moveToOtherNode(page, () => itemAction(findItem(list, FORM.id), 'move'));

            const modal = page.locator(MOVE_WARNING_MODAL);
            await expect(modal).toBeVisible();

            const moveReq = page.waitForResponse(matchRequest('PUT', `/rest/form/${FORM.id}/folder/${TARGET_NODE.folderId}`));
            await clickModalAction(modal, 'confirm');
            await moveReq;
            await modal.waitFor({ state: 'detached' });

            expect(await getFormFolderId(FORM.id)).toEqual(TARGET_NODE.folderId);
        });

        test('should list all selected forms in the confirmation', async ({ page }) => {
            const FIRST_FORM = IMPORTER.get(FORM_ONE);
            const SECOND_FORM = IMPORTER.get(FORM_TWO);
            const TARGET_NODE = IMPORTER.get(NODE_FULL);

            const list = findList(page, ITEM_TYPE_FORM);
            await selectItem(findItem(list, FIRST_FORM.id));
            await selectItem(findItem(list, SECOND_FORM.id));

            await moveToOtherNode(page, () => list.locator('gtx-form-list-header .group-actions [data-action="move"] button').click());

            const modal = page.locator(MOVE_WARNING_MODAL);
            await expect(modal).toBeVisible();
            await expect(findModalForms(modal)).toHaveCount(2);
            await expect(modal.locator(`.forms-to-move [data-id="${FIRST_FORM.id}"] .item-name`)).toHaveText(FIRST_FORM.name);
            await expect(modal.locator(`.forms-to-move [data-id="${SECOND_FORM.id}"] .item-name`)).toHaveText(SECOND_FORM.name);

            const firstMoveReq = page.waitForResponse(matchRequest('PUT', `/rest/form/${FIRST_FORM.id}/folder/${TARGET_NODE.folderId}`));
            const secondMoveReq = page.waitForResponse(matchRequest('PUT', `/rest/form/${SECOND_FORM.id}/folder/${TARGET_NODE.folderId}`));
            await clickModalAction(modal, 'confirm');
            await Promise.all([firstMoveReq, secondMoveReq]);

            expect(await getFormFolderId(FIRST_FORM.id)).toEqual(TARGET_NODE.folderId);
            expect(await getFormFolderId(SECOND_FORM.id)).toEqual(TARGET_NODE.folderId);
        });

        test('should not ask for confirmation when moving the form within the same node', async ({ page }) => {
            const FORM = IMPORTER.get(FORM_ONE);
            const TARGET_FOLDER = IMPORTER.get(FOLDER_A);

            const list = findList(page, ITEM_TYPE_FORM);
            await itemAction(findItem(list, FORM.id), 'move');

            const repoBrowser = page.locator('repository-browser');
            await repoBrowser.waitFor();
            // Navigate into the target folder of the same node
            await findRepoBrowserItem(findRepoBrowserList(repoBrowser, ITEM_TYPE_FOLDER), TARGET_FOLDER.id)
                .locator('.item-name a')
                .click();

            const moveReq = page.waitForResponse(matchRequest('PUT', `/rest/form/${FORM.id}/folder/${TARGET_FOLDER.id}`));
            await clickModalAction(repoBrowser, 'confirm');
            await moveReq;

            await expect(page.locator(MOVE_WARNING_MODAL)).not.toBeAttached();
            expect(await getFormFolderId(FORM.id)).toEqual(TARGET_FOLDER.id);
        });
    });
});

import { CmsFormElementI18nValue, CmsFormElementKeyI18nValuePair, Form, FormSaveRequest, NodeFeature, Variant } from '@gentics/cms-models';
import {
    clickNotificationAction,
    EntityImporter,
    findNotification,
    FORM_ONE,
    isVariant,
    ITEM_TYPE_FORM,
    LANGUAGE_DE,
    loginWithForm,
    matchesUrl,
    matchRequest,
    navigateToApp,
    NODE_MINIMAL,
    PAGE_ONE,
    pickSelectValue,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import { editorAction, expectItemOffline, expectItemPublished, findItem, findList, itemAction, selectNode } from './helpers';

test.describe('Form Management', () => {
    test.skip(() => !isVariant(Variant.ENTERPRISE), 'Requires Enterpise features');

    const IMPORTER = new EntityImporter();
    const NEW_FORM_NAME = 'Hello World';
    const CHANGE_FORM_NAME = 'Hello World again';
    const NEW_FORM_DESCRIPTION = 'This is an example text';

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
            await IMPORTER.importData([FORM_ONE]);
        });
    });

    test('should be possible to create a new form', async ({ page }) => {
        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);

        const list = findList(page, ITEM_TYPE_FORM);
        await list.locator('.header-controls [data-action="create-new-item"] button').click();

        const modal = page.locator('create-form-modal');
        const form = modal.locator('gtx-form-properties');

        await form.locator('[formcontrolname="name"] input').fill(NEW_FORM_NAME);
        await form.locator('[formcontrolname="description"] input').fill(NEW_FORM_DESCRIPTION);
        await pickSelectValue(form.locator('[formcontrolname="languages"]'), [LANGUAGE_DE]);

        const formUpdate = page.waitForResponse(matchRequest('POST', '/rest/form'));

        await modal.locator('.modal-footer [data-action="confirm"] button').click();

        const response = await formUpdate;
        const responseBody = await response.json();
        const formId = responseBody.item.id;

        await expect(list.locator(`[data-id="${formId}"]`)).toBeVisible();
    });

    test('should load forms on initial navigation', async ({ page }) => {
        const EDITING_FORM = IMPORTER.get(FORM_ONE);

        const loadReq = page.waitForResponse(matchRequest('GET', '/rest/form', {
            params: {
                folderId: `${EDITING_FORM.folderId}`,
            },
        }));
        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);
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

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);

        const list = findList(page, ITEM_TYPE_FORM);
        const item = findItem(list, EDITING_FORM.id);
        await itemAction(item, 'properties');

        const form = page.locator('gtx-form-properties');
        const successPicker = form.locator('[data-action="pick-success-page"]');
        const breadcrumbs = form.locator('.success-page-breadcrumbs .breadcrumb-path');

        // Select page
        await successPicker.locator('[data-action="browse"]').click();
        const repoBrowser = page.locator('repository-browser');
        await repoBrowser.locator(`repository-browser-list[data-type="page"] [data-id="${SUCCESS_PAGE.id}"] .item-checkbox label`).click();
        await repoBrowser.locator('.modal-footer [data-action="confirm"] button').click();

        // Validate that the page has been selected
        await page.waitForTimeout(3_000);
        expect(successPicker).toHaveAttribute('data-target-id', `${SUCCESS_PAGE.id}`);
        expect(successPicker.locator('.value-display input')).toHaveValue(SUCCESS_PAGE.name);
        expect(breadcrumbs).toHaveText(SUCCESS_FOLDER.name);

        // Save and validate the request
        let saveReq = page.waitForResponse(matchRequest('PUT', `/rest/form/${EDITING_FORM.id}`));
        await editorAction(page, 'save');
        let saveRes = await saveReq;
        let saveData: FormSaveRequest = await saveRes.request().postDataJSON();

        expect(saveData.successPageId).toEqual(SUCCESS_PAGE.id);
        expect(saveData.successNodeId).toEqual(SUCCESS_PAGE.masterNodeId);

        // Open the properties again and validate that the item has properly loaded the page
        const pageLoadReq = page.waitForResponse(matchRequest('GET', `/rest/page/load/${SUCCESS_PAGE.id}`));
        await page.waitForTimeout(2_000);
        await editorAction(page, 'close');

        // FIXME: Temporary fix for the item changed warning, even tho we saved it.
        const hasModal = await page.evaluate(() => window.document.querySelector('confirm-navigation-modal') != null);
        if (hasModal) {
            await page.click('confirm-navigation-modal gtx-button[type="alert"] button');
        }

        await itemAction(item, 'properties');
        await pageLoadReq;

        // Validate that the picker has the correct values loaded again
        expect(successPicker).toHaveAttribute('data-target-id', `${SUCCESS_PAGE.id}`);
        expect(breadcrumbs).toHaveText(SUCCESS_FOLDER.name);

        // Change it to use a success url instead
        await form.locator('gtx-radio-button[data-id="success-url"] label').click();
        await form.locator('[formControlName="successurl_i18n"] input').fill(SUCCESS_URL);

        // Save and validate the request
        saveReq = page.waitForResponse(matchRequest('PUT', `/rest/form/${EDITING_FORM.id}`));
        await editorAction(page, 'save');
        saveRes = await saveReq;
        saveData = await saveRes.request().postDataJSON();

        expect(saveData.successPageId).toEqual(0);
        expect(saveData.successNodeId).toEqual(0);
        expect(saveData.data.successurl_i18n[EDITING_FORM.languages[0]]).toEqual(SUCCESS_URL);
    });

    test('should be possible to publish the form after saving properties', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-18802',
        }],
    }, async ({page}) => {
        const EDITING_FORM = IMPORTER.get(FORM_ONE);

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);

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
            const publishReq = page.waitForResponse(matchRequest('PUT', `/rest/form/${EDITING_FORM.id}/online`, {
                params: {
                    at: '0',
                },
            }));

            // toast with success notification should have the "publish" action
            const publishToast = findNotification(page, `form-save-success-with-publish:${EDITING_FORM.id}`);
            await clickNotificationAction(publishToast);

            await publishReq;

            // form should be published now
            await expectItemPublished(item);
        });
    });

    test('should display an error message when form config is missing', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-18932',
        }],
    }, async ({ page }) => {
        const EDITING_FORM = IMPORTER.get(FORM_ONE);

        await test.step('Specialized Setup', async () => {
            // Block requests to the config
            await page.route(url => matchesUrl(url, '/ui-conf/form-editor.json'), route => {
                return route.abort('failed');
            });

            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);
        });

        const list = findList(page, ITEM_TYPE_FORM);
        const item = findItem(list, EDITING_FORM.id);

        await test.step('Diplay when creating new form', async () => {
            await list.locator('.header-controls [data-action="create-new-item"] button').click();

            const modal = page.locator('create-form-modal');
            const form = modal.locator('gtx-form-properties');

            await expect(form.locator('form')).not.toBeVisible();
            await expect(form.locator('.form-editor-error')).toBeVisible();

            await modal.locator('.modal-footer [data-action="cancel"] button').click();
        });

        await test.step('Display in Form Edit-Mode', async () => {
            await itemAction(item, 'edit');

            await expect(page.locator('content-frame gtx-form-editor .form-editor-error')).toBeVisible();
            await expect(page.locator('content-frame gtx-form-editor .form-editor-menu-container > *')).not.toBeAttached();

            await editorAction(page, 'close');
        });

        await test.step('Display in Form Properties', async () => {
            await itemAction(item, 'properties');

            await expect(page.locator('content-frame combined-properties-editor gtx-properties-editor form')).not.toBeVisible();
            await expect(page.locator('content-frame combined-properties-editor gtx-properties-editor .form-editor-error')).toBeVisible();
        });
    });

    // TODO: Flaky on CI
    test('should display the label value as title', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19032',
        }],
    }, async ({ page }) => {
        const EDITING_FORM = IMPORTER.get(FORM_ONE);
        const LABEL_TEXT = 'Hello World';

        await test.step('Generic Setup', async () => {
            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);
        });

        await test.step('Open Editor', async () => {
            const list = findList(page, ITEM_TYPE_FORM);
            const item = findItem(list, EDITING_FORM.id);
            await itemAction(item, 'edit');
        });

        await test.step('Edit Form', async () => {
            const editor = page.locator('content-frame gtx-form-editor');
            const menu = editor.locator('gtx-form-editor-menu');
            const list = editor.locator('.form-editor-form > gtx-form-editor-element-list');

            const menuEl = menu.locator('.form-editor-elements-container .form-editor-menu-element').first();
            await menuEl.dragTo(list.locator('gtx-form-element-drop-zone').first());

            const el = list.locator('gtx-form-editor-element');
            const header = el.locator('.form-element-container-header');
            await header.locator('.form-element-btn-properties-editor-toggle').click();

            const elEditor = el.locator('gtx-form-element-properties-editor');
            await elEditor.locator('[data-control="label"] input').fill(LABEL_TEXT);

            await expect(el.locator('.form-element-preview-container .label-property-value-container')).toHaveText(LABEL_TEXT);
        });
    });

    test('should edit and save selectable-options correctly', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19335',
        }]
    }, async ({ page }) => {
        const EDITING_FORM = IMPORTER.get(FORM_ONE);
        const KEY_TEXT = ' \n Hello World \t ';
        const VALUE_TEXT = ' \tFoo Bar Content! \n  ';

        await test.step('Generic Setup', async () => {
            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);
        });

        await test.step('Open Editor', async () => {
            const list = findList(page, ITEM_TYPE_FORM);
            const item = findItem(list, EDITING_FORM.id);
            await itemAction(item, 'edit');
        });

        await test.step('Edit Form', async () => {
            const editor = page.locator('content-frame gtx-form-editor');
            const menu = editor.locator('gtx-form-editor-menu');
            const list = editor.locator('.form-editor-form > gtx-form-editor-element-list');

            await page.waitForTimeout(2_000);

            const menuEl = menu.locator('.form-editor-elements-container .form-editor-menu-element').nth(3);
            await menuEl.dragTo(list.locator('gtx-form-element-drop-zone').first());

            const el = list.locator('gtx-form-editor-element');
            const header = el.locator('.form-element-container-header');
            await header.locator('.form-element-btn-properties-editor-toggle').click();

            const elEditor = el.locator('gtx-form-element-properties-editor');
            const options = elEditor.locator('[data-control="options"]');

            await options.locator('.add-button').click();

            await page.waitForTimeout(2_000);

            const entry = options.locator('gtx-sortable-list .list-entry-inputs');
            // Key
            entry.locator('> gtx-input input').fill(KEY_TEXT);

            // No idea why we need a timeout for this, but otherwise the key content
            // is getting put into the value sometimes
            await page.waitForTimeout(500);
            // Value
            entry.locator('gtx-i18n-input input').fill(VALUE_TEXT);
        });

        await test.step('Save and Validate', async () => {
            const saveReq = page.waitForResponse(matchRequest('PUT', '/rest/form/*'));
            await editorAction(page, 'save');
            const res = await saveReq;
            const req: FormSaveRequest = res.request().postDataJSON();

            expect(req.data.elements).toHaveLength(1);

            const entry = req.data.elements[0];
            expect(entry.type).toEqual('selectgroup');
            const options: CmsFormElementKeyI18nValuePair[] = entry.options;
            expect(Array.isArray(options)).toBe(true);
            expect(options).toHaveLength(1);
            expect(options[0].key).toEqual(KEY_TEXT.trim());
            expect(options[0].value_i18n[EDITING_FORM.languages[0]]).toEqual(VALUE_TEXT.trim());
        });
    });
});

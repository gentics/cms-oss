import { FormSaveRequest, NodeFeature, Variant } from '@gentics/cms-models';
import {
    EntityImporter,
    formOne,
    isVariant,
    ITEM_TYPE_FORM,
    LANGUAGE_DE,
    loginWithForm,
    matchesUrl,
    matchRequest,
    minimalNode,
    navigateToApp,
    pageOne,
    pickSelectValue,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import { editorAction, expectItemOffline, expectItemPublished, findItem, findList, itemAction, selectNode } from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('Form Management', () => {
    test.skip(() => !isVariant(Variant.ENTERPRISE), 'Requires Enterpise features');

    const IMPORTER = new EntityImporter();
    const NEW_FORM_NAME = 'Hello World';
    const CHANGE_FORM_NAME = 'Hello World again';
    const NEW_FORM_DESCRIPTION = 'This is an example text';
    // TODO: find a solution to not depend on the translated text
    const PUBLISH_BUTTON_TEXT = 'Veröffentlichen';

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
        await IMPORTER.setupFeatures(TestSize.MINIMAL, {
            [NodeFeature.FORMS]: true,
        });
        await IMPORTER.importData([formOne]);
    });

    test('should be possible to create a new form', async ({ page }) => {
        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);

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
        const EDITING_FORM = IMPORTER.get(formOne);

        const loadReq = page.waitForResponse(matchRequest('GET', '/rest/form', {
            params: {
                folderId: `${EDITING_FORM.folderId}`,
            },
        }));
        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);
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
        const SUCCESS_PAGE = IMPORTER.get(pageOne);
        const SUCCESS_FOLDER = IMPORTER.get(minimalNode);
        const EDITING_FORM = IMPORTER.get(formOne);
        const SUCCESS_URL = 'https://gentics.com';

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);

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
        const EDITING_FORM = IMPORTER.get(formOne);

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);

        const list = findList(page, ITEM_TYPE_FORM);
        const item = findItem(list, EDITING_FORM.id);

        // expect the form to be offline
        await expectItemOffline(item);

        await itemAction(item, 'properties');

        const propertiesform = page.locator('content-frame combined-properties-editor .properties-content gtx-form-properties');

        await propertiesform.locator('[formcontrolname="name"] input').fill(CHANGE_FORM_NAME);

        await editorAction(page, 'save');

        // toast with success notification should have the "publish" action
        const publishButton = page.locator('.gtx-toast .action');

        await expect(publishButton).toHaveText(PUBLISH_BUTTON_TEXT);

        // click the "publish" button
        await publishButton.click();

        // properties form should be closed
        await expect(propertiesform).toBeHidden();

        // form should be published now
        await expectItemPublished(item);
    });

    test('should display an error message when form config is missing', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-18932',
        }],
    }, async ({ page }) => {
        const EDITING_FORM = IMPORTER.get(formOne);

        await test.step('Specialized Setup', async () => {
            // Block requests to the config
            await page.route(url => matchesUrl(url, '/ui-conf/form-editor.json'), route => {
                return route.abort('failed');
            });

            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, IMPORTER.get(minimalNode)!.id);
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
});

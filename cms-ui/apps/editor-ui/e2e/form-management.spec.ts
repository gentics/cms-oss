import { NodeFeature, Variant } from '@gentics/cms-models';
import {
    EntityImporter,
    isVariant,
    ITEM_TYPE_FORM,
    LANGUAGE_DE,
    loginWithForm,
    matchesPath,
    minimalNode,
    navigateToApp,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import { findList, selectNode } from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('Form Management', () => {
    test.skip(() => !isVariant(Variant.ENTERPRISE), 'Requires Enterpise features');
    test.slow();

    const IMPORTER = new EntityImporter();
    const NEW_FORM_NAME = 'Hello World';
    const NEW_FORM_DESCRIPTION = 'This is an example text';

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

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);
    });

    test('should be possible to create a new form', async ({ page }) => {
        const list = findList(page, ITEM_TYPE_FORM);
        await list.locator('.header-controls [data-action="create-new-item"] button').click();

        const modal = page.locator('create-form-modal');
        const form = modal.locator('gtx-form-properties');

        await form.locator('[formcontrolname="name"] input').fill(NEW_FORM_NAME);
        await form.locator('[formcontrolname="description"] input').fill(NEW_FORM_DESCRIPTION);
        await form.locator('[formcontrolname="languages"] gtx-dropdown-trigger').scrollIntoViewIfNeeded();
        await form.locator('[formcontrolname="languages"] gtx-dropdown-trigger').click();
        await page.click(`gtx-dropdown-content [data-id="${LANGUAGE_DE}"]`);
        // Close the dropdown
        await page.click('gtx-scroll-mask');

        const formUpdate = page.waitForResponse(resp => resp.request().method() === 'POST'
            && matchesPath(resp.request().url(), '/rest/form')
            && resp.ok());

        await modal.locator('.modal-footer [data-action="confirm"] button').click();

        const response = await formUpdate;
        const responseBody = await response.json();
        const formId = responseBody.item.id;

        await expect(list.locator(`[data-id="${formId}"]`)).toBeVisible();
    });
});

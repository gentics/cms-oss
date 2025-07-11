import { test, expect } from '@playwright/test';
import { NodeFeature } from '@gentics/cms-models';
import {
    EntityImporter,
    TestSize,
    ITEM_TYPE_FORM,
    minimalNode,
    LANGUAGE_DE,
} from '@gentics/e2e-utils';
import {
    login,
    selectNode,
    findList,
    initPage,
} from './helpers';
import { AUTH_ADMIN } from './common';

test.describe('Form Management', () => {
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
        await initPage(page);
        await page.goto('/');
        await login(page, AUTH_ADMIN);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);
    });

    test('should be possible to create a new form', async ({ page }) => {
        const list = findList(page, ITEM_TYPE_FORM);
        await list.locator('.header-controls [data-action="create-new-item"]').click({ force: true });

        const modal = page.locator('create-form-modal');
        const form = modal.locator('gtx-form-properties');

        await form.locator('[formcontrolname="name"] input').fill(NEW_FORM_NAME);
        await form.locator('[formcontrolname="description"] input').fill(NEW_FORM_DESCRIPTION);
        await form.locator('[formcontrolname="languages"] select').selectOption(LANGUAGE_DE);

        const [response] = await Promise.all([
            page.waitForResponse(resp => resp.url().includes('/rest/form') && resp.status() === 200),
            modal.locator('.modal-footer [data-action="confirm"]').click({ force: true }),
        ]);

        const responseBody = await response.json();
        const formId = responseBody.item.id;

        await expect(list.locator(`[data-id="${formId}"]`)).toBeVisible();
    });
});

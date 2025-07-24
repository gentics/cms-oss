import { test, expect } from '@playwright/test';
import { NodeFeature, Variant } from '@gentics/cms-models';
import {
    EntityImporter,
    TestSize,
    ITEM_TYPE_PAGE,
    minimalNode,
    pageOne,
    isVariant,
    scheduleLinkChecker,
    BASIC_TEMPLATE_ID,
} from '@gentics/e2e-utils';
import {
    login,
    selectNode,
    findList,
    findItem,
    itemAction,
    initPage,
    findAlohaComponent,
    findDynamicFormModal,
    getAlohaIFrame,
} from './helpers';
import { AUTH_ADMIN } from './common';

test.describe('Link Checker', () => {
    test.skip(() => !isVariant(Variant.ENTERPRISE), 'Requires Enterpise features');

    const IMPORTER = new EntityImporter();
    const CLASS_LINKCHECKER_ITEM = 'aloha-gcnlinkchecker-item';
    const CLASS_LINKCHECKER_UNCHECKED = 'aloha-gcnlinkchecker-unchecked';
    const CLASS_LINKCHECKER_CHECKED = 'aloha-gcnlinkchecker-checked';
    const CLASS_LINKCHECKER_VALID = 'aloha-gcnlinkchecker-valid-url';
    const CLASS_LINKCHECKER_INVALID = 'aloha-gcnlinkchecker-invalid-url';

    test.beforeAll(async ({ request }, testInfo) => {
        testInfo.setTimeout(120_000);
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach(async ({ page, request, context }, testInfo) => {
        testInfo.setTimeout(120_000);
        await context.clearCookies();
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.setupTest(TestSize.MINIMAL);
        await IMPORTER.setupFeatures(TestSize.MINIMAL, {
            [NodeFeature.LINK_CHECKER]: true,
        });
        await IMPORTER.importData([scheduleLinkChecker]);
        await IMPORTER.executeSchedule(scheduleLinkChecker);
        await IMPORTER.syncTag(BASIC_TEMPLATE_ID, 'content');
        await initPage(page);
        await page.goto('/');
        await login(page, AUTH_ADMIN);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);
    });

    async function setupEditMode(page) {
        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, IMPORTER.get(pageOne)!.id);
        await itemAction(item, 'edit');
        const iframe = await getAlohaIFrame(page);
        await iframe.locator('main').waitFor({ state:'visible', timeout: 600000 });
        return iframe;
    }

    test('should detect a valid link on insert', async ({ page }) => {
        const iframe = await setupEditMode(page);
        const TEXT_CONTENT = 'Hello ';
        const LINK_URL = 'https://gentics.com';
        const LINK_TITLE = 'This is a title!';

        const content = iframe.locator('main [contenteditable="true"]');
        await content.fill(TEXT_CONTENT);
        // Activate the toolbar
        await content.click();

        const insertLinkButton = findAlohaComponent(page, { slot: 'insertLink', type: 'toggle-split-button' });
        await insertLinkButton.click();

        const modal = await findDynamicFormModal(page);
        const form = modal.locator('.modal-content .form-wrapper');

        await form.locator('[data-slot="url"] .target-input input').fill(LINK_URL);
        await form.locator('[data-slot="title"] input').fill(LINK_TITLE);

        const [checkResponse] = await Promise.all([
            page.waitForResponse(resp => resp.url().includes('/rest/linkChecker/check') && resp.status() === 200),
            modal.locator('.modal-footer [data-action="confirm"]').click(),
        ]);

        const link = content.locator('a');
        await expect(link).toHaveAttribute('href', LINK_URL);
        await expect(link).toHaveAttribute('title', LINK_TITLE);
        await expect(link).toHaveClass(new RegExp(CLASS_LINKCHECKER_ITEM));
        await expect(link).toHaveClass(new RegExp(CLASS_LINKCHECKER_CHECKED));
        await expect(link).toHaveClass(new RegExp(CLASS_LINKCHECKER_VALID));
    });

    test('should detect an invalid link on insert', async ({ page }) => {
        const iframe = await setupEditMode(page);
        const TEXT_CONTENT = 'Hello ';
        const LINK_DOMAIN = 'somedomainwhichwillsurelynotbetaken.com';
        const LINK_URL = `https://${LINK_DOMAIN}`;
        const LINK_TITLE = 'This is a title!';

        const content = iframe.locator('main [contenteditable="true"]');
        await content.fill(TEXT_CONTENT);
        // Activate the toolbar
        await content.click();

        const insertLinkButton = findAlohaComponent(page, { slot: 'insertLink', type: 'toggle-split-button' });
        await insertLinkButton.click();

        const modal = await findDynamicFormModal(page);
        const form = modal.locator('.modal-content .form-wrapper');

        await form.locator('[data-slot="url"] .target-input input').fill(LINK_URL);
        await form.locator('[data-slot="title"] input').fill(LINK_TITLE);

        const [checkResponse] = await Promise.all([
            page.waitForResponse(resp => resp.url().includes('/rest/linkChecker/check') && resp.status() === 200),
            modal.locator('.modal-footer [data-action="confirm"]').click(),
        ]);

        const link = content.locator('a');
        await expect(link).toHaveAttribute('href', LINK_URL);
        await expect(link).toHaveAttribute('title', LINK_TITLE);
        await expect(link).toHaveClass(new RegExp(CLASS_LINKCHECKER_ITEM));
        await expect(link).toHaveClass(new RegExp(CLASS_LINKCHECKER_CHECKED));
        await expect(link).toHaveClass(new RegExp(CLASS_LINKCHECKER_INVALID));
    });
});

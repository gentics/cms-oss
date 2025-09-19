import { NodeFeature, Variant } from '@gentics/cms-models';
import {
    EntityImporter,
    ITEM_TYPE_PAGE,
    TestSize,
    isVariant,
    loginWithForm,
    minimalNode,
    navigateToApp,
    pageOne,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import {
    findItem,
    findList,
    selectNode,
} from './helpers';

test.describe('Page Translation', () => {
    test.skip(() => !isVariant(Variant.ENTERPRISE), 'Requires Enterpise features');

    const IMPORTER = new EntityImporter();

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

    test.beforeEach(async ({ page, request, context }) => {
        await test.step('Client Setup', async () => {
            await context.clearCookies();
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
        });

        await test.step('Common Test Setup', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });

        await test.step('Specialized Test Setup', async () => {
            await IMPORTER.setupFeatures(TestSize.MINIMAL, {
                [NodeFeature.AUTOMATIC_TRANSLATION]: true,
            });
        });

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);
    });

    test.describe('Automatic Translations', () => {
        test('should be possible to translate a page automatically', async ({ page }) => {
            const pageData = IMPORTER.get(pageOne)!;
            const NEW_LANG = 'de';

            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, pageData.id);
            const languageIcon = item.locator(`.language-icon[data-id="${NEW_LANG}"] gtx-dropdown-trigger a`);
            const iconVisible = await languageIcon.isVisible();
            if (!iconVisible) {
                await item.locator('page-language-indicator .expand-toggle button').click();
                await page.waitForTimeout(2000);
            }
            await languageIcon.click({ force: true });

            await page.click('.page-language-context [data-action="translate"]');

            const modal = page.locator('translate-page-modal');
            await modal.waitFor();

            const autoTranslateButton = modal.locator('[data-action="auto-translate"]');
            await expect(autoTranslateButton).toBeVisible();
            await autoTranslateButton.click();

            /**
             * Flaky assertions could take longer to finish (i.e.: finished in the background)
             * Set waitMs to 0 to fix flakiness
             */
            // cy.get(`[data-id="${pageId}"]`)
            //     .first()
            //     .get('page-language-indicator')
            //     .first()
            //     .find('.language-icon.available')
            //     .should('have.length.at.least', 2);

            // TODO needs a test translation environment (translateLocally?)
            // // does not work for iframe
            // cy.get('content-frame', {timeout:5000})
            //     .first()
            //     .get('body')
            //     .first()
            //     .should('contain', 'Dies ist die Seite');
        });
    });
});

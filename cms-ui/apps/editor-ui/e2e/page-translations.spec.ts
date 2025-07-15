import { test, expect } from '@playwright/test';
import { NodeFeature, Variant } from '@gentics/cms-models';
import {
    EntityImporter,
    TestSize,
    ITEM_TYPE_PAGE,
    isVariant,
    minimalNode,
    pageOne,
    skipableSuite,
} from '@gentics/e2e-utils';
import {
    login,
    selectNode,
    findList,
    findItem,
    itemAction,
    initPage,
} from './helpers';
import { AUTH_ADMIN } from './common';

test.describe('Page Translation', () => {
    test.skip(() => !isVariant(Variant.ENTERPRISE), 'Requires Enterpise features');

    const IMPORTER = new EntityImporter();

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
            [NodeFeature.AUTOMATIC_TRANSLATION]: true,
        });
        await initPage(page);
        await page.goto('/');
        await login(page, AUTH_ADMIN);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);
    });

    test.describe('Automatic Translations', () => {
        test('should be possible to translate a page automatically', async ({ page }) => {
            const pageData = IMPORTER.get(pageOne)!;
            const NEW_LANG = 'de';

            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, pageData.id);
            const languageIcon = item.locator(`.language-icon[data-id="${NEW_LANG}"] gtx-dropdown-trigger`);
            const iconVisible = await languageIcon.isVisible();
            if (!iconVisible) {
                await item.locator('page-language-indicator .expand-toggle').click();
            }
            await languageIcon.click({ force: true });

            const translateButton = page.locator('.page-language-context [data-action="translate"]');
            await translateButton.click({ force: true });

            const modal = page.locator('translate-page-modal');
            await expect(modal).toBeVisible();

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


            // // does not work for iframe
            // cy.get('content-frame', {timeout:5000})
            //     .first()
            //     .get('body')
            //     .first()
            //     .should('contain', 'Dies ist die Seite');
        });
    });
});

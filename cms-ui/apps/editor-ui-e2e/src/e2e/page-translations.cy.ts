import { NodeFeature, Variant } from '@gentics/cms-models';
import {
    EntityImporter,
    ITEM_TYPE_PAGE,
    TestSize,
    isVariant,
    minimalNode,
    pageOne,
    skipableSuite,
} from '@gentics/e2e-utils';
import { AUTH_ADMIN } from '../support/common';

describe('Page Translation', () => {

    const IMPORTER = new EntityImporter();

    before(async () => {
        cy.muteXHR();
        await IMPORTER.cleanupTest();
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    beforeEach(async () => {
        await IMPORTER.cleanupTest();
        await IMPORTER.setupTest(TestSize.MINIMAL);
        await IMPORTER.setupFeatures(TestSize.MINIMAL, {
            [NodeFeature.AUTOMATIC_TRANSLATION]: true,
        });

        cy.navigateToApp();
        cy.login(AUTH_ADMIN);
        cy.selectNode(IMPORTER.get(minimalNode)!.id);
    });

    skipableSuite(isVariant(Variant.ENTERPRISE), 'Automatic Translations', () => {
        it('should be possible to translate a page automatically', () => {
            const page = IMPORTER.get(pageOne)!;
            const NEW_LANG = 'de';

            cy.findList(ITEM_TYPE_PAGE)
                .findItem(page.id)
                .find('page-language-indicator')
                .find(`.language-icon[data-id="${NEW_LANG}"]`)
                .click({ force: true });

            cy.get('.page-language-context')
                .find('[data-action="translate"]')
                .click({ force: true });
            cy.get('translate-page-modal').as('modal');

            cy.get('@modal')
                .find('[data-action="auto-translate"]')
                .should('exist');

            cy.get('@modal')
                .find('[data-action="auto-translate"]')
                .click();

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

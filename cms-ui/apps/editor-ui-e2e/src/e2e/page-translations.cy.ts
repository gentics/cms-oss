import {
    ENV_AUTOMATIC_TRANSLATION_ENABLED,
    EntityImporter,
    ITEM_TYPE_PAGE,
    TestSize,
    envAll,
    minimalNode,
    pageOne,
    skipableSuite,
} from '@gentics/e2e-utils';
import { AUTH_ADMIN } from '../support/app.po';

describe('Page Translation', () => {

    const IMPORTER = new EntityImporter();

    before(async () => {
        await IMPORTER.cleanupTest();
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    beforeEach(async () => {
        await IMPORTER.cleanupTest();
        await IMPORTER.setupTest(TestSize.MINIMAL);

        cy.navigateToApp();
        cy.login(AUTH_ADMIN);
        cy.selectNode(IMPORTER.get(minimalNode)!.id);
    });

    skipableSuite(envAll(ENV_AUTOMATIC_TRANSLATION_ENABLED), 'automatic translations', () => {
        it('should be possible to translate a page automatically', () => {
            const page = IMPORTER.get(pageOne)!;
            const NEW_LANG = 'de';

            cy.findItem(ITEM_TYPE_PAGE, page.id)
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
    })
});

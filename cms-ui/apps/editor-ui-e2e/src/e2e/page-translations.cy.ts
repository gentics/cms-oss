import {
    ENV_AUTOMATIC_TRANSLATION_ENABLED,
    EntityMap,
    ImportBootstrapData,
    TestSize,
    bootstrapSuite,
    cleanupTest,
    envAll,
    getItem,
    minimalNode,
    pageOne,
    setupTest,
    skipableSuite,
} from '@gentics/e2e-utils';

describe('Page Translation', () => {

    let bootstrap: ImportBootstrapData;
    let entities: EntityMap = {};

    before(() => {
        cy.wrap(cleanupTest()
            .then(() => bootstrapSuite(TestSize.MINIMAL))
            .then(data => {
                bootstrap = data;
            }),
        );
    });

    beforeEach(() => {
        cy.wrap(setupTest(TestSize.MINIMAL, bootstrap).then(data => {
            entities = data;
        }));
    });

    skipableSuite(envAll(ENV_AUTOMATIC_TRANSLATION_ENABLED), 'automatic translations', ()=>{
        it('should be possible to translate a page automatically', () => {
            cy.navigateToApp();
            cy.login('admin');
            cy.selectNode(getItem(minimalNode, entities)!.id);

            const page = getItem(pageOne, entities)!
            const pageId = page.id as any as number;

            cy.get(`[data-id="${pageId}"]`)
                .first()
                .get('page-language-indicator')
                .first()
                .find('.language-icon')
                .first()
                .click();

            cy.get('gtx-dropdown-item')
                .click();
            cy.get('translate-page-modal').as('modal');
            cy.get('@modal').find('[data-action="auto-translate"]')
                .should('exist');


            cy.get('@modal').find('[data-action="auto-translate"]')
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

import { TestSize } from '../support/common';
import { ImportBootstrapData, bootstrapSuite, cleanupTest, setupTest } from '../support/importer';

describe('Example', () => {

    let bootstrap: ImportBootstrapData;

    before(() => {
        cy.wrap(bootstrapSuite(TestSize.MINIMAL).then(data => {
            bootstrap = data;
        }));
    });

    beforeEach(() => {
        cy.wrap(setupTest(TestSize.MINIMAL, bootstrap));
    });

    afterEach(() => {
        cy.wrap(cleanupTest());
    });

    it('should have a node present', () => {
        cy.visit('http://localhost:8080/editor?skip-sso');
        cy.login('cms');
        cy.find('folder-contents > .title .title-name')
            .should('exist')
            .should('contain.text', 'Minimal');
    });
});

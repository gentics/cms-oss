import { TestSize, EntityMap, minimalNode, ImportBootstrapData, bootstrapSuite, cleanupTest, setupTest } from '@gentics/e2e-utils';
import { setup } from '../fixtures/auth.json';

describe('Login', () => {

    let bootstrap: ImportBootstrapData;
    let entities: EntityMap = {};

    before(() => {
        cy.wrap(cleanupTest(setup)
            .then(() => bootstrapSuite(setup, TestSize.MINIMAL))
            .then(data => {
                bootstrap = data;
            }),
        );
    });

    beforeEach(() => {
        cy.wrap(setupTest(setup, TestSize.MINIMAL, bootstrap).then(data => {
            entities = data;
        }));
    });

    afterEach(() => {
        cy.wrap(cleanupTest(setup));
    });

    it('should have the minimal node present', () => {
        cy.visit('http://localhost:8080/editor?skip-sso');
        cy.login('cms');
        cy.get('folder-contents > .title .title-name')
            .should('exist')
            .should('contain.text', minimalNode.node.name);
    });
});

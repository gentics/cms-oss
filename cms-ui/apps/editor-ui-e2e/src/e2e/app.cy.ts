import {
    EntityMap,
    IMPORT_TYPE,
    ImportBootstrapData,
    TestSize,
    bootstrapSuite,
    cleanupTest,
    folderA,
    folderB,
    getItem,
    minimalNode,
    setupTest,
} from '@gentics/e2e-utils';

describe('Login', () => {

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

    it('should have the minimal node present', () => {
        cy.navigateToApp();
        cy.login('admin');
        cy.selectNode(getItem(minimalNode, entities)!.id);
        cy.get('folder-contents > .title .title-name')
            .should('exist')
            .should('contain.text', minimalNode.node.name);
        const folders = [folderA, folderB];
        for (const folder of folders) {
            cy.findItem(folder[IMPORT_TYPE], getItem(folder, entities)!.id).should('exist');
        }

        cy.itemAction(folderA[IMPORT_TYPE], getItem(folderA, entities)!.id, 'properties');
        cy.get('content-frame')
            .should('exist');
    });
});

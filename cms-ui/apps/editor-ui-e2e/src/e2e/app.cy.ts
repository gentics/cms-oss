import {
    EntityImporter,
    IMPORT_TYPE,
    TestSize,
    folderA,
    folderB,
    minimalNode,
} from '@gentics/e2e-utils';
import { AUTH_ADMIN } from '../support/app.po';

describe('Login', () => {

    const IMPORTER = new EntityImporter();

    before(async () => {
        cy.muteXHR();
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

    it('should have the minimal node present', () => {
        cy.get('folder-contents > .title .title-name')
            .should('exist')
            .should('contain.text', minimalNode.node.name);
        const folders = [folderA, folderB];
        for (const folder of folders) {
            cy.findItem(folder[IMPORT_TYPE], IMPORTER.get(folder)!.id).should('exist');
        }

        cy.itemAction(folderA[IMPORT_TYPE], IMPORTER.get(folderA)!.id, 'properties');
        cy.get('content-frame')
            .should('exist');
    });
});

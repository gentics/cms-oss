import '@gentics/e2e-utils/commands';
import {
    EntityImporter,
    IMPORT_ID,
    ITEM_TYPE_FILE,
    ITEM_TYPE_IMAGE,
    TestSize,
    fileOne,
    imageOne,
    minimalNode,
} from '@gentics/e2e-utils';
import {
    AUTH_ADMIN,
    FIXTURE_TEST_FILE_DOC_1,
    FIXTURE_TEST_FILE_PDF_1,
    FIXTURE_TEST_FILE_TXT_1,
    FIXTURE_TEST_FILE_TXT_2,
    FIXTURE_TEST_IMAGE_JPG_1,
    FIXTURE_TEST_IMAGE_JPG_2,
    FIXTURE_TEST_IMAGE_PNG_1,
    FIXTURE_TEST_IMAGE_PNG_2,
} from '../support/common';

describe('Media Upload', () => {

    const IMPORTER = new EntityImporter();

    before(() => {
        cy.muteXHR();

        cy.wrap(IMPORTER.clearClient(), { log: false }).then(() => {
            return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
        }).then(() => {
            return cy.wrap(IMPORTER.bootstrapSuite(TestSize.MINIMAL), { log: false, timeout: 60_000 });
        });
    });

    beforeEach(() => {
        cy.muteXHR();

        cy.wrap(IMPORTER.clearClient(), { log: false }).then(() => {
            return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
        }).then(() => {
            return cy.loadBinaries([
                FIXTURE_TEST_IMAGE_JPG_1,
                FIXTURE_TEST_FILE_DOC_1,
            ]);
        }).then(fixtures => {
            IMPORTER.binaryMap = {
                [imageOne[IMPORT_ID]]: fixtures[FIXTURE_TEST_IMAGE_JPG_1],
                [fileOne[IMPORT_ID]]: fixtures[FIXTURE_TEST_FILE_DOC_1],
            };
        }).then(() => {
            return cy.wrap(IMPORTER.setupTest(TestSize.MINIMAL), { log: false, timeout: 60_000 });
        }).then(() => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.selectNode(IMPORTER.get(minimalNode)!.id);
        });
    });

    it('should be possible to upload a regular text file', () => {
        cy.uploadFiles(ITEM_TYPE_FILE, [FIXTURE_TEST_FILE_TXT_1]).then(allFiles => {
            cy.findList(ITEM_TYPE_FILE)
                .findItem(allFiles[FIXTURE_TEST_FILE_TXT_1].id)
                .should('exist');
            cy.findList(ITEM_TYPE_FILE)
                .find('.list-body item-list-row')
                .should('have.length', 2);
        });
    });

    it('should be possible to upload a image file', () => {
        cy.uploadFiles(ITEM_TYPE_IMAGE, [FIXTURE_TEST_IMAGE_JPG_2]).then(allFiles => {
            cy.findList(ITEM_TYPE_IMAGE)
                .findItem(allFiles[FIXTURE_TEST_IMAGE_JPG_2].id)
                .should('exist');
            cy.findList(ITEM_TYPE_IMAGE)
                .find('.list-body masonry-item')
                .should('have.length', 2);
        });
    });

    it('should be possible to upload a regular text file with drag-n-drop', () => {
        cy.uploadFiles(ITEM_TYPE_FILE, [FIXTURE_TEST_FILE_TXT_1], { dragAndDrop: true }).then(allFiles => {
            cy.findList(ITEM_TYPE_FILE)
                .findItem(allFiles[FIXTURE_TEST_FILE_TXT_1].id)
                .should('exist');
            cy.findList(ITEM_TYPE_FILE)
                .find('.list-body item-list-row')
                .should('have.length', 2);
        });
    });

    it('should be possible to upload a image file with drag-n-drop', () => {
        cy.uploadFiles(ITEM_TYPE_IMAGE, [FIXTURE_TEST_IMAGE_JPG_2], { dragAndDrop: true }).then(allFiles => {
            cy.findList(ITEM_TYPE_IMAGE)
                .findItem(allFiles[FIXTURE_TEST_IMAGE_JPG_2].id)
                .should('exist');
            cy.findList(ITEM_TYPE_IMAGE)
                .find('.list-body masonry-item')
                .should('have.length', 2);
        });
    });

    it('should be possible to upload multiple text files', () => {
        cy.uploadFiles(ITEM_TYPE_FILE, [
            FIXTURE_TEST_FILE_TXT_2,
            FIXTURE_TEST_FILE_PDF_1,
        ]).then(allFiles => {
            cy.findList(ITEM_TYPE_FILE)
                .findItem(allFiles[FIXTURE_TEST_FILE_TXT_2].id)
                .should('exist');
            cy.findList(ITEM_TYPE_FILE)
                .findItem(allFiles[FIXTURE_TEST_FILE_PDF_1].id)
                .should('exist');
            cy.findList(ITEM_TYPE_FILE)
                .find('.list-body item-list-row')
                .should('have.length', 3);
        });
    });

    it('should be possible to upload multiple image files', () => {
        cy.uploadFiles(ITEM_TYPE_IMAGE, [
            FIXTURE_TEST_IMAGE_PNG_1,
            FIXTURE_TEST_IMAGE_PNG_2,
        ]).then(allFiles => {
            cy.findList(ITEM_TYPE_IMAGE)
                .findItem(allFiles[FIXTURE_TEST_IMAGE_PNG_1].id)
                .should('exist');
            cy.findList(ITEM_TYPE_IMAGE)
                .findItem(allFiles[FIXTURE_TEST_IMAGE_PNG_2].id)
                .should('exist');
            cy.findList(ITEM_TYPE_IMAGE)
                .find('.list-body masonry-item')
                .should('have.length', 3);
        });
    });
});

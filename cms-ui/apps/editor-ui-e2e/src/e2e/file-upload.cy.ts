import { FileUploadResponse } from '@gentics/cms-models';
import {
    BinaryMap,
    EntityMap,
    IMPORT_ID,
    ImportBootstrapData,
    TestSize,
    bootstrapSuite,
    cleanupTest,
    fileOne,
    getItem,
    imageOne,
    minimalNode,
    setupTest,
} from '@gentics/e2e-utils';
import {
    FIXTURE_TEST_FILE_DOC_1,
    FIXTURE_TEST_FILE_TXT_1,
    FIXTURE_TEST_IMAGE_JPG_1,
    FIXTURE_TEST_IMAGE_JPG_2,
} from '../support/app.po';

describe('File Upload', () => {

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
        cy.wrap(cleanupTest()).then(() => {
            return cy.loadBinaries([
                FIXTURE_TEST_IMAGE_JPG_1,
                FIXTURE_TEST_FILE_DOC_1,
            ]);
        }).then(fixtures => {
            const binMap: BinaryMap = {
                [imageOne[IMPORT_ID]]: fixtures[FIXTURE_TEST_IMAGE_JPG_1],
                [fileOne[IMPORT_ID]]: fixtures[FIXTURE_TEST_FILE_DOC_1],
            };

            return cy.wrap(setupTest(TestSize.MINIMAL, bootstrap, binMap).then(data => {
                entities = data;
            }));
        }).then(() => {
            cy.navigateToApp();
            cy.login('admin');
            cy.selectNode(getItem(minimalNode, entities)!.id);
        });
    });

    it('should be possible to upload a regular text file', () => {
        cy.intercept({
            method: 'POST',
            pathname: '/rest/file/create',
        }).as('createRequest');

        // TODO: Move this all (fixture load, upload, and response handling) into a command?
        cy.fixture(FIXTURE_TEST_FILE_TXT_1, null).as('fileUploadData');
        cy.findList('file')
            .find('.list-header .header-controls [data-action="upload-item"] input[type="file"]')
            .selectFile('@fileUploadData', { force: true });

        cy.wait('@createRequest').then(intercept => {
            const res = intercept.response?.body as FileUploadResponse;
            // eslint-disable-next-line @typescript-eslint/no-unused-expressions
            expect(res.success).to.be.true;

            cy.findItem('file', res.file.id)
                .should('exist');
            cy.findList('file')
                .find('.list-body item-list-row')
                .should('have.length', 2);
        });
    });

    // TODO: Skipped for now - Template loading the 2nd time around doesn't properly work
    xit('should be possible to upload a image file', () => {
        cy.intercept({
            method: 'POST',
            pathname: '/rest/file/create',
        }).as('createRequest');

        cy.fixture(FIXTURE_TEST_IMAGE_JPG_2, null).as('fileUploadData');
        cy.findList('image')
            .find('.list-header .header-controls [data-action="upload-item"] input[type="file"]')
            .selectFile('@fileUploadData', { force: true });

        cy.wait('@createRequest').then(intercept => {
            const res = intercept.response?.body as FileUploadResponse;
            // eslint-disable-next-line @typescript-eslint/no-unused-expressions
            expect(res.success).to.be.true;

            cy.findItem('image', res.file.id)
                .should('exist');
            cy.findList('image')
                .find('.list-body masonry-item')
                .should('have.length', 2);
        });
    });
});

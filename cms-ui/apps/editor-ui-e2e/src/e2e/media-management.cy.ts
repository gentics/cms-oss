import { FileSaveRequest, ImageSaveRequest, SelectTagPartProperty, TagPropertyType } from '@gentics/cms-models';
import {
    EntityImporter,
    ITEM_TYPE_FILE,
    ITEM_TYPE_IMAGE,
    TestSize,
    minimalNode,
} from '@gentics/e2e-utils';
import { AUTH_ADMIN, FIXTURE_TEST_FILE_TXT_1, FIXTURE_TEST_IMAGE_JPG_1 } from '../support/app.po';

describe('Media Management', () => {

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

    it('should be possible to create a new file and edit the object-properties', () => {
        cy.uploadFiles(ITEM_TYPE_FILE, [FIXTURE_TEST_FILE_TXT_1]).then(allFiles => {
            const FILE = allFiles[FIXTURE_TEST_FILE_TXT_1];
            const OBJECT_PROPERTY = 'test_color';
            const COLOR_ID = 2;

            /* Open the Folder Properties and select a Object-Property to edit
             * ---------------------------- */
            cy.itemAction(ITEM_TYPE_FILE, FILE.id, 'properties');

            cy.openObjectPropertyEditor(OBJECT_PROPERTY)
                .findTagEditorElement(TagPropertyType.SELECT)
                .selectValue(COLOR_ID);

            /* Save the Object-Property changes
             * ---------------------------- */
            cy.intercept({
                method: 'POST',
                pathname: '/rest/file/save/**',
            }).as('updateRequest');

            cy.editorSave();

            // Wait for the folder to have reloaded
            cy.wait<FileSaveRequest>('@updateRequest')
                .then(data => {
                    const req = data.request.body;
                    const tag = req.file.tags?.[`object.${OBJECT_PROPERTY}`];
                    const options = (tag?.properties['select'] as SelectTagPartProperty).selectedOptions;

                    expect(options).to.have.length(1);
                    expect(options![0].id).to.equal(COLOR_ID);
                });
        });
    });

    it('should be possible to create a new image and edit the object-properties', () => {
        cy.uploadFiles(ITEM_TYPE_IMAGE, [FIXTURE_TEST_IMAGE_JPG_1]).then(allFiles => {
            const IMAGE = allFiles[FIXTURE_TEST_IMAGE_JPG_1];
            const OBJECT_PROPERTY = 'test_color';
            const COLOR_ID = 2;

            /* Open the Folder Properties and select a Object-Property to edit
             * ---------------------------- */
            cy.itemAction(ITEM_TYPE_IMAGE, IMAGE.id, 'properties');

            cy.openObjectPropertyEditor(OBJECT_PROPERTY)
                .findTagEditorElement(TagPropertyType.SELECT)
                .selectValue(COLOR_ID);

            /* Save the Object-Property changes
             * ---------------------------- */
            cy.intercept({
                method: 'POST',
                pathname: '/rest/image/save/**',
            }).as('updateRequest');

            cy.editorSave();

            cy.wait<ImageSaveRequest>('@updateRequest').then(data => {
                const req = data.request.body;
                const tag = req.image.tags?.[`object.${OBJECT_PROPERTY}`];
                const options = (tag?.properties['select'] as SelectTagPartProperty).selectedOptions;

                expect(options).to.have.length(1);
                expect(options![0].id).to.equal(COLOR_ID);
            });
        });
    });
});

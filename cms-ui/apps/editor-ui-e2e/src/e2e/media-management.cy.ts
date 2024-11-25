import { FileSaveRequest, ImageSaveRequest, SelectTagPartProperty, TagPropertyType } from '@gentics/cms-models';
import {
    EntityImporter,
    ITEM_TYPE_FILE,
    ITEM_TYPE_IMAGE,
    TestSize,
    minimalNode,
} from '@gentics/e2e-utils';
import { AUTH_ADMIN, FIXTURE_TEST_FILE_TXT_1, FIXTURE_TEST_IMAGE_JPG_1 } from '../support/common';

describe('Media Management', () => {

    const IMPORTER = new EntityImporter();

    const ALIAS_UPDATE_REQUEST = '@updateRequest';

    before(() => {
        cy.muteXHR();

        cy.wrap(null, { log: false }).then(() => {
            return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
        }).then(() => {
            return cy.wrap(IMPORTER.bootstrapSuite(TestSize.MINIMAL), { log: false, timeout: 60_000 });
        });
    });

    beforeEach(() => {
        cy.muteXHR();

        cy.wrap(null, { log: false }).then(() => {
            return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
        }).then(() => {
            return cy.wrap(IMPORTER.setupTest(TestSize.MINIMAL), { log: false, timeout: 60_000 });
        }).then(() => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.selectNode(IMPORTER.get(minimalNode)!.id);
        });
    });

    it('should be possible to create a new file and edit the object-properties', () => {
        cy.uploadFiles(ITEM_TYPE_FILE, [FIXTURE_TEST_FILE_TXT_1]).then(allFiles => {
            const FILE = allFiles[FIXTURE_TEST_FILE_TXT_1];
            const OBJECT_PROPERTY = 'test_color';
            const COLOR_ID = 2;

            /* Open the Folder Properties and select a Object-Property to edit
             * ---------------------------- */
            cy.findList(ITEM_TYPE_FILE)
                .findItem(FILE.id)
                .itemAction('properties');

            cy.openObjectPropertyEditor(OBJECT_PROPERTY)
                .findTagEditorElement(TagPropertyType.SELECT)
                .select(COLOR_ID);

            /* Save the Object-Property changes
             * ---------------------------- */
            cy.intercept({
                method: 'POST',
                pathname: '/rest/file/save/**',
            }, req => {
                req.alias = ALIAS_UPDATE_REQUEST;
            });

            cy.editorAction('save');

            // Wait for the folder to have reloaded
            cy.wait<FileSaveRequest>(ALIAS_UPDATE_REQUEST)
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
            cy.findList(ITEM_TYPE_IMAGE)
                .findItem(IMAGE.id)
                .itemAction('properties');

            cy.openObjectPropertyEditor(OBJECT_PROPERTY)
                .findTagEditorElement(TagPropertyType.SELECT)
                .select(COLOR_ID);

            /* Save the Object-Property changes
             * ---------------------------- */
            cy.intercept({
                method: 'POST',
                pathname: '/rest/image/save/**',
            }, req => {
                req.alias = ALIAS_UPDATE_REQUEST;
            });

            cy.editorAction('save');

            cy.wait<ImageSaveRequest>(ALIAS_UPDATE_REQUEST).then(data => {
                const req = data.request.body;
                const tag = req.image.tags?.[`object.${OBJECT_PROPERTY}`];
                const options = (tag?.properties['select'] as SelectTagPartProperty).selectedOptions;

                expect(options).to.have.length(1);
                expect(options![0].id).to.equal(COLOR_ID);
            });
        });
    });
});

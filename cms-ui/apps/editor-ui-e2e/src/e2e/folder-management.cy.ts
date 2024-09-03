/* eslint-disable @typescript-eslint/no-unused-expressions */
import {
    FolderCreateResponse,
    FolderSaveRequest,
    SelectTagPartProperty,
    TagPropertyType,
} from '@gentics/cms-models';
import {
    EntityImporter,
    ITEM_TYPE_FOLDER,
    TestSize,
    folderA,
    minimalNode,
} from '@gentics/e2e-utils';
import { AUTH_ADMIN } from '../support/common';

describe('Folder Management', () => {

    const IMPORTER = new EntityImporter();

    const ALIAS_FORM = '@form';
    const ALIAS_MODAL = '@modal';
    const ALIAS_CREATE_REQ = '@createRequest';
    const ALIAS_UPDATE_REQ = '@updateRequest';

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

    it('should be possible to create a new folder', () => {
        const NEW_FOLDER_NAME = 'Hello World';
        const NEW_FOLDER_PATH = 'example';

        /* Create the Folder
         * ---------------------------- */
        cy.findList(ITEM_TYPE_FOLDER)
            .find('.header-controls [data-action="create-new-item"]')
            .click({ force: true });
        cy.get('create-folder-modal').as(ALIAS_MODAL);
        cy.get(ALIAS_MODAL).find('folder-properties-form').as(ALIAS_FORM);

        cy.get(ALIAS_FORM)
            .find('[formcontrolname="name"] input')
            .type(NEW_FOLDER_NAME);

        cy.get(ALIAS_FORM)
            .find('[formcontrolname="directory"] input')
            .type(NEW_FOLDER_PATH);

        cy.intercept({
            method: 'POST',
            pathname: '/rest/folder/create',
        }).as(ALIAS_CREATE_REQ);

        cy.get(ALIAS_MODAL)
            .find('.modal-footer [data-action="confirm"]')
            .click({ force: true });

        // Wait for the folder to have reloaded
        cy.wait<any, FolderCreateResponse>(ALIAS_CREATE_REQ)
            .then(data => {
                const folder = data.response?.body?.folder;
                expect(folder).to.exist;
                cy.findList(ITEM_TYPE_FOLDER)
                    .findItem(folder!.id)
                    .should('exist');
            });
    });

    it('should be possible to edit the page properties', () => {
        const FOLDER = IMPORTER.get(folderA)!;
        const CHANGE_FOLDER_NAME = 'Foo bar change';

        // Confirm that the original name is correct
        cy.findList(ITEM_TYPE_FOLDER)
            .findItem(FOLDER.id)
            .find('.item-name .item-name-only')
            .should('have.text', FOLDER.name);

        cy.findList(ITEM_TYPE_FOLDER)
            .findItem(FOLDER.id)
            .itemAction('properties');
        cy.get('content-frame combined-properties-editor .properties-content folder-properties-form').as(ALIAS_FORM);

        cy.intercept({
            method: 'POST',
            pathname: '/rest/folder/save/**',
        }).as(ALIAS_UPDATE_REQ);

        // Clear the name and enter the new one
        cy.get(ALIAS_FORM)
            .find('[formcontrolname="name"] input')
            .type(`{selectall}{del}${CHANGE_FOLDER_NAME}`);

        cy.editorSave();

        // Wait for the update to be actually handled
        cy.wait(ALIAS_UPDATE_REQ).then(() => {
            cy.findList(ITEM_TYPE_FOLDER)
                .findItem(FOLDER.id)
                .find('.item-name .item-name-only')
                .should('have.text', CHANGE_FOLDER_NAME);
        });
    });

    it('should be possible to edit the folder object-properties', () => {
        const OBJECT_PROPERTY = 'test_color';
        const COLOR_ID = 2;
        const FOLDER = IMPORTER.get(folderA)!;

        cy.findList(ITEM_TYPE_FOLDER)
            .findItem(FOLDER.id)
            .itemAction('properties');
        cy.openObjectPropertyEditor(OBJECT_PROPERTY)
            .findTagEditorElement(TagPropertyType.SELECT)
            .selectValue(COLOR_ID);

        /* Save the Object-Property changes
         * ---------------------------- */
        cy.intercept({
            method: 'POST',
            pathname: '/rest/folder/save/**',
        }).as(ALIAS_UPDATE_REQ);

        cy.editorSave();

        cy.wait<FolderSaveRequest>(ALIAS_UPDATE_REQ).then(data => {
            const req = data.request.body;
            const tag = req.folder.tags?.[`object.${OBJECT_PROPERTY}`];
            const options = (tag?.properties['select'] as SelectTagPartProperty).selectedOptions;

            expect(options).to.have.length(1);
            expect(options![0].id).to.equal(COLOR_ID);
        });
    });
});

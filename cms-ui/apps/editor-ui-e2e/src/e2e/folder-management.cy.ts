import { Folder, FolderCreateResponse, FolderSaveRequest, SelectTagPartProperty, TagPropertyType } from '@gentics/cms-models';
import {
    EntityMap,
    ImportBootstrapData,
    TestSize,
    bootstrapSuite,
    cleanupTest,
    getItem,
    minimalNode,
    setupTest,
} from '@gentics/e2e-utils';
import type { Interception } from 'cypress/types/net-stubbing';

describe('Folder Management', () => {

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
        cy.wrap(cleanupTest()
            .then(() => setupTest(TestSize.MINIMAL, bootstrap))
            .then(data => {
                entities = data;
            }),
        );
    });

    it('should be possible to create a new folder and edit the object-properties', () => {
        cy.navigateToApp();
        cy.login('admin');
        cy.selectNode(getItem(minimalNode, entities)!.id);

        const NEW_FOLDER_NAME = 'Hello World';
        const NEW_FOLDER_PATH = 'example';
        const OBJECT_PROPERTY = 'test_color';
        const COLOR_ID = 2;

        /* Create the Folder
         * ---------------------------- */
        cy.findList('folder')
            .find('.header-controls [data-action="create-new-item"]')
            .click({ force: true });
        cy.get('create-folder-modal').as('modal');
        cy.get('@modal').find('folder-properties-form').as('form');

        cy.get('@form')
            .find('[formcontrolname="name"] input')
            .type(NEW_FOLDER_NAME);

        cy.get('@form')
            .find('[formcontrolname="directory"] input')
            .type(NEW_FOLDER_PATH);

        cy.intercept({
            method: 'POST',
            pathname: '/rest/folder/create',
        }).as('createRequest');

        cy.get('@modal')
            .find('.modal-footer [data-action="confirm"]')
            .click({ force: true });

        cy.wait('@createRequest')
            .then(data => cy.wrap((data.response?.body as FolderCreateResponse).folder).as('folder'));

        /* Open the Folder Properties and select a Object-Property to edit
         * ---------------------------- */
        cy.get('@folder').then(folder => {
            cy.itemAction('folder', (folder as any as Folder).id, 'properties');
        });

        cy.openObjectPropertyEditor(OBJECT_PROPERTY)
            .findTagEditorElement(TagPropertyType.SELECT)
            .selectValue(COLOR_ID);

        /* Save the Object-Property changes
         * ---------------------------- */
        cy.intercept({
            method: 'POST',
            pathname: '/rest/folder/save/**',
        }).as('saveRequest');

        cy.get('content-frame gtx-editor-toolbar .save-button [data-action="primary"]')
            .click({ force: true });

        cy.get('@saveRequest').then(data => {
            const req = (data as any as Interception).request.body as FolderSaveRequest;
            const tag = req.folder.tags?.[`object.${OBJECT_PROPERTY}`];
            const options = (tag?.properties['select'] as SelectTagPartProperty).selectedOptions;

            expect(options).to.have.length(1);
            expect(options![0].id).to.equal(COLOR_ID);
        });
    });
});

/* eslint-disable @typescript-eslint/no-unused-expressions */
import { PageCreateResponse, PageSaveRequest, SelectTagPartProperty, TagPropertyType } from '@gentics/cms-models';
import {
    EntityImporter,
    ITEM_TYPE_PAGE,
    LANGUAGE_EN,
    minimalNode,
    pageOne,
    TestSize,
} from '@gentics/e2e-utils';
import { AUTH_ADMIN } from '../support/app.po';

describe('Page Management', () => {

    const IMPORTER = new EntityImporter();

    before(async () => {
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

    it('should be possible to create a new Page', () => {
        const NEW_PAGE_NAME = 'Hello World';
        const NEW_PAGE_PATH = 'example';

        /* Create the Page
         * ---------------------------- */
        cy.findList(ITEM_TYPE_PAGE)
            .find('.header-controls [data-action="create-new-item"]')
            .click({ force: true });
        cy.get('create-page-modal').as('modal');
        cy.get('@modal').find('page-properties-form').as('form');

        cy.get('@form')
            .find('[formcontrolname="pageName"] input')
            .type(NEW_PAGE_NAME);

        cy.get('@form')
            .find('[formcontrolname="suggestedOrRequestedFileName"] input')
            .type(NEW_PAGE_PATH)
        cy.get('@form')
            .find('[formcontrolname="language"] .select-input')
            .selectValue(LANGUAGE_EN);

        cy.intercept({
            method: 'POST',
            pathname: '/rest/page/create',
        }).as('createRequest');

        cy.intercept({
            method: 'GET',
            pathname: '/rest/folder/getPages/**',
        }).as('folderLoad');

        cy.get('@modal')
            .find('.modal-footer [data-action="confirm"]')
            .click({ force: true });

        // Wait for the folder to have reloaded
        cy.wait('@folderLoad')
            .then(() => cy.editorClose())
            .then(() => cy.wait<any, PageCreateResponse>('@createRequest'))
            .then(data => {
                const page = data.response?.body?.page;
                expect(page).to.exist;
                cy.findItem(ITEM_TYPE_PAGE, page!.id)
                    .should('exist');
            });
    });

    it('should be possible to edit the page properties', () => {
        const PAGE = IMPORTER.get(pageOne)!;
        const CHANGE_PAGE_NAME = 'Foo bar change';

        // Confirm that the original name is correct
        cy.findItem(ITEM_TYPE_PAGE, PAGE.id)
            .should('exist');
        cy.findItem(ITEM_TYPE_PAGE, PAGE.id)
            .find('.item-name .item-name-only')
            .should('have.text', PAGE.name);

        cy.itemAction(ITEM_TYPE_PAGE, PAGE.id, 'properties');
        cy.get('content-frame combined-properties-editor .properties-content page-properties-form').as('form');

        cy.intercept({
            method: 'POST',
            pathname: '/rest/page/save/**',
        }).as('pageUpdate');

        // Clear the name and enter the new one
        cy.get('@form')
            .find('[formcontrolname="pageName"] input')
            .type(`{selectall}{del}${CHANGE_PAGE_NAME}`);

        cy.editorSave();

        // Wait for the update to be actually handled
        cy.wait('@pageUpdate').then(() => {
            cy.findItem(ITEM_TYPE_PAGE, PAGE.id)
                .find('.item-name .item-name-only')
                .should('have.text', CHANGE_PAGE_NAME);
        });
    });

    it('should be possible to edit the page object-properties', () => {
        const OBJECT_PROPERTY = 'test_color';
        const COLOR_ID = 2;
        const PAGE = IMPORTER.get(pageOne)!;

        cy.itemAction(ITEM_TYPE_PAGE, PAGE.id, 'properties');
        cy.openObjectPropertyEditor(OBJECT_PROPERTY)
            .findTagEditorElement(TagPropertyType.SELECT)
            .selectValue(COLOR_ID);

        /* Save the Object-Property changes
         * ---------------------------- */
        cy.intercept({
            method: 'POST',
            pathname: '/rest/page/save/**',
        }).as('saveRequest');

        cy.editorSave();

        cy.wait<PageSaveRequest>('@saveRequest').then(data => {
            const req = data.request.body;
            const tag = req.page.tags?.[`object.${OBJECT_PROPERTY}`];
            const options = (tag?.properties['select'] as SelectTagPartProperty).selectedOptions;

            expect(options).to.have.length(1);
            expect(options![0].id).to.equal(COLOR_ID);
        });
    });
});

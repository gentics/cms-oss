/* eslint-disable @typescript-eslint/no-unused-expressions */
import '@gentics/e2e-utils/commands';
import { PageCreateResponse, PageSaveRequest, SelectTagPartProperty, TagPropertyType } from '@gentics/cms-models';
import {
    EntityImporter,
    ITEM_TYPE_PAGE,
    LANGUAGE_EN,
    minimalNode,
    pageOne,
    TestSize,
} from '@gentics/e2e-utils';
import { AUTH_ADMIN } from '../support/common';

describe('Page Management', () => {

    const IMPORTER = new EntityImporter();

    const ALIAS_MODAL = '@modal';
    const ALIAS_FORM = '@form';
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

    it('should be possible to create a new Page', () => {
        const NEW_PAGE_NAME = 'Hello World';
        const NEW_PAGE_PATH = 'example';

        /* Create the Page
         * ---------------------------- */
        cy.findList(ITEM_TYPE_PAGE)
            .find('.header-controls [data-action="create-new-item"]')
            .click({ force: true });
        cy.get('create-page-modal').as(ALIAS_MODAL);
        cy.get(ALIAS_MODAL).find('page-properties-form').as(ALIAS_FORM);

        cy.get(ALIAS_FORM)
            .find('[formcontrolname="pageName"] input')
            .type(NEW_PAGE_NAME);

        cy.get(ALIAS_FORM)
            .find('[formcontrolname="suggestedOrRequestedFileName"] input')
            .type(NEW_PAGE_PATH)
        cy.get(ALIAS_FORM)
            .find('[formcontrolname="language"] .select-input')
            .selectValue(LANGUAGE_EN);

        cy.intercept({
            method: 'POST',
            pathname: '/rest/page/create',
        }, req => {
            req.alias = ALIAS_CREATE_REQ;
        });

        cy.get(ALIAS_MODAL)
            .find('.modal-footer [data-action="confirm"]')
            .click({ force: true });

        // Wait for the folder to have reloaded
        cy.wait<any, PageCreateResponse>(ALIAS_CREATE_REQ).then(data => {
            cy.editorAction('close');

            const page = data.response?.body?.page;
            expect(page).to.exist;
            cy.findList(ITEM_TYPE_PAGE)
                .findItem(page!.id)
                .should('exist');
        });
    });

    it('should be possible to edit the page properties', () => {
        const PAGE = IMPORTER.get(pageOne)!;
        const CHANGE_PAGE_NAME = 'Foo bar change';

        // Confirm that the original name is correct
        cy.findList(ITEM_TYPE_PAGE)
            .findItem(PAGE.id)
            .should('exist');
        cy.findList(ITEM_TYPE_PAGE)
            .findItem(PAGE.id)
            .find('.item-name .item-name-only')
            .should('have.text', PAGE.name);

        cy.findList(ITEM_TYPE_PAGE)
            .findItem(PAGE.id)
            .itemAction('properties');
        cy.get('content-frame combined-properties-editor .properties-content page-properties-form').as(ALIAS_FORM);

        cy.intercept({
            method: 'POST',
            pathname: '/rest/page/save/**',
        }, req => {
            req.alias = ALIAS_UPDATE_REQ;
        });

        // Clear the name and enter the new one
        // eslint-disable-next-line cypress/unsafe-to-chain-command
        cy.get(ALIAS_FORM)
            .find('[formcontrolname="pageName"] input')
            .clear()
            .type(CHANGE_PAGE_NAME);

        cy.editorAction('save');

        // Wait for the update to be actually handled
        cy.wait(ALIAS_UPDATE_REQ).then(() => {
            cy.findList(ITEM_TYPE_PAGE)
                .findItem(PAGE.id)
                .find('.item-name .item-name-only')
                .should('have.text', CHANGE_PAGE_NAME);
        });
    });

    it('should be possible to edit the page object-properties', () => {
        const OBJECT_PROPERTY = 'test_color';
        const COLOR_ID = 2;
        const PAGE = IMPORTER.get(pageOne)!;

        cy.findList(ITEM_TYPE_PAGE)
            .findItem(PAGE.id)
            .itemAction('properties');
        cy.openObjectPropertyEditor(OBJECT_PROPERTY)
            .findTagEditorElement(TagPropertyType.SELECT)
            .selectValue(COLOR_ID);

        /* Save the Object-Property changes
         * ---------------------------- */
        cy.intercept({
            method: 'POST',
            pathname: '/rest/page/save/**',
        }, req => {
            req.alias = ALIAS_UPDATE_REQ;
        });

        cy.editorAction('save');

        cy.wait<PageSaveRequest>(ALIAS_UPDATE_REQ).then(data => {
            const req = data.request.body;
            const tag = req.page.tags?.[`object.${OBJECT_PROPERTY}`];
            const options = (tag?.properties['select'] as SelectTagPartProperty).selectedOptions;

            expect(options).to.have.length(1);
            expect(options![0].id).to.equal(COLOR_ID);
        });
    });
});

/* eslint-disable @typescript-eslint/no-unused-expressions */
import { PageCreateResponse, PageSaveRequest, SelectTagPartProperty, TagPropertyType } from '@gentics/cms-models';
import {
    EntityImporter,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_NODE,
    ITEM_TYPE_PAGE,
    LANGUAGE_EN,
    minimalNode,
    NodeImportData,
    PACKAGE_MAP,
    PageImportData,
    pageOne,
    TestSize,
} from '@gentics/e2e-utils';
import '@gentics/e2e-utils/commands';
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

    describe('Minimal Setup', () => {
        const OBJ_PROP_CAT_TESTS = '2';
        const DEFAULT_OBJ_PROP_CAT = [
            '_others_',
            OBJ_PROP_CAT_TESTS,
        ];

        beforeEach(async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.setupTest(TestSize.MINIMAL);

            cy.navigateToApp();
            cy.window().then(win => {
                win.localStorage.setItem('GCMSUI_openObjectPropertyGroups', JSON.stringify(DEFAULT_OBJ_PROP_CAT));
            })
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
                .find('[formcontrolname="language"]')
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
            const ALIAS_ITEM = '@item';

            // Confirm that the original name is correct
            cy.findList(ITEM_TYPE_PAGE)
                .findItem(PAGE.id)
                .as(ALIAS_ITEM)
                .should('exist')
                .find('.item-name .item-name-only')
                .should('have.text', PAGE.name);

            cy.get(ALIAS_ITEM)
                .itemAction('properties');

            cy.get('content-frame combined-properties-editor .properties-content page-properties-form')
                .as(ALIAS_FORM);

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
                cy.get(ALIAS_ITEM)
                    .find('.item-name .item-name-only')
                    .should('have.text', CHANGE_PAGE_NAME);
            });
        });

        it('should have the testing object-property category open on default', () => {
            const PAGE = IMPORTER.get(pageOne)!;
            const ALIAS_REQ_BREADCRUMB = '@reqBreadcrumb';

            cy.findList(ITEM_TYPE_PAGE)
                .findItem(PAGE.id)
                .itemAction('properties');

            cy.intercept({
                pathname: '/rest/folder/breadcrumb/*',
            }, req => {
                req.alias = ALIAS_REQ_BREADCRUMB;
            });

            cy.wait(ALIAS_REQ_BREADCRUMB);

            cy.get(`content-frame combined-properties-editor .properties-tabs .tab-group[data-id="${OBJ_PROP_CAT_TESTS}"]`)
                .should('have.class', 'expanded')
                .find('.tab-link')
                .should('be.visible')
                .and('be.displayed');
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

    describe('Content without Language Setup', () => {
        /*
         * The setup is quite special, since it's quite the edge-case that is tested here.
         * We want the minimal content setup, but without any actual language assigned.
         */
        const CONTENT = PACKAGE_MAP[TestSize.MINIMAL].map(entity => {
            switch (entity[IMPORT_TYPE]) {
                case ITEM_TYPE_PAGE: {
                    // structuredClone does not copy symbol properties, so we have to do it manually.
                    const clone = structuredClone(entity) as PageImportData;
                    clone[IMPORT_TYPE] = entity[IMPORT_TYPE];
                    clone[IMPORT_ID] = entity[IMPORT_ID];

                    // Remove the language entirely
                    delete clone.language;

                    return clone;
                }
                case IMPORT_TYPE_NODE: {
                    // structuredClone does not copy symbol properties, so we have to do it manually.
                    const clone = structuredClone(entity) as NodeImportData;
                    clone[IMPORT_TYPE] = entity[IMPORT_TYPE];
                    clone[IMPORT_ID] = entity[IMPORT_ID];
                    clone.languages = [];

                    return clone;
                }

                default:
                    return entity;
            }
        });

        beforeEach(async () => {
            IMPORTER.client = null;
            await IMPORTER.cleanupTest();
            await IMPORTER.importData(CONTENT);
        });

        beforeEach(() => Promise.all([
            IMPORTER.client!.node.assignLanguage(IMPORTER.get(minimalNode)!.id, IMPORTER.languages['de']).send(),
            IMPORTER.client!.node.assignLanguage(IMPORTER.get(minimalNode)!.id, IMPORTER.languages['en']).send(),
        ]));

        it('should be possible to assign a language to a page with the language markers', () => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.selectNode(IMPORTER.get(minimalNode)!.id);

            const LANG = 'en';
            const ALIAS_ITEM = '@item';
            const ALIAS_UPDATE_REQUEST = '@updateRequest';
            const CLASS_AVAILABLE = 'available';

            // Validate initial state

            cy.findList(ITEM_TYPE_PAGE)
                .findItem(IMPORTER.get(pageOne)!.id)
                .as(ALIAS_ITEM)
                // Open the languages, since these are hidden on default (why?)
                .find('.language-indicator .expand-toggle')
                .click();

            cy.get(ALIAS_ITEM)
                .find('.language-indicator .language-icon')
                .should('not.have.class', CLASS_AVAILABLE);

            cy.intercept({
                pathname: '/rest/page/save/*',
            }, req => {
                req.alias = ALIAS_UPDATE_REQUEST;
            });

            // Assign the language via language indicator action

            cy.get(ALIAS_ITEM)
                .find(`.language-indicator .language-icon[data-id="${LANG}"] [data-action="page-language"]`)
                .openContext()
                .find('[data-action="set-source-language"]')
                .click();

            // Validate the update

            cy.wait<PageSaveRequest>(ALIAS_UPDATE_REQUEST).then(intercept => {
                expect(intercept.request.body.page.language).to.equal(LANG);
            });

            cy.get(ALIAS_ITEM)
                .find(`.language-indicator .language-icon[data-id="${LANG}"]`)
                .should('have.class', CLASS_AVAILABLE);
        });

        it('should be possible to assign a language in the page-properties', () => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.selectNode(IMPORTER.get(minimalNode)!.id);

            const LANG = 'en';
            const ALIAS_ITEM = '@item';
            const ALIAS_UPDATE_REQUEST = '@updateRequest';
            const CLASS_AVAILABLE = 'available';

            // Validate initial state

            cy.findList(ITEM_TYPE_PAGE)
                .findItem(IMPORTER.get(pageOne)!.id)
                .as(ALIAS_ITEM)
                // Open the languages, since these are hidden on default (why?)
                .find('.language-indicator .expand-toggle')
                .click();

            cy.get(ALIAS_ITEM)
                .find('.language-indicator .language-icon')
                .should('not.have.class', CLASS_AVAILABLE);

            cy.intercept({
                pathname: '/rest/page/save/*',
            }, req => {
                req.alias = ALIAS_UPDATE_REQUEST;
            });

            // Assign the language by opening the properties, selecting the language, and saving

            cy.get(ALIAS_ITEM)
                .itemAction('properties');

            cy.get('content-frame combined-properties-editor .properties-content page-properties-form')
                .as(ALIAS_FORM)
                .find('[formcontrolname="language"]')
                .selectValue(LANG);

            cy.editorAction('save');

            // Validate the update

            cy.wait<PageSaveRequest>(ALIAS_UPDATE_REQUEST).then(intercept => {
                expect(intercept.request.body.page.language).to.equal(LANG);
            });

            cy.get(ALIAS_ITEM)
                .find(`.language-indicator .language-icon[data-id="${LANG}"]`)
                .should('have.class', CLASS_AVAILABLE);
        });
    });
});

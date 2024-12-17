/* eslint-disable @typescript-eslint/no-unused-expressions */
import { MultiObjectMoveRequest, PageCreateResponse, PageSaveRequest, SelectTagPartProperty, TagPropertyType } from '@gentics/cms-models';
import {
    EntityImporter,
    folderA,
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
    const ALIAS_CANCEL_REQ = '@cancelRequest';

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
    });

    describe('Page Details', () => {
        const OBJ_PROP_CAT_TESTS = '2';
        const DEFAULT_OBJ_PROP_CAT = [
            '_others_',
            OBJ_PROP_CAT_TESTS,
        ];

        beforeEach(() => {
            cy.wrap(IMPORTER.clearClient(), { log: false }).then(() => {
                return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
            }).then(() => {
                return cy.wrap(IMPORTER.setupTest(TestSize.MINIMAL), { log: false, timeout: 60_000 });
            }).then(() => {
                cy.navigateToApp();
                return cy.login(AUTH_ADMIN);
            }).then(res => {
                cy.window().then(win => {
                    win.localStorage.setItem(`GCMSUI_USER-${res?.user.id}_openObjectPropertyGroups`, JSON.stringify(DEFAULT_OBJ_PROP_CAT));
                });
                cy.selectNode(IMPORTER.get(minimalNode)!.id);
            });
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
            cy.get(ALIAS_MODAL).find('gtx-page-properties').as(ALIAS_FORM);

            cy.get(ALIAS_FORM)
                .find('[formcontrolname="name"] input')
                .type(NEW_PAGE_NAME);

            cy.get(ALIAS_FORM)
                .find('[formcontrolname="fileName"] input')
                .type(NEW_PAGE_PATH)
            cy.get(ALIAS_FORM)
                .find('[formcontrolname="language"]')
                .select(LANGUAGE_EN);

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

        /*
         * SUP-17188: When editing the properties of a page, it incorrectly had a secondary save
         * option to save the properties and apply them to the language variants.
         * This option is meant only to be used for object-properties, and it should only display
         * the primary action actually.
         */
        it('should have only a primary save-action when editing page properties', () => {
            const PAGE = IMPORTER.get(pageOne)!;

            cy.findList(ITEM_TYPE_PAGE)
                .findItem(PAGE.id)
                .itemAction('properties');

            cy.get('content-frame gtx-editor-toolbar [data-action="save"] .primary-button')
                .should('not.have.class', 'has-secondary-actions');
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

            cy.get('content-frame combined-properties-editor .properties-content gtx-page-properties')
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
                .find('[formcontrolname="name"] input')
                .clear()
                .type(CHANGE_PAGE_NAME);

            cy.editorAction('save');

            // Wait for the update to be actually handled
            cy.wait<PageSaveRequest>(ALIAS_UPDATE_REQ).then(intercept => {
                expect(intercept.request.body.deriveFileName).to.equal(false);

                cy.get(ALIAS_ITEM)
                    .find('.item-name .item-name-only')
                    .should('have.text', CHANGE_PAGE_NAME);
            });
        });

        /*
         * SUP-17885: Page file-names should only be derived, when the properties would
         * set the file-name to empty.
         */
        it('should derive the file-name if none was provided during properties updates', () => {
            const PAGE = IMPORTER.get(pageOne)!;
            const ALIAS_ITEM = '@item';

            // Confirm that the original name is correct
            cy.findList(ITEM_TYPE_PAGE)
                .findItem(PAGE.id)
                .as(ALIAS_ITEM)
                .itemAction('properties');

            cy.get('content-frame combined-properties-editor .properties-content gtx-page-properties')
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
                .find('[formcontrolname="fileName"] input')
                .clear();

            cy.editorAction('save');

            // Wait for the update to be actually handled
            cy.wait<PageSaveRequest>(ALIAS_UPDATE_REQ).then(intercept => {
                expect(intercept.request.body.deriveFileName).to.equal(true);
                expect(intercept.request.body.page.fileName).to.equal('');
            });
        });

        /*
         * SUP-17472: Tab-Groups were expended on default, but height was incorrectly set,
         * which hid the category entries (hidden in the sense of not visible to the user).
         */
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
                .select(COLOR_ID);

            /* Save the Object-Property changes
             * ---------------------------- */
            cy.intercept({
                method: 'POST',
                pathname: '/rest/page/save/**',
            }, req => {
                req.alias = ALIAS_UPDATE_REQ;
            });

            cy.editorAction('save');

            cy.wait<PageSaveRequest>(ALIAS_UPDATE_REQ).then(intercept => {
                const req = intercept.request.body;
                const tag = req.page.tags?.[`object.${OBJECT_PROPERTY}`];
                const options = (tag?.properties['select'] as SelectTagPartProperty).selectedOptions;

                expect(options).to.have.length(1);
                expect(options![0].id).to.equal(COLOR_ID);
                // SUP-17885: Updating the object-properties should never update the file name
                expect(intercept.request.body.deriveFileName).to.equal(false);
            });
        });

        it('should cancel editing when the page properties view is closed', () => {
            const PAGE = IMPORTER.get(pageOne)!;

            cy.findList(ITEM_TYPE_PAGE)
                .findItem(PAGE.id)
                .itemAction('properties');

            cy.intercept({
                method: 'POST',
                pathname: '/rest/page/cancel/**',
            }, req => {
                req.alias = ALIAS_CANCEL_REQ;
            });

            cy.editorAction('close');

            cy.wait(ALIAS_CANCEL_REQ);
        });
    });

    describe('Page Details without Language Setup', () => {
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

        beforeEach(() => {
            cy.wrap(IMPORTER.clearClient(), { log: false }).then(() => {
                return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
            }).then(() => {
                return cy.wrap(IMPORTER.importData(CONTENT), { log: false, timeout: 60_000 });
            });
        });

        beforeEach(() => {
            cy.wrap(IMPORTER.clearClient(), { log: false }).then(() => {
                return cy.wrap(
                    Promise.all([
                        IMPORTER.client!.node.assignLanguage(IMPORTER.get(minimalNode)!.id, IMPORTER.languages['de']).send(),
                        IMPORTER.client!.node.assignLanguage(IMPORTER.get(minimalNode)!.id, IMPORTER.languages['en']).send(),
                    ]),
                    { log: false, timeout: 60_000 },
                );
            })
        });

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

            cy.get('content-frame combined-properties-editor .properties-content gtx-page-properties')
                .as(ALIAS_FORM)
                .find('[formcontrolname="language"]')
                .select(LANG);

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

    describe('List Actions', () => {
        beforeEach(() => {
            cy.wrap(IMPORTER.clearClient(), { log: false }).then(() => {
                return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
            }).then(() => {
                return cy.wrap(IMPORTER.setupTest(TestSize.MINIMAL), { log: false, timeout: 60_000 });
            }).then(() => {
                cy.navigateToApp();
            });
        });

        describe('Move', () => {
            const SORT_BY = 'cdate';
            const SORT_ORDER = 'desc';

            beforeEach(() => {
                cy.login(AUTH_ADMIN).then(res => {
                    cy.window().then(win => {
                        win.localStorage.setItem(`GCMSUI_USER-${res?.user.id}_folderSorting`, JSON.stringify({
                            sortBy: SORT_BY,
                            sortOrder: SORT_ORDER,
                        }));
                    });
                    cy.selectNode(IMPORTER.get(minimalNode)!.id);
                });
            });

            it('should move the page to the new folder', () => {
                const PAGE = IMPORTER.get(pageOne)!;
                const TARGET_FOLDER = IMPORTER.get(folderA)!;

                const ALIAS_REPO_BROWSER = '@repoBrowser';
                const ALIAS_MOVE_REQ = '@moveReq';

                cy.findList(PAGE.type)
                    .findItem(PAGE.id)
                    .itemAction('move');

                cy.get('gtx-dynamic-modal repository-browser')
                    .as(ALIAS_REPO_BROWSER)
                    .find(`repository-browser-list[data-type="${TARGET_FOLDER.type}"]`)
                    .findItem(TARGET_FOLDER.id)
                    .find('.item-primary .item-name .item-name-only')
                    .click();

                cy.intercept({
                    pathname: '/rest/page/move',
                }, intercept => {
                    intercept.alias = ALIAS_MOVE_REQ;
                });

                cy.get(ALIAS_REPO_BROWSER)
                    .find('.modal-footer gtx-button[data-action="confirm"]')
                    .click();

                cy.wait<MultiObjectMoveRequest>(ALIAS_MOVE_REQ).then(intercept => {
                    const req = intercept.request.body;
                    expect(req.ids).to.deep.equal([PAGE.id]);
                    expect(req.folderId).to.equal(TARGET_FOLDER.id);
                    expect(req.nodeId).to.equal(TARGET_FOLDER.nodeId);
                });
            });

            /* SUP-17891: Repo-Browser needs to load the correct sort config from the user-settings */
            it('should restore the correct sorting in the repository-browser', () => {
                const PAGE = IMPORTER.get(pageOne)!;

                cy.findList(PAGE.type)
                    .findItem(PAGE.id)
                    .itemAction('move');

                cy.get('gtx-dynamic-modal repository-browser repository-browser-list[data-type="folder"]')
                    .should('have.attr', 'data-sort-by', SORT_BY)
                    .should('have.attr', 'data-sort-order', SORT_ORDER);
            });
        });
    });
});

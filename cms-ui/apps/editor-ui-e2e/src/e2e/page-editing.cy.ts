/* eslint-disable @typescript-eslint/no-unused-expressions */
import { TAB_ID_CONSTRUCTS } from '@gentics/cms-integration-api-models';
import { PagePublishRequest, PageSaveRequest, StringTagPartProperty } from '@gentics/cms-models';
import {
    EntityImporter,
    ENV_ALOHA_PLUGIN_CITE,
    envAll,
    envNone,
    fileOne,
    imageOne,
    IMPORT_ID,
    ITEM_TYPE_PAGE,
    minimalNode,
    pageOne,
    skipableSuite,
    TestSize,
    trimAlohaEmpty,
} from '@gentics/e2e-utils';
import '@gentics/e2e-utils/commands';
import {
    ACTION_FORMAT_ABBR,
    ACTION_FORMAT_BOLD,
    ACTION_FORMAT_CITE,
    ACTION_FORMAT_CODE,
    ACTION_FORMAT_ITALIC,
    ACTION_FORMAT_QUOTE,
    ACTION_FORMAT_STRIKETHROUGH,
    ACTION_FORMAT_SUBSCRIPT,
    ACTION_FORMAT_SUPERSCRIPT,
    ACTION_FORMAT_UNDERLINE,
    ACTION_REMOVE_FORMAT,
    ACTION_SIMPLE_FORMAT_KEYS,
    ACTION_SIMPLE_FORMAT_MAPPING,
    AUTH_ADMIN,
    FIXTURE_TEST_FILE_DOC_1,
    FIXTURE_TEST_IMAGE_JPG_1,
    FORMAT_ABBR,
    FORMAT_QUOTE,
} from '../support/common';

describe('Page Editing', () => {

    const IMPORTER = new EntityImporter();

    const TAG_CONTENT = 'content';
    const TAG_PART_CONTENT_TEXT = 'text';
    const CLASS_ACTIVE = 'active';
    const ALIAS_CONTENT = '@content';
    const ALIAS_CONTROL_BUTTON = '@ctlBtn';
    const ALIAS_SAVE = '@save';
    const ALIAS_CONTROLS = '@controls';
    const ALIAS_TABS = '@tabs';
    const ALIAS_CANCEL_REQ = '@cancelRequest';

    before(() => {
        cy.muteXHR();
        cy.wrap(IMPORTER.clearClient(), { log: false })
            .then(() => {
                return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 })
            })
            .then(() => {
                return cy.wrap(IMPORTER.bootstrapSuite(TestSize.MINIMAL), { log: false, timeout: 60_000 });
            });
    });

    beforeEach(() => {
        cy.muteXHR();

        cy.wrap(IMPORTER.clearClient(), { log: false })
            .then(() => {
                return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 })
            })
            .then(() => {
                return cy.wrap(IMPORTER.setupTest(TestSize.MINIMAL), { log: false, timeout: 60_000 });
            })
            .then(() => {
                cy.navigateToApp();
                cy.login(AUTH_ADMIN);
                cy.selectNode(IMPORTER.get(minimalNode)!.id);
            });
    });

    describe('Edit Mode', () => {
        beforeEach(() => {
            const ALIAS_ALOHA_PAGE_REQ = '@alohaPageReq';

            cy.intercept({
                method: 'GET',
                pathname: '/alohapage',
            }, req => {
                req.alias = ALIAS_ALOHA_PAGE_REQ;
            });

            cy.findList(ITEM_TYPE_PAGE)
                .findItem(IMPORTER.get(pageOne)!.id)
                .itemAction('edit');

            cy.wait(ALIAS_ALOHA_PAGE_REQ).then(() => {
                cy.getAlohaIFrame()
                    // Additional wait, for aloha to initialze all the blocks
                    .find('main [contenteditable="true"]', { timeout: 60_000 })
                    .as(ALIAS_CONTENT);
            });

            cy.get('project-editor content-frame gtx-page-editor-controls')
                .as(ALIAS_CONTROLS);
        });

        it('should be able to add new text to the content-editable', () => {
            const TEXT_CONTENT = 'Foo bar hello world';

            // eslint-disable-next-line cypress/unsafe-to-chain-command
            cy.get(ALIAS_CONTENT)
                .clear()
                // Replace default text with the test content
                .type(TEXT_CONTENT);

            cy.intercept<PageSaveRequest>({
                method: 'POST',
                pathname: '/rest/page/save/*',
            }, request => {
                // Only alias it, if it is the "actual" save request.
                // There's a periodic "save" request being sent which only "updates" the ID,
                // to keep the page in the "locked" state.
                if (request.body.page?.tags) {
                    request.alias = ALIAS_SAVE;
                }
            });

            cy.editorAction('save');

            cy.wait<PageSaveRequest, never>(ALIAS_SAVE).then(intercept => {
                const req = intercept.request.body;
                // The request should send the entered text content as property of the content now.
                // Since aloha may or may not add paragraphs to wrap it properly, we check for both.
                const value = (req?.page?.tags?.[TAG_CONTENT]?.properties?.[TAG_PART_CONTENT_TEXT] as StringTagPartProperty)?.stringValue || '';
                const trimmed = trimAlohaEmpty(value);
                expect(trimmed).to.be.included([`<p>${TEXT_CONTENT}</p>`, TEXT_CONTENT]);
            });
        });

        it('should cancel editing when the edit view is closed', () => {
            cy.intercept({
                method: 'POST',
                pathname: '/rest/page/cancel/**',
            }, req => {
                req.alias = ALIAS_CANCEL_REQ;
            });

            cy.editorAction('close');

            cy.wait(ALIAS_CANCEL_REQ);
        });

        describe('Formatting', () => {
            describe('add and remove basic formats', () => {
                const TEXT_CONTENT = 'test content';

                // If the cite plugin is not enabled, then the quote button is a regular simple
                // format, which can be easily tested here.
                const FORMATS_TO_TEST = ACTION_SIMPLE_FORMAT_KEYS.slice(0);
                if (!envNone(ENV_ALOHA_PLUGIN_CITE)) {
                    const idx = FORMATS_TO_TEST.indexOf(ACTION_FORMAT_QUOTE);
                    if (idx > -1) {
                        FORMATS_TO_TEST.splice(idx, 1);
                    }
                }

                for (const action of FORMATS_TO_TEST) {
                    it(`should format "${action}" correctly`, () => {
                    // eslint-disable-next-line cypress/unsafe-to-chain-command
                        cy.get(ALIAS_CONTENT)
                        // Clear the content first
                            .clear()
                        // Replace default text with the test content
                            .type(TEXT_CONTENT)
                            .rangeSelection(0, null, true);

                        cy.get(ALIAS_CONTROLS)
                            .findAlohaComponent({ slot: action })
                            .btn()
                            .as(ALIAS_CONTROL_BUTTON);

                        // button should not be marked as active yet
                        cy.get(ALIAS_CONTROL_BUTTON)
                            .should('not.have.class', CLASS_ACTIVE)
                            .click();

                        cy.get(ALIAS_CONTENT)
                            .should('have.formatting', TEXT_CONTENT, [ACTION_SIMPLE_FORMAT_MAPPING[action]]);

                        // button should be marked as active now
                        cy.get(ALIAS_CONTROL_BUTTON)
                            .should('have.class', CLASS_ACTIVE);

                        // Now we select the text again, and remove the formatting again
                        cy.get(ALIAS_CONTENT)
                            .rangeSelection(0, null, true);

                        // Should remove the formatting with this
                        cy.get(ALIAS_CONTROL_BUTTON).click();

                        cy.get(ALIAS_CONTENT)
                            .should('have.formatting', TEXT_CONTENT, []);

                        // button should be marked as inactive now
                        cy.get(ALIAS_CONTROL_BUTTON)
                            .should('not.have.class', CLASS_ACTIVE);
                    });
                }
            });

            describe('format a range with multiple formats', () => {
                const TEXT_CONTENT = 'test content';

                /*
             * Note: The amount created with combinationMatrix, actually breaks cypress.
             * At some point we might configure the mempry correctly, or detect *what* is
             * hogging so much memory (probably Aloha), causing the test to break.
             */
                // const FORMATS = combinationMatrix(ACTION_FORMAT_KEYS);
                const FORMATS = [
                    [
                        ACTION_FORMAT_BOLD,
                        ACTION_FORMAT_ITALIC,
                        ACTION_FORMAT_UNDERLINE,
                        ACTION_FORMAT_CODE,
                        ACTION_FORMAT_SUBSCRIPT,
                    ],
                    [
                        ACTION_FORMAT_BOLD,
                        ACTION_FORMAT_STRIKETHROUGH,
                        ACTION_FORMAT_CITE,
                        ACTION_FORMAT_SUPERSCRIPT,
                    ],
                ];

                for (const group of FORMATS) {
                    it(`should handle formats ["${group.join('", "')}"] correctly`, () => {
                    // eslint-disable-next-line cypress/unsafe-to-chain-command
                        cy.get(ALIAS_CONTENT)
                        // Clear the content first
                            .clear()
                        // Replace default text with the test content
                            .type(TEXT_CONTENT)
                            .rangeSelection(0, null, true);

                        for (const action of group) {
                            cy.get(ALIAS_CONTROLS)
                                .findAlohaComponent({ slot: action })
                                .click();
                        }

                        cy.get(ALIAS_CONTENT)
                            .should('have.formatting', TEXT_CONTENT, group.map(key => ACTION_SIMPLE_FORMAT_MAPPING[key]));
                    });
                }
            });

            describe('remove formatting in nested formats correctly', () => {
                const FULL_CONTENT = 'foo bar hello world test content this is a test text';
                const TEXT_CONTENT = 'test content';

                const BASE_ONE = [
                    ACTION_FORMAT_BOLD,
                    ACTION_FORMAT_ITALIC,
                    ACTION_FORMAT_UNDERLINE,
                    ACTION_FORMAT_SUBSCRIPT,
                ];
                const BASE_TWO = [
                    ACTION_FORMAT_STRIKETHROUGH,
                    ACTION_FORMAT_CITE,
                    ACTION_FORMAT_SUPERSCRIPT,
                ];

                const CONTROLS = [
                    { apply: BASE_ONE, remove: [ACTION_FORMAT_BOLD] },
                    { apply: BASE_ONE, remove: [ACTION_FORMAT_ITALIC] },
                    { apply: BASE_ONE, remove: [ACTION_FORMAT_UNDERLINE] },
                    { apply: BASE_ONE, remove: [ACTION_FORMAT_SUBSCRIPT] },

                    { apply: BASE_TWO, remove: [ACTION_FORMAT_STRIKETHROUGH] },
                    { apply: BASE_TWO, remove: [ACTION_FORMAT_CITE] },
                    { apply: BASE_TWO, remove: [ACTION_FORMAT_SUPERSCRIPT] },
                ];

                for (const ctl of CONTROLS) {
                    it(`should apply ["${ctl.apply.join('", "')}"] and remove ["${ctl.remove.join('", "')}"] correctly`, () => {
                    // eslint-disable-next-line cypress/unsafe-to-chain-command
                        cy.get(ALIAS_CONTENT)
                            .clear()
                            .type(FULL_CONTENT)
                            .textSelection(TEXT_CONTENT, true);

                        // Make sure the buttons are properly reset
                        for (const action of ACTION_SIMPLE_FORMAT_KEYS) {
                            cy.get(ALIAS_CONTROLS)
                                .findAlohaComponent({ slot: action })
                                .btn()
                                .should('not.have.class', CLASS_ACTIVE);
                        }

                        for (const action of ctl.apply) {
                            cy.get(ALIAS_CONTROLS)
                                .findAlohaComponent({ slot: action })
                                .click();
                        }

                        cy.get(ALIAS_CONTENT)
                            .should('have.formatting', TEXT_CONTENT, ctl.apply.map(key => ACTION_SIMPLE_FORMAT_MAPPING[key]));

                        for (const action of ctl.remove) {
                            cy.get(ALIAS_CONTROLS)
                                .findAlohaComponent({ slot: action })
                                .click();
                        }

                        cy.get(ALIAS_CONTENT)
                            .should('have.formatting', TEXT_CONTENT, ctl.apply
                                .filter(key => !ctl.remove.includes(key))
                                .map(key => ACTION_SIMPLE_FORMAT_MAPPING[key]),
                            );
                    });
                }
            });

            describe('remove formatting with the removeFormatting button', () => {
                const FULL_CONTENT = 'foo bar hello world test content this is a test text';
                const TEXT_CONTENT = 'test content';

                // const FORMATS = combinationMatrix(ACTION_FORMAT_KEYS);
                const FORMATS = [ACTION_SIMPLE_FORMAT_KEYS];

                for (const group of FORMATS) {
                    it(`should be able to clear formats ["${group.join('", "')}]`, () => {
                    // eslint-disable-next-line cypress/unsafe-to-chain-command
                        cy.get(ALIAS_CONTENT)
                            .clear()
                            .type(FULL_CONTENT)
                            .textSelection(TEXT_CONTENT, true);

                        for (const action of group) {
                            cy.get(ALIAS_CONTROLS)
                                .findAlohaComponent({ slot: action })
                                .click();
                        }

                        cy.get(ALIAS_CONTENT).rangeSelection(0, null, true);
                        cy.get(ALIAS_CONTROLS)
                            .findAlohaComponent({ slot: ACTION_REMOVE_FORMAT })
                            .click();

                        cy.get(ALIAS_CONTENT)
                            .should('have.formatting', FULL_CONTENT, []);

                        for (const action of ACTION_SIMPLE_FORMAT_KEYS) {
                            cy.get(ALIAS_CONTROLS)
                                .findAlohaComponent({ slot: action })
                                .btn()
                                .should('not.have.class', CLASS_ACTIVE);
                        }
                    });
                }
            });

            it('should format and manage abbreviations correctly', () => {
                const FULL_CONTENT = 'foo bar hello world test content this is a test text';
                const TEXT_CONTENT = 'test content';
                const TITLE_CONTENT = 'something fancy';

                const CLASS_INPUT_ACTIVE = 'input-active';
                const ATTR_TITLE = 'title';

                const ALIAS_WRAPPER = '@btnWrapper';
                const ALIAS_BUTTON = '@btnPrimary';

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_CONTENT)
                    .clear()
                    .type(FULL_CONTENT)
                    .textSelection(TEXT_CONTENT, true);

                // Format it as abbr
                cy.get(ALIAS_CONTROLS)
                    .findAlohaComponent({ slot: ACTION_FORMAT_ABBR })
                    .as(ALIAS_BUTTON)
                    .find('.gtx-editor-toggle-split-button')
                    .as(ALIAS_WRAPPER);

                cy.get(ALIAS_BUTTON)
                    .click();

                // Verify formatting worked
                cy.get(ALIAS_CONTENT)
                    .should('have.formatting', TEXT_CONTENT, ['abbr']);

                // Verify no title has been set yet
                cy.get(ALIAS_CONTENT)
                    .find(FORMAT_ABBR)
                    .then($elem => {
                        expect($elem.attr(ATTR_TITLE)).to.equal('');
                    });

                // Verify button state, and click the secondary button
                cy.get(ALIAS_WRAPPER)
                    .should('have.class', CLASS_ACTIVE)
                    .should('have.class', CLASS_INPUT_ACTIVE)
                    .should('be.visible');

                cy.get(ALIAS_BUTTON)
                    .click({ action: 'secondary' });

                // Enter the title in the dropdown, and confirm input with "enter"
                cy.findDynamicDropdown(ACTION_FORMAT_ABBR)
                    .find('gtx-aloha-input-renderer input')
                    .type(`${TITLE_CONTENT}{enter}`);

                // Verify the title has been updated
                cy.get(ALIAS_CONTENT)
                    .find(FORMAT_ABBR)
                    .then($elem => {
                        expect($elem.attr(ATTR_TITLE)).to.equal(TITLE_CONTENT);
                    });

                // Remove the formatting again
                cy.get(ALIAS_BUTTON).click();

                // Verify formatting has been removed
                cy.get(ALIAS_CONTENT)
                    .should('have.formatting', FULL_CONTENT, []);
            });

            /*
            * TODO: This test only works, because we have the `extra/cite` plugin enabled.
            * Otherwise, the quote formatting would work differently.
            * We should make it possible to test different aloha configs, but not sure how just yet.
            * Additionally, the `note` feature is not really working in the context of the CMS,
            * which is why we have the entire test skipped and the plugin disabled so far.
            */
            skipableSuite(envAll(ENV_ALOHA_PLUGIN_CITE), 'With "extra/cite" aloha plugin active', () => {
                it('should format and manage a inline quote correctly', () => {
                    const TEXT_CONTENT = 'test content';
                    const SOURCE_CONTENT = 'https://gentics.com';
                    const NOTE_CONTENT = 'something something, this is a note from 2024';

                    const SLOT_SOURCE = 'source';
                    const SLOT_NOTE = 'note';
                    const ATTR_CITE = 'cite';
                    const ATTR_CITE_ID = 'data-cite-id';

                    const ALIAS_MODAL = '@modal';

                    // eslint-disable-next-line cypress/unsafe-to-chain-command
                    cy.get(ALIAS_CONTENT)
                        .clear()
                        .type(TEXT_CONTENT)
                        .rangeSelection(0, -2, true);

                    cy.findAlohaComponent({ slot: ACTION_FORMAT_QUOTE })
                        .click();

                    cy.findDynamicFormModal(ACTION_FORMAT_QUOTE)
                        .as(ALIAS_MODAL);

                    cy.get(ALIAS_MODAL)
                        .findAlohaComponent({ slot: SLOT_SOURCE, type: 'input' })
                        .find('input')
                        .type(SOURCE_CONTENT);

                    cy.get(ALIAS_MODAL)
                        .findAlohaComponent({ slot: SLOT_NOTE, type: 'input' })
                        .find('input')
                        .type(NOTE_CONTENT);

                    cy.get(ALIAS_MODAL)
                        .find('.modal-footer gtx-button[data-action="confirm"]')
                        .click();

                    cy.get(ALIAS_CONTENT)
                        .should('have.formatting', TEXT_CONTENT, [FORMAT_QUOTE])
                        .find(FORMAT_QUOTE)
                        .then($quote => {
                            expect($quote.attr(ATTR_CITE)).to.equal(SOURCE_CONTENT);
                            const id = $quote.attr(ATTR_CITE_ID);
                            cy.find(`#cite-note-${id}`)
                                .should('have.text', NOTE_CONTENT);
                        });
                });
            });
        });

        describe('Links', () => {
            beforeEach(() => {
                cy.wrap(IMPORTER.clearClient(), { log: false }).then(() => {
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
                    return cy.wrap(IMPORTER.importData([imageOne, fileOne], TestSize.MINIMAL), { log: false, timeout: 60_000 });
                });
            });

            const ALIAS_MODAL = '@modal';
            const ALIAS_FORM = '@form';

            it('should insert new links correctly', () => {
                const TEXT_CONTENT = 'Hello ';
                const LINK_URL = 'https://gentics.com';
                const LINK_ANCHOR = 'example';
                const LINK_TARGET = '_blank';
                const LINK_LANGUAGE = 'de';
                const LINK_TITLE = 'This is a title!';

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_CONTENT)
                    .clear()
                    .type(TEXT_CONTENT);

                cy.findAlohaComponent({ slot: 'insertLink', type: 'toggle-split-button' })
                    .click();

                cy.findDynamicFormModal()
                    .as(ALIAS_MODAL)
                    .find('.modal-content .form-wrapper')
                    .as(ALIAS_FORM);

                /*
                 * Fill out the link form
                 */

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="url"] .target-input input')
                    .clear()
                    .type(LINK_URL);

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="url"] .anchor-input input')
                    .clear()
                    .type(LINK_ANCHOR);

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="title"] input')
                    .clear()
                    .type(LINK_TITLE);

                cy.get(ALIAS_FORM)
                    .find('> [data-slot="target"] gtx-select')
                    .select(LINK_TARGET);

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="lang"] input')
                    .clear()
                    .type(LINK_LANGUAGE);

                /*
                 * Confirm the modal so the link get's inserted
                 */

                cy.get(ALIAS_MODAL)
                    .find('.modal-footer [data-action="confirm"]')
                    .click();

                /*
                 * Validate HTML
                 */

                cy.get(ALIAS_CONTENT)
                    .find('a')
                    .then($elem => {
                        expect($elem.attr('href')).to.equal(`${LINK_URL}#${LINK_ANCHOR}`);
                        expect($elem.attr('hreflang')).to.equal(LINK_LANGUAGE);
                        expect($elem.attr('target')).to.equal(LINK_TARGET);
                        expect($elem.attr('title')).to.equal(LINK_TITLE);
                    });
            });

            it('should be able to select an internal image as link', () => {
                const TEXT_CONTENT = 'Hello ';
                const ITEM_NODE = IMPORTER.get(minimalNode)!;
                const LINK_ITEM = IMPORTER.get(imageOne)!;
                const LINK_ANCHOR = 'example';
                const LINK_TARGET = '_blank';
                const LINK_LANGUAGE = 'de';
                const LINK_TITLE = 'This is a title!';

                const ALIAS_REPO_BROWSER = '@repoBrowser';

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_CONTENT)
                    .clear()
                    .type(TEXT_CONTENT);

                cy.findAlohaComponent({ slot: 'insertLink', type: 'toggle-split-button' })
                    .click();

                cy.findDynamicFormModal()
                    .as(ALIAS_MODAL)
                    .find('.modal-content .form-wrapper')
                    .as(ALIAS_FORM);

                /*
                 * Fill out the link form
                 */

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="url"] .target-wrapper button[data-action="select"]')
                    .click();

                cy.get('gtx-dynamic-modal repository-browser')
                    .as(ALIAS_REPO_BROWSER)
                    .find('repository-browser-list[data-type="image"]')
                    .findItem(LINK_ITEM.id)
                    .find('>icon-checkbox gtx-checkbox, >.thumbnail-overlay .top-bar gtx-checkbox')
                    .check();

                cy.get(ALIAS_REPO_BROWSER)
                    .find('.modal-footer gtx-button[data-action="confirm"]')
                    .click();

                cy.get(ALIAS_FORM)
                    .find('> [data-slot="url"] .target-input')
                    .should('have.class', 'internal')
                    .find('.internal-input .page-language')
                    .should('not.exist');

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="url"] .anchor-input input')
                    .clear()
                    .type(LINK_ANCHOR);

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="title"] input')
                    .clear()
                    .type(LINK_TITLE);

                cy.get(ALIAS_FORM)
                    .find('> [data-slot="target"] gtx-select')
                    .select(LINK_TARGET);

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="lang"] input')
                    .clear()
                    .type(LINK_LANGUAGE);

                /*
                 * Confirm the modal so the link get's inserted
                 */

                cy.get(ALIAS_MODAL)
                    .find('.modal-footer [data-action="confirm"]')
                    .click();

                /*
                 * Validate HTML
                 */

                cy.get(ALIAS_CONTENT)
                    .find('a')
                    .then($elem => {
                        expect($elem.attr('href')).to.equal(`/rest/file/content/load/${LINK_ITEM.id}?nodeId=${ITEM_NODE.id}`);
                        expect($elem.attr('hreflang')).to.equal(LINK_LANGUAGE);
                        expect($elem.attr('target')).to.equal(LINK_TARGET);
                        expect($elem.attr('title')).to.equal(LINK_TITLE);
                        expect($elem.attr('data-gentics-aloha-repository')).to.equal('com.gentics.aloha.GCN.Page');
                        expect($elem.attr('data-gcn-target-label')).to.equal(LINK_ITEM.name);
                        expect($elem.attr('data-gentics-aloha-object-id')).to.equal(`10011.${LINK_ITEM.id}`);
                        expect($elem.attr('data-gcn-channelid')).to.equal(`${ITEM_NODE.id}`);
                    });
            });

            it('should be able to select an internal file as link', () => {
                const TEXT_CONTENT = 'Hello ';
                const ITEM_NODE = IMPORTER.get(minimalNode)!;
                const LINK_ITEM = IMPORTER.get(fileOne)!;
                const LINK_ANCHOR = 'example';
                const LINK_TARGET = '_blank';
                const LINK_LANGUAGE = 'de';
                const LINK_TITLE = 'This is a title!';

                const ALIAS_REPO_BROWSER = '@repoBrowser';

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_CONTENT)
                    .clear()
                    .type(TEXT_CONTENT);

                cy.findAlohaComponent({ slot: 'insertLink', type: 'toggle-split-button' })
                    .click();

                cy.findDynamicFormModal()
                    .as(ALIAS_MODAL)
                    .find('.modal-content .form-wrapper')
                    .as(ALIAS_FORM);

                /*
                 * Fill out the link form
                 */

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="url"] .target-wrapper button[data-action="select"]')
                    .click();

                cy.get('gtx-dynamic-modal repository-browser')
                    .as(ALIAS_REPO_BROWSER)
                    .find('repository-browser-list[data-type="file"]')
                    .findItem(LINK_ITEM.id)
                    .find('>icon-checkbox gtx-checkbox, >.thumbnail-overlay .top-bar gtx-checkbox')
                    .check();

                cy.get(ALIAS_REPO_BROWSER)
                    .find('.modal-footer gtx-button[data-action="confirm"]')
                    .click();

                cy.get(ALIAS_FORM)
                    .find('> [data-slot="url"] .target-input')
                    .should('have.class', 'internal')
                    .find('.internal-input .page-language')
                    .should('not.exist');

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="url"] .anchor-input input')
                    .clear()
                    .type(LINK_ANCHOR);

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="title"] input')
                    .clear()
                    .type(LINK_TITLE);

                cy.get(ALIAS_FORM)
                    .find('> [data-slot="target"] gtx-select')
                    .select(LINK_TARGET);

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="lang"] input')
                    .clear()
                    .type(LINK_LANGUAGE);

                /*
                 * Confirm the modal so the link get's inserted
                 */

                cy.get(ALIAS_MODAL)
                    .find('.modal-footer [data-action="confirm"]')
                    .click();

                /*
                 * Validate HTML
                 */

                cy.get(ALIAS_CONTENT)
                    .find('a')
                    .then($elem => {
                        expect($elem.attr('href')).to.equal(`/rest/file/content/load/${LINK_ITEM.id}?nodeId=${ITEM_NODE.id}`);
                        expect($elem.attr('hreflang')).to.equal(LINK_LANGUAGE);
                        expect($elem.attr('target')).to.equal(LINK_TARGET);
                        expect($elem.attr('title')).to.equal(LINK_TITLE);
                        expect($elem.attr('data-gentics-aloha-repository')).to.equal('com.gentics.aloha.GCN.Page');
                        expect($elem.attr('data-gcn-target-label')).to.equal(LINK_ITEM.name);
                        expect($elem.attr('data-gentics-aloha-object-id')).to.equal(`10008.${LINK_ITEM.id}`);
                        expect($elem.attr('data-gcn-channelid')).to.equal(`${ITEM_NODE.id}`);
                    });
            });

            it('should be able to select an internal page as link', () => {
                const TEXT_CONTENT = 'Hello ';
                const ITEM_NODE = IMPORTER.get(minimalNode)!;
                const LINK_ITEM = IMPORTER.get(pageOne)!;
                const LINK_ANCHOR = 'example';
                const LINK_TARGET = '_blank';
                const LINK_LANGUAGE = 'de';
                const LINK_TITLE = 'This is a title!';

                const ALIAS_REPO_BROWSER = '@repoBrowser';

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_CONTENT)
                    .clear()
                    .type(TEXT_CONTENT);

                cy.findAlohaComponent({ slot: 'insertLink', type: 'toggle-split-button' })
                    .click();

                cy.findDynamicFormModal()
                    .as(ALIAS_MODAL)
                    .find('.modal-content .form-wrapper')
                    .as(ALIAS_FORM);

                /*
                 * Fill out the link form
                 */

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="url"] .target-wrapper button[data-action="select"]')
                    .click();

                cy.get('gtx-dynamic-modal repository-browser')
                    .as(ALIAS_REPO_BROWSER)
                    .find('repository-browser-list[data-type="page"]')
                    .findItem(LINK_ITEM.id)
                    .find('>icon-checkbox gtx-checkbox, >.thumbnail-overlay .top-bar gtx-checkbox')
                    .check();

                cy.get(ALIAS_REPO_BROWSER)
                    .find('.modal-footer gtx-button[data-action="confirm"]')
                    .click();

                cy.get(ALIAS_FORM)
                    .find('> [data-slot="url"] .target-input')
                    .should('have.class', 'internal')
                    .find('.internal-input .page-language')
                    .should('exist')
                    .should('have.text', LINK_ITEM.language);

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="url"] .anchor-input input')
                    .clear()
                    .type(LINK_ANCHOR);

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="title"] input')
                    .clear()
                    .type(LINK_TITLE);

                cy.get(ALIAS_FORM)
                    .find('> [data-slot="target"] gtx-select')
                    .select(LINK_TARGET);

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_FORM)
                    .find('> [data-slot="lang"] input')
                    .clear()
                    .type(LINK_LANGUAGE);

                /*
                 * Confirm the modal so the link get's inserted
                 */

                cy.get(ALIAS_MODAL)
                    .find('.modal-footer [data-action="confirm"]')
                    .click();

                /*
                 * Validate HTML
                 */

                cy.get(ALIAS_CONTENT)
                    .find('a')
                    .then($elem => {
                        expect($elem.attr('href')).to.equal(`/alohapage?real=newview&realid=${LINK_ITEM.id}&nodeid=${ITEM_NODE.id}`);
                        expect($elem.attr('hreflang')).to.equal(LINK_LANGUAGE);
                        expect($elem.attr('target')).to.equal(LINK_TARGET);
                        expect($elem.attr('title')).to.equal(LINK_TITLE);
                        expect($elem.attr('data-gentics-aloha-repository')).to.equal('com.gentics.aloha.GCN.Page');
                        expect($elem.attr('data-gcn-target-label')).to.equal(LINK_ITEM.name);
                        expect($elem.attr('data-gentics-aloha-object-id')).to.equal(`10007.${LINK_ITEM.id}`);
                        expect($elem.attr('data-gcn-channelid')).to.equal(`${ITEM_NODE.id}`);
                    });
            });
        });

        describe('Typography', () => {
            const FULL_CONTENT = 'foo bar hello world test content this is a test text';
            const TEXT_CONTENT = 'test content';
            const SLOT_TYPO = 'typographyMenu';
            const ALIAS_TYPO_BUTTON = '@typographyButton';
            const CLASS_CUSTOMIZED = 'aloha-customized';

            const HEADER_TYPOGRAPHY = [
                'h1',
                'h2',
                'h3',
                'h4',
                'h5',
                'h6',
            ]
            const TYPOGRAPHIES = [
                ...HEADER_TYPOGRAPHY,
                'pre',
            ];

            for (const TYPOGRAPHY_NAME of TYPOGRAPHIES) {
                it(`should apply "${TYPOGRAPHY_NAME}" to the selection`, () => {
                    // eslint-disable-next-line cypress/unsafe-to-chain-command
                    cy.get(ALIAS_CONTENT)
                        .clear()
                        .type(FULL_CONTENT)
                        .textSelection(TEXT_CONTENT, true);

                    cy.get(ALIAS_CONTENT)
                        .then($el => {
                            expect($el.find(TYPOGRAPHY_NAME)).to.have.lengthOf(0);
                        });

                    cy.findAlohaComponent({ slot: SLOT_TYPO })
                        .click();

                    cy.findDynamicDropdown('')
                        .find(`.select-menu-entry[data-id="${TYPOGRAPHY_NAME}"]`)
                        .click();

                    cy.get(ALIAS_CONTENT)
                        .find(TYPOGRAPHY_NAME)
                        .should('exist')
                        .should('have.text', TEXT_CONTENT);
                });
            }

            it('should apply the header id on headings', () => {
                const EXAMPLE_ID = 'hello-world-something';
                const ALIAS_TYPO_ELEM = '@typographyElement';

                // eslint-disable-next-line cypress/unsafe-to-chain-command
                cy.get(ALIAS_CONTENT)
                    .clear()
                    .type(FULL_CONTENT)
                    .textSelection(TEXT_CONTENT, true);

                cy.get(ALIAS_CONTENT)
                    .then($el => {
                        expect($el.find(HEADER_TYPOGRAPHY[0])).to.have.lengthOf(0);
                    });

                cy.findAlohaComponent({ slot: SLOT_TYPO })
                    .as(ALIAS_TYPO_BUTTON)
                    .click();

                cy.findDynamicDropdown()
                    .find(`.select-menu-entry[data-id="${HEADER_TYPOGRAPHY[0]}"]`)
                    .click();

                cy.get(ALIAS_CONTENT)
                    .find(HEADER_TYPOGRAPHY[0])
                    .should('exist')
                    .as(ALIAS_TYPO_ELEM)
                    .should('have.text', TEXT_CONTENT)
                    .should('have.id', 'test-content')
                    .should('not.have.class', CLASS_CUSTOMIZED);

                cy.get(ALIAS_TYPO_BUTTON)
                    .click({ action: 'secondary' });

                cy.findDynamicDropdown()
                    .find('input')
                    .type(`${EXAMPLE_ID}{enter}`);

                cy.get(ALIAS_TYPO_ELEM)
                    .should('exist')
                    .should('have.text', TEXT_CONTENT)
                    .should('have.id', EXAMPLE_ID)
                    .should('have.class', CLASS_CUSTOMIZED);
            });
        });

        describe('Character Picker', () => {
            it('should be possible to insert special characters', () => {
                cy.get(ALIAS_CONTENT)
                    .clear();

                cy.findAlohaComponent({ slot: 'characterPicker', type: 'context-button' })
                    .openContext()
                    .find('button.symbol-grid-cell[title="Small delta"]')
                    .click();

                cy.get(ALIAS_CONTENT)
                    .should('have.text', 'δ');
            });
        });

        describe('Actions', () => {
            it('should be possible to manage the publish time of the page', () => {
                const ALIAS_MODAL = '@modal';
                const ALIAS_PUBLISH_REQ = '@publishReq';

                const now = new Date();
                const oneDay = 1000 * 60 * 60 * 24;
                const PUBLISH_AT_DATE = new Date(now.getTime() + oneDay);

                cy.editorAction('editor-context')
                    .find('gtx-dropdown-item[data-action="time-management"]')
                    .click({ force: true });

                cy.intercept<PagePublishRequest>({
                    pathname: '/rest/page/publish/*',
                }, req => {
                    expect(req.body.alllang).to.equal(false);
                    expect(req.body.at).to.equal(Math.trunc(PUBLISH_AT_DATE.getTime() / 1000));
                    req.alias = ALIAS_PUBLISH_REQ;
                });

                cy.get('gtx-dynamic-modal time-management-modal')
                    .as(ALIAS_MODAL)
                    .find('.modal-content .timemgmt-form-current gtx-date-time-picker[data-control="publishAt"]')
                    .pickDate(PUBLISH_AT_DATE);

                cy.get(ALIAS_MODAL)
                    .find('.modal-footer gtx-button[data-action="confirm"]')
                    .click();

                cy.wait(ALIAS_PUBLISH_REQ);
            });
        });
    });

    /*
     * SUP-17542: Constructs were loaded via `/rest/construct`, which is for administration.
     * The endpoint for editable constructs for the page, is `/rest/construct/list`.
     * This test verifies that the request has used the correct endpoint.
     */
    it('should load the constructs for the edit-mode correctly', () => {
        const ALIAS_VALID_REQ = '@validReq';

        cy.intercept({
            method: 'GET',
            pathname: '/rest/construct',
        }, req => {
            // Requests with '_' in the query are from aloha, and should be ignored here.
            if (!req.query['_']) {
                expect(true).to.equal(false, 'Invalid Request to "/rest/construct" has been sent!');
            }
        });

        cy.intercept({
            method: 'GET',
            pathname: '/rest/construct/list',
            query: {
                nodeId: `${IMPORTER.get(minimalNode)!.id}`,
                pageId: `${IMPORTER.get(pageOne)!.id}`,
            },
        }, req => {
            req.alias = ALIAS_VALID_REQ
        });

        cy.findList(ITEM_TYPE_PAGE)
            .findItem(IMPORTER.get(pageOne)!.id)
            .itemAction('edit');

        cy.get('project-editor content-frame gtx-page-editor-controls')
            .as(ALIAS_CONTROLS);

        cy.get('project-editor content-frame gtx-page-editor-tabs')
            .as(ALIAS_TABS);

        cy.getAlohaIFrame()
            // Additional wait, for aloha to initialze all the blocks
            .find('main [contenteditable="true"]', { timeout: 60_000 })
            .as(ALIAS_CONTENT);

        cy.get(ALIAS_TABS)
            .find(`[data-id="${TAB_ID_CONSTRUCTS}"]`)
            .click();

        // Will fail if it hasn't been called
        cy.wait(ALIAS_VALID_REQ);

        // Select the content to make the constructs selectable
        cy.get(ALIAS_CONTENT)
            .click();

        cy.get(ALIAS_CONTROLS)
            .find('gtx-construct-controls .no-constructs')
            .should('not.exist');
    });
});

describe('Page Preview', () => {

    const IMPORTER = new EntityImporter();

    const ALIAS_CONTENT = '@content';
    const ALIAS_CONTROLS = '@controls';
    const ALIAS_TABS = '@tabs';

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
            return cy.wrap(IMPORTER.setupTest(TestSize.MINIMAL), { log: false, timeout: 60_000 });
        }).then(() => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.selectNode(IMPORTER.get(minimalNode)!.id);
            cy.findList(ITEM_TYPE_PAGE)
                .findItem(IMPORTER.get(pageOne)!.id)
                .find('.item-primary .item-name .item-name-only')
                .click();
        });
    });

    /*
     * SUP-17223: Issue where when a page is open in preview mode, and opens in edit-mode,
     * the edit-mode doesn't properly initialize the constructs.
     */
    it('should load constructs correctly when switching to edit mode', () => {

        // High timeout for all of aloha to finish loading
        cy.get('project-editor content-frame iframe[name="master-frame"][loaded="true"]', { timeout: 60_000 })
            .its('0.contentDocument.body')
            .find('main', { timeout: 60_000 })
            .should('exist');

        /*
         * This wait is *required*, as otherwise the subscriptions aren't setup,
         * which cause this whole issue in the first place.
         */
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(2_000);

        // Now switch to the edit-mode
        cy.editorAction('edit');

        // High timeout for all of aloha to finish loading
        cy.get('project-editor content-frame iframe[name="master-frame"][loaded="true"]', { timeout: 60_000 })
            .its('0.contentDocument.body')
            // Additional wait, for aloha to initialze all the blocks
            .find('main [contenteditable="true"]', { timeout: 60_000 })
            .as(ALIAS_CONTENT)

        cy.get('project-editor content-frame gtx-page-editor-controls')
            .as(ALIAS_CONTROLS);
        cy.get('project-editor content-frame gtx-page-editor-tabs')
            .as(ALIAS_TABS);

        cy.get(ALIAS_TABS)
            .find(`[data-id="${TAB_ID_CONSTRUCTS}"]`)
            .click();

        cy.get(ALIAS_CONTENT)
            .click();

        cy.get(ALIAS_CONTROLS)
            .find('gtx-construct-controls .no-constructs')
            .should('not.exist');

        cy.get(ALIAS_CONTROLS)
            .find('gtx-construct-controls .groups-container')
            .should('exist')
            .find('.construct-category')
            .should('exist')
            .should('have.lengthOf', 2);
    });
});

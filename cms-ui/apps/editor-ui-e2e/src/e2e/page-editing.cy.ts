/* eslint-disable @typescript-eslint/no-unused-expressions */
import { PageSaveRequest, StringTagPartProperty } from '@gentics/cms-models';
import {
    EntityImporter,
    ENV_ALOHA_PLUGIN_CITE,
    envAll,
    envNone,
    ITEM_TYPE_PAGE,
    minimalNode,
    pageOne,
    skipableSuite,
    TestSize,
    trimAlohaEmpty,
} from '@gentics/e2e-utils';
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

    before(async () => {
        cy.muteXHR();
        await IMPORTER.cleanupTest();
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    beforeEach(async () => {
        cy.muteXHR();
        await IMPORTER.cleanupTest();
        await IMPORTER.setupTest(TestSize.MINIMAL);

        cy.navigateToApp();
        cy.login(AUTH_ADMIN);
        cy.selectNode(IMPORTER.get(minimalNode)!.id);
        cy.findList(ITEM_TYPE_PAGE)
            .findItem(IMPORTER.get(pageOne)!.id)
            .itemAction('edit');

        // High timeout for all of aloha to finish loading
        cy.get('project-editor content-frame iframe[name="master-frame"][loaded="true"]', { timeout: 60_000 })
            .its('0.contentDocument.body')
            // Additional wait, for aloha to initialze all the blocks
            .find('main [contenteditable="true"]', { timeout: 60_000 })
            .as(ALIAS_CONTENT)

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
                request.alias = 'save';
            }
        });

        cy.editorSave();

        cy.wait<PageSaveRequest, never>(ALIAS_SAVE).then(intercept => {
            const req = intercept.request.body;
            // The request should send the entered text content as property of the content now.
            // Since aloha may or may not add paragraphs to wrap it properly, we check for both.
            const value = (req?.page?.tags?.[TAG_CONTENT]?.properties?.[TAG_PART_CONTENT_TEXT] as StringTagPartProperty)?.stringValue || '';
            const trimmed = trimAlohaEmpty(value);
            expect(trimmed).to.be.included([`<p>${TEXT_CONTENT}</p>`, TEXT_CONTENT]);
        });
    });

    describe('add and remove basic formats', () => {
        const TEXT_CONTENT = 'test content';

        // If the cite plugin is not enabled, then the quote button is a regular simple
        // format, which can be easily tested here.
        const FORMATS_TO_TEST = ACTION_SIMPLE_FORMAT_KEYS.slice(0);
        if (envNone(ENV_ALOHA_PLUGIN_CITE)) {
            FORMATS_TO_TEST.push(ACTION_FORMAT_QUOTE);
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
                        .btnClick();
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
                        .btnClick();
                }

                cy.get(ALIAS_CONTENT)
                    .should('have.formatting', TEXT_CONTENT, ctl.apply.map(key => ACTION_SIMPLE_FORMAT_MAPPING[key]));

                for (const action of ctl.remove) {
                    cy.get(ALIAS_CONTROLS)
                        .findAlohaComponent({ slot: action })
                        .btnClick();
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
                        .btnClick();
                }

                cy.get(ALIAS_CONTENT).rangeSelection(0, null, true);
                cy.get(ALIAS_CONTROLS)
                    .findAlohaComponent({ slot: ACTION_REMOVE_FORMAT })
                    .btnClick();

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
        const ALIAS_SECONDARY_BUTTON = '@btnSecondary';

        // eslint-disable-next-line cypress/unsafe-to-chain-command
        cy.get(ALIAS_CONTENT)
            .clear()
            .type(FULL_CONTENT)
            .textSelection(TEXT_CONTENT, true);

        // Format it as abbr
        cy.get(ALIAS_CONTROLS)
            .findAlohaComponent({ slot: ACTION_FORMAT_ABBR })
            .find('.gtx-editor-toggle-split-button')
            .as(ALIAS_WRAPPER)
            .btn()
            .as(ALIAS_BUTTON)
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

        // Verify button state, and click it
        cy.get(ALIAS_WRAPPER)
            .should('have.class', CLASS_ACTIVE)
            .should('have.class', CLASS_INPUT_ACTIVE)
            .btn({ action: 'secondary' })
            .as(ALIAS_SECONDARY_BUTTON)
            .should('be.visible')
            .click();

        // Enter the title in the dropdown
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
                .btnClick();

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
                .btnClick();

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
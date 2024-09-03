/* eslint-disable @typescript-eslint/no-unused-expressions */
import { PageSaveRequest, StringTagPartProperty } from '@gentics/cms-models';
import {
    EntityImporter,
    ITEM_TYPE_PAGE,
    minimalNode,
    pageOne,
    TestSize,
    trimAlohaEmpty,
} from '@gentics/e2e-utils';
import {
    ACTION_FORMAT_BOLD,
    ACTION_FORMAT_CITE,
    ACTION_FORMAT_CODE,
    ACTION_FORMAT_ITALIC,
    ACTION_FORMAT_KEYS,
    ACTION_FORMAT_MAPPING,
    ACTION_FORMAT_REMOVE,
    ACTION_FORMAT_STRIKETHROUGH,
    ACTION_FORMAT_SUBSCRIPT,
    ACTION_FORMAT_SUPERSCRIPT,
    ACTION_FORMAT_UNDERLINE,
    AUTH_ADMIN,
} from '../support/common';

describe('Page Management', () => {

    const IMPORTER = new EntityImporter();

    const TAG_CONTENT = 'content';
    const TAG_PART_CONTENT_TEXT = 'text';
    const CLASS_ACTIVE = 'active';
    const ALIAS_CONTENT = '@content';
    const ALIAS_CONTROL_BUTTON = '@ctlBtn';
    const ALIAS_SAVE = '@save';

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

    it('should be able to add and remove basic formats', () => {
        const TEXT_CONTENT = 'test content';

        for (const action of ACTION_FORMAT_KEYS) {
            // eslint-disable-next-line cypress/unsafe-to-chain-command
            cy.get(ALIAS_CONTENT)
                // Clear the content first
                .clear()
                // Replace default text with the test content
                .type(TEXT_CONTENT)
                .rangeSelection(0, null, true);

            cy.toolbarFindControl(action).btn().as(ALIAS_CONTROL_BUTTON);
            // button should not be marked as active yet
            cy.get(ALIAS_CONTROL_BUTTON)
                .should('not.have.class', CLASS_ACTIVE)
                .click();

            cy.get(ALIAS_CONTENT)
                .should('have.formatting', TEXT_CONTENT, [ACTION_FORMAT_MAPPING[action]]);

            // button should be marked as active now
            cy.get(ALIAS_CONTROL_BUTTON)
                .should('have.class', CLASS_ACTIVE);

            // Now we select the text again, and remove the formatting again
            cy.get(ALIAS_CONTENT).rangeSelection(0, null, true);

            // Should remove the formatting with this
            cy.get(ALIAS_CONTROL_BUTTON).click();

            cy.get(ALIAS_CONTENT)
                .should('have.formatting', TEXT_CONTENT, []);

            // button should be marked as inactive now
            cy.get(ALIAS_CONTROL_BUTTON)
                .should('not.have.class', CLASS_ACTIVE);
        }
    });

    it('should be possible to format a range with multiple formats', () => {
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
            // eslint-disable-next-line cypress/unsafe-to-chain-command
            cy.get(ALIAS_CONTENT)
                // Clear the content first
                .clear()
                // Replace default text with the test content
                .type(TEXT_CONTENT)
                .rangeSelection(0, null, true);

            for (const action of group) {
                cy.toolbarFindControl(action).btnClick();
            }

            cy.get(ALIAS_CONTENT)
                .should('have.formatting', TEXT_CONTENT, group.map(key => ACTION_FORMAT_MAPPING[key]));
        }
    });

    it('should remove formatting in nested formats correctly', () => {
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
            // eslint-disable-next-line cypress/unsafe-to-chain-command
            cy.get(ALIAS_CONTENT)
                .clear()
                .type(FULL_CONTENT)
                .textSelection(TEXT_CONTENT, true);

            // Make sure the buttons are properly reset
            for (const action of ACTION_FORMAT_KEYS) {
                cy.toolbarFindControl(action)
                    .btn()
                    .should('not.have.class', CLASS_ACTIVE);
            }

            for (const action of ctl.apply) {
                cy.toolbarFindControl(action).btnClick();
            }

            cy.get(ALIAS_CONTENT)
                .should('have.formatting', TEXT_CONTENT, ctl.apply.map(key => ACTION_FORMAT_MAPPING[key]));

            for (const action of ctl.remove) {
                cy.toolbarFindControl(action).btnClick();
            }

            cy.get(ALIAS_CONTENT)
                .should('have.formatting', TEXT_CONTENT, ctl.apply
                    .filter(key => !ctl.remove.includes(key))
                    .map(key => ACTION_FORMAT_MAPPING[key]));
        }
    });

    it('should be able to remove formatting the removeFormatting button', () => {
        const FULL_CONTENT = 'foo bar hello world test content this is a test text';
        const TEXT_CONTENT = 'test content';

        // const FORMATS = combinationMatrix(ACTION_FORMAT_KEYS);
        const FORMATS = [ACTION_FORMAT_KEYS];

        for (const group of FORMATS) {
            // eslint-disable-next-line cypress/unsafe-to-chain-command
            cy.get(ALIAS_CONTENT)
                .clear()
                .type(FULL_CONTENT)
                .textSelection(TEXT_CONTENT, true);

            for (const action of group) {
                cy.toolbarFindControl(action).btnClick();
            }

            cy.get(ALIAS_CONTENT).rangeSelection(0, null, true);
            cy.toolbarFindControl(ACTION_FORMAT_REMOVE).btnClick();

            cy.get(ALIAS_CONTENT)
                .should('have.formatting', FULL_CONTENT, []);

            for (const action of ACTION_FORMAT_KEYS) {
                cy.toolbarFindControl(action)
                    .btn()
                    .should('not.have.class', CLASS_ACTIVE);
            }
        }
    });
});

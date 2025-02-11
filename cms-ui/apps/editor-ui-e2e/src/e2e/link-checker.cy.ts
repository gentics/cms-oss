/* eslint-disable @typescript-eslint/no-unused-expressions */
import { NodeFeature, Variant } from '@gentics/cms-models';
import {
    EntityImporter,
    isVariant,
    ITEM_TYPE_PAGE,
    minimalNode,
    pageOne,
    skipableSuite,
    TestSize,
    scheduleLinkChecker,
} from '@gentics/e2e-utils';
import '@gentics/e2e-utils/commands';
import {
    AUTH_ADMIN,
} from '../support/common';

skipableSuite(isVariant(Variant.ENTERPRISE) ,'Link Checker', () => {

    const IMPORTER = new EntityImporter();

    const CLASS_LINKCHECKER_ITEM = 'aloha-gcnlinkchecker-item';
    const CLASS_LINKCHECKER_UNCHECKED = 'aloha-gcnlinkchecker-unchecked';
    const CLASS_LINKCHECKER_CHECKED = 'aloha-gcnlinkchecker-checked';
    const CLASS_LINKCHECKER_VALID = 'aloha-gcnlinkchecker-valid-url';
    const CLASS_LINKCHECKER_INVALID = 'aloha-gcnlinkchecker-invalid-url';

    const ALIAS_CONTENT = '@content';
    const ALIAS_CONTROLS = '@controls';
    const ALIAS_TABS = '@tabs';
    const ALIAS_MODAL = '@modal';
    const ALIAS_FORM = '@form';
    const ALIAS_CHECK_REQ = '@checkReq';

    before(() => {
        cy.muteXHR();
        cy.wrap(IMPORTER.clearClient(), { log: false })
            .then(() => {
                return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
            })
            .then(() => {
                return cy.wrap(IMPORTER.bootstrapSuite(TestSize.MINIMAL), { log: false, timeout: 60_000 });
            });
    });

    beforeEach(() => {
        cy.muteXHR();
        cy.wrap(null, { log: false })
            .then(() => {
                return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 })
            })
            .then(() => {
                return cy.wrap(IMPORTER.setupTest(TestSize.MINIMAL), { log: false, timeout: 60_000 });
            })
            .then(() => cy.wrap(IMPORTER.setupFeatures(TestSize.MINIMAL, {
                [NodeFeature.LINK_CHECKER]: true,
            } as any), { log: false, timeout: 60_000 }))
            .then(() => cy.wrap(IMPORTER.importData([
                scheduleLinkChecker,
            ])))
            .then(() => {
                return cy.wrap(IMPORTER.executeSchedule(scheduleLinkChecker), { log: false, timeout: 60_000 });
            })
            .then(() => {
                cy.navigateToApp();
                cy.login(AUTH_ADMIN);
                cy.selectNode(IMPORTER.get(minimalNode)!.id);
            });
    });

    function setupEditMode() {
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

        cy.get('project-editor content-frame gtx-editor-toolbar gtx-page-editor-tabs')
            .as(ALIAS_TABS);
    }

    describe('Live Inline Links', () => {
        beforeEach(() => {
            setupEditMode();
        });

        it('should detect a valid link on insert', () => {
            const TEXT_CONTENT = 'Hello ';
            const LINK_URL = 'https://gentics.com';
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
                .find('> [data-slot="title"] input')
                .clear()
                .type(LINK_TITLE);

            /*
             * Intercept check requests to wait for them
             */

            cy.intercept({
                pathname: '/rest/linkChecker/check',
            }, intercept => {
                intercept.alias = ALIAS_CHECK_REQ;
            });

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
                .should('have.attr', 'href', LINK_URL)
                .should('have.attr', 'title', LINK_TITLE)
                .should('have.class', CLASS_LINKCHECKER_ITEM)
                .should('have.class', CLASS_LINKCHECKER_UNCHECKED);

            cy.wait(ALIAS_CHECK_REQ);

            cy.get(ALIAS_CONTENT)
                .find('a')
                .should('have.attr', 'href', LINK_URL)
                .should('have.attr', 'title', LINK_TITLE)
                .should('have.class', CLASS_LINKCHECKER_ITEM)
                .should('have.class', CLASS_LINKCHECKER_CHECKED)
                .should('have.class', CLASS_LINKCHECKER_VALID);

            cy.get(ALIAS_TABS)
                .find('.editor-tab[data-id="gtx.link-checker"]')
                .click();

            cy.get(ALIAS_CONTROLS)
                .find('gtx-link-checker-controls .links-container gtx-dropdown-list')
                .should('not.exist');
        });

        it('should detect a invalid link on insert', () => {
            const TEXT_CONTENT = 'Hello ';
            const LINK_DOMAIN = 'somedomainwhichwillsurelynotbetaken.com';
            const LINK_URL = `https://${LINK_DOMAIN}`;
            const LINK_TITLE = 'This is a title!';
            const ALIAS_LINK_BUTTON = '@linkBtn';

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
                .find('> [data-slot="title"] input')
                .clear()
                .type(LINK_TITLE);

            /*
             * Intercept check requests to wait for them
             */

            cy.intercept({
                pathname: '/rest/linkChecker/check',
            }, intercept => {
                intercept.alias = ALIAS_CHECK_REQ;
            });

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
                .should('have.attr', 'href', LINK_URL)
                .should('have.attr', 'title', LINK_TITLE)
                .should('have.class', CLASS_LINKCHECKER_ITEM)
                .should('have.class', CLASS_LINKCHECKER_UNCHECKED);

            cy.wait(ALIAS_CHECK_REQ);

            cy.get(ALIAS_CONTENT)
                .find('a')
                .should('have.attr', 'href', LINK_URL)
                .should('have.attr', 'title', LINK_TITLE)
                .should('have.class', CLASS_LINKCHECKER_ITEM)
                .should('have.class', CLASS_LINKCHECKER_CHECKED)
                .should('have.class', CLASS_LINKCHECKER_INVALID);

            cy.get(ALIAS_TABS)
                .find('.editor-tab[data-id="gtx.link-checker"]')
                .click();

            cy.get(ALIAS_CONTROLS)
                .find('gtx-link-checker-controls .links-container gtx-dropdown-list')
                .should('exist')
                .as(ALIAS_LINK_BUTTON)
                .find('.link-label')
                .should('have.text', LINK_DOMAIN);

            /*
             * Remove link via controls
             */

            cy.get(ALIAS_LINK_BUTTON)
                .find('.link-opener')
                .click();

            cy.get('gtx-dropdown-content-wrapper gtx-dropdown-content .link-options .link-delete-button')
                .click();

            cy.get(ALIAS_CONTENT)
                .find('a')
                .should('not.exist');
        });
    });

    describe('Live Constructs', () => {
        const CLASS_BLOCK_HANDLE = 'aloha-block-handle';
        const CLASS_BLOCK_INDICATOR = 'aloha-gcnlinkchecker-block-indicator';

        describe('With configured Constructs', () => {
            beforeEach(() => {
                // Setup the custom aloha config for this test
                cy.intercept({
                    path: '/internal/minimal/files/js/aloha-config.js',
                }, {
                    fixture: 'aloha/link-checker-config.js',
                });

                setupEditMode();
            });

            it('should display the construct link status', () => {
                cy.getAlohaIFrame()
                    .find(`header .${CLASS_BLOCK_HANDLE} .${CLASS_BLOCK_INDICATOR}`)
                    .should('exist');
            });
        });

        describe('Without configured Constructs', () => {
            beforeEach(() => {
                setupEditMode();
            });

            it('should display the construct link status', () => {
                cy.getAlohaIFrame()
                    .find(`header .${CLASS_BLOCK_HANDLE} .${CLASS_BLOCK_INDICATOR}`)
                    .should('not.exist');
            });
        });
    });
});

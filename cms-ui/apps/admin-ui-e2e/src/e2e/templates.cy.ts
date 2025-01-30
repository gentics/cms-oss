import '@gentics/e2e-utils/commands';
import { AUTH_ADMIN } from '../support/app.po';
import { EntityImporter, TestSize, minimalNode } from '@gentics/e2e-utils';
import { AccessControlledType } from '@gentics/cms-models';

describe('Templates Module', () => {
    const IMPORTER = new EntityImporter();

    const ALIAS_MODULE = '@module';
    const ALIAS_TEMPLATE_TABLE = '@templateTable';
    const NODE_NAME = "empty node";
    const TEMPLATE_NAME = "[Test] Basic Template";
    const LINK_TO_NODE_ACTION = 'linkToNode';
    const LINK_TO_NODE_MODAL = 'gtx-assign-templates-to-nodes-modal';
    const LINK_TO_FOLDER_ACTION = 'linkToFolder';
    const LINK_TO_FOLDER_MODAL = 'gtx-assign-templates-to-folders-modal';

    before(() => {
        cy.muteXHR();
        cy.wrap(IMPORTER.bootstrapSuite(TestSize.MINIMAL));
    });

    beforeEach(() => {
        cy.muteXHR();
        // If this client isn't recreated for WHATEVER reason, the CMS gives back a 401 for importer requests.
        IMPORTER.client = null;

        cy.wrap(null, { log: false })
            .then(() => {
                return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
            })
            .then(() => {
                return cy.wrap(IMPORTER.syncPackages(TestSize.MINIMAL), { log: false, timeout: 60_000 });
            });

        cy.navigateToApp();
        cy.login(AUTH_ADMIN);

        // Table loading
        const ALIAS_NODE_TABLE_LOAD_REQ = '@nodeTableLoadReq';
        const ALIAS_TEMPLATE_TABLE_LOAD_REQ = '@templateTableLoadReq';

        cy.intercept({
            method: 'GET',
            pathname: '/rest/node',
        }, req => {
            req.alias = ALIAS_NODE_TABLE_LOAD_REQ;
        });

        cy.intercept({
            method: 'GET',
            pathname: '/rest/template',
        }, req => {
            req.alias = ALIAS_TEMPLATE_TABLE_LOAD_REQ;
        });

        cy.navigateToModule('templates')
            .as(ALIAS_MODULE);

        cy.wait(ALIAS_NODE_TABLE_LOAD_REQ).then(() => {
            cy.get(ALIAS_MODULE)
                .find("gtx-table")
                .findTableRowContainingText(NODE_NAME)
                .find(`.data-column[data-id="name"]`)
                .click();

            cy.wait(ALIAS_TEMPLATE_TABLE_LOAD_REQ).then(() => {
                cy.get(ALIAS_MODULE)
                    .find("gtx-table")
                    .as(ALIAS_TEMPLATE_TABLE);
            });
        });
    });

    it('should open node assignment modal for single template', () => {
        cy.get(ALIAS_TEMPLATE_TABLE)
            .findTableRowContainingText(TEMPLATE_NAME)
            .findTableAction(LINK_TO_NODE_ACTION)
            .click();

        // we just expect the modal to be opened
        cy.get(LINK_TO_NODE_MODAL);
    });

    it('should open node assignment modal for template selection', () => {
        cy.get(ALIAS_TEMPLATE_TABLE)
            .findTableRowContainingText(TEMPLATE_NAME)
            .selectTableRow();
        cy.get(ALIAS_TEMPLATE_TABLE)
            .findTableAction(LINK_TO_NODE_ACTION)
            .click();

        // we just expect the modal to be opened
        cy.get(LINK_TO_NODE_MODAL);
    });

    it('should open folder assignment modal for single template', () => {
        cy.get(ALIAS_TEMPLATE_TABLE)
            .findTableRowContainingText(TEMPLATE_NAME)
            .findTableAction(LINK_TO_FOLDER_ACTION)
            .click();

        // we just expect the modal to be opened
        cy.get(LINK_TO_FOLDER_MODAL);
    });

    it('should open folder assignment modal for template selection', () => {
        cy.get(ALIAS_TEMPLATE_TABLE)
            .findTableRowContainingText(TEMPLATE_NAME)
            .selectTableRow();
        cy.get(ALIAS_TEMPLATE_TABLE)
            .findTableAction(LINK_TO_FOLDER_ACTION)
            .click();

        // we just expect the modal to be opened
        cy.get(LINK_TO_FOLDER_MODAL);
    });
});

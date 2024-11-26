/* eslint-disable @typescript-eslint/no-unused-expressions */
import {
    FormCreateResponse,
    NodeFeature,
    Variant,
} from '@gentics/cms-models';
import {
    EntityImporter,
    ITEM_TYPE_FORM,
    LANGUAGE_DE,
    TestSize,
    isVariant,
    minimalNode,
    skipableSuite,
} from '@gentics/e2e-utils';
import { AUTH_ADMIN } from '../support/common';

skipableSuite(isVariant(Variant.ENTERPRISE), 'Form Management', () => {

    const IMPORTER = new EntityImporter();

    const ALIAS_FORM = '@form';
    const ALIAS_MODAL = '@modal';
    const ALIAS_LANG_LOAD_REQ = '@langLoadRequest';
    const ALIAS_CREATE_REQ = '@createRequest';
    const ALIAS_UPDATE_REQ = '@updateRequest';

    before(() => {
        cy.muteXHR();

        cy.wrap(null, { log: false }).then(() => {
            return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
        }).then(() => {
            return cy.wrap(IMPORTER.bootstrapSuite(TestSize.MINIMAL), { log: false, timeout: 60_000 });
        });
    });

    beforeEach(() => {
        cy.muteXHR();

        cy.wrap(null, { log: false }).then(() => {
            return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
        }).then(() => {
            return cy.wrap(IMPORTER.setupTest(TestSize.MINIMAL), { log: false, timeout: 60_000 });
        }).then(() => {
            return cy.wrap(IMPORTER.setupFeatures(TestSize.MINIMAL, {
                [NodeFeature.FORMS]: true,
            }), { log: false, timeout: 60_000 });
        }).then(() => {
            const NODE_ID = IMPORTER.get(minimalNode)!.id;
            // Languages have to be loaded, otherwise the create-modal might not have the
            // languages available and not show any (which would break the test).
            cy.intercept({
                method: 'GET',
                pathname: `/rest/node/${NODE_ID}/languages`,
            }, req => {
                req.alias = ALIAS_LANG_LOAD_REQ;
            });
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.selectNode(NODE_ID);
            cy.wait(ALIAS_LANG_LOAD_REQ);
        });
    });

    it('should be possible to create a new form', () => {
        const NEW_FORM_NAME = 'Hello World';
        const NEW_FORM_DESCRIPTION = 'This is an example text';

        /* Create the Folder
         * ---------------------------- */
        cy.findList(ITEM_TYPE_FORM)
            .find('.header-controls [data-action="create-new-item"]')
            .click({ force: true });
        cy.get('create-form-modal').as(ALIAS_MODAL);
        cy.get(ALIAS_MODAL).find('gtx-form-properties').as(ALIAS_FORM);

        cy.get(ALIAS_FORM)
            .find('[formcontrolname="name"] input')
            .type(NEW_FORM_NAME);

        cy.get(ALIAS_FORM)
            .find('[formcontrolname="description"] input')
            .type(NEW_FORM_DESCRIPTION);

        cy.get(ALIAS_FORM)
            .find('[formcontrolname="languages"]')
            .select(LANGUAGE_DE);

        cy.intercept({
            method: 'POST',
            pathname: '/rest/form',
        }, req => {
            req.alias = ALIAS_CREATE_REQ;
        });

        cy.get(ALIAS_MODAL)
            .find('.modal-footer [data-action="confirm"]')
            .click({ force: true });

        // Wait for the folder to have reloaded
        cy.wait<any, FormCreateResponse>(ALIAS_CREATE_REQ)
            .then(data => {
                const form = data.response?.body?.item;
                expect(form).to.exist;
                cy.findList(ITEM_TYPE_FORM)
                    .findItem(form!.id)
                    .should('exist');
            });
    });
});

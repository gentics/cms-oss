import { AccessControlledType, ConstructNodeLinkRequest, NodePageLanguageCode, NodeUrlMode } from '@gentics/cms-models';
import {
    EntityImporter,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_NODE,
    LANGUAGE_DE,
    LANGUAGE_EN,
    NodeImportData,
    TestSize,
} from '@gentics/e2e-utils';
import '@gentics/e2e-utils/commands';
import { AUTH_ADMIN } from '../support/app.po';

const EXAMPLE_NODE_ONE: NodeImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_NODE,
    [IMPORT_ID]: 'constructExampleNodeOne',

    node: {
        name: 'Construct Example Node #1',
        host: 'construct01.localhost',
        hostProperty: '',
        publishDir : '',
        binaryPublishDir : '',
        pubDirSegment : true,
        https : false,
        publishImageVariants : false,
        publishFs : false,
        publishFsPages : false,
        publishFsFiles : false,
        publishContentMap : false,
        publishContentMapPages : false,
        publishContentMapFiles : false,
        publishContentMapFolders : false,
        urlRenderWayPages: NodeUrlMode.AUTOMATIC,
        urlRenderWayFiles: NodeUrlMode.AUTOMATIC,
        omitPageExtension : false,
        pageLanguageCode : NodePageLanguageCode.FILENAME,
        meshPreviewUrlProperty : '',
    },
    description: 'Test Node',

    languages: [LANGUAGE_DE],
    templates: [],
};

const EXAMPLE_NODE_TWO: NodeImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_NODE,
    [IMPORT_ID]: 'constructExampleNodeTwo',

    node: {
        name: 'Construct Example Node #2',
        host: 'construct02.localhost',
        hostProperty: '',
        publishDir : '',
        binaryPublishDir : '',
        pubDirSegment : true,
        https : false,
        publishImageVariants : false,
        publishFs : false,
        publishFsPages : false,
        publishFsFiles : false,
        publishContentMap : false,
        publishContentMapPages : false,
        publishContentMapFiles : false,
        publishContentMapFolders : false,
        urlRenderWayPages: NodeUrlMode.AUTOMATIC,
        urlRenderWayFiles: NodeUrlMode.AUTOMATIC,
        omitPageExtension : false,
        pageLanguageCode : NodePageLanguageCode.FILENAME,
        meshPreviewUrlProperty : '',
    },
    description: 'Test Node',

    languages: [LANGUAGE_EN],
    templates: [],
};

describe('Constructs Module', () => {

    const IMPORTER = new EntityImporter();

    const TEST_CONSTRUCT_ID = '13';

    const ALIAS_MODULE = '@module';
    const ALIAS_TAB_CONTENT = '@tabContent';
    const ALIAS_CONSTRUCT_TABLE_LOAD_REQ = '@constructTableLoadReq';

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
            })
            .then(() => {
                return cy.wrap(IMPORTER.importData([
                    EXAMPLE_NODE_ONE,
                    EXAMPLE_NODE_TWO,
                ], TestSize.NONE), { log: false, timeout: 60_000 });
            });

        cy.navigateToApp();
        cy.login(AUTH_ADMIN);

        cy.intercept({
            method: 'GET',
            pathname: '/rest/construct',
        }, req => {
            req.alias = ALIAS_CONSTRUCT_TABLE_LOAD_REQ;
        });

        cy.navigateToModule('constructs', AccessControlledType.CONSTRUCT_ADMIN)
            .as(ALIAS_MODULE);
    });

    describe('Constructs', () => {
        const ALIAS_TABLE = '@table';

        beforeEach(() => {
            // Wait for the table to finish loading
            cy.wait(ALIAS_CONSTRUCT_TABLE_LOAD_REQ);

            // eslint-disable-next-line cypress/unsafe-to-chain-command
            cy.get(ALIAS_MODULE)
                .find('> gtx-tabs')
                .selectTab('constructs')
                .as(ALIAS_TAB_CONTENT)
                .find('gtx-table')
                .as(ALIAS_TABLE);
        });

        it('should properly remove and assign the constructs to the node', () => {
            cy.get(ALIAS_TABLE)
                .findTableRow(TEST_CONSTRUCT_ID)
                .findTableAction('assignConstructToNodes')
                .click();

            const ALIAS_MODAL = '@modal';
            const ALIAS_NODE_TABLE = '@nodeTable';
            const ALIAS_LINK_REQ = '@linkReq';

            let nodeId: number | undefined;

            cy.get('gtx-assign-constructs-to-nodes-modal')
                .as(ALIAS_MODAL)
                .find('.modal-content gtx-table')
                .as(ALIAS_NODE_TABLE)
                // Select the first node
                .find('.data-row:not(.selected) .select-column gtx-checkbox label')
                .first()
                .then($elem => {
                    nodeId = parseInt($elem.parents('.data-row').attr('data-id') || '', 10);
                    return $elem;
                })
                .click();

            cy.intercept({
                method: 'POST',
                pathname: '/rest/construct/unlink/nodes',
            }, () => {
                expect(false).to.equal(true, 'Invalid Request to "/rest/construct/unlink/nodes" has been sent!');
            });

            cy.intercept({
                method: 'POST',
                pathname: '/rest/construct/link/nodes',
            }, req => {
                req.alias = ALIAS_LINK_REQ;
            });

            cy.get(ALIAS_MODAL)
                .find('.modal-footer gtx-button[data-action="confirm"]')
                .click();

            cy.wait<ConstructNodeLinkRequest>(ALIAS_LINK_REQ).then(inter => {
                expect(inter.request.body.ids).to.deep.equal([nodeId]);
            });
        });
    });
});

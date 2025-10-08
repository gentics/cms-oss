import { Node, Template } from '@gentics/cms-models';
import {
    BASIC_TEMPLATE_ID,
    clickTableRow,
    emptyNode,
    EntityImporter,
    findTableAction,
    findTableRowById,
    findTrableRowById,
    loginWithForm,
    matchRequest,
    minimalNode,
    navigateToApp,
    selectTableRow,
    selectTrableRow,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, Locator, test } from '@playwright/test';
import { AUTH } from './common';
import { clickModalAction, navigateToModule } from './helpers';

const LINK_TO_NODE_ACTION = 'linkToNode';
const LINK_TO_NODE_MODAL = 'gtx-assign-templates-to-nodes-modal';
const LINK_TO_FOLDER_ACTION = 'linkToFolder';
const LINK_TO_FOLDER_MODAL = 'gtx-assign-templates-to-folders-modal';

test.describe.configure({ mode: 'serial' });
test.describe('Templates Module', () => {
    const IMPORTER = new EntityImporter();

    let testTemplate: Template;
    let testNode: Node;

    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach(async ({ page, request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();

        // Clean and setup test data
        await IMPORTER.cleanupTest();
        await IMPORTER.syncPackages(TestSize.MINIMAL);
        await IMPORTER.setupTest(TestSize.MINIMAL);

        testTemplate = IMPORTER.get(BASIC_TEMPLATE_ID as any) as any;
        testNode = IMPORTER.get(minimalNode);

        // Navigate to the app and log in
        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);

        // Navigate to the scheduler
        await navigateToModule(page, 'templates');

        // select our "empty node", as unassign from the test node, and it
        // wouldn't show up anymore and just cause issues.
        const nodeTable = page.locator('gtx-node-table');
        await clickTableRow(findTableRowById(nodeTable, IMPORTER.dummyNode));
    });

    test('should open node assignment modal for single template', async ({ page }) => {
        const tplRow = findTableRowById(page, testTemplate.id);

        const modal = page.locator(LINK_TO_NODE_MODAL);
        const nodeTable = modal.locator('gtx-node-table');
        const nodeRow = findTableRowById(nodeTable, testNode.id);

        await test.step('Unassign from node', async () => {
            const nodeLoad = page.waitForResponse(matchRequest('GET', '/rest/node'));
            const tplNodesLoad = page.waitForResponse(matchRequest('GET', `/rest/template/${testTemplate.id}/nodes`));

            await findTableAction(tplRow, LINK_TO_NODE_ACTION).click();
            await Promise.all([nodeLoad, tplNodesLoad]);

            await expect(nodeRow).toContainClass('selected');
            await selectTableRow(nodeRow);

            const unassignReq = page.waitForResponse(matchRequest('DELETE', `/rest/node/${testNode.id}/templates/${testTemplate.id}`));
            await clickModalAction(modal, 'confirm');
            await unassignReq;
        });

        await test.step('Assign to node', async () => {
            const nodeLoad = page.waitForResponse(matchRequest('GET', '/rest/node'));
            const tplNodesLoad = page.waitForResponse(matchRequest('GET', `/rest/template/${testTemplate.id}/nodes`));

            await findTableAction(tplRow, LINK_TO_NODE_ACTION).click();
            await Promise.all([nodeLoad, tplNodesLoad]);

            await expect(nodeRow).not.toContainClass('selected');
            await selectTableRow(nodeRow);

            const unassignReq = page.waitForResponse(matchRequest('PUT', `/rest/node/${testNode.id}/templates/${testTemplate.id}`));
            await clickModalAction(modal, 'confirm');
            await unassignReq;
        });
    });

    // TODO: Create node assignment variant with multiple templates

    test('should handle simple folder assignments correctly', async ({ page }) => {
        let folderRow: Locator;

        await test.step('Open Folder assign modal', async () => {
            const tplRow = findTableRowById(page, testTemplate.id);
            const folderLoad = page.waitForResponse(matchRequest('GET', `/rest/template/${testTemplate.id}/folders`));

            await findTableAction(tplRow, LINK_TO_FOLDER_ACTION).click();
            const modal = page.locator(LINK_TO_FOLDER_MODAL);

            const trable = modal.locator('gtx-folder-trable');
            folderRow = findTrableRowById(trable, testNode.folderId);

            await folderLoad;
            await expect(folderRow).toContainClass('selected');
        });

        await test.step('Unassign test node', async () => {
            const unassignReq = page.waitForResponse(matchRequest('POST', '/rest/template/unlink'));
            await selectTrableRow(folderRow);
            await unassignReq;
        });

        await test.step('Assign test node', async () => {
            const assignReq = page.waitForResponse(matchRequest('POST', '/rest/template/link'));
            await selectTrableRow(folderRow);
            await assignReq;
        });
    });

    // TODO: Create a folder assignment variant with multiple templates
});

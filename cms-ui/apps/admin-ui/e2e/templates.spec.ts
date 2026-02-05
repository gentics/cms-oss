import { Node, Template, TemplateResponse, TemplateSaveRequest } from '@gentics/cms-models';
import {
    BASIC_TEMPLATE_ID,
    clickModalAction,
    clickTableRow,
    EntityImporter,
    findTableAction,
    findTableRowById,
    findTrableRowById,
    loginWithForm,
    matchRequest,
    navigateToApp,
    NODE_MINIMAL,
    pickSelectValue,
    selectTableRow,
    selectTrableRow,
    TestSize,
    waitForResponseFrom,
} from '@gentics/e2e-utils';
import { expect, Locator, Response, test } from '@playwright/test';
import { AUTH } from './common';
import { findEntityTableActionButton, navigateToModule } from './helpers';

const LINK_TO_NODE_ACTION = 'linkToNode';
const LINK_TO_NODE_MODAL = 'gtx-assign-templates-to-nodes-modal';
const LINK_TO_FOLDER_ACTION = 'linkToFolder';
const LINK_TO_FOLDER_MODAL = 'gtx-assign-templates-to-folders-modal';

test.describe('Templates Module', () => {
    const IMPORTER = new EntityImporter();

    let templateModule: Locator;
    let templateTable: Locator;
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

        testTemplate = IMPORTER.get(BASIC_TEMPLATE_ID as any);
        testNode = IMPORTER.get(NODE_MINIMAL);

        // Navigate to the app and log in
        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);

        // Navigate to the scheduler
        templateModule = await navigateToModule(page, 'templates');

        // select our "empty node", as unassign from the test node, and it
        // wouldn't show up anymore and just cause issues.
        const nodeTable = templateModule.locator('gtx-node-table');
        await clickTableRow(await findTableRowById(nodeTable, IMPORTER.dummyNode));
        templateTable = templateModule.locator('gtx-template-table');
    });

    test('should be able to create a template', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19607',
        }],
    }, async ({ page }) => {
        const TEMPLATE_NAME = 'Test Template';
        const TEMPLATE_MARKUP = '1';
        let templateId: number;
        let listReq: Promise<Response>;

        await test.step('Create Template', async () => {
            await findEntityTableActionButton(templateTable, 'create').click();
            const modal = page.locator('gtx-create-template-modal');
            const form = modal.locator('gtx-template-properties form');

            await form.locator('[formControlName="name"] input').fill(TEMPLATE_NAME);
            await pickSelectValue(form.locator('[formControlName="markupLanguage"]'), TEMPLATE_MARKUP);

            const saveReq = waitForResponseFrom(page, 'POST', '/rest/template');
            listReq = waitForResponseFrom(page, 'GET', '/rest/template');
            await clickModalAction(modal, 'confirm');
            const saveRes = await saveReq;
            templateId = (await saveRes.json() as TemplateResponse).template.id;
        });

        await test.step('Visible in Table', async () => {
            await listReq;
            await expect(await findTableRowById(templateTable, templateId)).toBeVisible();
        });
    });

    test('should open node assignment modal for single template', async ({ page }) => {
        const tplRow = await findTableRowById(page, testTemplate.id);

        let modal: Locator;
        let nodeTable: Locator;
        let nodeRow: Locator;

        await test.step('Unassign from node', async () => {
            const nodeLoad = page.waitForResponse(matchRequest('GET', '/rest/node'));
            const tplNodesLoad = page.waitForResponse(matchRequest('GET', `/rest/template/${testTemplate.id}/nodes`));

            await findTableAction(tplRow, LINK_TO_NODE_ACTION).click();
            await Promise.all([nodeLoad, tplNodesLoad]);

            modal = page.locator(LINK_TO_NODE_MODAL);
            nodeTable = modal.locator('gtx-node-table');
            nodeRow = await findTableRowById(nodeTable, testNode.id);

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
            const tplRow = await findTableRowById(page, testTemplate.id);
            const folderLoad = page.waitForResponse(matchRequest('GET', `/rest/template/${testTemplate.id}/folders`));

            await findTableAction(tplRow, LINK_TO_FOLDER_ACTION).click();
            const modal = page.locator(LINK_TO_FOLDER_MODAL);

            const trable = modal.locator('gtx-folder-trable');
            folderRow = findTrableRowById(trable, testNode.folderId);

            await folderLoad;
            await expect(folderRow).toContainClass('selected');
        });

        await test.step('Unassign test node', async () => {
            const unassignReq = page.waitForResponse(matchRequest('POST', `/rest/template/unlink/${testTemplate.id}`));
            await selectTrableRow(folderRow);
            await unassignReq;
        });

        await test.step('Assign test node', async () => {
            const assignReq = page.waitForResponse(matchRequest('POST', `/rest/template/link/${testTemplate.id}`));
            await selectTrableRow(folderRow);
            await assignReq;
        });
    });

    // TODO: Create a folder assignment variant with multiple templates

    test('should be able to edit properties', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19534',
        }],
    }, async ({ page }) => {
        const NEW_DESCRIPTION = 'Hello World foobar';

        await test.step('Open properties window', async () => {
            const tplRow = await findTableRowById(page, testTemplate.id);
            await tplRow.locator('.data-column[data-id="name"]').click();
        });

        const detail = page.locator('gtx-template-detail');
        const manager = detail.locator('gtx-properties-manager');

        await test.step('Update description', async () => {
            const form = manager.locator('.editor-wrapper form');
            await form.locator('[formControlName="description"] input').fill(NEW_DESCRIPTION);

            await page.waitForTimeout(500); // Wait for form data to propagate
            const saveReq = waitForResponseFrom(page, 'POST', `/rest/template/${testTemplate.id}`);
            await detail.locator('gtx-entity-detail-header [data-action="save"] button').click();
            const saveRes = await saveReq;
            const saveBody = saveRes.request().postDataJSON() as TemplateSaveRequest;

            expect(saveBody.template.description).toEqual(NEW_DESCRIPTION);
        });
    });
});

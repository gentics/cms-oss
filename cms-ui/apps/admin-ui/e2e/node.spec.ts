import {
    clickTableRow,
    EntityImporter,
    findTableRowById,
    loginWithForm,
    navigateToApp,
    NODE_MINIMAL,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import { navigateToModule } from './helpers';

const SELECTORS = {
    MASTER: 'gtx-node-master',
    EDITOR: 'gtx-node-editor',
    MESH_PROJECT_NAME_INPUT: 'gtx-node-properties gtx-input[formcontrolname="meshProjectName"] input',
} as const;

test.describe('Node Settings', () => {

    const IMPORTER = new EntityImporter();

    test.beforeAll(async ({ request }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
        });

        await test.step('Test Bootstrapping', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
        });
    });

    test.beforeEach(async ({ page, request, context }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await context.clearCookies();
            await IMPORTER.clearClient();
        });

        await test.step('Common Test Setup', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.syncPackages(TestSize.MINIMAL);
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
    });

    test.describe('Properties Tab', () => {
        // SUP-19754: meshProjectName field was always empty in the edit view,
        // even when set via REST and returned by the API.
        test('should show the saved value in meshProjectName when opening node settings', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-19754',
            }],
        }, async ({ page }) => {
            const MESH_PROJECT_NAME = 'sup-19754-test-project';
            const node = IMPORTER.get(NODE_MINIMAL);

            await test.step('Set meshProjectName via REST', async () => {
                await IMPORTER.client.node.update(node.id, {
                    node: {
                        meshProjectName: MESH_PROJECT_NAME,
                    },
                }).send();
            });

            await test.step('Navigate to Nodes module', async () => {
                const master = await navigateToModule(page, 'nodes');
                const masterTable = master.locator(SELECTORS.MASTER).or(master);
                await masterTable.waitFor({ state: 'visible' });

                const row = findTableRowById(masterTable, node.id);
                await row.waitFor({ state: 'visible' });
                await clickTableRow(row);

                const editor = page.locator(SELECTORS.EDITOR);
                await editor.waitFor({ state: 'visible' });
            });

            await test.step('meshProjectName field should not be empty', async () => {
                const input = page.locator(SELECTORS.MESH_PROJECT_NAME_INPUT);
                await expect(input).toBeVisible();
                await expect(input).toHaveValue(MESH_PROJECT_NAME);
            });
        });
    });
});

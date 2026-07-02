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
    MASTER: 'gtx-group-master',
    EDITOR: 'gtx-group-detail',
    TAB_GROUP_USERS: 'li.tab-link[data-id="groupUsers"]',
    TAB_GROUP_SUBS: 'li.tab-link[data-id="subgroups"]',
} as const;
const NODE_SUPER_GROUP_ID = 2;
const NODE_SUB_SUPER_GROUP_NAME = 'Node Sub Super Group';

test.describe('Group Settings', () => {

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

    test.describe('Subgroup', () => {
        test('should set the new subgroup of "Node Super Admin" up and make sure it is assignable, and the supergroup is not', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-19628',
            }],
        }, async ({ page }) => {
            await test.step('Navigate to the default Node Super Group editor', async () => {
                const master = await navigateToModule(page, 'groups');
                const masterTable = master.locator(SELECTORS.MASTER).or(master);
                await masterTable.waitFor({ state: 'visible' });

                const row = findTableRowById(masterTable, NODE_SUPER_GROUP_ID);
                await row.waitFor({ state: 'visible' });
                await clickTableRow(row);

                const editor = page.locator(SELECTORS.EDITOR);
                await editor.waitFor({ state: 'visible' });
            });

            await test.step('Node Super Admin group should not allow manipulating users', async () => {
                const tab = page.locator(SELECTORS.TAB_GROUP_USERS);
                await tab.click();
                const actionButtons = page.locator('gtx-group-detail gtx-tab[data-id="groupUsers"] .entity-table-actions-bar');
                await actionButtons.waitFor();
                expect(actionButtons.locator('.table-action-button button.btn').filter({ hasText: 'Neuen Benutzer in Gruppe erstellen' })).not.toBeVisible();
                expect(actionButtons.locator('.table-action-button button.btn').filter({ hasText: 'Benutzer zuweisen' })).not.toBeVisible();
            });

            await test.step('Node Super Admin group should allow creating a subgroup', async () => {
                const tab = page.locator(SELECTORS.TAB_GROUP_SUBS);
                await tab.click();
                const actionButtons = page.locator('gtx-group-detail gtx-tab[data-id="subgroups"] .entity-table-actions-bar');
                await actionButtons.waitFor();
                expect(actionButtons.locator('.table-action-button button.btn').filter({ hasText: 'Neue Untergruppe erstellen' })).toBeVisible();
                await actionButtons.locator('.table-action-button button.btn').filter({ hasText: 'Neue Untergruppe erstellen' }).click();
                const createGroupModal = page.locator('gtx-create-group-modal');
                await createGroupModal.waitFor();
                await createGroupModal.locator('gtx-input[formcontrolname="name"] input').fill(NODE_SUB_SUPER_GROUP_NAME);
                await createGroupModal.locator('gtx-button[gtxactionallowed="group.createGroup"]').click();
                await expect(page.locator('gtx-group-detail gtx-group-table .grid-row.data-row .grid-cell.data-column[data-id="name"] .cell-content-wrapper')
                    .filter({ hasText: NODE_SUB_SUPER_GROUP_NAME })).toBeVisible();
            });

            await test.step('Node Sub Super Admin group should allow manipulating users', async () => {
                await page.locator('gtx-group-detail gtx-tab[data-id="subgroups"]')
                    .locator('.gtx-entity-details-tab-content-header-buttons gtx-button[data-action="cancel"]').click();

                const masterTable = page.locator(SELECTORS.MASTER);
                await masterTable.waitFor({ state: 'visible' });
                const row = masterTable.locator('gtx-table .grid-row.data-row .grid-cell.data-column[data-id="name"] .cell-content-wrapper')
                    .filter({ hasText: NODE_SUB_SUPER_GROUP_NAME });
                await row.waitFor({ state: 'visible' });
                await row.click();

                const editor = page.locator(SELECTORS.EDITOR);
                await editor.waitFor({ state: 'visible' });

                const tab = page.locator(SELECTORS.TAB_GROUP_USERS);
                await tab.click();
                const actionButtons = page.locator('gtx-group-detail gtx-tab[data-id="groupUsers"] .entity-table-actions-bar');
                await actionButtons.waitFor();
                expect(actionButtons.locator('.table-action-button button.btn').filter({ hasText: 'Neuen Benutzer in Gruppe erstellen' })).toBeVisible();
                expect(actionButtons.locator('.table-action-button button.btn').filter({ hasText: 'Benutzer zuweisen' })).toBeVisible();
            });
        });
    });
});

import { GroupResponse } from '@gentics/cms-models';
import {
    clickModalAction,
    clickTableRow,
    EntityImporter,
    findTableRowById,
    loginWithForm,
    navigateToApp,
    selectTab,
    TestSize,
    waitForResponseFrom,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import { navigateToModule } from './helpers';

const NODE_SUPER_GROUP_ID = 2;
const NODE_SUB_SUPER_GROUP_NAME = 'Node Sub Super Group';

test.describe('Group Module', () => {

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
            const master = await navigateToModule(page, 'groups');
            const masterTable = master.locator('gtx-group-table');
            const editor = page.locator('gtx-group-detail');
            let subGroupId: number = null;

            await test.step('Navigate to the default Node Super Group editor', async () => {
                const row = await findTableRowById(masterTable, NODE_SUPER_GROUP_ID);
                await clickTableRow(row);

                await expect(editor).toBeVisible();
            });

            const tabs = editor.locator('.gtx-entity-detail > gtx-tabs');

            await test.step('Node Super Admin group should not allow manipulating users', async () => {
                const usersTab = await selectTab(tabs, 'groupUsers');
                const usersTable = usersTab.locator('gtx-user-table');

                await expect(usersTable.locator('.entity-table-actions-bar [data-action="create"]')).toBeHidden();
                await expect(usersTable.locator('.entity-table-actions-bar [data-action="assign-to-groups"]')).toBeHidden();
            });

            await test.step('Node Super Admin group should allow creating a subgroup', async () => {
                const subGroupsTab = await selectTab(tabs, 'subgroups');
                const groupsTable = subGroupsTab.locator('gtx-group-table');

                const createSubGroupButton = groupsTable.locator('.entity-table-actions-bar [data-action="create-subgroup"]');
                await createSubGroupButton.waitFor();
                await expect(createSubGroupButton).toBeVisible();
                await createSubGroupButton.click();

                // Create the sub-group, wait for the response, and save the ID of the new group for later
                const createGroupModal = page.locator('gtx-create-group-modal');
                await createGroupModal.locator('.modal-content gtx-input[formcontrolname="name"] input').fill(NODE_SUB_SUPER_GROUP_NAME);
                const createReq = waitForResponseFrom(page, 'PUT', `/rest/group/${NODE_SUPER_GROUP_ID}/groups`);
                await clickModalAction(createGroupModal, 'confirm');
                const createRes = await createReq;
                const resBody: GroupResponse = await createRes.json();
                subGroupId = resBody.group.id;

                // The new group should be added to the table correctly
                const newGroupRow = await findTableRowById(groupsTable, subGroupId);
                await expect(newGroupRow).toBeVisible();

                // Close the editor
                await subGroupsTab.locator('gtx-entity-detail-header [data-action="cancel"]').click();
            });

            await test.step('Node Sub Super Admin group should allow manipulating users', async () => {
                const subGroupRow = await findTableRowById(masterTable, subGroupId);
                await clickTableRow(subGroupRow);

                await expect(editor).toBeVisible();

                const usersTab = await selectTab(tabs, 'groupUsers');
                const usersTable = usersTab.locator('gtx-user-table');

                await expect(usersTable.locator('.entity-table-actions-bar [data-action="create"]')).toBeVisible();
                await expect(usersTable.locator('.entity-table-actions-bar [data-action="assign-users"]')).toBeVisible();
            });
        });
    });
});

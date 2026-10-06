import { AccessControlledType, GcmsPermission, GroupResponse } from '@gentics/cms-models';
import {
    clickModalAction,
    clickTableRow,
    dismissNotifications,
    EntityImporter,
    expandTrableRow,
    findTableRowById,
    findTrableRowById,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    loginWithForm,
    navigateToApp,
    selectTab,
    TestSize,
    waitForResponseFrom,
} from '@gentics/e2e-utils';
import { expect, Locator, Page, test } from '@playwright/test';
import { AUTH } from './common';
import {
    editRowPermissions,
    expectOnlySuccessNotification,
    expectRowPermissions,
    groupTypePermissionNotificationId,
    groupTypePermissionPath,
    navigateToModule,
} from './helpers';

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

    test.describe('Permissions', () => {
        const TEST_GROUP: GroupImportData = {
            [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
            [IMPORT_ID]: 'group_permissions_target',

            name: 'group_permissions_target',
            description: 'Groups: Permissions Target',
            permissions: [],
        };

        /** Column which contains the name of the element in the permissions trable */
        const NAME_COLUMN = 'label';

        let groupId: number;

        test.beforeEach(async () => {
            await IMPORTER.importData([TEST_GROUP]);
            groupId = IMPORTER.get(TEST_GROUP).id;
        });

        async function openAdminPermissions(page: Page): Promise<Locator> {
            const master = await navigateToModule(page, 'groups');
            const row = await findTableRowById(master.locator('gtx-group-table'), groupId);
            await clickTableRow(row);

            const editor = page.locator('gtx-group-detail');
            await expect(editor).toBeVisible();

            const tab = await selectTab(editor.locator('.gtx-entity-detail > gtx-tabs'), 'admin-permissions');
            const trable = tab.locator('gtx-permissions-trable');
            await expect(trable).toBeVisible();

            return trable;
        }

        async function getTypePermissions(type: AccessControlledType): Promise<Partial<Record<GcmsPermission, boolean>>> {
            const res = await IMPORTER.client.group.getPermission(groupId, type).send();
            return res.perms.reduce((acc, perm) => {
                acc[perm.type] = perm.value;
                return acc;
            }, {});
        }

        test('should add and remove administrative permissions', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-20288',
            }],
        }, async ({ page }) => {
            const trable = await openAdminPermissions(page);
            const row = findTrableRowById(trable, AccessControlledType.INBOX);

            await test.step('Initial permissions are not granted', async () => {
                await expectRowPermissions(row, {
                    [GcmsPermission.READ]: false,
                    [GcmsPermission.SET_PERMISSION]: false,
                });
            });

            await test.step('Add permissions', async () => {
                await editRowPermissions(page, row, NAME_COLUMN, {
                    perms: {
                        [GcmsPermission.READ]: true,
                        [GcmsPermission.SET_PERMISSION]: true,
                    },
                }, groupTypePermissionPath(groupId, AccessControlledType.INBOX));

                await expectOnlySuccessNotification(page, groupTypePermissionNotificationId(groupId));
                await expectRowPermissions(row, {
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: true,
                });
                expect(await getTypePermissions(AccessControlledType.INBOX)).toMatchObject({
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: true,
                });
            });

            await dismissNotifications(page);

            await test.step('Remove permissions', async () => {
                await editRowPermissions(page, row, NAME_COLUMN, {
                    perms: {
                        [GcmsPermission.SET_PERMISSION]: false,
                    },
                }, groupTypePermissionPath(groupId, AccessControlledType.INBOX));

                await expectOnlySuccessNotification(page, groupTypePermissionNotificationId(groupId));
                await expectRowPermissions(row, {
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: false,
                });
                expect(await getTypePermissions(AccessControlledType.INBOX)).toMatchObject({
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: false,
                });
            });
        });

        test('should reload the expanded row and its children when applying permissions to sub-objects', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-20288',
            }],
        }, async ({ page }) => {
            const trable = await openAdminPermissions(page);
            const adminRow = findTrableRowById(trable, AccessControlledType.ADMIN);
            const childRows = [
                findTrableRowById(trable, AccessControlledType.USER_ADMIN),
                findTrableRowById(trable, AccessControlledType.GROUP_ADMIN),
                findTrableRowById(trable, AccessControlledType.CONTENT_ADMIN),
            ];

            await test.step('Expand the administration row', async () => {
                await expandTrableRow(adminRow);
                await expect(adminRow).toContainClass('expanded');

                for (const child of childRows) {
                    await expect(child).toBeVisible();
                    await expectRowPermissions(child, {
                        [GcmsPermission.READ]: false,
                        [GcmsPermission.SET_PERMISSION]: false,
                    });
                }
            });

            await test.step('Add permissions to the row and sub-objects', async () => {
                await editRowPermissions(page, adminRow, NAME_COLUMN, {
                    subObjects: true,
                    perms: {
                        [GcmsPermission.READ]: true,
                        [GcmsPermission.SET_PERMISSION]: true,
                    },
                }, groupTypePermissionPath(groupId, AccessControlledType.ADMIN));

                await expectOnlySuccessNotification(page, groupTypePermissionNotificationId(groupId));

                // Row has to stay expanded, and the children have to be reloaded as well
                await expect(adminRow).toContainClass('expanded');
                await expectRowPermissions(adminRow, {
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: true,
                });
                for (const child of childRows) {
                    await expect(child).toBeVisible();
                    await expectRowPermissions(child, {
                        [GcmsPermission.READ]: true,
                        [GcmsPermission.SET_PERMISSION]: true,
                    });
                }
            });

            await dismissNotifications(page);

            await test.step('Remove permissions from the row and sub-objects', async () => {
                await editRowPermissions(page, adminRow, NAME_COLUMN, {
                    subObjects: true,
                    perms: {
                        [GcmsPermission.SET_PERMISSION]: false,
                    },
                }, groupTypePermissionPath(groupId, AccessControlledType.ADMIN));

                await expectOnlySuccessNotification(page, groupTypePermissionNotificationId(groupId));

                await expect(adminRow).toContainClass('expanded');
                await expectRowPermissions(adminRow, {
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: false,
                });
                for (const child of childRows) {
                    await expect(child).toBeVisible();
                    await expectRowPermissions(child, {
                        [GcmsPermission.READ]: true,
                        [GcmsPermission.SET_PERMISSION]: false,
                    });
                }
            });
        });
    });
});

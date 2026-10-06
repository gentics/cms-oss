import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import {
    dismissNotifications,
    EntityImporter,
    expandTrableRow,
    findTrableRowById,
    FOLDER_A,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    loginWithForm,
    navigateToApp,
    NODE_MINIMAL,
    selectTab,
    TestSize,
    UserImportData,
} from '@gentics/e2e-utils';
import { expect, Locator, Page, test } from '@playwright/test';
import { AUTH } from './common';
import {
    editRowPermissions,
    expectOnlySuccessNotification,
    expectRowPermissions,
    groupInstancePermissionNotificationId,
    groupInstancePermissionPath,
    navigateToModule,
} from './helpers';

test.describe('Folder Module', () => {

    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'folders';

    const TEST_GROUP: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_reader`,

        description: 'Folders: Reader',
        name: `group_${NAMESPACE}_reader`,
        permissions: [],
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_reader`,

        group: TEST_GROUP,

        email: 'something@example.com',
        firstName: 'Folders',
        lastName: 'Reader',
        login: `${NAMESPACE}_reader`,
        password: 'thisisapassword123',
    };

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

    test.beforeEach(async ({ request, context }) => {
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

        await test.step('Test User Setup', async () => {
            TEST_GROUP.permissions = [
                {
                    type: AccessControlledType.NODE,
                    instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                    subObjects: true,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                    ],
                },
                {
                    type: AccessControlledType.ADMIN,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                    ],
                },
                {
                    type: AccessControlledType.CONTENT_ADMIN,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                    ],
                },
            ];

            await IMPORTER.importData([
                TEST_GROUP,
                TEST_USER,
            ]);
        });
    });

    test('should be able to expand the node and open the folder details', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20289',
        }],
    }, async ({ page }) => {
        await test.step('Open Admin-UI', async () => {
            await navigateToApp(page);
            await loginWithForm(page, TEST_USER);
        });

        const module = await navigateToModule(page, 'folders');
        const trable = module.locator('gtx-folder-trable');

        await test.step('Expand the node', async () => {
            const nodeRow = findTrableRowById(trable, IMPORTER.get(NODE_MINIMAL).folderId);
            await expandTrableRow(nodeRow);
            await expect(nodeRow).toContainClass('expanded');

            // Expanding the row must not trigger any other action, like opening a modal
            await expect(page.locator('gtx-dynamic-modal')).toHaveCount(0);
        });

        await test.step('Open the folder details', async () => {
            const folderRow = findTrableRowById(trable, IMPORTER.get(FOLDER_A).id);
            await expect(folderRow).toBeVisible();
            await folderRow.locator('.data-column[data-id="name"]').click();

            await expect(page.locator('gtx-folder-detail')).toBeVisible();
        });
    });

    test.describe('Group Permissions', () => {
        const PARENT_GROUP: GroupImportData = {
            [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
            [IMPORT_ID]: `group_${NAMESPACE}_perms_parent`,

            name: `group_${NAMESPACE}_perms_parent`,
            description: 'Folders: Permissions Parent',
            permissions: [],
        };

        const CHILD_GROUP: GroupImportData = {
            [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
            [IMPORT_ID]: `group_${NAMESPACE}_perms_child`,

            parent: PARENT_GROUP,

            name: `group_${NAMESPACE}_perms_child`,
            description: 'Folders: Permissions Child',
            permissions: [],
        };

        /** Column which contains the name of the group in the group trable */
        const NAME_COLUMN = 'name';
        /** ID of the "Node Super Admin" group, which is the root of the visible groups. */
        const NODE_SUPER_GROUP_ID = 2;

        let parentId: number;
        let childId: number;
        let folderId: number;

        test.beforeEach(async ({ page }) => {
            await IMPORTER.importData([PARENT_GROUP, CHILD_GROUP]);
            parentId = IMPORTER.get(PARENT_GROUP).id;
            childId = IMPORTER.get(CHILD_GROUP).id;
            folderId = IMPORTER.get(FOLDER_A).id;

            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
        });

        async function openGroupPermissions(page: Page): Promise<Locator> {
            const module = await navigateToModule(page, 'folders');
            const folderTrable = module.locator('gtx-folder-trable');

            const nodeRow = findTrableRowById(folderTrable, IMPORTER.get(NODE_MINIMAL).folderId);
            await expandTrableRow(nodeRow);
            const folderRow = findTrableRowById(folderTrable, folderId);
            await folderRow.locator('.data-column[data-id="name"]').click();

            const detail = page.locator('gtx-folder-detail');
            await expect(detail).toBeVisible();

            const tab = await selectTab(detail.locator('.gtx-entity-detail > gtx-tabs'), 'group-permissions');
            const groupTrable = tab.locator('gtx-group-trable');
            await expect(groupTrable).toBeVisible();

            return groupTrable;
        }

        /**
         * Expands the group rows down to the parent test group.
         */
        async function expandToParentGroup(trable: Locator): Promise<Locator> {
            for (const id of [NODE_SUPER_GROUP_ID, IMPORTER.testRootGroup.id]) {
                const row = findTrableRowById(trable, id);
                await expandTrableRow(row);
                await expect(row).toContainClass('expanded');
            }

            const parentRow = findTrableRowById(trable, parentId);
            await expect(parentRow).toBeVisible();
            return parentRow;
        }

        async function getFolderPermissions(groupId: number): Promise<Partial<Record<GcmsPermission, boolean>>> {
            const res = await IMPORTER.client.group.getInstancePermission(groupId, AccessControlledType.FOLDER, folderId).send();
            return res.perms.reduce((acc, perm) => {
                acc[perm.type] = perm.value;
                return acc;
            }, {});
        }

        test('should add and remove folder permissions of a group', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-20288',
            }],
        }, async ({ page }) => {
            const trable = await openGroupPermissions(page);
            const parentRow = await expandToParentGroup(trable);

            await test.step('Initial permissions are not granted', async () => {
                await expectRowPermissions(parentRow, {
                    [GcmsPermission.READ]: false,
                    [GcmsPermission.SET_PERMISSION]: false,
                });
            });

            await test.step('Add permissions', async () => {
                await editRowPermissions(page, parentRow, NAME_COLUMN, {
                    perms: {
                        [GcmsPermission.READ]: true,
                        [GcmsPermission.SET_PERMISSION]: true,
                    },
                }, groupInstancePermissionPath(parentId, AccessControlledType.FOLDER, folderId));

                await expectOnlySuccessNotification(page, groupInstancePermissionNotificationId(parentId));
                await expectRowPermissions(parentRow, {
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: true,
                });
                expect(await getFolderPermissions(parentId)).toMatchObject({
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: true,
                });
            });

            await dismissNotifications(page);

            await test.step('Remove permissions', async () => {
                await editRowPermissions(page, parentRow, NAME_COLUMN, {
                    perms: {
                        [GcmsPermission.SET_PERMISSION]: false,
                    },
                }, groupInstancePermissionPath(parentId, AccessControlledType.FOLDER, folderId));

                await expectOnlySuccessNotification(page, groupInstancePermissionNotificationId(parentId));
                await expectRowPermissions(parentRow, {
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: false,
                });
                expect(await getFolderPermissions(parentId)).toMatchObject({
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: false,
                });
            });
        });

        test('should reload the expanded group and its sub-groups when applying permissions to sub-groups', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-20288',
            }],
        }, async ({ page }) => {
            const trable = await openGroupPermissions(page);
            const parentRow = await expandToParentGroup(trable);
            const childRow = findTrableRowById(trable, childId);

            await test.step('Expand the parent group', async () => {
                await expandTrableRow(parentRow);
                await expect(parentRow).toContainClass('expanded');
                await expect(childRow).toBeVisible();
                await expectRowPermissions(childRow, {
                    [GcmsPermission.READ]: false,
                    [GcmsPermission.SET_PERMISSION]: false,
                });
            });

            await test.step('Add permissions to the group and sub-groups', async () => {
                await editRowPermissions(page, parentRow, NAME_COLUMN, {
                    subGroups: true,
                    perms: {
                        [GcmsPermission.READ]: true,
                        [GcmsPermission.SET_PERMISSION]: true,
                    },
                }, groupInstancePermissionPath(parentId, AccessControlledType.FOLDER, folderId));

                await expectOnlySuccessNotification(page, groupInstancePermissionNotificationId(parentId));

                // Row has to stay expanded, and the sub-group has to be reloaded as well
                await expect(parentRow).toContainClass('expanded');
                await expectRowPermissions(parentRow, {
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: true,
                });
                await expect(childRow).toBeVisible();
                await expectRowPermissions(childRow, {
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: true,
                });
                expect(await getFolderPermissions(childId)).toMatchObject({
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: true,
                });
            });

            await dismissNotifications(page);

            await test.step('Remove permissions from the group and sub-groups', async () => {
                await editRowPermissions(page, parentRow, NAME_COLUMN, {
                    subGroups: true,
                    perms: {
                        [GcmsPermission.SET_PERMISSION]: false,
                    },
                }, groupInstancePermissionPath(parentId, AccessControlledType.FOLDER, folderId));

                await expectOnlySuccessNotification(page, groupInstancePermissionNotificationId(parentId));

                await expect(parentRow).toContainClass('expanded');
                await expectRowPermissions(parentRow, {
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: false,
                });
                await expect(childRow).toBeVisible();
                await expectRowPermissions(childRow, {
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: false,
                });
                expect(await getFolderPermissions(childId)).toMatchObject({
                    [GcmsPermission.READ]: true,
                    [GcmsPermission.SET_PERMISSION]: false,
                });
            });
        });
    });
});

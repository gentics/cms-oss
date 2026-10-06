import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import {
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
    TestSize,
    UserImportData,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { navigateToModule } from './helpers';

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
});

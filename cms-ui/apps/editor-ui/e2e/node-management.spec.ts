import { AccessControlledType, GcmsPermission } from "@gentics/cms-models";
import {
    EntityImporter,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    ImportPermissions,
    loginWithForm,
    navigateToApp,
    NODE_MINIMAL,
    openContext,
    TestSize,
    UserImportData,
} from "@gentics/e2e-utils";
import { cloneWithSymbols } from "@gentics/ui-core/utils/clone-with-symbols";
import { test, expect, Page } from "@playwright/test";
import { editorAction, selectNode } from "./helpers";

test.describe('Node Management', () => {

    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'nodemngt';

    const TEST_GROUP_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_editor`,

        description: 'Node Management: Editor',
        name: `group_${NAMESPACE}_editor`,
        permissions: [],
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_editor`,

        group: TEST_GROUP_BASE,

        email: 'something@example.com',
        firstName: 'NodeManagement',
        lastName: 'Editor',
        login: `${NAMESPACE}_editor`,
        password: 'nodetest',
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
    });

    async function setupWithPermissions(page: Page, permissions: ImportPermissions[]): Promise<void> {
        await test.step('Test User Setup', async () => {
            const TEST_GROUP = cloneWithSymbols(TEST_GROUP_BASE);
            TEST_GROUP.permissions = permissions;

            await IMPORTER.importData([
                TEST_GROUP,
                TEST_USER,
            ]);
        });

        await test.step('Open Editor-UI', async () => {
            await navigateToApp(page);
            await loginWithForm(page, TEST_USER);
            await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);
        });
    }

    test('should be possible to edit the node properties', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL)!.folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.UPDATE, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_FOLDER, value: true },
                ],
            }
        ]);

        const NEW_NODE_NAME = 'testtesttest';

        const nodeTitle = page.locator('folder-contents > .title');
        const folderOptions = await openContext(nodeTitle.locator('folder-context-menu gtx-dropdown-list'));
        await folderOptions.locator('[data-action="node-properties"]').click();

        const form = page.locator('content-frame combined-properties-editor .properties-content gtx-node-properties');
        await form.locator('[formcontrolname="name"] input').fill(NEW_NODE_NAME);
        await page.waitForTimeout(500); // Have to wait for internals to propagate

        await editorAction(page, 'save');

        await expect(nodeTitle.locator('.title-name')).toHaveText(NEW_NODE_NAME);
    });

    test('should not be possible to edit the node properties without permissions', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19638',
        }],
    }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL)!.folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            }
        ]);

        const nodeTitle = page.locator('folder-contents > .title');
        const folderOptions = await openContext(nodeTitle.locator('folder-context-menu gtx-dropdown-list'));
        await folderOptions.locator('[data-action="node-properties"]').click();

        const form = page.locator('content-frame combined-properties-editor .properties-content gtx-node-properties');
        await expect(form.locator('[formcontrolname="name"] input')).toBeDisabled();
    });
});

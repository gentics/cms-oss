import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import {
    EntityImporter,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_NODE,
    IMPORT_TYPE_USER,
    LANGUAGE_EN,
    loginWithForm,
    matchRequest,
    navigateToApp,
    NODE_MINIMAL,
    NodeImportData,
    TestSize,
    UserImportData,
} from '@gentics/e2e-utils';
import { cloneWithSymbols } from '@gentics/ui-core/utils/clone-with-symbols';
import test, { expect, Locator, Response } from '@playwright/test';
import { navigateToModule } from './helpers';

test.describe('Content-Maintenance Module', () => {

    const IMPORTER = new EntityImporter();
    let module: Locator;
    let nodeLoadReq: Promise<Response>;

    const BASE_GROUP: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: 'contentMaintenance_group_base',

        name: 'contentMaintenance_base',
        description: 'Content-Maintenance: Base',

        permissions: [
            {
                type: AccessControlledType.ADMIN,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
            {
                type: AccessControlledType.MAINTENANCE,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
        ],
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: 'contentMaintenance_user_test',

        email: 'test@example.com',
        firstName: 'Content-Maintenance',
        lastName: 'Test',
        group: BASE_GROUP,

        login: 'contentMaintenance_test',
        password: 'foobar2000',
    };

    const DUMMY_NODES: NodeImportData[] = [];
    for (let i = 0; i < 15; i++) {
        DUMMY_NODES.push({
            [IMPORT_TYPE]: IMPORT_TYPE_NODE,
            [IMPORT_ID]: `contentMaintenance_node_${i}`,

            node: {
                name: `Dummy Node #${i+1}`,
                host: `dummy${i}.com`,
            },
            languages: [LANGUAGE_EN],
        });
    }

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

    test.beforeEach(async ({ request, context, page }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await context.clearCookies();
            await IMPORTER.clearClient();
        });

        await test.step('Common Test Setup', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });

        await test.step('Import specialized Test Data', async () => {
            await IMPORTER.importData(DUMMY_NODES);
            const TEST_GROUP = cloneWithSymbols(BASE_GROUP);

            DUMMY_NODES.concat(NODE_MINIMAL).forEach(importData => {
                const node = IMPORTER.get(importData);
                TEST_GROUP.permissions.push({
                    type: AccessControlledType.NODE,
                    instanceId: `${node.folderId}`,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                    ],
                });
            });

            await IMPORTER.importData([TEST_GROUP, TEST_USER]);
        });

        await test.step('Open Admin-UI', async () => {
            await navigateToApp(page);
            await loginWithForm(page, TEST_USER);
            nodeLoadReq = page.waitForResponse(matchRequest('GET', '/rest/node'));
            module = await navigateToModule(page, 'content-maintenance');
        });
    });

    test('node table should be paginated', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19259',
        }],
    }, async () => {
        const nodeTable = module.locator('gtx-node-publish-process-table');

        await nodeLoadReq;

        await expect(nodeTable.locator('.grid-row.data-row')).toHaveCount(10);

        const pagination = nodeTable.locator('.table-pagination');
        await pagination.locator('[data-action="next"]').click();

        await expect(nodeTable.locator('.grid-row.data-row')).toHaveCount(6);
    });
});

import { AccessControlledType, ContentPackageListResponse, Feature, GcmsPermission, Variant } from "@gentics/cms-models";
import {
    EntityImporter,
    envAll,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    isVariant,
    loginWithForm,
    navigateToApp,
    TestSize,
    UserImportData,
    waitForResponseFrom
} from "@gentics/e2e-utils";
import { expect, Locator, Response, test } from "@playwright/test";
import { navigateToModule } from "./helpers";

test.describe.configure({ mode: 'serial' });
test.describe('Content-Staging Module', () => {
    test.skip(() => !isVariant(Variant.ENTERPRISE), 'Content-Staging is an enterprise feature');

    const IMPORTER = new EntityImporter();
    const namespace = 'content-staging';
    let module: Locator;

    const TEST_GROUP: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${namespace}`,

        name: `${namespace}`,
        description: 'Content-Staging',

        permissions: [
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
            {
                type: AccessControlledType.CONTENT_STAGING_ADMIN,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
        ],
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${namespace}`,

        email: 'test@example.com',
        firstName: 'Content-Staging',
        lastName: 'Test',
        group: TEST_GROUP,

        login: `${namespace}_user`,
        password: 'foobar2000',
    };

    test.beforeAll(async ({ request }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
        });

        await test.step('Test Bootstrapping', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.setupFeatures({ [Feature.CONTENT_STAGING]: true });
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
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });

        await test.step('Test User Import', async () => {
            await IMPORTER.importData([TEST_GROUP, TEST_USER]);
        });
    });

    test('should paginate elements correctly', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19232',
        }],
    }, async ({ page }) => {
        const PKG_COUNT = 15;
        const MAX_PER_PAGE = 10;

        await test.step('Import Dummy Packages', async () => {
            for (let i = 0; i < PKG_COUNT; i++) {
                await IMPORTER.client.contentStaging.create({
                    name: `dummy_package_${i+1}`,
                    description: 'Integration Test dummy staging package',
                }).send();
            }
        });

        let initialPageLoadReq = waitForResponseFrom(page, 'GET', '/rest/content/package', {
            params: {
                page: '1',
            }
        });

        await test.step('Open Admin-UI', async () => {
            await navigateToApp(page);
            await loginWithForm(page, TEST_USER);
            module = await navigateToModule(page, 'content-staging');
        });

        const initialPageRes = await initialPageLoadReq;
        const initialPageItems = await initialPageRes.json() as ContentPackageListResponse;

        expect(initialPageItems.items).toHaveLength(MAX_PER_PAGE);
        expect(initialPageItems.numItems).toEqual(PKG_COUNT);

        const table = module.locator('gtx-content-package-table');
        const rows = table.locator('.grid-row.data-row');
        const pagination = table.locator('.table-pagination');

        await expect(rows).toHaveCount(MAX_PER_PAGE);
        await expect(pagination).toBeVisible();

        const pageChangeReq = waitForResponseFrom(page, 'GET', '/rest/content/package', {
            params: {
                page: '2',
            },
        });
        await pagination.locator('[data-action="next"]').click();
        await pageChangeReq;

        await expect(rows).toHaveCount(PKG_COUNT - MAX_PER_PAGE);
        await expect(pagination).toBeVisible();
    });
});

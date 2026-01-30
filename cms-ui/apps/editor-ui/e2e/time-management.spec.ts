import { AccessControlledType, GcmsPermission, PagePublishRequest, PageSaveRequest } from '@gentics/cms-models';
import {
    clickModalAction,
    EntityImporter,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    ITEM_TYPE_PAGE,
    loginWithForm,
    matchRequest,
    navigateToApp,
    NODE_MINIMAL,
    PAGE_ONE,
    pickDate,
    pickSelectValue,
    TestSize,
    UserImportData,
} from '@gentics/e2e-utils';
import {expect, Locator, test} from '@playwright/test';
import { findItem, findList, itemAction, selectNode } from './helpers';

test.describe('Time Management', () => {

    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'timemngt';

    const TEST_GROUP_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_base`,

        name: `${NAMESPACE}_base`,
        description: 'Time Management: Base',
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_base`,

        email: 'test@example.com',
        firstName: 'Time Management',
        lastName: 'User',
        login: `${NAMESPACE}_user`,
        password: 'timemanagementtest',

        group: TEST_GROUP_BASE,
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

    test.beforeEach(async ({ page, request, context }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await context.clearCookies();
            await IMPORTER.clearClient();
        });

        await test.step('Common Test Setup', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });

        const NODE = IMPORTER.get(NODE_MINIMAL);

        await test.step('Test User Setup', async () => {
            // We have to assemble the group permissions here, because we
            // need the imported data refs.
            // Create a copy, so we have a clean base object
            const TEST_GROUP = {...TEST_GROUP_BASE};

            TEST_GROUP.permissions = [
                {
                    type: AccessControlledType.NODE,
                    instanceId: `${NODE.folderId}`,
                    // subObjects: true,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                        { type: GcmsPermission.READ_ITEMS, value: true },
                        { type: GcmsPermission.UPDATE_ITEMS, value: true },
                        { type: GcmsPermission.PUBLISH_PAGES, value: true },
                    ],
                },
            ];

            await IMPORTER.importData([
                TEST_GROUP,
                TEST_USER,
            ]);
        });

        await navigateToApp(page, '/');
        await loginWithForm(page, TEST_USER);
        await selectNode(page, NODE.id);
    });

    // TODO: Flaky on CI
    test('should be able to remove the planned publish date', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19024',
        }],
    }, async ({ page }) => {
        const PAGE_TO_EDIT = IMPORTER.get(PAGE_ONE);

        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, PAGE_TO_EDIT.id);

        let modal: Locator;
        let publishAt: Locator;

        await test.step('Schedule page publish', async () => {
            await itemAction(item, 'time-management');

            modal = page.locator('gtx-time-management-modal');

            // exactly 24h in the future, also known as tomorrow
            const futureDate = new Date(new Date().getTime() + (1_000 * 3_600 * 24));
            publishAt = modal.locator('[data-control="current_publishAt"]');
            await pickDate(publishAt, futureDate);

            const publishReq = page.waitForResponse(matchRequest('POST', `/rest/page/publish/${PAGE_TO_EDIT.id}`));
            await clickModalAction(modal, 'confirm');

            const publishRes = await publishReq;
            const reqBody: PagePublishRequest = publishRes.request().postDataJSON();
            const diff = Math.trunc(futureDate.getTime() / 1_000) - reqBody.at;
            expect([0, 3_600]).toContain(diff)
        });

        await test.step('Clear page publish', async () => {
            await itemAction(item, 'time-management');
            await publishAt.locator('.clear-button button').click();

            const clearReq = page.waitForResponse(matchRequest('POST', `/rest/page/save/${PAGE_TO_EDIT.id}`));
            await clickModalAction(modal, 'confirm');

            const clearRes = await clearReq;
            const reqBody: PageSaveRequest = clearRes.request().postDataJSON();
            expect(reqBody.clearPublishAt).toEqual(true);
            expect(reqBody.clearOfflineAt).toEqual(false);
        });
    });
});

import {
    EntityImporter,
    IMPORT_TYPE,
    TestSize,
    FOLDER_A,
    FOLDER_B,
    loginWithForm,
    NODE_MINIMAL,
    navigateToApp,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import {
    findItem,
    findList,
    itemAction,
    selectNode,
} from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('App', () => {
    const IMPORTER = new EntityImporter();

    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);

        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach(async ({ page, request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);

        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.setupTest(TestSize.MINIMAL);

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);
    });

    test('should have the minimal node present', async ({ page }) => {
        const title = page.locator('folder-contents > .title .title-name');
        await expect(title).toHaveText(NODE_MINIMAL.node.name);

        const folders = [FOLDER_A, FOLDER_B];
        for (const folder of folders) {
            const list = findList(page, folder[IMPORT_TYPE]);
            const item = findItem(list, IMPORTER.get(folder)!.id);
            await expect(item).toBeVisible();
        }

        const folderAList = findList(page, FOLDER_A[IMPORT_TYPE]);
        const folderAItem = findItem(folderAList, IMPORTER.get(FOLDER_A)!.id);
        await itemAction(folderAItem, 'properties');

        const contentFrame = page.locator('content-frame');
        await expect(contentFrame).toBeVisible();
    });
});

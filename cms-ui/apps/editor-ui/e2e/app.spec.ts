import {
    EntityImporter,
    IMPORT_TYPE,
    TestSize,
    folderA,
    folderB,
    loginWithForm,
    minimalNode,
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
test.describe('Login', () => {
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
        await selectNode(page, IMPORTER.get(minimalNode)!.id);
    });

    test('should have the minimal node present', async ({ page }) => {
        const title = page.locator('folder-contents > .title .title-name');
        await expect(title).toHaveText(minimalNode.node.name);

        const folders = [folderA, folderB];
        for (const folder of folders) {
            const list = findList(page, folder[IMPORT_TYPE]);
            const item = findItem(list, IMPORTER.get(folder)!.id);
            await expect(item).toBeVisible();
        }

        const folderAList = findList(page, folderA[IMPORT_TYPE]);
        const folderAItem = findItem(folderAList, IMPORTER.get(folderA)!.id);
        await itemAction(folderAItem, 'properties');

        const contentFrame = page.locator('content-frame');
        await expect(contentFrame).toBeVisible();
    });
});

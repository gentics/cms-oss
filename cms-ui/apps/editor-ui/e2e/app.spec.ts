import { test, expect } from '@playwright/test';
import {
    EntityImporter,
    IMPORT_TYPE,
    TestSize,
    folderA,
    folderB,
    minimalNode,
} from '@gentics/e2e-utils';
import {
    login,
    selectNode,
    findList,
    findItem,
    itemAction,
    initPage,
} from './helpers';
import { AUTH_ADMIN } from './common';

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
        await initPage(page);
        await page.goto('/');
        await login(page, AUTH_ADMIN);
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

import {
    EntityImporter,
    IMPORT_TYPE,
    TestSize,
    FOLDER_A,
    FOLDER_B,
    loginWithForm,
    matchRequest,
    NODE_MINIMAL,
    navigateToApp,
    onRequest,
    waitForResponseFrom,
} from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import {
    findItem,
    findList,
    itemAction,
    selectNode,
} from './helpers';

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

    test('should stop polling for messages when logged out by the backend', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20043',
        }],
    }, async ({ page, context }) => {
        // Messages are polled every 30 seconds, so we need to wait for multiple intervals
        test.setTimeout(120_000);

        const POLL_TIMEOUT = 40_000;

        // Wait for the folder contents to be loaded, so only the polling requests are affected
        const list = findList(page, FOLDER_A[IMPORT_TYPE]);
        await expect(findItem(list, IMPORTER.get(FOLDER_A).id)).toBeVisible();

        // Invalidate the session in the backend
        await context.clearCookies({ name: 'GCN_SESSION_SECRET' });

        // The next poll fails, which should log the user out
        const failedPoll = await waitForResponseFrom(page, 'GET', '/rest/msg/list', {
            skipStatus: true,
            timeout: POLL_TIMEOUT,
        });
        expect(failedPoll.ok()).toBe(false);

        await expect(page.locator('gtx-login')).toBeVisible();

        let pollCount = 0;
        onRequest(page, matchRequest('GET', '/rest/msg/list'), () => {
            pollCount++;
        });

        // Wait longer than a poll-interval, to make sure no further polling occurs
        // eslint-disable-next-line playwright/no-wait-for-timeout
        await page.waitForTimeout(POLL_TIMEOUT);
        expect(pollCount).toBe(0);
    });

    test('should prompt a re-login when an action is performed after being logged out by the backend', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20043',
        }],
    }, async ({ page, context }) => {
        const list = findList(page, FOLDER_A[IMPORT_TYPE]);
        const item = findItem(list, IMPORTER.get(FOLDER_A).id);
        const nameLink = item.locator('.item-name a');
        await expect(nameLink).toBeVisible();

        // Invalidate the session in the backend
        await context.clearCookies({ name: 'GCN_SESSION_SECRET' });

        // Navigate into the folder, which requires a valid session
        await nameLink.click();

        await expect(page.locator('gtx-login')).toBeVisible();
        await expect(page).toHaveURL(/\/login\?returnUrl=/);

        // The user is informed about being logged out
        const dialog = page.locator('gtx-modal-dialog');
        await expect(dialog).toBeVisible();
        await dialog.locator('.modal-footer gtx-button button').click();
        await expect(dialog).toBeHidden();

        // Logging in again brings the user back into the app
        await loginWithForm(page, AUTH.admin);
        await expect(page.locator('project-editor')).toBeVisible();
    });
});

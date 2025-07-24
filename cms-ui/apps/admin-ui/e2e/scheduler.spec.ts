import { test, expect } from '@playwright/test';
import {
    EntityImporter, IMPORT_ID, IMPORT_TYPE, IMPORT_TYPE_TASK, ScheduleTaskImportData, TestSize, findTableRowById, selectTab, clickTableRow,
} from '@gentics/e2e-utils';
import { AUTH_ADMIN } from './common';
import { loginWithForm, navigateToModule, navigateToApp } from './helpers';

const EXAMPLE_TASK_ONE: ScheduleTaskImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_TASK,
    [IMPORT_ID]: 'taskExampleOne',
    name: 'taskOne',
    command: 'sleep.sh',
};

test.describe.configure({ mode: 'serial' });
test.describe('Scheduler Module', () => {
    const IMPORTER = new EntityImporter();

    test.beforeAll(async ({ request }, testInfo) => {
        testInfo.setTimeout(120_000);
        IMPORTER.setApiContext(request);
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach(async ({ page, request, context }, testInfo) => {
        testInfo.setTimeout(120_000);
        await context.clearCookies();
        // Reset importer client to avoid 401 errors
        IMPORTER.setApiContext(request);

        // Clean and setup test data
        await IMPORTER.cleanupTest();
        await IMPORTER.syncPackages(TestSize.MINIMAL);
        await IMPORTER.importData([EXAMPLE_TASK_ONE], TestSize.NONE);

        // Navigate to the app and log in
        await navigateToApp(page);
        await loginWithForm(page, AUTH_ADMIN);

        // Navigate to the scheduler
        await navigateToModule(page, 'scheduler');
    });

    test.describe('Tasks', () => {
        test('should properly save modified tasks', async ({ page }) => {
            // Locate table and the specific task row
            await selectTab(page, 'tasks');
            const taskId = `${IMPORTER.get(EXAMPLE_TASK_ONE)?.id}`;
            const row = findTableRowById(page, taskId);
            await clickTableRow(row);

            // Ensure detail view is visible
            const detailView = page.locator('gtx-schedule-task-detail');
            await expect(detailView).toBeVisible();

            // Change the task name
            const newName = 'New Task Name';
            await detailView.locator('[formcontrolname="name"] input').fill(newName);

            // Submit changes and wait for save
            await page.click('.gtx-save-button button');

            // Verify table reload and updated name
            await page.waitForResponse(resp =>
                resp.url().includes('/rest/scheduler/task') && resp.request().method() === 'GET',
            );
            await expect(findTableRowById(page, taskId)).toContainText(newName);
        });
    });
});

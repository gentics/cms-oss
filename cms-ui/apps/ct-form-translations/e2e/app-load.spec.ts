import { EntityImporter, TestSize } from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';

import { GLOBAL_SCOPE_ID } from './common';
import {
    findScopeTab,
    findScopeTabBar,
    findTable,
    findToolbar,
    navigateToTool,
    waitForToolReady,
} from './helpers';

/**
 * Smoke tests: verifies that the tool boots up against a real CMS and renders
 * its scaffolding (header, scope tabs, toolbar, table) without any errors.
 *
 * These tests do NOT make assertions about the contents of the global
 * translations — what's there is data-dependent.
 */
test.describe('form-translations · App Load', () => {
    const IMPORTER = new EntityImporter();

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
            await IMPORTER.syncPackages(TestSize.MINIMAL);
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });

        await test.step('Open tool with system session', async () => {
            /* setupTest already triggered auto-login on the importer client. */
            await navigateToTool(page);
            await waitForToolReady(page);
        });
    });

    test('should render the tool shell with header, tabs, toolbar and table', async ({ page }) => {
        await expect(page.locator('[data-region="header"] h1')).toBeVisible();
        await expect(findScopeTabBar(page)).toBeVisible();
        await expect(findToolbar(page)).toBeVisible();
        await expect(findTable(page)).toBeVisible();
    });

    test('should default to the Global scope tab being active', async ({ page }) => {
        const globalTab = findScopeTab(page, GLOBAL_SCOPE_ID);
        await expect(globalTab).toBeVisible();
        await expect(globalTab).toHaveAttribute('data-active', 'true');
    });

    test('should render at least one language column in the table', async ({ page }) => {
        const langHeaders = findTable(page).locator('thead th.col-lang');
        await expect(langHeaders.first()).toBeVisible();
        const count = await langHeaders.count();
        expect(count, 'CMS should provide at least one language').toBeGreaterThan(0);
    });

    test('should NOT show the save bar on a fresh load (no unsaved changes)', async ({ page }) => {
        await expect(page.locator('[data-region="save-bar"]')).toHaveAttribute('data-visible', 'false');
    });
});

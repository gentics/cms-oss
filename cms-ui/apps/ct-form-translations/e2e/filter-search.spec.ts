import { EntityImporter, TestSize } from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';

import { KNOWN_PLACEHOLDERS } from './common';
import {
    findFilterButton,
    findSearchInput,
    findTable,
    findTableRow,
    setFilter,
    setSearch,
    navigateToTool,
    waitForToolReady,
} from './helpers';

test.describe('form-translations · Search & Filter', () => {
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

        await test.step('Open tool', async () => {
            await navigateToTool(page);
            await waitForToolReady(page);
        });
    });

    test('should narrow the row list when a search term matches a key', async ({ page }) => {
        /* "submit" is a substring of the well-known submit-button placeholder. */
        await setSearch(page, 'submit');

        await expect(findTableRow(page, KNOWN_PLACEHOLDERS.SUBMIT_BUTTON)).toBeVisible();
        /* Cancel button shouldn't be in the result set. */
        await expect(findTableRow(page, KNOWN_PLACEHOLDERS.CANCEL_BUTTON)).toHaveCount(0);
    });

    test('should reflect the shown/total counter when filtering by search', async ({ page }) => {
        const counter = page.locator('[data-name="counter"]');

        const totalBefore = Number(await counter.getAttribute('data-total'));
        const shownBefore = Number(await counter.getAttribute('data-shown'));
        expect(totalBefore).toBeGreaterThan(0);
        expect(shownBefore).toBe(totalBefore);

        await setSearch(page, 'submit');

        const shownAfter = Number(await counter.getAttribute('data-shown'));
        expect(shownAfter).toBeGreaterThan(0);
        expect(shownAfter).toBeLessThan(totalBefore);
    });

    test('should show the empty state for a no-match search term', async ({ page }) => {
        await setSearch(page, 'zzz_this_key_should_not_exist_zzz');

        await expect(findTable(page)).toHaveCount(0);
        await expect(page.locator('[data-name="empty-state"]')).toBeVisible();
    });

    test('should clear search results when the search input is cleared', async ({ page }) => {
        const counter = page.locator('[data-name="counter"]');
        const totalBefore = Number(await counter.getAttribute('data-total'));

        await setSearch(page, 'submit');
        await findSearchInput(page).fill('');

        await expect.poll(async () => Number(await counter.getAttribute('data-shown')))
            .toBe(totalBefore);
    });

    test('should toggle the active state of the filter buttons', async ({ page }) => {
        await expect(findFilterButton(page, 'all')).toHaveAttribute('data-active', 'true');
        await expect(findFilterButton(page, 'incomplete')).toHaveAttribute('data-active', 'false');

        await setFilter(page, 'incomplete');

        await expect(findFilterButton(page, 'all')).toHaveAttribute('data-active', 'false');
        await expect(findFilterButton(page, 'incomplete')).toHaveAttribute('data-active', 'true');
    });
});

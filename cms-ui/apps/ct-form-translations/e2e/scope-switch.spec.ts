import { EntityImporter, TestSize } from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';

import { GLOBAL_SCOPE_ID, KNOWN_PLACEHOLDERS } from './common';
import {
    autoAcceptNextConfirm,
    autoDismissNextConfirm,
    clickScopeTab,
    expectCellState,
    fillCell,
    findScopeTab,
    findTableRow,
    navigateToTool,
    waitForToolReady,
} from './helpers';

test.describe('form-translations · Scope Switching', () => {
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

    /**
     * Resolve the id of the first non-global scope tab. The CMS provides the
     * form-type list via `/rest/form/types`, so this is data-dependent — if
     * the CMS has no form types configured we skip the scope-switching tests
     * that need a second tab.
     */
    async function getFirstFormTypeScopeId(page): Promise<string | null> {
        const tabs = page.locator('[data-region="scope-tabs"] [data-action="select-scope"]');
        const count = await tabs.count();
        for (let i = 0; i < count; i++) {
            const id = await tabs.nth(i).getAttribute('data-id');
            if (id && id !== GLOBAL_SCOPE_ID) return id;
        }
        return null;
    }

    test('should switch the active tab when another scope is clicked', async ({ page }) => {
        const otherScopeId = await getFirstFormTypeScopeId(page);
        test.skip(!otherScopeId, 'CMS has no form-type scopes configured');

        await clickScopeTab(page, otherScopeId!);

        await expect(findScopeTab(page, otherScopeId!)).toHaveAttribute('data-active', 'true');
        await expect(findScopeTab(page, GLOBAL_SCOPE_ID)).toHaveAttribute('data-active', 'false');
        await expect(page.locator('[data-name="scope-card"]'))
            .toHaveAttribute('data-scope-id', otherScopeId!);
    });

    test('should prompt for confirmation and STAY on the active tab when the user cancels', async ({ page }) => {
        const otherScopeId = await getFirstFormTypeScopeId(page);
        test.skip(!otherScopeId, 'CMS has no form-type scopes configured');

        await findTableRow(page, KNOWN_PLACEHOLDERS.LOADING).waitFor({ state: 'visible' });
        await fillCell(page, KNOWN_PLACEHOLDERS.LOADING, 'en', 'dirty-value [scope-switch]');
        await expectCellState(page, KNOWN_PLACEHOLDERS.LOADING, 'en', 'dirty');

        autoDismissNextConfirm(page);
        await findScopeTab(page, otherScopeId!).click();

        /* Tab did not switch */
        await expect(findScopeTab(page, GLOBAL_SCOPE_ID)).toHaveAttribute('data-active', 'true');
        /* Dirty state preserved */
        await expectCellState(page, KNOWN_PLACEHOLDERS.LOADING, 'en', 'dirty');
    });

    test('should discard changes and switch tab when the user confirms', async ({ page }) => {
        const otherScopeId = await getFirstFormTypeScopeId(page);
        test.skip(!otherScopeId, 'CMS has no form-type scopes configured');

        await findTableRow(page, KNOWN_PLACEHOLDERS.LOADING).waitFor({ state: 'visible' });
        await fillCell(page, KNOWN_PLACEHOLDERS.LOADING, 'en', 'dirty-value [scope-switch]');
        await expectCellState(page, KNOWN_PLACEHOLDERS.LOADING, 'en', 'dirty');

        autoAcceptNextConfirm(page);
        await findScopeTab(page, otherScopeId!).click();

        await expect(findScopeTab(page, otherScopeId!)).toHaveAttribute('data-active', 'true');

        /* Switch back to global and verify the change was discarded. */
        await clickScopeTab(page, GLOBAL_SCOPE_ID);
        await findTableRow(page, KNOWN_PLACEHOLDERS.LOADING).waitFor({ state: 'visible' });
        await expectCellState(page, KNOWN_PLACEHOLDERS.LOADING, 'en', 'saved');
    });
});

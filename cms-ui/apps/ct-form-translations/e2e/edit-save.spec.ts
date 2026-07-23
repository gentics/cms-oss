import { EntityImporter, TestSize } from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';

import { KNOWN_PLACEHOLDERS } from './common';
import {
    clickDiscardAndConfirm,
    clickSaveAndWait,
    expectCellState,
    expectCellValue,
    expectSaveBarVisible,
    fillCell,
    findCellInput,
    findSaveButton,
    findTableRow,
    navigateToTool,
    readDirtyCount,
    waitForToolReady,
} from './helpers';

test.describe('form-translations · Edit, Save & Discard', () => {
    const IMPORTER = new EntityImporter();

    /**
     * The placeholder + language combination targeted by these tests. We use a
     * well-known global placeholder so the row exists regardless of CMS-side
     * defaults. Language must be one that the CMS reports as available.
     */
    const TARGET_KEY  = KNOWN_PLACEHOLDERS.LOADING;
    const TARGET_LANG = 'en';

    /** Token suffix added to differentiate the test value from any real translation. */
    const TEST_TOKEN = '[e2e-edit-save]';

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
            await findTableRow(page, TARGET_KEY).waitFor({ state: 'visible' });
        });
    });

    test('should mark a cell as dirty when its value changes', async ({ page }) => {
        const originalValue = await findCellInput(page, TARGET_KEY, TARGET_LANG).inputValue();
        await expectCellState(page, TARGET_KEY, TARGET_LANG, originalValue === '' ? 'empty' : 'saved');

        await fillCell(page, TARGET_KEY, TARGET_LANG, `${originalValue} ${TEST_TOKEN}`);

        await expectCellState(page, TARGET_KEY, TARGET_LANG, 'dirty');
        await expectSaveBarVisible(page, true);
        expect(await readDirtyCount(page)).toBe(1);
    });

    test('should disable save and discard while there are no changes', async ({ page }) => {
        await expect(findSaveButton(page)).toBeDisabled();
    });

    test('should persist a change across reload after saving', async ({ page }) => {
        const originalValue = await findCellInput(page, TARGET_KEY, TARGET_LANG).inputValue();
        const newValue = `${originalValue} ${TEST_TOKEN}`.trim();

        await fillCell(page, TARGET_KEY, TARGET_LANG, newValue);
        await clickSaveAndWait(page);

        await expectCellState(page, TARGET_KEY, TARGET_LANG, 'saved');
        await expectCellValue(page, TARGET_KEY, TARGET_LANG, newValue);

        await test.step('Reload and re-verify persistence', async () => {
            await navigateToTool(page);
            await waitForToolReady(page);
            await findTableRow(page, TARGET_KEY).waitFor({ state: 'visible' });
            await expectCellValue(page, TARGET_KEY, TARGET_LANG, newValue);
        });
    });

    test('should revert dirty changes when discard is confirmed', async ({ page }) => {
        const originalValue = await findCellInput(page, TARGET_KEY, TARGET_LANG).inputValue();
        await fillCell(page, TARGET_KEY, TARGET_LANG, `${originalValue} ${TEST_TOKEN}`);
        await expectCellState(page, TARGET_KEY, TARGET_LANG, 'dirty');

        await clickDiscardAndConfirm(page);

        await expectCellValue(page, TARGET_KEY, TARGET_LANG, originalValue);
        await expectCellState(page, TARGET_KEY, TARGET_LANG, originalValue === '' ? 'empty' : 'saved');
        await expectSaveBarVisible(page, false);
    });

    test('should count multiple dirty cells in the same scope', async ({ page }) => {
        const langHeaders = page.locator('gtx-translations-table thead th.col-lang');
        const headerCount = await langHeaders.count();
        test.skip(headerCount < 2, 'Needs at least 2 languages configured');

        const secondLang = await langHeaders.nth(1).getAttribute('data-lang');
        expect(secondLang, 'second language header should expose data-lang').not.toBeNull();

        await fillCell(page, TARGET_KEY, TARGET_LANG, `value-a ${TEST_TOKEN}`);
        await fillCell(page, TARGET_KEY, secondLang!, `value-b ${TEST_TOKEN}`);

        expect(await readDirtyCount(page)).toBe(2);
    });
});

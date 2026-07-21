/**
 * Locator and interaction helpers for the form-translations custom tool.
 *
 * Selector philosophy (per the CMS UI integration-test guide):
 *  - Use Angular component selectors plus descriptive `data-*` attributes.
 *  - Prefer `data-action` for things that respond to user input
 *    (e.g. `data-action="save"`), and `data-id` to identify specific entities
 *    (e.g. `data-id="form_submit_button"` for a translation row).
 *  - Avoid CSS class names — they're styling concerns and change too often.
 */

import { expect, Locator, Page, test } from '@playwright/test';

/* =====================================================================
 *  Top-level regions
 * ===================================================================== */

/** Root of the rendered tool UI. */
export function findShell(page: Page): Locator {
    return page.locator('gtx-shell [data-region="shell"]');
}

/** The horizontal tab strip listing the global + form-type scopes. */
export function findScopeTabBar(page: Page): Locator {
    return page.locator('gtx-scope-tabs [data-region="scope-tabs"]');
}

/** The search/filter row above the table. */
export function findToolbar(page: Page): Locator {
    return page.locator('gtx-translations-toolbar [data-region="toolbar"]');
}

/** The bordered translations grid. */
export function findTable(page: Page): Locator {
    return page.locator('gtx-translations-table [data-region="translations-table"]');
}

/** The bottom save bar. Only visible when there are unsaved changes. */
export function findSaveBar(page: Page): Locator {
    return page.locator('gtx-save-bar [data-region="save-bar"]');
}

/* =====================================================================
 *  Scope tabs
 * ===================================================================== */

/** A single scope tab by scope id (e.g. `"global"` or a form type key like `"andp"`). */
export function findScopeTab(page: Page, scopeId: string): Locator {
    return findScopeTabBar(page).locator(`[data-action="select-scope"][data-id="${scopeId}"]`);
}

export async function clickScopeTab(page: Page, scopeId: string): Promise<void> {
    await test.step(`Switch to scope "${scopeId}"`, async () => {
        await findScopeTab(page, scopeId).click();
        await expect(findScopeTab(page, scopeId)).toHaveAttribute('data-active', 'true');
    });
}

/* =====================================================================
 *  Toolbar (search + filter)
 * ===================================================================== */

export function findSearchInput(page: Page): Locator {
    return findToolbar(page).locator('[data-action="search"]');
}

export async function setSearch(page: Page, term: string): Promise<void> {
    await test.step(`Set search term to "${term}"`, async () => {
        await findSearchInput(page).fill(term);
    });
}

export function findFilterButton(page: Page, filter: 'all' | 'incomplete'): Locator {
    return findToolbar(page).locator(`[data-action="filter"][data-id="${filter}"]`);
}

export async function setFilter(page: Page, filter: 'all' | 'incomplete'): Promise<void> {
    await test.step(`Set filter to "${filter}"`, async () => {
        await findFilterButton(page, filter).click();
        await expect(findFilterButton(page, filter)).toHaveAttribute('data-active', 'true');
    });
}

/* =====================================================================
 *  Translations table
 * ===================================================================== */

/** Row for a given placeholder key. */
export function findTableRow(page: Page, placeholderKey: string): Locator {
    return findTable(page).locator(`tr[data-name="row"][data-id="${placeholderKey}"]`);
}

/** Cell within a row for a specific language. */
export function findTableCell(page: Page, placeholderKey: string, langCode: string): Locator {
    return findTableRow(page, placeholderKey).locator(`td[data-name="cell"][data-lang="${langCode}"]`);
}

export function findCellInput(page: Page, placeholderKey: string, langCode: string): Locator {
    return findTableCell(page, placeholderKey, langCode).locator(
        `input[data-action="edit-cell"][data-key="${placeholderKey}"][data-lang="${langCode}"]`,
    );
}

export type CellState = 'saved' | 'dirty' | 'empty';

/** Read the visual state attribute of a cell. */
export async function getCellState(page: Page, placeholderKey: string, langCode: string): Promise<CellState> {
    const wrapper = findTableCell(page, placeholderKey, langCode).locator('.cell-input-wrapper');
    const state = await wrapper.getAttribute('data-state');
    return (state ?? 'saved') as CellState;
}

export async function fillCell(
    page: Page,
    placeholderKey: string,
    langCode: string,
    value: string,
): Promise<void> {
    await test.step(`Set "${placeholderKey}" [${langCode}] = "${value}"`, async () => {
        await findCellInput(page, placeholderKey, langCode).fill(value);
    });
}

export async function expectCellValue(
    page: Page,
    placeholderKey: string,
    langCode: string,
    value: string,
): Promise<void> {
    await expect(findCellInput(page, placeholderKey, langCode)).toHaveValue(value);
}

export async function expectCellState(
    page: Page,
    placeholderKey: string,
    langCode: string,
    state: CellState,
): Promise<void> {
    await expect(findTableCell(page, placeholderKey, langCode).locator('.cell-input-wrapper'))
        .toHaveAttribute('data-state', state);
}

/* =====================================================================
 *  Save bar
 * ===================================================================== */

export function findSaveButton(page: Page): Locator {
    return findSaveBar(page).locator('[data-action="save"]');
}

export function findDiscardButton(page: Page): Locator {
    return findSaveBar(page).locator('[data-action="discard"]');
}

/** Read the dirty-cell counter displayed in the save bar. */
export async function readDirtyCount(page: Page): Promise<number> {
    const attr = await findSaveBar(page).locator('[data-name="dirty-count"]').getAttribute('data-count');
    return attr === null ? 0 : Number(attr);
}

export async function expectSaveBarVisible(page: Page, visible: boolean): Promise<void> {
    await expect(findSaveBar(page)).toHaveAttribute('data-visible', String(visible));
}

/* =====================================================================
 *  High-level interactions
 * ===================================================================== */

/**
 * Wait until the tool has finished its bootstrap (session check + initial scope load)
 * and the scope tabs are visible. Call this once per test, right after navigation.
 */
export async function waitForToolReady(page: Page): Promise<void> {
    await test.step('Wait for tool ready', async () => {
        await page.locator('[data-name="bootstrap-loading"]').waitFor({ state: 'hidden' });
        await findScopeTabBar(page).waitFor({ state: 'visible' });
    });
}

/**
 * Saves the active scope and waits for the save bar to disappear (which
 * happens once draft == saved again).
 *
 * Accepts a confirmation dialog handler for browsers where `window.confirm`
 * pops up — only used when we expect a confirm (e.g. discard); save itself
 * doesn't confirm. The dialog handler is set globally on the page.
 */
export async function clickSaveAndWait(page: Page): Promise<void> {
    await test.step('Save and wait', async () => {
        await findSaveButton(page).click();
        await expect(findSaveBar(page)).toHaveAttribute('data-visible', 'false', { timeout: 10_000 });
    });
}

/**
 * Clicks "Discard" and accepts the confirmation dialog that the tool shows
 * (the inner `window.confirm`). The dialog auto-acceptor is wired up on the
 * page before clicking.
 */
export async function clickDiscardAndConfirm(page: Page): Promise<void> {
    await test.step('Discard with confirm', async () => {
        page.once('dialog', dialog => void dialog.accept());
        await findDiscardButton(page).click();
        await expect(findSaveBar(page)).toHaveAttribute('data-visible', 'false', { timeout: 5_000 });
    });
}

/**
 * Navigates to the form-translations tool
 *
 * Uses the `ENV_E2E_APP_PATH` env that `createConfiguration` sets up — i.e.
 * the same base URL as for the regular `navigateToApp` helper
 */
export async function navigateToTool(page: Page): Promise<void> {
    await test.step(`Navigate to tool`, async () => {
        let appPath = process.env['E2E_APP_PATH'] ?? '/tools/form-translations/';
        if (appPath === '/') {
            appPath = '';
        } else if (appPath.endsWith('/')) {
            appPath = appPath.slice(0, -1);
        }
        await page.goto(`${appPath}/`);
    });
}

/* =====================================================================
 *  Confirm-dialog helpers
 * ===================================================================== */

/**
 * Register a one-shot handler that accepts the next `window.confirm` dialog.
 * The tool uses native confirms for "discard all changes?" and for switching
 * scopes while dirty.
 */
export function autoAcceptNextConfirm(page: Page): void {
    page.once('dialog', dialog => void dialog.accept());
}

/**
 * Register a one-shot handler that dismisses (i.e. clicks "Cancel" on) the
 * next `window.confirm` dialog.
 */
export function autoDismissNextConfirm(page: Page): void {
    page.once('dialog', dialog => void dialog.dismiss());
}

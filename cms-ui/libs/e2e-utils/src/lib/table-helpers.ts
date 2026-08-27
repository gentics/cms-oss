import test, { expect, Locator, Page } from '@playwright/test';
import { getSourceLocator } from './playwright-helpers';
import { ClickOptions } from './playwright-types';

export function findTableAction(source: Page | Locator, id: string): Locator {
    return source.locator(`.action-column .action-button[data-id="${id}"]`)
        .or(source.locator(`.header-row .action-column .action-button[data-id="${id}"]`))
        .first();
}

/**
 * Finds a table row that contains the specified text
 */
export async function findTableRowByText(source: Page | Locator, text: string, exact: boolean = false): Promise<Locator> {
    source = await getSourceLocator(source, 'gtx-table');
    return source.locator('.data-row .data-column', {
        hasText: exact ? new RegExp(`^[\\s]*${text}[\\s]*$`) : text,
    }).locator('..');
}

/**
 * Finds a table row by its data-id attribute
 */
export async function findTableRowById(source: Page | Locator, id: number | string, disableNavigation: boolean = false): Promise<Locator> {
    source = await getSourceLocator(source, 'gtx-table');
    const row = source.locator(`.data-row[data-id="${id}"]`);

    if (!disableNavigation) {
        await ensureTableRowVisible(source, row);
    } else {
        await expect(row).toBeVisible();
    }

    return row;
}

/**
 * `Locator#isVisible()` ignores its `timeout` option and checks the DOM exactly once,
 * without waiting/polling. That makes it race-prone right after navigation/pagination,
 * when the table may not have finished rendering (or tearing down) yet.
 * This waits/polls properly, like `expect(...).toBeVisible()`, but returns a boolean
 * instead of throwing.
 */
async function isRowVisible(row: Locator, timeout: number): Promise<boolean> {
    try {
        await expect(row).toBeVisible({ timeout });
        return true;
    } catch {
        return false;
    }
}

/**
 * Waits for the table to have finished (re-)loading after a pagination change.
 * @param table The table to check.
 * @param previousFirst The `data-id` of the first row before the pagination change, if any.
 * When provided, this waits until the first row's `data-id` differs from it, to make sure the
 * new page has actually been rendered rather than reading stale data mid-transition.
 * @returns The `data-id` of the (new) first row.
 */
async function tableLoaded(table: Locator, previousFirst?: string | number): Promise<string> {
    if (previousFirst == null) {
        return table.locator('.data-row').first().getAttribute('data-id');
    }

    return table.locator('.data-row').first().filter({
        hasNot: table.locator(`.data-row[data-id="${previousFirst}"]`),
    }).getAttribute('data-id');
}

/**
 * Ensures that the specified table row is visible, navigating through the table's pagination
 * to find it if it isn't visible on the currently displayed page.
 * @param table The table which contains the row.
 * @param row The row to make visible.
 * @param options Options for the visibility checks.
 * @param options.timeout How long to wait for the row to become visible on a given page,
 * before considering it not present there and moving on. Default `2_000`.
 */
export async function ensureTableRowVisible(
    table: Locator,
    row: Locator,
    options: { timeout?: number } = { timeout: 2_000 },
): Promise<void> {
    let rowVisible = await isRowVisible(row, options.timeout ?? 2_000);
    if (rowVisible) {
        return;
    }

    await test.step('Looking for table row in pagination', async () => {
        const pagination = table.locator('.table-pagination');
        // We need the pagination to be available
        await expect(pagination).toBeVisible();

        // In case that we aren't on the first page, we should move there and then iterate
        // through all the pages until we find our row.
        const startPage = parseInt(await pagination.getAttribute('data-current'), 10);
        const lastPage = parseInt(await pagination.getAttribute('data-last'), 10);
        let previousFirst: string;

        if (startPage !== 1) {
            await pagination.locator('[data-action="change-page"][data-page="1"]').click();
            previousFirst = await tableLoaded(table, previousFirst);
            rowVisible = await isRowVisible(row, options.timeout ?? 2_000);
            if (rowVisible) {
                return;
            }
        } else {
            // Still need to get the initial first row id for later checks
            previousFirst = await tableLoaded(table, previousFirst);
        }

        let currentPage = 1;
        while ((await pagination.getAttribute('data-has-next')) === 'true') {
            await pagination.locator('[data-action="next"]').click();
            currentPage++;
            // Wait for load to happen
            previousFirst = await tableLoaded(table, previousFirst);

            // We only need to do a manual check for pages which aren't the last one.
            // On the last page, we use the expect below to make it fail if it's still
            // not visible on the last page.
            rowVisible = await isRowVisible(row, options.timeout ?? 2_000);
            if (currentPage < lastPage && rowVisible) {
                return;
            }
        }

        await expect(row).toBeVisible();
    });
}

export function clickTableRow(row: Locator, options?: ClickOptions): Promise<void> {
    return row.locator('.data-column.clickable').first().click(options);
}

export async function selectTableRow(row: Locator): Promise<void> {
    return row.locator('.select-column gtx-checkbox label').click();
}

/**
 * Expands a trable row (tree-table row)
 */
export async function expandTrableRow(row: Locator): Promise<void> {
    await row.locator('.row-expansion').click();
}

/**
 * Finds a trable row by its data-id attribute
 */
export function findTrableRowById(source: Page | Locator, id: number | string): Locator {
    return source.locator(`gtx-trable .data-row[data-id="${id}"]`);
}

/**
 * Finds a trable row by its text content
 */
export function findTrableRowByText(source: Page | Locator, text: string, exact: boolean = false): Locator {
    return source.locator('gtx-trable .data-row .data-column', {
        hasText: exact ? new RegExp(`^[\\s]*${text}[\\s]*$`) : text,
    }).locator('..');
}

export async function selectTrableRow(row: Locator): Promise<void> {
    return row.locator('.select-column .selection-checkbox  label, .inline-selection .selection-checkbox label').click();
}

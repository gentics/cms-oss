import { Locator, Page } from '@playwright/test';
import { ClickOptions } from './playwright-types';

export function findTableRow(source: Page | Locator, id: string | number): Locator {
    return source.locator(`gtx-table .data-row[data-id="${id}"]`);
}

export function findTableAction(source: Page | Locator, id: string): Locator {
    return source.locator(`.action-column .action-button[data-id="${id}"], .header-row .action-column .action-button[data-id="${id}"]`);
}

/**
 * Finds a table row that contains the specified text
 */
export function findTableRowByText(source: Page | Locator, text: string): Locator {
    return source.locator('gtx-table .data-row').filter({ hasText: text });
}

/**
 * Finds a table row by its data-id attribute
 */
export function findTableRowById(source: Page | Locator, id: number | string): Locator {
    return source.locator(`gtx-table .data-row[data-id="${id}"]`);
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
    await row.locator('.row-expansion-wrapper').click();
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
export function findTrableRowByText(source: Page | Locator, text: string): Locator {
    return source.locator('gtx-trable .data-row').filter({ hasText: text });
}

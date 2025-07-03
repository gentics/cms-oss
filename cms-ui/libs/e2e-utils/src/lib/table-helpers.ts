import { Locator, Page } from '@playwright/test';
import { ClickOptions } from './playwright-types';

export function findTableAction(source: Page | Locator, id: string): Locator {
    return source.locator(`.action-column .action-button[data-id="${id}"]`)
        .or(source.locator(`.header-row .action-column .action-button[data-id="${id}"]`))
        .first();
}

/**
 * Finds a table row that contains the specified text
 */
export function findTableRowByText(source: Page | Locator, text: string, exact: boolean = false): Locator {
    return source.locator('gtx-table .data-row .data-column', {
        hasText: exact ? new RegExp(`^[\\s]*${text}[\\s]*$`) : text,
    }).locator('..');
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

export async function selectTab(source: Page, id: number | string): Promise<void> {
    return source.locator(`gtx-tabs .tab-link[data-id="${id}"]`).click();
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

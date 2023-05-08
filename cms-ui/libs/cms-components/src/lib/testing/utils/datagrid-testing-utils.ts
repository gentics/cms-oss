import { DebugElement } from '@angular/core';

/**
 * @description Click checkboxes in DevExtreme component dxDataGrid
 * @param el fixture.debugElement containing dxDataGrid markup
 * @param rowIndices of rows whose selecting checkboxes shall be clicked
 */
export function checkBoxesClick(el: DebugElement, rowIndices: number[]): void {
    const checkBoxes: NodeList = el.nativeElement.querySelectorAll('.dx-datagrid-rowsview .dx-command-select');
    checkBoxes.forEach((element: HTMLElement, index: number) => rowIndices.includes(index) && element.click());
}

/**
 * @param el fixture.debugElement containing dxDataGrid markup
 * @returns Nodelist of CSS-selected paging buttons (only 5 page buttons exist and one is selected as active)
 */
export function getPageIndices(el: DebugElement): NodeList {
    return el.nativeElement.querySelectorAll('.dx-pages .dx-page');
}

/**
 * @param el fixture.debugElement containing dxDataGrid markup
 * @returns page number of currently selected page (page numbers start with 1 unlike the API page index)
 */
export function getSelectedPageIndex(el: DebugElement): number {
    return parseInt(el.nativeElement.querySelectorAll('.dx-pages .dx-page.dx-selection')[0].textContent, 10);
}

/**
 * @description Click one of the paging button to choose a page as active
 * @param el fixture.debugElement containing dxDataGrid markup
 * @param pageIndex (only 5 page buttons exist and one is selected as active)
 */
export function clickPageIndex(el: DebugElement, pageIndex: 0 | 1 | 2 | 3 | 4 ): void {
    getPageIndices(el).forEach((pageButton: HTMLElement, i: number) => {
        if (i === pageIndex) {
            pageButton.click();
        }
    });
}

/**
 * @param el fixture.debugElement containing dxDataGrid markup
 * @returns a string array containing the currently visible column header titles
 */
export function getVisibleColumnsHeaderTitles(el: DebugElement): string[] {
    const retVal = [];
    el.nativeElement.querySelectorAll('.dx-datagrid-text-content').forEach((c: HTMLElement) => {
        if (c.textContent !== '') {
            retVal.push(c.textContent);
        }
    });
    return retVal;
}

/**
 * @param el fixture.debugElement containing dxDataGrid markup
 * @returns Nodelist of CSS-selected data rows
 */
export function getDataRows(el: DebugElement): NodeList {
    return el.nativeElement.querySelectorAll('.dx-datagrid-table .dx-row.dx-data-row');
}

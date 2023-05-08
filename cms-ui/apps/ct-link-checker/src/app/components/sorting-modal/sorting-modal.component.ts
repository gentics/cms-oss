import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { IModalDialog } from '@gentics/ui-core';

import { Page, PagingSortOption, PagingSortOrder, SortField } from '@gentics/cms-models';

/**
 * A dialog used to select the sorting field and direction for a given type.
 */
@Component({
    selector: 'sorting-modal',
    templateUrl: './sorting-modal.tpl.html',
    styleUrls: ['./sorting-modal.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SortingModalComponent implements IModalDialog, OnInit {

    sortOptions: PagingSortOption<Page>[];
    availableFields: SortField[];

    constructor() {}

    ngOnInit(): void {
        this.availableFields = ['name' , 'cdate' , 'edate', 'pdate', 'filename' , 'template' , 'priority'];
    }

    toggleSort(field: keyof Page): void {
        const sortOrder = this.sortOptions.find(s => s.attribute === field);
        const currentOrder = sortOrder || { attribute: field };

        switch (currentOrder.sortOrder) {
            case PagingSortOrder.Asc:
                currentOrder.sortOrder = PagingSortOrder.Desc;
                break;
            case PagingSortOrder.Desc:
                currentOrder.sortOrder = PagingSortOrder.None;
                break;
            case PagingSortOrder.None:
            default:
                currentOrder.sortOrder = PagingSortOrder.Asc;
                break;
        }

        if (!sortOrder) {
            this.sortOptions.push(currentOrder);
        }

        this.sortOptions = this.sortOptions.filter(s => s.sortOrder !== PagingSortOrder.None);
    }

    resetOrder(): void {
        this.sortOptions = [];
    }

    getOrder(field: keyof Page): number {
        return this.sortOptions.findIndex(s => s.attribute === field) + 1;
    }

    isSortedBy(field: keyof Page): PagingSortOrder {
        const fieldOrder = this.sortOptions.filter(s => s.sortOrder !== PagingSortOrder.None).find(s => s.attribute === field);
        return fieldOrder && fieldOrder.sortOrder || PagingSortOrder.None;
    }

    isSortedByString(field: keyof Page): string {
        const sortedBy = this.isSortedBy(field);

        switch (sortedBy) {
            case PagingSortOrder.Desc:
                return 'desc';
            case PagingSortOrder.Asc:
                return 'asc';
            default:
                return '';
        }
    }

    setSort(): void {
        this.closeFn(this.sortOptions.filter(s => s.sortOrder !== PagingSortOrder.None));
    }

    closeFn(val: any): void { }
    cancelFn(val?: any): void { }

    registerCloseFn(close: (val: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }
}

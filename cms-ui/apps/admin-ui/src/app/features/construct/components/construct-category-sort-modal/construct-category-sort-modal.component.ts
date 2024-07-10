import { BO_ID, ConstructCategoryBO, TableLoadEndEvent, TableSortEvent, sortEntityRow } from '@admin-ui/common';
import { ConstructCategoryHandlerService } from '@admin-ui/core/providers/construct-category-handler/construct-category-handler.service';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { BaseModal, TableRow } from '@gentics/ui-core';

@Component({
    selector: 'gtx-construct-category-sort-modal',
    templateUrl: './construct-category-sort-modal.component.html',
    styleUrls: ['./construct-category-sort-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructCategorySortModal extends BaseModal<boolean> {

    public rows: TableRow<ConstructCategoryBO>[] = [];
    public disabled = false;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected handler: ConstructCategoryHandlerService,
    ) {
        super();
    }

    public rowsLoaded(event: TableLoadEndEvent<ConstructCategoryBO>): void {
        this.rows = event.rows;
    }

    public sortRows(event: TableSortEvent<ConstructCategoryBO>): void {
        this.rows = sortEntityRow(this.rows, event.from, event.to);
    }

    async updateSorting(): Promise<void> {
        this.disabled = true;
        this.changeDetector.markForCheck();

        try {
            await this.handler.sort(this.rows.map(row => row.item[BO_ID])).toPromise();
            this.closeFn(true);
        } catch (error) {
            this.disabled = false;
            this.changeDetector.markForCheck();
        }
    }
}

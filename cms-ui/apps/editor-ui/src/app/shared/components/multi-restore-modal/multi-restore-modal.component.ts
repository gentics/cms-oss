import { Component, OnDestroy } from '@angular/core';
import { File, Folder, Form, Image, ItemType, Page, SortField } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { Subscription } from 'rxjs';

export interface LanguageVariantMap {
    [pageId: number]: Page[];
}

/**
 * A modal that lets the user choose actions when deleting inherited/localized files
 */
@Component({
    selector: 'multi-restore-modal',
    templateUrl: './multi-restore-modal.tpl.html',
    styleUrls: ['./multi-restore-modal.scss'],
    standalone: false
})
export class MultiRestoreModalComponent implements IModalDialog, OnDestroy {
    files: File[] = [];
    folders: Folder[] = [];
    forms: Form[] = [];
    images: Image[] = [];
    pages: Page[] = [];

    filterTerm = '';
    sortBy: SortField;
    sortOrder: 'asc' | 'desc';
    selection: { [type: string]: number[] } = {};
    private subscription: Subscription;

    ngOnDestroy(): void {
        if (this.subscription) {
            this.subscription.unsubscribe();
        }
    }

    /**
     * Returns true is at least one item is selected.
     */
    itemsSelected(): boolean {
        return this.selectionCount() > 0;
    }

    /**
     * Returns the number of selected items.
     */
    selectionCount(): number {
        const selection = Object.keys(this.selection)
            .reduce((selection: number[], type: string) => selection.concat(this.selection[type]), []);
        return selection.length;
    }

    /**
     * Returns type of items to restore. There can be only one.
     */
    selectionType(): ItemType {
        return [
            this.files,
            this.folders,
            this.forms,
            this.images,
            this.pages,
        ].find((entities) => entities.length > 0)[0].type;
    }

    selectionChanged(type: ItemType, selection: number[]): void {
        this.selection[type] = selection;
    }

    restoreSelected(): void {
        this.closeFn(this.selection);
    }

    closeFn(val?: { [type: string]: number[] }): void { }
    cancelFn(val?: any): void {}

    registerCloseFn(close: (val: { [type: string]: number[] }) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }

}

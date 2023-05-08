import { Component, OnInit } from '@angular/core';
import { File as FileModel } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { iconForItemType } from '../../../common/utils/icon-for-item-type';

/**
 * A modal for the user to
 */
@Component({
    selector: 'file-name-conflict-modal',
    templateUrl: './file-name-conflict-modal.tpl.html',
    styleUrls: ['./file-name-conflict-modal.scss']
})
export class FileNameConflictModal implements IModalDialog, OnInit {
    closeFn: (files: FileModel[]) => void;
    cancelFn: (val?: any) => void;

    conflictingFiles: FileModel[] = [];
    totalFileCount = 0;
    selected: { [key: string]: boolean } = {};
    iconForItemType = iconForItemType;

    constructor() { }

    ngOnInit(): void {
        // set all files to checked by default
        this.conflictingFiles.forEach(file => this.toggleSelection(file, true));
    }

    isSelected(item: FileModel): boolean {
        return !!this.selected[this.getKey(item)];
    }

    areAllSelected(): boolean {
        return this.selectedCount() === this.conflictingFiles.length;
    }

    toggleSelection(item: FileModel, checked: boolean): void {
        this.selected[this.getKey(item)] = checked;
    }

    toggleSelectAll(): void {
        if (this.areAllSelected()) {
            this.deselectAll();
        } else {
            this.selectAll();
        }
    }

    selectAll(): void {
        this.conflictingFiles.forEach(file => this.toggleSelection(file, true));
    }

    deselectAll(): void {
        this.conflictingFiles.forEach(file => this.toggleSelection(file, false));
    }

    selectedCount(): number {
        return Object.keys(this.selected).filter(key => !!this.selected[key]).length;
    }

    getConfirmButtonType(): string {
        return 0 < this.selectedCount() ? 'warning' : 'primary';
    }

    confirm(): void {
        let filesToReplace = this.conflictingFiles.filter(file => this.isSelected(file));
        this.closeFn(filesToReplace);
    }

    getKey(item: FileModel): string {
        return `${item.type}_${item.id}`;
    }

    registerCloseFn(close: (val: FileModel[]) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }
}

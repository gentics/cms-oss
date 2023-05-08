import { Component } from '@angular/core';
import { IModalDialog } from '@gentics/ui-core';
import { InheritableItem, ItemType } from '@gentics/cms-models';
import { LanguageVariantMap } from '../../../common/models';
import { iconForItemType } from '../../../common/utils/icon-for-item-type';

export interface MultiPagesOfflineResult {
    delete: InheritableItem[];
    unlocalize: InheritableItem[];
}

/**
 * A dialog used to indicate which variants of a page's languages to take offline.
 * When closed, the dialog promise will resolve to an array of page ids to be taken offline.
 */
@Component({
    selector: 'take-pages-offline-modal',
    templateUrl: './take-pages-offline-modal.tpl.html',
    styleUrls: ['./take-pages-offline-modal.scss']
})
export class TakePagesOfflineModal implements IModalDialog {

    closeFn: (idsToDelete: number[]) => void;
    cancelFn: (val?: any) => void;

    // Should be passed in by the function which creates the modal
    pagesToTakeOffline: InheritableItem[];
    pageLanguageVariants: LanguageVariantMap;

    itemType: ItemType = 'page';
    selectedLanguageVariants: { [pageId: number]: number[] } = {};
    iconForItemType = iconForItemType;

    get deleteCount(): number {
        return this.flattenMap(this.selectedLanguageVariants).length;
    }

    constructor() {}

    ngOnInit(): void {
        this.pagesToTakeOffline.forEach(item => {
            this.selectedLanguageVariants[item.id] = [item.id];
        });
    }

    confirm(): void {
        const idsToTakeOffline = this.flattenMap(this.selectedLanguageVariants);
        this.closeFn(idsToTakeOffline);
    }

    /**
     * Handles changes to the language variants selection for pages.
     */
    onLanguageSelectionChange(itemId: number, variantIds: number[], checkLocalizations: boolean = false): void {
        this.selectedLanguageVariants[itemId] = variantIds;
    }

    registerCloseFn(close: (idsToDelete: number[]) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }

    /**
     * Given a map of { id: T[] }, flattens it into an array of T.
     */
    private flattenMap<T>(hashMap: { [id: number]: T[] }): T[] {
        return Object.keys(hashMap).reduce((all, id) => all.concat(hashMap[+id]), []);
    }
}

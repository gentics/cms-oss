import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Feature, Form, InheritableItem, ItemType, Page } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { itemIsLocalized } from '../../../common/utils/item-is-localized';
import { LocalizationInfo, LocalizationMap, LocalizationsService } from '../../../core/providers/localizations/localizations.service';
import { ApplicationStateService } from '@editor-ui/app/state';

export interface MultiDeleteResult {
    delete: InheritableItem[];
    deleteForms: FormLanguageVariantMap;
    unlocalize: InheritableItem[];
    localizations: LocalizationMap; // LocalizationInfo[]; //
}

export interface PageLanguageVariantMap {
    [pageId: number]: Page[];
}

export interface FormLanguageVariantMap {
    [formId: number]: string[];
}

/**
 * A modal that lets the user choose actions when deleting inherited/localized files
 */
@Component({
    selector: 'multi-delete-modal-modal',
    templateUrl: './multi-delete-modal.component.html',
    styleUrls: ['./multi-delete-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MultiDeleteModal extends BaseModal<MultiDeleteResult> implements OnInit, OnDestroy {

    // Should be passed in by the function which creates the modal
    inheritedItems: InheritableItem[];
    localizedItems: InheritableItem[];
    otherItems: InheritableItem[];
    pageLanguageVariants: PageLanguageVariantMap;
    formLanguageVariants: FormLanguageVariantMap;
    itemLocalizations: LocalizationMap;

    itemType: ItemType;

    /** Meaning: `{ pageId: languageVariantPageId[] }` */
    selectedPageLanguageVariants: { [pageId: number]: number[] } = {};

    /** Meaning: `{ formId: languageCodesOfForm[] }` */
    selectedFormLanguageVariants: { [formId: number]: string[] } = {};

    deleteCount: number;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private localizationService: LocalizationsService,
        private appState: ApplicationStateService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.itemType = this.getType();

        [...this.otherItems, ...this.localizedItems].forEach(item => {
            if (this.itemType === 'page') {
                this.selectedPageLanguageVariants[item.id] = [item.id];
            }
            if (this.itemType === 'form') {
                this.selectedFormLanguageVariants[item.id] = (item as Form).languages;
            }
        });

        if (this.itemType === 'page') {
            this.deleteCount = this.flattenMap(this.selectedPageLanguageVariants).length;
        } else if (this.itemType === 'form') {
            this.deleteCount = this.flattenMap(this.selectedFormLanguageVariants).length;
        } else {
            this.deleteCount = this.otherItems.length + this.localizedItems.length;
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    confirm(): void {
        let itemsToDelete: InheritableItem[] = [];
        let itemsToUnlocalize: InheritableItem[] = [];

        if (this.itemType === 'page') {
            const flatVariants = this.flattenMap(this.pageLanguageVariants);
            const flatSelection = this.flattenMap(this.selectedPageLanguageVariants);
            const allSelectedItems = flatVariants.filter(item => -1 < flatSelection.indexOf(item.id));

            itemsToDelete = allSelectedItems.filter(page => !itemIsLocalized(page));
            itemsToUnlocalize = allSelectedItems.filter(itemIsLocalized);
        } else {
            itemsToDelete = this.otherItems;
            itemsToUnlocalize = this.localizedItems;
        }

        this.closeFn({
            delete: itemsToDelete,
            deleteForms: this.selectedFormLanguageVariants,
            unlocalize: itemsToUnlocalize,
            localizations: this.itemLocalizations,
        });
    }

    /**
     * Handles changes to the language variants selection for pages. When `checkLocalizations` is true, we also
     * check that we have the needed localization info for any newly-selected language variants, and if not we
     * get it from the server.
     */
    onPageLanguageSelectionChange(itemId: number, variantIds: number[], checkLocalizations: boolean = false): void {
        if (!checkLocalizations || !this.appState.now.features[Feature.MULTICHANNELLING]) {
            this.selectedPageLanguageVariants[itemId] = variantIds;
            return;
        }
        const uncheckedIds = this.getUncheckedLocalizationIds(variantIds);
        if (uncheckedIds.length < 1) {
            this.selectedPageLanguageVariants[itemId] = variantIds;
            return;
        }

        this.subscriptions.push(this.localizationService.getLocalizationMap(uncheckedIds, this.itemType).pipe(
            take(1),
        ).subscribe(newMap => {
            Object.assign(this.itemLocalizations, newMap);
            this.selectedPageLanguageVariants[itemId] = variantIds;
            this.changeDetector.markForCheck();
        }));
    }

    onFormLanguageSelectionChange(itemId: number, languageCodes: string[]): void {
        this.selectedFormLanguageVariants[itemId] = languageCodes;
    }

    /**
     * Returns an array of localization info arrays which correspond to the given item plus any language
     * variants (in the case of page items) which are selected.
     */
    getAllLocalizations(itemId: number): LocalizationInfo[] {
        if (Object.keys(this.itemLocalizations).length === 0) {
            return [];
        }
        if (this.itemType === 'form') {
            return [];
        }
        const selectedVariants = this.selectedPageLanguageVariants[itemId];

        if (Array.isArray(selectedVariants) && selectedVariants.length > 0) {
            const allLocalizations = selectedVariants.map(itemId => this.itemLocalizations[itemId]);
            return this.flattenArray(allLocalizations);
        } else {
            return [];
        }
    }

    isNoneSelected(item: InheritableItem): boolean {
        const a = this.selectedPageLanguageVariants[item.id]?.length === 0;
        const b = this.selectedFormLanguageVariants[item.id]?.length === 0;
        return a || b;
    }

    /**
     * Given a map of { id: T[] }, flattens it into an array of T.
     */
    private flattenMap<T>(hashMap: { [id: number]: T[] }): T[] {
        return Object.keys(hashMap).reduce((all, id) => all.concat(hashMap[+id]), []);
    }

    /**
     * Flattens a 2d array into a simple array.
     */
    private flattenArray<T>(arr: T[][]): T[] {
        return arr.reduce((flattened, current) => flattened.concat(current), []);
    }

    private getUncheckedLocalizationIds(itemIds: number[]): number[] {
        const knownIds = Object.keys(this.itemLocalizations).map(id => +id);
        return itemIds.filter(id => knownIds.indexOf(id) === -1);
    }

    /**
     * Get the item type which is being deleted.
     */
    private getType(): ItemType {
        if (this.otherItems.length > 0) {
            return this.otherItems[0].type;
        }
        if (this.localizedItems.length > 0) {
            return this.localizedItems[0].type;
        }
        if (this.inheritedItems.length > 0) {
            return this.inheritedItems[0].type;
        }
    }
}

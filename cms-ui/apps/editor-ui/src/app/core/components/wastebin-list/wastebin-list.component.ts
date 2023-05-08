import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { InheritableItem, Item, ItemType, Language, Normalized, Page } from '@gentics/cms-models';
import { PaginationService } from 'ngx-pagination';
import { iconForItemType } from '../../../common/utils/icon-for-item-type';
import { EntityStateUtil } from '../../../shared/util/entity-states';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';

function filterBy(filter: string, item: InheritableItem, prop: string): boolean {
    let val: string = (<any> item)[prop];
    if (!val) {
        return false;
    }
    return val.toLowerCase().indexOf(filter.toLowerCase()) > -1;
}

@Component({
    selector: 'wastebin-list',
    templateUrl: './wastebin-list.component.html',
    styleUrls: ['./wastebin-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [PaginationService]
})
export class WastebinList implements OnChanges, OnInit {

    @Input()
    items: InheritableItem<Normalized>[];

    @Input()
    type: ItemType;

    @Input()
    filter = '';

    @Input()
    selectedItems: number[] = [];

    @Output()
    selectionChange = new EventEmitter<number[]>();

    currentPage = 1;
    listSize = 5;
    itemsPerPage = 5;
    filteredItems: InheritableItem[] = [];

    selected: { [id: number]: boolean } = {};
    icon: string;

    constructor(
        private entityResolver: EntityResolver,
    ) {}

    ngOnInit(): void {
        this.icon = iconForItemType(this.type);
        this.filteredItems = this.items;
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['filter'] || changes['items']) {
            this.updateDispalyItems();
        }

        if (changes['selectedItems']) {
            this.selected = {};
            if (this.selectedItems) {
                this.selectedItems.forEach(id => {
                    this.selected[id] = true;
                });
            }
        }
    }

    updateDispalyItems(): void {
        if (this.items && this.filter !== '') {
            this.filteredItems = this.items
                .filter(item => item != null)
                .filter(item => filterBy(this.filter, item, 'name') || filterBy(this.filter, item, 'fileName'));
        } else {
            this.filteredItems = this.items
                .filter(item => item != null);
        }
    }

    toggleShowAll(): void {
        if (this.itemsPerPage === this.listSize) {
            this.itemsPerPage = Number.MAX_SAFE_INTEGER;
            this.currentPage = 1;
        } else {
            this.itemsPerPage = this.listSize;
        }
    }

    /**
     * Because we group pages, in order to get an accurate count, we need to look inside the
     * languageVariants object. If no languageVariants, just add one per item.
     */
    getItemCount(): number {
        return this.getAllIds().length;
    }

    /**
     * Returns an array of all the visible item ids, accounting for filtering and page language variants.
     */
    private getAllIds(): number[] {
        if (this.type !== 'page') {
            return this.filteredItems.map(item => item.id);
        }

        return this.filteredItems.reduce((allPages: number[], page: Page<Normalized>) => {
            if (page == null) {
                return allPages;
            }

            let variantIds =  Object.keys(page.languageVariants || {}).map(lang => page.languageVariants[+lang]);
            let idsToAdd = 0 < variantIds.length ? variantIds : [page.id];
            return allPages.concat(idsToAdd);
        }, []);
    }

    getDeletedByName(item: InheritableItem): string {
        let user = item.deleted.by;
        return `${user.firstName} ${user.lastName}`;
    }

    /**
     * Returns true if the given item is selected.
     */
    isSelected(item: InheritableItem<Normalized>): boolean {
        if (item.type === 'page') {
            // for pages, we consider the row selected if any of its language variants are selected
            let variants = this.getVariants(item as Page<Normalized>);
            return 0 < variants.filter(page => this.selected[page.id] === true).length;
        } else {
            return this.selected[item.id] === true;
        }
    }

    /**
     * @returns TRUE if item has been deleted and is in wastebin
     */
    isDeleted(item: Item): boolean {
        return EntityStateUtil.stateDeleted(item);
    }

    /**
     * Returns true if all items in the list are selected (including all language variants for pages)
     */
    allSelected(): boolean {
        if (!this.selectedItems || this.selectedItems.length === 0) {
            return false;
        }
        return this.selectedItems.length === this.getItemCount();
    }

    toggleSelectAll(selected: boolean): void {
        if (selected === false) {
            this.selectionChange.emit([]);
        } else {
            this.selectionChange.emit(this.getAllIds());
        }
    }

    toggleSelection(item: InheritableItem<Normalized>, selected: boolean): void {
        let ids: number[] = [];
        const deletableVariantIds = this.items.filter(item => this.isDeleted(item)).map(item => item.id);
        if (item.type === 'page') {
            // for pages, we need to toggle the selected state of all of its language variants
            ids = this.getVariants(item as Page<Normalized>)
                .filter(item => this.isDeleted(item))
                .map(item => item.id);
        } else {
            ids = [item.id];
        }
        this.updateSelection(ids, selected);
    }

    toggleVariantSelection(variant: Page, selected: boolean): void {
        this.updateSelection([variant.id], selected);
    }

    updateSelection(ids: number[], selected: boolean): void {
        let selectedItems = this.selectedItems || [];
        let newSelection: number[] = [];

        if (selected === true) {
            newSelection = selectedItems.concat(ids);
        } else {
            newSelection = selectedItems.slice();
            ids.forEach(id => newSelection.splice(newSelection.indexOf(id), 1));
        }
        this.selectionChange.emit(newSelection);
    }

    /**
     * Given a page, return an array containing all its language variants.
     */
    getVariants(page: Page<Normalized>): Page<Normalized>[] {
        if (!page.languageVariants || Object.keys(page.languageVariants).length === 0) {
            return [page];
        }
        return Object.keys(page.languageVariants)
            .map(languageId => this.entityResolver.getPage(page.languageVariants[+languageId]));
    }

    getLanguages(page: Page): Language[] {
        if (page.type !== 'page' || !page.languageVariants) {
            return [];
        }
        return Object.keys(page.languageVariants).map(id => this.entityResolver.getLanguage(+id));
    }
}

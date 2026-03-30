/* eslint-disable @typescript-eslint/naming-convention */
import { ChangeDetectorRef, Component, computed, input, model, OnChanges, OnDestroy, OnInit, signal } from '@angular/core';
import { Item, Language, Node, StagedItemsMap } from '@gentics/cms-models';
import { ChangesOf } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';
import { emptyItemInfo, FolderPermissionData, ItemsInfo, UIMode } from '../../../common/models';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { ApplicationStateService } from '../../../state';

export interface ItemLoadData<T> {
    items: T[];
    totalCount: number;
}

@Component({
    template: '',
    standalone: false,
})
export abstract class BaseItemListComponent<T extends Item> implements OnInit, OnChanges, OnDestroy {

    // Navigation
    public readonly folderId = input.required<number>();
    public readonly node = input.required<Node>();
    public readonly nodeLanguages = input.required<Language[]>();

    // Selection handling
    public readonly selectable = input<boolean>();
    public readonly selection = model<number[]>();

    // Misc inputs
    public readonly activeItemId = input<number>();
    public readonly startPageId = input<number>();
    public readonly acceptUploads = input<boolean>();
    public readonly permissions = input<FolderPermissionData>();
    public readonly uiMode = input<UIMode>();

    // Loading state
    public readonly loadingItems = model<boolean>();

    // Pagination
    public totalCount: number;
    public pageSize = 10;
    public currentPage = 1;
    public items: T[] = [];

    // Misc state
    public activeLanguage: Language;
    /**
     * Items which have previously been loaded once.
     * Kept in here to be able to reference these with selected ids.
     */
    protected readonly cachedItems = signal<Record<number, T>>({});
    /** Array of selected items. Is updated/handled via selectable */
    public readonly selectedItems = computed(() => {
        const cache = this.cachedItems();
        return this.selection().map((id) => cache[id]);
    });

    /** Compatibility object for older components which still use this format */
    public itemsInfo: ItemsInfo = {
        ...emptyItemInfo,
    };

    // Mappings from global state
    public showAllLanguages: boolean;
    public showStatusIcons: boolean;
    public showDeleted: boolean;
    public searchTerm: string;
    public elasticSearchQueryActive: boolean;

    public stagingMap: StagedItemsMap = {};

    // Internal data handling
    protected subscriptions: Subscription[] = [];
    protected pageLoadSubscription: Subscription | null = null;

    constructor(
        public changeDetector: ChangeDetectorRef,
        public errorHandler: ErrorHandler,
        public appState: ApplicationStateService,
    ) {}

    public abstract loadItems(
        folderId: number,
        nodeId: number,
        page: number,
        pageSize: number,
    ): Observable<ItemLoadData<T>>;

    public ngOnInit(): void {
        this.subscriptions.push(this.appState.select((state) => state.folder.displayStatusIcons).subscribe((show) => {
            this.showStatusIcons = show;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select((state) => state.folder.displayDeleted).subscribe((show) => {
            this.showDeleted = show;
            this.reload();
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select((state) => state.folder.displayAllLanguages).subscribe((show) => {
            this.showAllLanguages = show;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select((state) => state.folder.searchTerm).subscribe((term) => {
            this.searchTerm = term;
            this.reload();
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select((state) => state.folder.searchFiltersVisible).subscribe((active) => {
            this.elasticSearchQueryActive = active;
            this.changeDetector.markForCheck();
        }));
    }

    public ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.folderId || changes.node || changes.showDeleted) {
            this.reload();
        }
    }

    public ngOnDestroy(): void {
        this.subscriptions.forEach((s) => s.unsubscribe());
        if (this.pageLoadSubscription != null) {
            this.pageLoadSubscription.unsubscribe();
        }
    }

    protected loadPageWith(
        page: number,
        pageSize: number,
    ): void {
        if (this.pageLoadSubscription != null) {
            this.pageLoadSubscription.unsubscribe();
        }

        this.loadingItems.set(true);
        this.updateItemsInfo();

        this.pageLoadSubscription = this.loadItems(this.folderId(), this.node().id, page, pageSize).subscribe({
            next: (data) => {
                this.currentPage = page;
                this.pageSize = pageSize;
                this.totalCount = data.totalCount;
                this.items = data.items;

                // Add items to cache
                const cache = this.cachedItems();
                for (const singleItem of this.items) {
                    cache[singleItem.id] = singleItem;
                }
                this.cachedItems.set(cache);

                this.loadingItems.set(false);
                this.updateItemsInfo();

                this.changeDetector.markForCheck();
            },
            error: (err) => {
                this.errorHandler.catch(err);
                this.loadingItems.set(false);
                this.updateItemsInfo();

                this.changeDetector.markForCheck();
            },
        });
    }

    protected updateItemsInfo(): void {
        this.itemsInfo = {
            ...emptyItemInfo,
            fetchAll: this.loadingItems(),
            fetching: this.loadingItems(),
            currentPage: this.currentPage,
            itemsPerPage: this.pageSize,
            hasMore: Math.ceil(this.totalCount / this.pageSize) > this.currentPage,
            list: this.items.map((item) => item.id),
            selected: this.selection(),
            total: this.totalCount,
        };
    }

    public reload(): void {
        this.loadPageWith(this.currentPage, this.pageSize);
    }

    public loadPage(page: number): void {
        this.loadPageWith(page, this.pageSize);
    }
}

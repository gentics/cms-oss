import { ChangeDetectorRef, Component, input, model, OnChanges, OnDestroy, OnInit } from '@angular/core';
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
    public folderId = input.required<number>();
    public node = input.required<Node>();
    public nodeLanguages = input.required<Language[]>();

    // Selection handling
    public selectable = input<boolean>();
    public selection = model<number[]>();

    // Misc inputs
    public activeItemId = input<number>();
    public startPageId = input<number>();
    public permissions = input<FolderPermissionData>();
    public uiMode = input<UIMode>();

    // Loading state
    public loadingItems = model<boolean>();

    // Pagination
    public totalCount: number;
    public pageSize = 10;
    public currentPage = 1;
    public items: T[] = [];

    // Misc state
    public activeLanguage: Language;

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
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select((state) => state.folder.displayAllLanguages).subscribe((show) => {
            this.showAllLanguages = show;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select((state) => state.folder.searchTerm).subscribe((term) => {
            this.searchTerm = term;
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

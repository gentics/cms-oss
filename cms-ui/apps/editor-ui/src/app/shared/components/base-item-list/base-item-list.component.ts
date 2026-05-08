import { ChangeDetectorRef, Component, computed, input, model, OnChanges, OnDestroy, OnInit, signal } from '@angular/core';
import { discard } from '@gentics/cms-components';
import { Feature, Item, Language, Node, PagingSortOrder, StagedItemsMap } from '@gentics/cms-models';
import { ChangesOf, randomId, toValidNumber } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { catchError, debounceTime, distinctUntilChanged, Observable, of, Subject, Subscription, switchMap, tap } from 'rxjs';
import { emptyItemInfo, FolderPermissionData, ItemLoadData, ItemsInfo, UIMode } from '../../../common/models';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { ApplicationStateService } from '../../../state';

@Component({
    template: '',
    standalone: false,
})
export abstract class BaseItemListComponent<T extends Item> implements OnInit, OnChanges, OnDestroy {

    public readonly UNIQUE_ID = `item-list-${randomId()}`;

    /* Navigation
     * ===================================================================== */

    /** The current folder id where this list is being displayed in */
    public readonly folderId = input.required<number>();
    /** The current node in where this list is being displayed in */
    public readonly node = input.required<Node>();
    /** Languages of the current node */
    public readonly nodeLanguages = input.required<Language[]>();

    /* Selection Handling
     * ===================================================================== */

    /** If this lists items can be selected */
    public readonly selectable = input<boolean>(true);
    /** A set of item IDs which are marked as selected */
    public readonly selection = model<Set<number>>(new Set());
    /** All currently selected items from this list */
    public readonly selectedItems = computed(() => {
        const cache = this.cachedItems();
        return Array.from(this.selection()).map((id) => cache[id]);
    });

    /* Misc Inputs
     * ===================================================================== */

    /** The currently active item, which should be highlighted in the list. */
    public readonly activeItemId = input<number>();
    /** Permissions of the editor */
    public readonly permissions = input<FolderPermissionData>();
    /** Which mode the UI is currently in */
    public readonly uiMode = input<UIMode>();
    /** The term the list is currently filtered by */
    public readonly filterTerm = input<string>('');

    /* Loading State
     * ===================================================================== */

    /** If this list is currently loading items */
    public readonly loadingItems = model<boolean>();

    /* Pagination
     * ===================================================================== */

    /** Total count of items for this item type in the folder */
    public readonly totalCount = signal<number>(0);
    /** Maxiumum amount of how many items are displayed per page */
    public readonly pageSize = signal<number>(10);
    /** The current list page number (Starts with 1) */
    public readonly currentPage = signal<number>(1);
    /** The currently displayed items displayed in the list */
    public readonly items = signal<T[]>([]);

    /* Sorting
     * ===================================================================== */

    /** The attribute name by which the items are to be sorted by */
    public readonly sortBy = signal<string>('name');
    /** The order by which the items are to be sorted by */
    public readonly sortOrder = signal<PagingSortOrder>(PagingSortOrder.Asc);

    /* Misc State
     * ===================================================================== */

    /** If the list is currently collapsed */
    public readonly collapsed = signal<boolean>(false);

    /**
     * Items which have previously been loaded once.
     * Kept in here to be able to reference these with selected ids.
     */
    protected readonly cachedItems = signal<Record<number, T>>({});

    /**
     * Compatibility object for older components which still use this format.
     * @deprecated Only here for legacy components, don't rely on this.
     */
    public readonly itemsInfo = computed<ItemsInfo>(() => {
        return {
            ...emptyItemInfo,
            fetchAll: this.loadingItems(),
            fetching: this.loadingItems(),
            currentPage: this.currentPage(),
            itemsPerPage: this.pageSize(),
            hasMore: Math.ceil(this.totalCount() / this.pageSize()) > this.currentPage(),
            list: this.items().map((item) => item.id),
            selected: Array.from(this.selection()),
            total: this.totalCount(),
            displayFields: this.displayFields(),
        };
    });

    /* Global State mappings
     * ========================================================================
     * In the optimal case, most of these would be moved to the parent component,
     * as changes to most of these settings need to be reflected on all lists,
     * and prevents uncessessary subscriptions and couplings to the state.
     */

    /** If the items should display all available languages */
    public readonly showAllLanguages = signal<boolean>(false);
    /** If the language information should also contain additional language status indicators */
    public readonly showStatusIcons = signal<boolean>(false);
    /** If the wastebin feature is enabled */
    public readonly wastebinEnabled = signal<boolean>(false);
    /** If the list should display deleted items */
    public readonly showDeleted = signal<boolean>(false);
    /** The search term that is currently searched by */
    public readonly searchTerm = signal<string>('');
    /** If a elasticsearch query is currently active */
    public readonly elasticSearchQueryActive = signal<boolean>(false);
    /** List of fields that should be displayed in the list item */
    public readonly displayFields = signal<string[]>([]);

    /** Staging data of the current folder */
    public readonly stagingMap = signal<StagedItemsMap>({});

    /* Internals
     * ===================================================================== */

    protected subscriptions: Subscription[] = [];
    /** Subject to trigger when a load should occur. */
    protected loadSubject = new Subject<{ page: number; pageSize: number }>();
    /** Subscription of loading all items if neccessary */
    private loadAllSubscription: Subscription | null = null;

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
        this.subscriptions.push(this.appState.select((state) => state.folder.displayStatusIcons).pipe(
            distinctUntilChanged(isEqual),
        ).subscribe((show) => {
            this.showStatusIcons.set(show);
        }));

        this.subscriptions.push(this.appState.select((state) => state.folder.displayDeleted).pipe(
            distinctUntilChanged(isEqual),
        ).subscribe((show) => {
            this.showDeleted.set(show);
            this.reload();
        }));

        this.subscriptions.push(this.appState.select((state) => state.folder.displayAllLanguages).pipe(
            distinctUntilChanged(isEqual),
        ).subscribe((show) => {
            this.showAllLanguages.set(show);
        }));

        this.subscriptions.push(this.appState.select((state) => state.folder.searchTerm).pipe(
            distinctUntilChanged(isEqual),
        ).subscribe((term) => {
            this.searchTerm.set(term);
            this.reload();
        }));

        this.subscriptions.push(this.appState.select((state) => state.folder.searchFiltersVisible).pipe(
            distinctUntilChanged(isEqual),
        ).subscribe((active) => {
            this.elasticSearchQueryActive.set(active);
        }));

        this.subscriptions.push(this.appState.select((state) => state.features[Feature.WASTEBIN]).subscribe((enabled) => {
            this.wastebinEnabled.set(enabled);
        }));

        this.subscriptions.push(this.loadSubject.asObservable().pipe(
            debounceTime(50),
            tap(() => {
                this.loadingItems.set(true);
            }),
            switchMap((data) => this.loadPageWith(data.page, data.pageSize)),
            tap(() => {
                this.loadingItems.set(false);
            }),
        ).subscribe());

        // Cuase the lists to actually load
        this.reload();
    }

    public ngOnChanges(changes: ChangesOf<this>): void {
        let doReload = false;
        if (changes.showDeleted) {
            doReload = true;
        }

        if ((changes.folderId && !changes.folderId.firstChange) || (changes.node && !changes.node.firstChange)) {
            // When we change the parent or node, we have to clear the cached items, as they are only
            // used for selection handling. Also cancel the loading of all items as we are changing the parent now.
            if (this.loadAllSubscription != null) {
                this.loadAllSubscription.unsubscribe();
                this.loadAllSubscription = null;
            }
            this.cachedItems.set({});
            this.stagingMap.set({});

            doReload = true;
        }

        if (doReload) {
            this.reload();
        }
    }

    public ngOnDestroy(): void {
        this.subscriptions.forEach((s) => s.unsubscribe());
    }

    public updateItemSelection(itemId: number, setSelected: boolean): void {
        const copy = new Set(this.selection());

        if (setSelected) {
            copy.add(itemId);
        } else {
            copy.delete(itemId);
        }

        this.selection.set(copy);
    }

    public toggleAllSelection(toggleValue: boolean): void {
        // If all items were already selected, then we simply have to clear it
        if (!toggleValue) {
            this.selection.set(new Set());
            return;
        }

        const allIds = Object.keys(this.cachedItems());

        // If we have all items already cached/fetched once, then we can simply reuse them
        if (allIds.length === this.totalCount()) {
            this.selection.set(new Set(allIds.map((id) => toValidNumber(id))));
            return;
        }

        if (this.loadAllSubscription != null) {
            this.loadAllSubscription.unsubscribe();
        }

        // Now we need to load all items in this folder, in order to be able to select all of them.
        // Maybe we can switch this behaviour to match the admin-ui instead, where only pages are
        // actually selected, to prevent additional loading of elements like this.
        this.loadAllSubscription = this.loadItems(this.folderId(), this.node().id, 1, -1).subscribe({
            next: (data) => {
                this.cacheNewItems(data.items);
                this.totalCount.set(data.totalCount);
                this.selection.set(new Set(data.items.map((item) => item.id)));
            },
            error: (err) => {
                this.errorHandler.catch(err);
            },
        });

        this.subscriptions.push(this.loadAllSubscription);
    }

    public reload(): void {
        this.loadSubject.next({ page: this.currentPage(), pageSize: this.pageSize() });
    }

    public loadPage(page: number): void {
        this.loadSubject.next({ page: page, pageSize: this.pageSize() });
    }

    private cacheNewItems(items: T[]): void {
        const cache = this.cachedItems();
        for (const singleItem of items) {
            cache[singleItem.id] = singleItem;
        }
        this.cachedItems.set(cache);
    }

    private updateStageMap(data?: StagedItemsMap): void {
        if (!data) {
            return;
        }
        const merged = {
            ...this.stagingMap(),
            ...data,
        };
        this.stagingMap.set(merged);
    }

    private loadPageWith(
        page: number,
        pageSize: number,
    ): Observable<void> {
        return this.loadItems(this.folderId(), this.node().id, page, pageSize).pipe(
            discard((data) => {
                this.currentPage.set(page);
                this.pageSize.set(pageSize);
                this.totalCount.set(data.totalCount);
                this.items.set(data.items);
                this.updateStageMap(data.stagingData);

                // Add items to cache
                this.cacheNewItems(data.items);
            }),
            catchError((err) => {
                this.errorHandler.catch(err);
                return of();
            }),
        );
    }
}

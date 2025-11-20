import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChange,
} from '@angular/core';
import {
    FolderItemType,
    FolderItemTypePlural,
    Item,
    Language,
    Node as NodeModel,
    Normalized,
    StagedItemsMap,
} from '@gentics/cms-models';
import { isEqual } from 'lodash-es';
import { PaginationInstance, PaginationService } from 'ngx-pagination';
import { BehaviorSubject, Observable, Subscription, combineLatest } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, map, startWith } from 'rxjs/operators';
import { EditorPermissions, ItemsInfo, UIMode, getNoPermissions } from '../../../common/models';
import { areItemsLoading } from '../../../common/utils/are-items-loading';
import { iconForItemType } from '../../../common/utils/icon-for-item-type';
import { UploadProgressReporter } from '../../../core/providers/api';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { UserSettingsService } from '../../../core/providers/user-settings/user-settings.service';
import { ApplicationStateService, FolderActionsService, UsageActionsService } from '../../../state';

interface ItemsHashMap {
    [id: number]: Item;
}

@Component({
    selector: 'item-list',
    templateUrl: './item-list.component.html',
    styleUrls: ['./item-list.component.scss'],
    providers: [PaginationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ItemListComponent implements OnInit, OnChanges, OnDestroy {

    @Input()
    public itemsInfo: ItemsInfo;

    @Input()
    public items: Item<Normalized>[];

    @Input()
    public itemType: FolderItemType;

    @Input()
    public filterTerm = '';

    @Input()
    public activeNode: NodeModel;

    @Input()
    public currentFolderId: number;

    @Input()
    public canCreateItem = true;

    @Input()
    public startPageId: number;

    @Input()
    public acceptUploads = '';

    @Input()
    public uploadProgress: UploadProgressReporter;

    @Input()
    public folderPermissions: EditorPermissions = getNoPermissions();

    @Input()
    public linkPaths = false;

    @Input()
    public itemInEditor: Item = undefined;

    @Input()
    public uiMode: UIMode = UIMode.EDIT;

    @Input()
    public stagingMap: StagedItemsMap;

    @Output()
    public pageChange = new EventEmitter<number>();

    @Output()
    public itemsPerPageChange = new EventEmitter<number>();

    selectedItems: Item[] = [];

    icon = '';
    showGrid = true;

    languages$: Observable<Language[]>;
    showAllLanguages$: Observable<boolean>;
    showStatusIcons$: Observable<boolean>;
    showDeleted$: Observable<boolean>;
    showImagesGridView$: Observable<boolean>;
    private subscriptions: Subscription[] = [];
    paginationConfig: PaginationInstance = {
        itemsPerPage: 10,
        currentPage: 1,
    };

    selectedItems$: Observable<Item[]>;

    areItemsLoading$: Observable<boolean>;

    private itemType$ = new BehaviorSubject<FolderItemType>(null);
    private itemHash$ = new BehaviorSubject<ItemsHashMap>({});

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
        private usageActions: UsageActionsService,
        private entityResolver: EntityResolver,
        private folderActions: FolderActionsService,
        private userSettings: UserSettingsService,
    ) { }

    ngOnInit(): void {
        this.languages$ = this.appState.select(state => state.folder.activeNodeLanguages.list).pipe(
            map(list => list.map(id => this.entityResolver.getLanguage(id))),
        );
        this.showAllLanguages$ = this.appState.select(state => state.folder.displayAllLanguages);
        this.showStatusIcons$ = this.appState.select(state => state.folder.displayStatusIcons);
        this.showDeleted$ = this.appState.select(state => state.folder.displayDeleted);
        this.showImagesGridView$ = this.appState.select(state => state.folder.displayImagesGridView);

        // When the current folder changes, reset the pagination to page 1
        const resetPaginationSub = this.appState.select(state => state.folder.activeFolder)
            .subscribe(() => this.paginationConfig.currentPage = 1);
        this.subscriptions.push(resetPaginationSub);

        const itemType$: Observable<FolderItemType> = this.itemType$.asObservable().pipe(
            distinctUntilChanged(isEqual),
        );

        const itemsInfo$ = combineLatest([
            this.appState.select(state => state.folder),
            itemType$,
        ]).pipe(
            map(([folderState, itemType]) => folderState[`${itemType}s` as FolderItemTypePlural]),
        );

        const selected$: Observable<number[]> = itemsInfo$.pipe(
            filter(itemsInfo => !!itemsInfo),
            map(itemsInfo => itemsInfo.selected),
            distinctUntilChanged(isEqual),
        );

        this.selectedItems$ = combineLatest([selected$, this.itemHash$]).pipe(
            map(([selectedIds, itemHash]) => {
                return selectedIds
                    .map(id => itemHash[id])
                    .filter(item => !!item);
            }),
        );

        // When the "usage" displayField is selected, we need to then get the usage for each item in the list,
        // so that the user is not required to refresh the list manually.

        const itemsInfoPipe$ = itemsInfo$.pipe(
            map(itemsInfo => itemsInfo && itemsInfo.displayFields),
            filter(val => !!val),
            map((fields: string[]) => fields.indexOf('usage') >= 0),
            debounceTime(200),
            distinctUntilChanged(isEqual),
        );
        this.subscriptions.push(combineLatest([
            itemsInfoPipe$,
            this.appState.select(state => state.editor.saving).pipe(startWith(false)),
        ]).subscribe(([displayUsage, saving]) => {
            if (displayUsage && !saving) {
                this.getTotalUsage();
            }
        }));

        this.subscriptions.push(itemType$.subscribe(itemType => {
            this.icon = iconForItemType(itemType);
            this.changeDetector.markForCheck();
        }));

        this.paginationConfig.currentPage = this.itemsInfo.currentPage;
        this.paginationConfig.itemsPerPage = this.itemsInfo.itemsPerPage;

        // Listen for itemsPerPage changes and change pagination
        this.subscriptions.push(combineLatest([
            this.itemsPerPageChange,
            itemType$,
        ]).subscribe(([pageSize, itemType]) => {
            if ((Math.ceil(this.itemsInfo.list.length / pageSize)) < this.paginationConfig.currentPage) {
                this.paginationConfig.currentPage = 1;
                this.folderActions.setCurrentPage(this.itemType, this.paginationConfig.currentPage);
            }
            this.paginationConfig.itemsPerPage = pageSize;
            this.userSettings.setItemsPerPage(itemType, pageSize);
        }));

        this.areItemsLoading$ = this.appState.select(state => state.folder).pipe(
            map(areItemsLoading),
            distinctUntilChanged(isEqual),
        );
    }

    ngOnChanges(changes: { [K in keyof ItemListComponent]?: SimpleChange }): void {
        const itemsInfoChanges = changes.itemsInfo;
        let listChanged = itemsInfoChanges
            && itemsInfoChanges.previousValue
            && !this.listsAreEqual(itemsInfoChanges.previousValue.list, itemsInfoChanges.currentValue.list);


        if (changes.itemType) {
            this.itemType$.next(this.itemType);
        }

        const items = changes.items;

        if (items) {
            if (!Array.isArray(this.items)) {
                this.items = [];
            } else {
                this.items = this.items.filter(item => item != null);
            }
        }

        if (
            items
            && items.previousValue?.length > 0
            && this.items?.length > 0
            && (this.items[0]?.type === 'image' || this.items[0]?.type === 'page')
        ) {
            (this.items || []).forEach(item => {
                if (!item.usage) {
                    listChanged = true;
                }
            })
        }

        if (itemsInfoChanges) {
            if (this.itemsInfo.fetchAll === false) {
                this.paginationConfig.totalItems = this.itemsInfo.total;
            } else if (this.itemsInfo.fetching === false) {
                this.paginationConfig.totalItems = undefined;
            }
            this.paginationConfig.itemsPerPage = this.itemsInfo.itemsPerPage;
            this.paginationConfig.currentPage = this.itemsInfo.currentPage;
        }

        if (listChanged) {
            this.getTotalUsage();
        }

        if (changes.items) {
            this.updateItemHash();
        }
    }

    updateItemHash(): void {
        const itemHash: ItemsHashMap = {};
        for (const item of this.items) {
            if (item == null) {
                continue;
            }
            itemHash[item.id] = item;
        }
        this.itemHash$.next(itemHash);
    }

    /**
     * Compares arrays of numbers for equality (i.e. the same numbers in the same sequence)
     */
    private listsAreEqual(list1: number[], list2: number[]): boolean {
        if (list1.length !== list2.length) {
            return false;
        }
        for (let i = 0; i < list1.length; i++) {
            if (list1[i] !== list2[i]) {
                return false;
            }
        }
        return true;
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(subscription => subscription.unsubscribe());
    }

    getTotalUsage(): void {
        if (this.activeNode) {
            this.usageActions.getTotalUsage(this.itemType, this.itemsInfo.list, this.activeNode.id);
        }
    }

    /**
     * Tracking function for ngFor for better performance.
     */
    identify(index: number, item: Item): number {
        return item?.id ?? index;
    }

    /**
     * Returns true is the item is in the selectedItems array.
     */
    isSelected(item: Item): boolean {
        if (item == null) {
            return false;
        }

        return this.itemsInfo.selected.indexOf(item.id) >= 0;
    }

}

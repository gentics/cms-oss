import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    OnDestroy,
    OnInit,
    QueryList,
    ViewChild,
    ViewChildren,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {
    AppState,
    EditorPermissions,
    GtxChipSearchPropertyNumber,
    GtxChipSearchSearchFilterMap,
    ItemsInfo,
    UIMode,
    getNoPermissions,
} from '@editor-ui/app/common/models';
import { areItemsLoading } from '@editor-ui/app/common/utils/are-items-loading';
import { isLiveUrl } from '@editor-ui/app/common/utils/is-live-url';
import { UploadProgressReporter } from '@editor-ui/app/core/providers/api';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { NavigationService } from '@editor-ui/app/core/providers/navigation/navigation.service';
import { PermissionService } from '@editor-ui/app/core/providers/permissions/permission.service';
import { UploadConflictService } from '@editor-ui/app/core/providers/upload-conflict/upload-conflict.service';
import { UserSettingsService } from '@editor-ui/app/core/providers/user-settings/user-settings.service';
import { ListService } from '@editor-ui/app/list-view/providers/list/list.service';
import { BreadcrumbsService } from '@editor-ui/app/shared/providers/breadcrumbs.service';
import {
    ApplicationStateService,
    ChangeListSelectionAction,
    FocusListAction,
    FolderActionsService,
    SetActiveContentPackageAction,
    SetListPageAction,
    SetListPerPageAction,
    SetUIModeAction,
} from '@editor-ui/app/state';
import {
    Folder,
    FolderItemType,
    FolderItemTypePlural,
    Item,
    ItemType,
    Node,
    NodeFeature,
    StagedItemsMap,
} from '@gentics/cms-models';
import { IBreadcrumbRouterLink, ModalService, SplitViewContainerComponent } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import {
    BehaviorSubject,
    Observable,
    Subscription,
    combineLatest,
    of,
} from 'rxjs';
import {
    debounceTime,
    defaultIfEmpty,
    distinctUntilChanged,
    filter,
    first,
    map,
    mergeMap,
    publishReplay,
    refCount,
    skip,
    startWith,
    switchMap,
    take,
    tap,
    withLatestFrom,
} from 'rxjs/operators';
import { ItemListComponent } from '../item-list/item-list.component';

export interface ShowPathStatus {
    image: boolean;
    file: boolean;
    page: boolean;
}

/**
 * Lists out all the contents of a folder. The actual rendering of the lists is handled by the ItemList component.
 * The responsibility of this component is simply fetching and piping the list data to the ItemList components.
 */
@Component({
    selector: 'folder-contents',
    templateUrl: './folder-contents.component.html',
    styleUrls: ['./folder-contents.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FolderContentsComponent implements OnInit, OnDestroy {

    /**
     * Available entity identifier string.
     * Default set is `folder`, `page`, `file`, `image`.
     * Optional entity `form` to be activated by Node features `forms`.
     */
    itemTypes$: Observable<FolderItemType[]>;
    loading$: Observable<boolean>;
    activeNode$: Observable<Node>;
    /** If current folder in node couldn't be found, this is the real node ID of the folder. */
    nodeIdActual$ = new BehaviorSubject<number>(null);
    activeFolder$: Observable<Folder>;

    folderNotFound$: Observable<boolean>;
    nodeNotFound$: Observable<boolean>;
    folderNotFoundInNode$: Observable<boolean>;

    noErrors$: Observable<boolean>;
    displayErrorNodeNotFound$: Observable<boolean>;
    displayErrorFolderNotFound$: Observable<boolean>;
    displayErrorFolderNotFoundInNode$: Observable<boolean>;
    displayErrorFolderNotFoundAndNodeNotFound$: Observable<boolean>;

    advancedSearchActive$: Observable<boolean>;
    searchFiltersActive$: Observable<boolean>;
    searchFiltersNodeNames$: Observable<string>;

    currentFolderDisplayName$: Observable<string>;

    activeNodeId: number;
    currentFolder$: Observable<Folder>;
    breadcrumbs$: Observable<IBreadcrumbRouterLink[]>;
    /** Rendered current search path breadcrumb string to display the path where Elastic Search queries recursively from. */
    breadcrumbsString$: Observable<string>;
    nodes$: Observable<Node[]>;

    uiMode$: Observable<UIMode>;
    inStagingMode$: Observable<boolean>;
    stagingMap$: Observable<StagedItemsMap>;

    multilineExpanded$: Observable<boolean>;
    isInherited$: Observable<boolean>;
    showPath$: Observable<ShowPathStatus>;

    filterTerm$: Observable<string>;
    contentPackage$: Observable<string>;
    searchTerm$: Observable<string>;
    startPageId$: Observable<number>;
    itemInEditor$: Observable<Item>;
    permissions$: Observable<EditorPermissions>;
    currentFolderId: number;
    currentFolderPath = '';
    currentFolder: Folder;
    subscriptions: Subscription[] = [];
    fileUploadProgress: UploadProgressReporter;
    imageUploadProgress: UploadProgressReporter;

    @ViewChild('fileDropTextOverlay', { static: true })
    fileDropTextOverlay: ElementRef<HTMLElement>;

    fileDropLabelLeft = '0';

    @ViewChildren(ItemListComponent, { read: ElementRef })
    itemLists: QueryList<ElementRef<HTMLElement>>;

    private currentItemTypes: FolderItemType[] = [];

    constructor(
        public listService: ListService,
        private breadcrumbsService: BreadcrumbsService,
        private appState: ApplicationStateService,
        private navigationService: NavigationService,
        private route: ActivatedRoute,
        private permissions: PermissionService,
        private entityResolver: EntityResolver,
        private folderActions: FolderActionsService,
        private uploadConflictService: UploadConflictService,
        private splitViewContainer: SplitViewContainerComponent,
        private userSettings: UserSettingsService,
        private modalService: ModalService,
        private i18n: I18nService,
    ) {
        this.currentFolder$ = combineLatest([
            this.appState.select(state => state.folder.activeFolder),
            this.appState.select(state => state.entities.folder),
        ]).pipe(
            filter(([folderId, loadedFolders]) => folderId != null && loadedFolders != null && loadedFolders[folderId] != null),
            map(([folderId, loadedFolders]) => loadedFolders[folderId]),
            filter(folder => folder.permissionsMap != null),
            publishReplay(1),
            refCount(),
        );

        // Get multiline state
        this.multilineExpanded$ = appState.select(state => state.ui.itemListBreadcrumbsExpanded);

        // Only recreate breadcrumbs when the dependent state changes
        this.breadcrumbs$ = appState.select(state => state).pipe(
            distinctUntilChanged((a, b) => {
                if (a.folder.activeNode !== b.folder.activeNode) {
                    return false;
                }
                if (a.folder.breadcrumbs !== b.folder.breadcrumbs || a.entities.folder !== b.entities.folder) {
                    return false;
                }
                if (a.folder.breadcrumbs.list.length !== b.folder.breadcrumbs.list.length) {
                    return false;
                }
                const valueDiff = a.folder.breadcrumbs.list.every((id, index) => {
                    return b.folder.breadcrumbs.list[index] === id
                        && a.entities.folder[id].name === b.entities.folder[id].name;
                });
                if (!valueDiff) {
                    return false;
                }
                return this.nodeNameHasNotChanged(a, b);
            }),
            map(state => this.createBreadcrumbs(state)),
            withLatestFrom(this.multilineExpanded$),
            map(([breadcrumbs, isMultilineExpanded]) => {
                if (breadcrumbs.length > 1) {
                    breadcrumbs.splice(breadcrumbs.length - 1, 1);
                }
                return !isMultilineExpanded ? this.breadcrumbsService.addTooltip(breadcrumbs) : breadcrumbs;
            }),
        );

        this.breadcrumbsString$ = this.breadcrumbs$.pipe(
            map(breadcrumbs => breadcrumbs.map(breadcrumb => breadcrumb.text).join(' / ')),
        );

        this.nodes$ = combineLatest([
            appState.select(state => state.folder.nodes.list),
            appState.select(state => state.entities.node),
        ]).pipe(
            map(([nodeIds, loadedNodes]) => nodeIds
                .map(id => loadedNodes[id])
                .filter(node => node != null),
            ),
            distinctUntilChanged((nodeArrayA, nodeArrayB) =>
                nodeArrayA.length === nodeArrayB.length
                && nodeArrayA.every((ref, index) => nodeArrayB[index] === ref),
            ),
        );

        this.uiMode$ = appState.select(state => state.ui.mode);
        this.inStagingMode$ = this.uiMode$.pipe(
            map(mode => mode === UIMode.STAGING),
        );
        this.stagingMap$ = appState.select(state => state.contentStaging.stagingMap);
    }

    get isModalOpen(): boolean {
        return this.modalService.openModals.length === 0;
    }

    ngOnInit(): void {
        this.itemTypes$ = combineLatest([
            this.appState.select(state => state.folder.activeNode),
            this.appState.select(state => state.features.nodeFeatures),
        ]).pipe(
            map(([activeNodeId, nodeFeatures]) => {
                const activeNodeFeatures = nodeFeatures[activeNodeId];
                const isActiveFeatureForms = Array.isArray(activeNodeFeatures) && activeNodeFeatures.includes(NodeFeature.FORMS);
                const itemTypes: FolderItemType[] = ['folder', 'page', 'file', 'image'];
                if (isActiveFeatureForms) {
                    itemTypes.push('form');
                }
                return itemTypes;
            }),
            tap(types => this.currentItemTypes = types),
        );

        this.initFolderContents();
        this.listService.init(this.route);
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    /**
     * Sets up the streams and subscriptions used in the component.
     */
    initFolderContents(): void {
        const entityState$ = this.appState.select(state => state.entities);
        const activeNodeId$ = this.appState
            .select(state => state.folder.activeNode);

        const nodeSub = activeNodeId$.pipe(
            filter(nodeId => !!nodeId),
        ).subscribe(nodeId => {
            this.activeNodeId = nodeId;
            this.setEmptySelection();
            this.userSettings.setLastNodeId(nodeId);
        });
        this.subscriptions.push(nodeSub);

        const folderSub = this.currentFolder$.subscribe(item => {
            this.currentFolder = item;
            this.currentFolderPath = this.getFolderPath(item);
        });
        this.subscriptions.push(folderSub);

        const activeFolderId$ = this.appState
            .select(state => state.folder.activeFolder);

        this.activeFolder$ = activeFolderId$.pipe(
            mergeMap(activeFolderId => this.appState.select(state => state.entities.folder[activeFolderId])),
        );

        // check if folder does exist
        this.folderNotFound$ = combineLatest([
            this.appState.select(state => state.folder.activeFolder),
            this.appState.select(state => state.entities.folder),
            this.appState.select(state => state.folder.breadcrumbs.fetching),
        ]).pipe(
            map(([folderId, loadedFolders, fetching]) => !fetching && (folderId == null || loadedFolders[folderId] == null)),
        );

        this.advancedSearchActive$ = combineLatest([
            this.appState.select(state => state.folder.searchFiltersVisible),
            this.appState.select(state => state.folder.searchTerm),
        ]).pipe(
            map(([visible, term]) => visible && term !== ''),
        );

        this.searchFiltersNodeNames$ = this.appState.select(state => state.folder.searchFilters).pipe(
            switchMap(searchFilters => {
                const currentNodeFilter: GtxChipSearchSearchFilterMap[keyof GtxChipSearchSearchFilterMap] = searchFilters['nodeId'];
                // if search filter is set for a specific node, get it to display in search results headline
                if (
                    Array.isArray(currentNodeFilter) &&
                    currentNodeFilter.length > 0 &&
                    !currentNodeFilter.some(nodeFilter => nodeFilter.value === 'all' as any)
                ) {
                    const currentSearchFilterNodeIds = (currentNodeFilter as GtxChipSearchPropertyNumber[]).map(nodeFilter => nodeFilter.value);
                    return this.appState.select(state => state.entities.node).pipe(
                        first(),
                        map(indexedNodes => currentSearchFilterNodeIds.map(nodeId => indexedNodes[nodeId])),
                        map((nodes: Node[]) => nodes.map(n => n.name).join(', ')),
                    );
                } else if (
                    Array.isArray(currentNodeFilter) &&
                    currentNodeFilter.length > 0 &&
                    currentNodeFilter.some(nodeFilter => nodeFilter.value === 'all' as any)
                ) {
                    return of('all');
                } else {
                    return of(undefined);
                }
            }),
        );

        this.searchFiltersActive$ = this.appState.select(state => state.folder.searchFiltersVisible);

        // check if active node exists
        this.nodeNotFound$ = combineLatest([
            this.appState.select(state => state.folder.activeNode),
            this.appState.select(state => state.entities.node),
        ]).pipe(
            map(([nodeId, loadedNodes]) => nodeId == null || loadedNodes[nodeId] == null),
        );

        // check if active node contains active folder
        this.folderNotFoundInNode$ = combineLatest([
            this.folderNotFound$.pipe(startWith(false)),
            this.nodeNotFound$.pipe(startWith(false)),
            activeNodeId$.pipe(startWith(null)),
            activeFolderId$.pipe(startWith(null)),
        ]).pipe(
            mergeMap(([folderNotFound, nodeNotFound, activeNodeId, activeFolderId]) => {
                // if both active node and active folder exist
                if (!(!folderNotFound && !nodeNotFound && Number.isInteger(activeNodeId) && Number.isInteger(activeFolderId))) {
                    return of(false);
                }

                // check if active folder is in active node
                return this.appState.select(state => state.entities.folder).pipe(
                    map(folders => {
                        const stateFolder = folders[activeFolderId];
                        if (!stateFolder) {
                            return false;
                        }

                        const folderNotFoundInNode = stateFolder.nodeId !== activeNodeId;

                        // if folder does'nt exist in node, get node where folder actually exists
                        if (folderNotFoundInNode) {
                            this.nodeIdActual$.next(folders[activeFolderId].nodeId);
                        }

                        return folderNotFoundInNode;
                    }),
                );
            }),
        );

        const activeFolderSub = activeFolderId$.subscribe(id => {
            this.currentFolderId = id;
            this.splitViewContainer.scrollLeftPanelTo(0);
        });
        this.subscriptions.push(activeFolderSub);

        this.activeNode$ = this.appState.select(state => state.folder.activeNode).pipe(
            filter(nodeId => !!nodeId),
            switchMap(nodeId => this.appState.select(state => state.entities.node[nodeId])),
            filter(node => !!node),
        );

        this.isInherited$ = this.activeNode$.pipe(
            map(activeNode => activeNode && activeNode.inheritedFromId !== activeNode.id),
        );

        this.loading$ = this.appState.select(state => state.folder).pipe(
            map(areItemsLoading),
            distinctUntilChanged(isEqual),
            debounceTime(100),
            publishReplay(1),
            refCount(),
        );

        this.currentFolderDisplayName$ = this.loading$.pipe(
            startWith(false),
            filter(isLoading => !isLoading),
            switchMap(() => this.currentFolder$),
            map((currentFolder) => currentFolder.name),
        );

        const clearSelectionSub = this.loading$.pipe(
            skip(1),
            filter(loading => !loading),
            switchMap(() => activeFolderId$.pipe(take(1))),
            distinctUntilChanged(isEqual),
        ).subscribe(() => {
            this.setEmptySelection();
        });
        this.subscriptions.push(clearSelectionSub);

        this.setEmptySelection();

        this.filterTerm$ = this.appState.select(state => state.folder.filterTerm).pipe(
            withLatestFrom(this.appState.select(state => state.entities.node)),
            // we do not want to set the filter
            // term when user pastes a liveUrl
            filter(([term, nodes]) => !isLiveUrl(term, Object.values(nodes).map((node: Node) => node.host))),
            map(result => result[0]),
            publishReplay(1),
            refCount(),
        );

        this.searchTerm$ = this.appState.select(state => state.folder.searchTerm).pipe(
            distinctUntilChanged(isEqual),
            publishReplay(1),
            refCount(),
        );

        this.subscriptions.push(this.searchTerm$.subscribe(searchTerm => {
            if (searchTerm) {
                this.appState.dispatch(new FocusListAction());
            }
        }));

        this.startPageId$ = activeFolderId$.pipe(
            switchMap(folderId => {
                return entityState$.pipe(
                    map(s => s.folder[folderId] && s.folder[folderId].startPageId),
                );
            }),
            distinctUntilChanged(isEqual),
            publishReplay(1),
            refCount(),
        );

        this.itemInEditor$ = this.appState.select(state => state.editor.editorIsOpen).pipe(
            switchMap(isOpen => {
                if (!isOpen) {
                    return of(null);
                }

                return combineLatest([
                    this.appState.select(state => state.editor.itemType),
                    this.appState.select(state => state.editor.itemId),
                ]).pipe(
                    switchMap(([type, id]) => this.appState.select(state => state.entities[type][id])),
                );
            }),
        );

        this.permissions$ = this.permissions.all$.pipe(
            startWith(getNoPermissions()),
            defaultIfEmpty(getNoPermissions()),
        )

        const notFound$: Observable<[boolean, boolean, boolean]> = combineLatest([
            this.nodeNotFound$,
            this.folderNotFound$,
            this.folderNotFoundInNode$,
        ]);

        this.displayErrorNodeNotFound$ = notFound$.pipe(
            map(([nodeNotFound, folderNotFound, folderNotFoundInNode]) => {
                return (nodeNotFound && !folderNotFound && !folderNotFoundInNode);
            }),
        );
        this.displayErrorFolderNotFound$ = notFound$.pipe(
            map(([nodeNotFound, folderNotFound, folderNotFoundInNode]) => {
                return (!nodeNotFound && folderNotFound && !folderNotFoundInNode);
            }),
        );
        this.displayErrorFolderNotFoundInNode$ = notFound$.pipe(
            map(([nodeNotFound, folderNotFound, folderNotFoundInNode]) => {
                return (!nodeNotFound && !folderNotFound && folderNotFoundInNode);
            }),
        );
        this.displayErrorFolderNotFoundAndNodeNotFound$ = notFound$.pipe(
            map(([nodeNotFound, folderNotFound]) => {
                return (folderNotFound && nodeNotFound);
            }),
        );

        this.noErrors$ = combineLatest([
            this.displayErrorNodeNotFound$,
            this.displayErrorFolderNotFound$,
            this.displayErrorFolderNotFoundInNode$,
            this.displayErrorFolderNotFoundAndNodeNotFound$,
        ]).pipe(
            map(([
                displayErrorNodeNotFound,
                displayErrorFolderNotFound,
                displayErrorFolderNotFoundInNode,
                displayErrorFolderNotFoundAndNodeNotFound,
            ]) => (
                !displayErrorNodeNotFound &&
            !displayErrorFolderNotFound &&
            !displayErrorFolderNotFoundInNode &&
            !displayErrorFolderNotFoundAndNodeNotFound
            )),
        );

        this.showPath$ = this.appState.select(state => state.folder).pipe(
            map(folderState => {
                return {
                    image: folderState.images.showPath,
                    form: folderState.forms.showPath,
                    file: folderState.files.showPath,
                    page: folderState.pages.showPath,
                };
            }),
        );

        this.subscriptions.push(combineLatest([
            this.appState.select(state => state.contentStaging.activePackage).pipe(
                distinctUntilChanged(isEqual),
            ),
            this.itemTypes$.pipe(
                skip(1),
            ),
        ]).pipe(
            filter(([active, ununsed]) => active),
        ).subscribe(([unused, itemTypes]) => {
            for (const type of itemTypes) {
                this.reloadItemType(type);
            }
        }));
    }

    /**
     * In order to correctly position the "drop file to upload" text, we need to inspect the properties
     * of the parent overlay. I was not able to find a pure css way to do this.
     */
    setFileDropLabelLeft(): void {
        const overlayWidth = this.fileDropTextOverlay.nativeElement.offsetWidth;
        const overlayLeft = this.fileDropTextOverlay.nativeElement.getBoundingClientRect().left;
        const labelLeft = overlayWidth / 2 - 100 + overlayLeft;
        this.fileDropLabelLeft = `${labelLeft}px`;
    }

    /**
     * When the page is changed by the pagination controls, update the currentPage in the state and
     * scroll the active ItemList into view.
     */
    pageChange(type: FolderItemType, pageNumber: number): void {
        const itemList = this.itemLists.toArray()
            .map(elRef => elRef.nativeElement)
            .find(itemList => itemList.classList.contains(type));

        if (itemList) {
            this.splitViewContainer.scrollLeftPanelTo(itemList.offsetTop);
        }

        const itemsInfo: ItemsInfo = this.appState.now.folder[`${type}s` as FolderItemTypePlural];
        this.folderActions.setCurrentPage(type, pageNumber);
        if (!itemsInfo.fetchAll) {
            this.loadMore(type, pageNumber);
        }
    }

    /**
     * Load the rest of the items from a list that has been truncated server-side.
     */
    loadMore(itemType: ItemType, pageNumber: number): void {
        const folderState = this.appState.now.folder;
        const searchTerm = folderState.searchTerm;
        this.folderActions.getItemsOfTypeInFolder(itemType, this.currentFolderId, searchTerm, false, pageNumber);
    }

    /**
     * On changing maximum items per page aka. pagesize.
     *
     * ~~Page size is bigger than 10 and not all items are loaded, then force load them.~~
     */
    itemsPerPageChange(type: FolderItemType, pageSize: number): void {
        // Propagate to state for `ListService` to listen to.
        this.appState.dispatch(new SetListPerPageAction(type, pageSize));
        // When changing pagesize reset current page to first to prevent wrong page being displayed.
        this.appState.dispatch(new SetListPageAction(type, 1));

        this.reloadItemType(type);
    }

    reloadItemType(type: FolderItemType): void {
        const itemsInfo: ItemsInfo = this.appState.now.folder[`${type}s` as FolderItemTypePlural];
        const activeFolderId = this.appState.now.folder.activeFolder;
        const searchTerm = this.appState.now.folder.searchTerm;
        switch (type) {
            case 'folder':
                this.folderActions.getFolders(activeFolderId, itemsInfo.fetchAll, searchTerm);
                break;
            case 'page':
                this.folderActions.getPages(activeFolderId, itemsInfo.fetchAll, searchTerm);
                break;
            case 'file':
                this.folderActions.getFiles(activeFolderId, itemsInfo.fetchAll, searchTerm);
                break;
            case 'image':
                this.folderActions.getImages(activeFolderId, itemsInfo.fetchAll, searchTerm);
                break;
            case 'form':
                this.folderActions.getForms(activeFolderId, itemsInfo.fetchAll, searchTerm);
                break;
        }
    }

    async leaveStagingMode(): Promise<void> {
        const dialog = await this.modalService.dialog({
            title: this.i18n.translate('modal.leave_content_staging_mode_title'),
            body: this.i18n.translate('modal.leave_content_staging_mode_body'),
            buttons: [
                {
                    label: this.i18n.translate('modal.cancel'),
                    returnValue: false,
                    type: 'secondary',
                    flat: true,
                },
                {
                    label: this.i18n.translate('modal.confirm'),
                    returnValue: true,
                },
            ],
        });
        const doLeave = await dialog.open();

        if (doLeave) {
            this.setEmptySelection();
            this.appState.dispatch(new SetActiveContentPackageAction(null));
            this.appState.dispatch(new SetUIModeAction(UIMode.EDIT));
        }
    }

    private sortOrderEqual(a: ItemsInfo, b: ItemsInfo): boolean {
        return a.sortBy === b.sortBy && a.sortOrder === b.sortOrder;
    }

    private setEmptySelection(): void {
        this.appState.dispatch(new ChangeListSelectionAction(null, 'clear'));
    }

    allowedMimeTypes(type: ItemType): string {
        switch (type) {
            case 'image':
                return 'image/*';
            case 'file':
                return '*, !image/*';
            default:
                return '';
        }
    }

    allFilesAreImages(files: File[]): boolean {
        return files ? files.every(file => file.type.startsWith('image/')) : false;
    }

    getUploadProgress(type: ItemType): UploadProgressReporter {
        switch (type) {
            case 'file': return this.fileUploadProgress;
            case 'image': return this.imageUploadProgress;
            default: return;
        }
    }

    uploadFiles(files: File[]): void {
        this.subscriptions.push(this.uploadConflictService
            .uploadFilesWithConflictsCheck(files, this.activeNodeId, this.currentFolderId)
            .subscribe(),
        );
    }

    goToBaseFolder(): void {
        const currentNode = this.entityResolver.getNode(this.activeNodeId);
        if (currentNode) {
            this.navigationService.list(currentNode.id, currentNode.folderId || currentNode.id).navigate();
        }
    }

    goToCurrentFolder(): void {
        const currentNode = this.entityResolver.getNode(this.activeNodeId);
        if (currentNode) {
            this.navigationService.list(this.nodeIdActual$.getValue(), this.currentFolderId).navigate();
        }
    }

    goToDefaultNode(): void {
        this.folderActions.navigateToDefaultNode();
    }

    expandedChanged(multilineExpanded: boolean): void {
        this.userSettings.setItemListBreadcrumbsExpanded(multilineExpanded);
    }

    /**
     * Returns true if the name of the active node has not changed between the two states.
     */
    private nodeNameHasNotChanged(a: AppState, b: AppState): boolean {
        const nameA = a.entities.node[a.folder.activeNode] && a.entities.node[a.folder.activeNode].name;
        const nameB = b.entities.node[b.folder.activeNode] && b.entities.node[b.folder.activeNode].name;
        return nameA === nameB;
    }

    createBreadcrumbs(state: AppState): IBreadcrumbRouterLink[] {
        const nodeId = state.folder.activeNode;
        return state.folder.breadcrumbs.list
            .filter((value, index, array) => array.indexOf(value) === index)
            .map(id => this.entityResolver.getFolder(id))
            .filter(folder => !!folder)
            .map(folder => {
                return {
                    text: folder.name,
                    route: ['/editor', { outlets: { list: ['node', nodeId, 'folder', folder.id] } }],
                    id: folder.id,
                };
            });
    }

    getFolderPath(item: Folder): string {
        const currentPath = (item ).path.slice(1);
        return currentPath.slice(0, -1);
    }
}

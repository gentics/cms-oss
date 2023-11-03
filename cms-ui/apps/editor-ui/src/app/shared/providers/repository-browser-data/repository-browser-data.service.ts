import {
    Injectable,
    OnDestroy,
} from '@angular/core';
import { RepositoryBrowserDataServiceAPI } from '@editor-ui/app/common/models';
import { isLiveUrl } from '@editor-ui/app/common/utils/is-live-url';
import { Api } from '@editor-ui/app/core/providers/api/api.service';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { ApplicationStateService } from '@editor-ui/app/state';
import {
    AllowedSelection,
    AllowedSelectionType,
    Favourite,
    File,
    Folder,
    FolderListOptions,
    Form,
    FormListOptions,
    Image,
    IndexById,
    InheritableItem,
    ItemInNode,
    Language,
    MarkupLanguageType,
    Node,
    NodeLanguagesResponse,
    NodeResponse,
    Page,
    PageListOptions,
    PageResponse,
    Raw,
    RepoItem,
    RepositoryBrowserDataServiceOptions,
    RepositoryBrowserSorting,
    SearchPagesOptions,
    SortField,
    Tag,
    Template,
    TemplateResponse,
    TotalUsageResponse,
    Usage,
} from '@gentics/cms-models';
import { isEqual } from 'lodash';
import {
    BehaviorSubject,
    combineLatest,
    forkJoin,
    Observable,
    Subscription,
} from 'rxjs';
import {
    distinctUntilChanged,
    filter,
    map,
    mapTo,
    publishReplay,
    refCount,
    skip,
    startWith,
    switchMap,
    take,
} from 'rxjs/operators';

/**
 * A service that handles all data retrieval, navigation and item selection
 * for the {@link RepositoryBrowser} component. It is mostly disconnected from the state,
 * thus does not affect other content lists. The public API is implemented as Observable streams,
 * which can be used directly in the component and covered in simpler tests than a component.
 *
 * A note on "Pick from favourites" support:
 *   The current solution is hacky in that it relies on using a magic value for the favourites "folder"
 *   and appending a "hidden" __favourite__ property to items to reliably forward the correct
 *   parent folder and node id to the backend. This should be refactored at a later point.
 *
 */
@Injectable()
export class RepositoryBrowserDataService implements OnDestroy, RepositoryBrowserDataServiceAPI {

    /** Emits true if the user can submit, false otherwise */
    canSubmit$: Observable<boolean>;

    private currentNodeIdSubjectWrapper = (() => {
        const currentNodeIdSubject = new BehaviorSubject<number>(undefined);
        const currentNodeSubject = new BehaviorSubject<Node>(undefined);
        const currentAvailableLanguagesSubject = new BehaviorSubject<Language[]>([]);
        return {
            next: (nodeId: number) => {
                /** Update node id */
                currentNodeIdSubject.next(nodeId);

                if (nodeId === -1) {
                    /** node id -1 is a special case that denotes the list of favourites */
                    currentAvailableLanguagesSubject.next([]);
                    return;
                }

                /** if a real node is set, then its languages have to be fetched */
                this.subscriptions.add(this.api.folders.getLanguagesOfNode(nodeId).subscribe((nodeLanguages: NodeLanguagesResponse) => {
                    const availableLanguages = nodeLanguages.languages;
                    /** update available languages */
                    currentAvailableLanguagesSubject.next(availableLanguages);
                    /** set current language if it is undefined or not included in the new available languages */
                    const currentLanguage = this.currentContentLanguageSubject.value;
                    if (!currentLanguage ||
                            (currentLanguage && !availableLanguages.map(language => language.id).includes(currentLanguage.id))) {
                        if (availableLanguages.length > 0) {
                            /** choose first available language, as is done in the folder contents component */
                            this.currentContentLanguageSubject.next(availableLanguages[0]);
                        }
                    }
                }));

                this.subscriptions.add(this.api.folders.getNode(nodeId).subscribe(({ node }: NodeResponse) => {
                    currentNodeSubject.next(node);
                }))
            },
            value: () => currentNodeIdSubject.value,
            asObservable: () => currentNodeIdSubject.asObservable(),
            getCurrentAvailableLanguages$: () => currentAvailableLanguagesSubject.asObservable(),
            getCurrentNode$: () => currentNodeSubject.asObservable(),
        };
    })();

    /** Emits the ID of the current node when it changes */
    currentNodeId$ = this.currentNodeIdSubjectWrapper.asObservable();

    /** Emits the current node when it changes */
    currentNode$ = this.currentNodeIdSubjectWrapper.getCurrentNode$();

    /** Emits the available languages of the current node */
    currentAvailableLanguages$ = this.currentNodeIdSubjectWrapper.getCurrentAvailableLanguages$();

    /**
     * Emits the content language of the repository browser data.
     * If the current node does not support languages, the value is `undefined`.
     */
    private currentContentLanguageSubject = new BehaviorSubject<Language>(undefined);
    currentContentLanguage$ = this.currentContentLanguageSubject.asObservable();

    /** Emits the current filter term when it changes */
    filter$ = new BehaviorSubject('');

    /** Emits true if the user has enough permissions to submit the selection, false otherwise */
    hasPermissions$: Observable<boolean>;

    /** Emits which item types the repo browser should display. */
    itemTypesToDisplay$ = new BehaviorSubject<AllowedSelectionType[]>([]);

    /** Emits whether to display the favourites "node". */
    isDisplayingFavourites$ = new BehaviorSubject(false);

    /** Emits whether the current viewed folder is the favourites folder. */
    isDisplayingFavouritesFolder$ = new BehaviorSubject(false);

    /** Tracks when items are fetched from the API */
    loading$ = new BehaviorSubject(false);

    /** List of available nodes */
    nodes$ = new BehaviorSubject<Node<Raw>[]>([]);

    /**
     * List of parent folders/page/template of the current folder.
     * Can be a Page or Template when contenttags/templatetags should be selected.
     */
    parentItems$ = new BehaviorSubject<Array<Folder<Raw> | Page<Raw> | Template<Raw> | Node<Raw>>>([]);

    /** Emits the current search term when it changes */
    search$ = new BehaviorSubject('');

    /** List of items selected by the user */
    selected$ = new BehaviorSubject<ItemInNode[]>([]);

    /** Whether to display the favourites in the node selector */
    showFavourites$ = new BehaviorSubject(false);

    /** Emits the sort order for every item type. Tags only support sorting by name. */
    sortOrder$ = new BehaviorSubject<RepositoryBrowserSorting>({
        folder: { field: 'name', order: 'asc' },
        form: { field: 'name', order: 'asc' },
        page: { field: 'name', order: 'asc' },
        file: { field: 'name', order: 'asc' },
        image: { field: 'name', order: 'asc' },
        template: { field: 'name', order: 'asc' },
        contenttag: { field: 'name', order: 'asc' },
        templatetag: { field: 'name', order: 'asc' },
    });

    /** Emits the startPageId for current parent folder */
    startPageId$: Observable<number | undefined>;

    folders$: Observable<Folder<Raw>[]>;
    forms$: Observable<Form<Raw>[]>;
    pages$: Observable<Page<Raw>[]>;
    files$: Observable<File<Raw>[]>;
    images$: Observable<Image<Raw>[]>;
    templates$: Observable<Template<Raw>[]>;
    tags$: Observable<Tag[]>;

    private allowed: AllowedSelection;
    private currentParent$ = new BehaviorSubject<Folder<Raw> | Page<Raw> | Template<Raw> | Node<Raw>>(undefined);
    private favouritesFolder: any = { name: 'Favourites', id: -1 };
    private items$ = new BehaviorSubject<RepoItem[]>([]);
    private nodeFolders: { [nodeId: number]: Folder<Raw> } = { };
    private pickingFolder: boolean;
    private requestSubscriptions = new Subscription();
    private requiredPermissions: (
        selected: ItemInNode[],
        parent: Folder | Page | Template | Node,
        node: Node,
        currentContentLanguage?: Language
    ) => Observable<boolean>;
    private selectMultiple: boolean;
    private subscriptions = new Subscription();
    private includeMlId: MarkupLanguageType[];

    constructor(
        private api: Api,
        private errorHandler: ErrorHandler,
        private appState: ApplicationStateService,
        private entitityResolver: EntityResolver,
        private notification: I18nNotification) { }

    init(options: RepositoryBrowserDataServiceOptions): void {
        this.setOptions(options);
        this.setupObservables();
        this.prefillDataFromAppState();
        this.fetchNodesFromAPI();
        this.fetchContentsFromAPI();
        this.determineFavouritesVisibility();
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    changeFolder(folder: number | Folder): void {
        if (typeof folder === 'number') {
            folder = this.items$.value.find(item => item.id === (folder as number) && item.type === 'folder') as Folder;
        }

        this.changeParent(folder);
    }

    changeNode(node: number | Node | 'favourites'): void {
        if (node === 'favourites') {
            return this.switchToFavourites();
        } else if (typeof node === 'number') {
            node = this.nodes$.value.find(n => n.id === node);
        }

        if (!node || !node.id) {
            throw new Error('Invalid node in RepositoryBrowserDatService.changeNode()');
        }

        this.parentItems$.next([ this.entitityResolver.denormalizeEntity('node', node) ]);
        this.currentNodeIdSubjectWrapper.next(node.id);
        this.changeFolder(this.nodeFolders[node.id]);
    }

    changeParent(newParent: Folder | Page | Template | Node): void {
        if (newParent === this.favouritesFolder) {
            this.switchToFavourites();
            this.determineItemTypesToDisplay();
            return;
        }

        const parentType = newParent.type !== 'channel' ? newParent.type : 'node';
        this.currentParent$.next(this.entitityResolver.denormalizeEntity(parentType, newParent));

        if (this.isDisplayingFavourites$.value && (newParent as any).__favourite__) {
            this.currentNodeIdSubjectWrapper.next(((newParent as any).__favourite__ as Favourite).nodeId);
        }

        this.determineItemTypesToDisplay();
        this.fetchContentsFromAPI();
    }

    get currentNodeId(): number {
        return this.currentNodeIdSubjectWrapper.value();
    }

    get currentParent(): Folder<Raw> | Page<Raw> | Template<Raw> | Node<Raw> {
        return this.currentParent$.value;
    }

    get isPickingFolder(): boolean {
        return this.pickingFolder;
    }

    isSelected(item: RepoItem, nodeId?: number): boolean {
        if (nodeId == null) {
            nodeId = this.currentNodeIdSubjectWrapper.value();
        }

        return this.selected$.value.some(sel =>
            sel.id === item.id && sel.type === item.type && sel.nodeId === nodeId,
        );
    }

    get selectedItems(): ItemInNode[] {
        return this.selected$.value;
    }

    selectItem(item: RepoItem): void {
        let nodeId = this.currentNodeIdSubjectWrapper.value();

        const itemIsAFavourite = this.isDisplayingFavourites$.value && (item as any).__favourite__;
        if (itemIsAFavourite && nodeId < 0) {
            nodeId = ((item as any).__favourite__ as Favourite).nodeId;
        }

        if (!this.isSelected(item, nodeId)) {
            // Tags don't contain information about their parent objects,
            // so we add the information as a magic "__parent__" property.
            const itemWithNodeId: any = Object.assign({}, item, { nodeId });
            const type = item.type.toLowerCase();
            if (type === 'contenttag' || type === 'templatetag') {
                itemWithNodeId.__parent__ = this.currentParent$.value;
            }

            if (this.selectMultiple) {
                this.selected$.next(this.selected$.value.concat(itemWithNodeId));
            } else {
                this.selected$.next([itemWithNodeId]);
            }
        }
    }

    deselectItem(item: RepoItem | ItemInNode): void {
        const selectedItems = this.selected$.value;
        const nodeId = this.currentNodeIdSubjectWrapper.value();
        const newSelection = selectedItems.filter(selected => {
            if (selected.id !== item.id || selected.type !== item.type) {
                return true;
            } else if (nodeId > 0) {
                return selected.nodeId !== nodeId;
            } else {
                return selected.nodeId !== (item as any).__favourite__.nodeId;
            }
        });

        if (selectedItems.length != newSelection.length) {
            this.selected$.next(newSelection);
        }
    }

    setFilter(filter: string): void {
        if (filter !== this.filter$.value) {
            this.filter$.next(filter);
        }
    }

    setContentLanguage(contentLanguage: Language): void {
        this.currentContentLanguageSubject.next(contentLanguage);
        this.fetchContentsFromAPI();
    }

    setSearch(search: string): void {
        if (search !== this.search$.value) {
            const mightSearchPage = this.allowed.contenttag || this.allowed.page;
            const hosts = Object.values(this.appState.now.entities.node).map((node: Node) => node.host);
            const searchingLiveUrl = mightSearchPage
                && this.nodes$.value.some(node => isLiveUrl(search, hosts));

            if (searchingLiveUrl) {
                this.search$.next('');
                this.filter$.next('');
                this.openLiveUrl(search);
            } else {
                this.search$.next(search);
                this.filter$.next('');
                this.fetchContentsFromAPI();
            }

        }
    }

    setSorting(type: AllowedSelectionType, field: SortField, order: 'asc' | 'desc'): void {
        const currentSortOrder = this.sortOrder$.value[type];
        if (field !== currentSortOrder.field || order !== currentSortOrder.order) {
            const newSortOrder = { ...this.sortOrder$.value };
            newSortOrder[type] = { field, order };

            this.sortOrder$.next(newSortOrder);
            this.fetchContentsFromAPI();
        }
    }

    switchToFavourites(): void {
        this.parentItems$.next([this.favouritesFolder]);
        this.currentParent$.next(this.favouritesFolder);
        this.currentNodeIdSubjectWrapper.next(-1);
        this.fetchFavouritesFromAPI();
    }

    getTotalUsageForCurrentItemsOfType(type: 'file' | 'form' | 'image' | 'page'): void {
        // emits current item of given type immediately due to publishReplay in filteredItems$.
        // typescript would trip over more specific union types at the filter function below, thus we use these super type
        let itemsOfType$: Observable<InheritableItem<Raw>[]> | null = null;
        switch (type) {
            case 'file':
                itemsOfType$ = this.files$;
                break;
            case 'form':
                itemsOfType$ = this.forms$;
                break;
            case 'image':
                itemsOfType$ = this.images$;
                break;
            case 'page':
                itemsOfType$ = this.pages$;
                break;
        }

        if (itemsOfType$) {
            itemsOfType$.pipe(
                take(1),
                map((itemsOfType: InheritableItem<Raw>[]) => {
                    return itemsOfType
                        .filter((itemOfType: InheritableItem<Raw>) => typeof itemOfType.id === 'number')
                        .map((itemOfType: InheritableItem<Raw>) => itemOfType.id);
                }),
                switchMap((itemIds: number[]) => {
                    return this.api.folders.getTotalUsage(type, itemIds, this.currentNodeIdSubjectWrapper.value())
                }),
            ).subscribe((result: TotalUsageResponse) => {
                // similar to implementation in usage-state-actions.ts
                const usage: { [id: number]: Usage } = result.infos;
                // all object keys are ids as numbers
                const idsWithUsage: number[] = Object.keys(usage)
                    .map(key => parseInt(key, 10))
                    .filter((key: number) => !isNaN(key));

                const updatedItems: RepoItem[] = this.items$.value.map((item: RepoItem) => {
                    if (item.type === type) {
                        if (idsWithUsage.includes(item.id)) {
                            item.usage = usage[item.id];
                        }
                    }
                    return Object.assign({}, item);
                });

                this.items$.next(updatedItems);
            })
        }
    }

    private setOptions({ selectMultiple, allowedSelection, ...options }: RepositoryBrowserDataServiceOptions): void {
        this.allowed = allowedSelection;
        this.selectMultiple = selectMultiple;
        this.includeMlId = options.includeMlId;
        this.pickingFolder = !selectMultiple
            && allowedSelection.folder
            && !allowedSelection.form
            && !allowedSelection.contenttag
            && !allowedSelection.file
            && !allowedSelection.image
            && !allowedSelection.page
            && !allowedSelection.templatetag;

        this.requiredPermissions = options.requiredPermissions || (() => Observable.of(true));

        if (options.startNode != null) {
            this.currentNodeIdSubjectWrapper.next(options.startNode);
        }
        let contentLanguage: Language;
        if (options.contentLanguage != null) {
            const languages: IndexById<Language> = this.appState.now.entities.language;
            for (let languageKey of Object.keys(languages)) {
                const language: any = languages[languageKey];
                if (this.isLanguage(language) && language.code === options.contentLanguage) {
                    contentLanguage = language;
                }
            }
        }
        if (contentLanguage) {
            this.currentContentLanguageSubject.next(contentLanguage);
        } else {
            // set content language per default to current folder language, if the node supports languages.
            const activeLanguage = this.entitityResolver.getLanguage(this.appState.now.folder.activeLanguage);
            this.currentContentLanguageSubject.next(activeLanguage);
        }

        if (options.startFolder != null) {
            // When a startFolder is passed, try to use the real folder from the entity state
            // or fall back to a fake folder object to make the initial data fetching work.
            const fakeFolder = {
                type: 'folder',
                id: options.startFolder,
                nodeId: options.startNode != null
                    ? options.startNode
                    : this.appState.now.folder.activeNode,
            } as Partial<Folder<Raw>> as Folder<Raw>;

            if (options.startNode == null || options.startNode === this.appState.now.folder.activeNode) {
                const realFolder = this.entitityResolver.denormalizeEntity('folder', this.entitityResolver.getFolder(options.startFolder));
                this.currentParent$.next(realFolder || fakeFolder);
            } else {
                this.currentParent$.next(fakeFolder);
            }
        }
    }

    private setupObservables(): void {
        this.subscriptions.unsubscribe();
        this.subscriptions = new Subscription();

        this.canSubmit$ = combineLatest(this.selected$, this.currentParent$).pipe(
            map(([selected, parent]) => {
                if (this.pickingFolder && parent === this.favouritesFolder && this.parentItems$.value.length === 1) {
                    return false;
                }

                return (selected && selected.length > 0) || (this.pickingFolder && parent != null &&
                    (parent.type === 'folder' || parent.type === 'node' || parent.type === 'channel'));
            }),
            distinctUntilChanged(isEqual),
        );

        this.hasPermissions$ = combineLatest([
            this.selected$,
            this.currentParent$,
            this.currentNodeId$,
            this.nodes$,
            this.currentContentLanguage$,
        ]).pipe(
            switchMap(([selected, parent, nodeId, nodes, currentContentLanguage]) => {
                return this.determineUserPermissions(selected, parent, nodeId, nodes, currentContentLanguage);
            }),
            switchMap(hasPerms => {
                return (this.loading$.value ? this.loading$ : this.loading$.pipe(skip(1))).pipe(
                    filter(loading => loading === false),
                    take(1),
                    mapTo(hasPerms),
                );
            }),
            publishReplay(1),
            refCount(),
        );

        const filteredItems$ = combineLatest(this.items$, this.filter$).pipe(
            map(([items, filter]) => {
                return filter ? this.filterItems(items, filter) : items;
            }),
            startWith([]),
            publishReplay(1),
            refCount(),
        );

        type FilterFn = (item: RepoItem) => boolean;
        const itemStream = <T>(callback: FilterFn) => (
            filteredItems$.pipe(
                map(items => items.filter(callback) ),
                distinctUntilChanged(itemArraysAreIdentical),
            ) as Observable<T[]>
        );

        this.folders$ = itemStream<Folder<Raw>>(item => item.type === 'folder');
        this.forms$ = itemStream<Form<Raw>>(item => item.type === 'form');
        this.pages$ = itemStream<Page<Raw>>(item => item.type === 'page');
        this.files$ = itemStream<File<Raw>>(item => item.type === 'file');
        this.images$ = itemStream<Image<Raw>>(item => item.type === 'image');
        this.templates$ = itemStream<Template<Raw>>(item => item.type === 'template');
        this.tags$ = itemStream<Tag>(item => item.type === 'TEMPLATETAG' || item.type === 'CONTENTTAG');

        this.startPageId$ = this.currentParent$.pipe(
            map(parent => parent && parent.type === 'folder' && parent.startPageId || undefined),
            distinctUntilChanged(isEqual),
        );

        this.subscriptions.add(
            this.parentItems$.pipe(
                map(parents => parents && parents[0] === this.favouritesFolder),
                distinctUntilChanged(isEqual),
            ).subscribe(this.isDisplayingFavourites$),
        );

        this.subscriptions.add(
            this.parentItems$.pipe(
                map(parents => parents && parents.length === 1 && parents[0] === this.favouritesFolder),
                distinctUntilChanged(isEqual),
            ).subscribe(this.isDisplayingFavouritesFolder$),
        );

        this.subscriptions.add(
            this.appState.select(state => state.favourites)
                .subscribe(() => this.determineFavouritesVisibility()),
        );

    }

    private prefillDataFromAppState(): void {
        const folderState = this.appState.now.folder;

        const nodes = folderState.nodes.list.map(id => this.entitityResolver.denormalizeEntity('node', this.entitityResolver.getNode(id)));
        this.nodes$.next(nodes);

        this.nodeFolders = {};
        for (const node of nodes) {
            this.nodeFolders[node.id] = this.entitityResolver.denormalizeEntity('folder', this.entitityResolver.getFolder(node.folderId));
        }

        if (this.currentNodeIdSubjectWrapper.value() === undefined) {
            this.currentNodeIdSubjectWrapper.next(folderState.activeNode);
        }

        if (this.currentParent$.value === undefined) {
            this.currentParent$.next(this.entitityResolver.denormalizeEntity('folder', this.entitityResolver.getFolder(folderState.activeFolder)));
            const breadcumbs = folderState.breadcrumbs.list.map(id => this.entitityResolver.denormalizeEntity('folder', this.entitityResolver.getFolder(id)));
            this.parentItems$.next(breadcumbs);
        }
    }

    private fetchNodesFromAPI(): void {
        this.subscriptions.add(
            this.api.folders.getNodes()
                .subscribe(res => {
                    const nodes = res.nodes;

                    this.nodeFolders = {};
                    for (const node of nodes) {
                        const nodeFolder = res.folders.find(f => f.id === node.folderId);
                        this.nodeFolders[node.id] = nodeFolder;
                    }

                    if (!itemArraysAreIdentical(this.nodes$.value, nodes)) {
                        this.nodes$.next(nodes);
                    }
                },
                this.errorHandler.catch),
        );
    }

    private fetchContentsFromAPI(): void {
        this.requestSubscriptions.unsubscribe();

        const requests: Observable<any>[] = [];

        let nodeId = this.currentNodeIdSubjectWrapper.value();
        if (this.isDisplayingFavourites$.value) {
            nodeId = (this.currentParent$.value as any as Favourite).nodeId;
        }

        const node = this.nodes$.value.find(node => node.id === nodeId);
        if (node) {
            const parentItem = this.currentParent$.value;
            let parentFolderId: number;
            let parentType = parentItem && parentItem.type;
            if (!parentItem) {
                parentFolderId = node.folderId;
            } else if (parentType === 'folder') {
                parentFolderId = parentItem.id;
            } else if (parentType === 'node' || parentType === 'channel') {
                parentFolderId = (parentItem as Node<Raw>).id;
            } else {
                parentFolderId = (parentItem as Page<Raw> | Template<Raw>).folderId;
            }

            let itemRequests: Observable<RepoItem[]>[] = [];

            const currentContentLanguageCode = this.currentContentLanguageSubject.value ? this.currentContentLanguageSubject.value.code : undefined;
            if (parentType === 'page') {
                itemRequests = [this.fetchContentsOfType('contenttag', parentItem.id, nodeId, currentContentLanguageCode)];
            } else if (parentType === 'template') {
                itemRequests = [this.fetchContentsOfType('templatetag', parentItem.id, nodeId)];
            } else {
                // Decide which item types to fetch, but at least folders
                const allowed = this.allowed;
                const needToFetch = (type: AllowedSelectionType) => {
                    itemRequests.push(this.fetchContentsOfType(type, parentFolderId, nodeId, currentContentLanguageCode));
                };

                needToFetch('folder');
                if (allowed.page || allowed.contenttag) {
                    needToFetch('page');
                }
                if (allowed.file) {
                    needToFetch('file');
                }
                if (allowed.form) {
                    needToFetch('form');
                }
                if (allowed.image) {
                    const typeIdent = 'image';
                    this.sortOrder$.value[typeIdent].field = this.appState.now.folder.images.sortBy;
                    this.sortOrder$.value[typeIdent].order = this.appState.now.folder.images.sortOrder;
                    needToFetch(typeIdent);
                }
                if (allowed.template || allowed.templatetag) {
                    needToFetch('template');
                }
            }

            const allItemRequests = Observable.forkJoin<RepoItem[]>(...itemRequests)
                .do(itemArrays => {
                    const allItems: RepoItem[] = [].concat(...itemArrays);
                    this.items$.next(allItems);
                }, error => {
                    this.notification.show({
                        type: 'alert',
                        message: error.message || error,
                        delay: 10000,
                    });
                });

            const breadcrumbsRequest = this.api.folders.getBreadcrumbs(parentFolderId, { nodeId })
                .map(res => res.folders)
                .do((breadcrumbs: Array<Folder<Raw> | Page<Raw> | Template<Raw> | Node<Raw>>) => {
                    if (parentType === 'page' || parentType === 'template') {
                        breadcrumbs = [...breadcrumbs, parentItem];
                    }

                    // When selecting "B" from favourites, change "A > B > C" to "Favourites > B > C"
                    if (this.isDisplayingFavourites$.value) {
                        if (this.parentItems$.value.length === 1) {
                            breadcrumbs = [this.favouritesFolder, breadcrumbs[breadcrumbs.length - 1]];
                        } else {
                            const favParent = this.parentItems$.value[1];
                            let index = breadcrumbs.length - 1;
                            while (index >= 0 && (breadcrumbs[index].id !== favParent.id || breadcrumbs[index].type !== favParent.type)) {
                                index -= 1;
                            }

                            if (index >= 0) {
                                breadcrumbs = [this.favouritesFolder, ...breadcrumbs.slice(index)];
                            } else {
                                breadcrumbs = [this.favouritesFolder, ...breadcrumbs];
                            }
                        }

                        const nodeCount = this.nodes$.value.length;
                        if (nodeCount > 1) {
                            // When navigating to a folder from favourites, add the node name
                            // to the first folder breadcumb when there are multiple nodes
                            const favChild = breadcrumbs[1] as Folder<Raw> | Page<Raw> | Template<Raw>;
                            if (favChild && favChild.path) {
                                const nodeName = (/^\/([^/]+)\//.exec(favChild.path))[1];
                                favChild.name = `${nodeName} / ${favChild.name}`;
                            }
                        }
                    }

                    this.parentItems$.next(breadcrumbs);
                });

            requests.push(allItemRequests, breadcrumbsRequest);
        }

        this.loading$.next(true);
        this.requestSubscriptions = Observable.forkJoin(...requests)
            .subscribe(
                () => this.loading$.next(false),
                this.errorHandler.catch,
            );
    }

    private fetchContentsOfType(
        itemType: AllowedSelectionType,
        parentId: number,
        nodeId: number,
        language?: string,
    ): Observable<RepoItem[]> {
        const sorting = this.sortOrder$.value[itemType];

        const listOptionsWithoutSorting: FolderListOptions = this.search$.value
            ? { nodeId,
                search: this.search$.value,
                recursive: true,
                folder: true,
                ...( language && { language }),
                ...( itemType === 'page' && { langvars: true }),
            }
            : { nodeId };

        const listOptionsWithSorting: FolderListOptions = {
            ...listOptionsWithoutSorting,
            sortby: sorting.field,
            sortorder: sorting.order,
            ...( language && { language }),
            ...( itemType === 'page' && { langvars: true }),
        };

        switch (itemType) {
            case 'folder': {
                return this.api.folders
                    .getFolders(parentId, listOptionsWithSorting)
                    .map(res => res.folders);
            }

            case 'form': {
                const formListRequest: FormListOptions = this.search$.value ?
                {
                    folderId: parentId,
                    q: this.search$.value,
                    recursive: true,
                }
                : { folderId: parentId };
                return this.api.forms
                    .getForms(formListRequest)
                    .map(res => res.items);
            }

            case 'page': {
                const requestTemplate = (itemType === 'page' && this.allowed.template || this.allowed.templatetag);
                let listOptions: PageListOptions = { ...listOptionsWithSorting };

                if (requestTemplate) {
                    listOptions = { ...listOptions, template: true, langvars: true };
                }

                if (this.includeMlId) {
                    listOptions = { ...listOptions, includeMlId: this.includeMlId };
                }

                return this.api.folders.getPages(parentId, listOptions).pipe(
                    map(res => res.pages),
                );
            }

            case 'image': {
                return this.api.folders.getImages(parentId, listOptionsWithSorting).pipe(
                    map(res => res.files),
                );
            }

            case 'file': {
                return this.api.folders.getFiles(parentId, listOptionsWithSorting).pipe(
                    map(res => res.files),
                );
            }

            case 'contenttag': {
                return this.api.folders
                    .getItem(parentId, 'page', listOptionsWithoutSorting)
                    .map((response: PageResponse) => {
                        const tagsHash = response.page.tags;
                        const tagsArray = Object.keys(tagsHash).map(key => tagsHash[key]);
                        return sortArrayByName(tagsArray, sorting.order);
                    });
            }

            case 'template': {
                return this.api.folders
                    .getTemplates(parentId, listOptionsWithSorting)
                    .map(response => response.templates)
                    .map(templates => {
                        // The API does not return a "type" for templates, so we manually add one.
                        for (const template of templates) {
                            template.type = 'template';
                        }
                        return templates;
                    });
            }

            case 'templatetag': {
                return this.api.folders
                    .getItem(parentId, 'template', listOptionsWithoutSorting)
                    .map((res: TemplateResponse) => {
                        const tagsHash = res.template.templateTags;
                        const tagsArray = Object.keys(tagsHash).map(key => tagsHash[key]);
                        return sortArrayByName(tagsArray, sorting.order);
                    });
            }

            default:
                return Observable.of([]);
        }
    }

    private determineUserPermissions(
        selected: ItemInNode[],
        parent: Folder | Page | Template | Node,
        nodeId: number, nodes: Node[],
        currentContentLanguage?: Language,
    ): Observable<boolean> {

        if (this.requiredPermissions == null) {
            return Observable.of(true);
        }
        if (!parent || !nodeId) {
            return Observable.of(false);
        }

        if (this.isDisplayingFavouritesFolder$.value) {
            // Inside the "Favourites" folder, folder permissions make no sense.
            if (this.pickingFolder) {
                return Observable.of(true);
            }

            // Permission management across multiple nodes and possibly moved folders is not trivial,
            // therefore we just call the `requiredPermissions` functions for every selected item.
            const observables = selected.map((selectedItem: any) => {
                const parent = { id: selectedItem.motherId || selectedItem.parentId, type: 'folder' } as Folder;
                const node = this.nodes$.value.find(n => n.id === selectedItem.__favourite__.nodeId);
                return this.requiredPermissions([selectedItem], parent, node, currentContentLanguage);
            });

            return combineLatest(observables).pipe(
                map(permissions => permissions.every(perm => !!perm)),
            );
        }

        return this.requiredPermissions(selected, parent, nodes.filter(n => n.id === nodeId)[0], currentContentLanguage)
            .map(returnValue => !!returnValue);
    }

    private determineFavouritesVisibility(): void {
        const displayedTypes = this.determineItemTypesToDisplay();
        const favourites = this.appState.now.favourites.list;
        const hasSelectableFav = favourites.some(item => displayedTypes.indexOf(item.type) >= 0);

        this.showFavourites$.next(hasSelectableFav);
    }

    /**
     * Filters an array of items and returns a new array containing only the items
     * with a matching name (all types), file name or id (both only for pages).
     */
    private filterItems(items: RepoItem[], filterTerm: string): RepoItem[] {
        const filterIsNumeric = /^\s*\d+\s*$/.test(filterTerm);
        const filterLower = filterTerm.toLowerCase();

        return items.filter(item => {
            if (item.name.toLowerCase().indexOf(filterLower) >= 0) {
                return true;
            }

            const fileName = (item as Page<Raw>).fileName;
            if (fileName && fileName.toLowerCase().indexOf(filterLower) >= 0) {
                return true;
            }

            if (filterIsNumeric && item.type === 'page' && item.id === (+filterTerm)) {
                return true;
            }

            return false;
        });
    }

    private fetchFavouritesFromAPI(): void {
        this.determineItemTypesToDisplay();
        const displayedTypes = this.itemTypesToDisplay$.value;
        const favouritesToLoad = this.appState.now.favourites.list.filter(fav => displayedTypes.indexOf(fav.type) >= 0);

        this.requestSubscriptions.unsubscribe();
        const requests = favouritesToLoad.map(({ id, type, nodeId }) =>
            this.api.folders.getItem(id, type, { nodeId })
                // If a favourite has been deleted, there will be an error here which we can ignore,
                // and just pass on a null value which we can filter out later.
                .catch(() => Observable.of(null)),
        );

        this.items$.next([]);
        this.loading$.next(true);

        this.requestSubscriptions = Observable.forkJoin(requests)
            .subscribe(responses => {
                const items: RepoItem[] = responses
                    // filter out null values caused by any errors in getting the favourite items
                    .filter(response => !!response)
                    .map((response: any) =>
                        response.folder || response.page || response.file || response.image || response.template,
                    );
                items.forEach((item: any, index) => item.__favourite__ = favouritesToLoad[index]);

                this.loading$.next(false);
                this.items$.next(items);
            }, this.errorHandler.catch);
    }

    private determineItemTypesToDisplay(): AllowedSelectionType[] {
        const currentParent = this.currentParent$.value;
        const parentType = currentParent && currentParent.type;
        const subject = this.itemTypesToDisplay$;
        const allowed = this.allowed;

        let typesToDisplay: AllowedSelectionType[];

        if (parentType === 'page') {
            typesToDisplay = allowed.contenttag ? ['contenttag'] : [];
        } else if (parentType === 'template') {
            typesToDisplay = allowed.templatetag ? ['templatetag'] : [];
        } else {
            typesToDisplay = ['folder'];

            if (allowed.page || allowed.contenttag) {
                typesToDisplay.push('page');
            }

            if (allowed.file) {
                typesToDisplay.push('file');
            }

            if (allowed.form) {
                typesToDisplay.push('form');
            }

            if (allowed.image) {
                typesToDisplay.push('image');
            }

            if (allowed.template) {
                typesToDisplay.push('template');
            }

            if (allowed.templatetag) {
                typesToDisplay.push('template');
            }
        }

        if (arraysAreIdentical(typesToDisplay, subject.value)) {
            return subject.value;
        } else {
            subject.next(typesToDisplay);
            return typesToDisplay;
        }
    }

    private openLiveUrl(url: string): void {
        const fittingNodes = this.nodes$.value;
        if (!fittingNodes.length) {
            return;
        }

        const options: SearchPagesOptions = {
            liveUrl: url.replace(/^https?:\/\//, ''),
            update: false,
            template: true,
            langvars: true,
        };
        this.loading$.next(true);

        interface Result {
            node: Node<Raw>;
            page?: Page<Raw>;
            found: boolean;
        }

        // Because one domain can fit multiple nodes, we need to find the node containing the page
        const nodeRequests: Observable<Result>[] = fittingNodes.map(node =>
            this.api.folders.searchPages(node.id, options)
                .map(response => ({ node, page: response.page, found: true }))
                .catch(() => Observable.of({ node, found: false })),
        );

        const searchSub = forkJoin(nodeRequests)
            .subscribe(allResults => {
                const succeeded = allResults.filter(res => res.found);

                if (succeeded.length) {
                    let resultToUse: Result;
                    let resultForCurrentNode = succeeded.find(result => result.node.id === this.currentNodeIdSubjectWrapper.value());

                    // If the user pastes a URL that matches in more than one node, show a warning message
                    if (succeeded.length === 1) {
                        resultToUse = succeeded[0];
                    } else if (resultForCurrentNode) {
                        resultToUse = resultForCurrentNode;
                    } else {
                        resultToUse = succeeded[0];
                        this.notification.show({
                            message: 'message.page_liveurl_multiple_nodes',
                            translationParams: {
                                count: succeeded.length,
                                nodes: succeeded.map(s => s.node.name).join('\n'),
                            },
                        });
                    }

                    // Change to matching node and parent
                    let { node, page } = resultToUse;
                    if (node.id !== this.currentNodeIdSubjectWrapper.value()) {
                        this.currentNodeIdSubjectWrapper.next(node.id);
                    }

                    if (this.allowed.contenttag) {
                        this.changeParent(page);
                    } else {
                        // Small workaround: To compare by reference, we replace the parent item
                        // after fetching the data from the API.
                        this.currentParent$.next({ id: page.folderId, type: 'folder' } as Folder<Raw>);
                        const sub = this.loading$
                            .skip(1)
                            .skipWhile(loading => loading === true)
                            .take(1)
                            .subscribe(() => {
                                const folder = this.parentItems$.value.find(p => p.id === page.folderId);
                                if (folder) {
                                    this.currentParent$.next(folder);
                                }

                                const pageByRef = this.items$.value.find(item => item.type === 'page' && item.id === page.id);
                                this.selectItem(pageByRef);
                            });
                        this.subscriptions.add(sub);
                        this.fetchContentsFromAPI();
                    }
                } else {
                    this.loading$.next(false);
                    this.notification.show({
                        message: 'message.page_liveurl_not_found',
                        translationParams: { url },
                    });
                }
            });
        this.subscriptions.add(searchSub);
    }

    private isLanguage = (language: any): language is Language => {
        return typeof (language as Language).code === 'string';
    }

}

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function itemArraysAreIdentical<T extends { id: number, inherited?: boolean }>(a: T[], b: T[]): boolean {
    return a.length === b.length && a.every((left, index) =>
        left.id === b[index].id && left.inherited === b[index].inherited && Object.keys(left) === Object.keys(b[index]));
}

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function arraysAreIdentical<T>(a: T[], b: T[]): boolean {
    return a.length === b.length && a.every((left, index) => b[index] === left);
}

function sortArrayByName<T extends { name: string }>(array: T[], order: 'asc' | 'desc'): T[] {
    if (order === 'asc') {
        return array.sort((a, b) => a.name < b.name ? -1 : (a.name > b.name ? 1 : 0));
    } else {
        return array.sort((a, b) => a.name < b.name ? 1 : (a.name > b.name ? -1 : 0));
    }
}

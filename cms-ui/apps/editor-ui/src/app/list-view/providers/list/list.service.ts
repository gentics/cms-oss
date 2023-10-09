import { LocationStrategy } from '@angular/common';
import { Injectable, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { File as FileModel, Folder, Form, Image, Node, Normalized, Page } from '@gentics/cms-models';
import { isEqual } from'lodash-es'
import { Observable, Subscription, combineLatest, merge } from 'rxjs';
import {
    debounceTime,
    distinctUntilChanged,
    filter,
    map,
    pairwise,
    publishReplay,
    refCount,
    skip,
    switchMap,
    switchMapTo,
    tap,
    withLatestFrom,
} from 'rxjs/operators';
import { AppState, GtxChipSearchSearchFilterMap, ItemsInfo } from '../../../common/models';
import { isLiveUrl } from '../../../common/utils/is-live-url';
import { ListUrlParams, NavigationService } from '../../../core/providers/navigation/navigation.service';
import { ApplicationStateService, FolderActionsService } from '../../../state';

function sortOrderEqual(a: ItemsInfo, b: ItemsInfo): boolean {
    return a.sortBy === b.sortBy && a.sortOrder === b.sortOrder;
}

/**
 * The ListService orchestrates all the various parts of the state which can affect the contents of the list view (current folder, search
 * and filtering, sorting, pagination settings), triggers API calls at the appropriate times, and exposes the final lists via its public
 * observable properties.
 */
@Injectable()
export class ListService implements OnDestroy {

    itemInfoStreams: {
        folder: Observable<ItemsInfo>;
        form: Observable<ItemsInfo>;
        page: Observable<ItemsInfo>;
        image: Observable<ItemsInfo>;
        file: Observable<ItemsInfo>;
    };
    itemStreams: {
        folder: Observable<Folder<Normalized>[]>;
        form: Observable<Form<Normalized>[]>;
        page: Observable<Page<Normalized>[]>;
        image: Observable<Image<Normalized>[]>;
        file: Observable<FileModel<Normalized>[]>;
    };

    updatingByUrlParams = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private state: ApplicationStateService,
        private folderActions: FolderActionsService,
        private navigationService: NavigationService,
        private location: LocationStrategy,
        private router: Router,
    ) {}

    /**
     * Initializes all data streams.
     */
    init(route: ActivatedRoute): void {
        const onLogin$ = this.state.select(state => state.auth).pipe(
            distinctUntilChanged(isEqual, state => state.currentUserId),
            filter(state => state.isLoggedIn === true),
        );
        const routeParams$ = onLogin$.pipe(
            switchMapTo(route.params as Observable<ListUrlParams>),
            publishReplay(1),
            refCount(),
        );

        const activeFolderId$ = onLogin$.pipe(
            switchMapTo(this.state.select(state => state.folder.activeFolder)),
        );
        const searchTerm$ = this.state.select(state => state.folder.searchTerm);

        const filterTerm$ = combineLatest([
            this.state.select(state => state.folder.filterTerm),
            this.state.select(state => state.entities.node),
        ]).pipe(
            // we do not want to set the filter
            // term when user pastes a liveUrl
            filter(([term, nodes]) => !isLiveUrl(term, Object.values(nodes).map((node: Node) => node.host))),
            map(([term]) => term),
        );

        const searchFiltersVisible$ = this.state.select(state => state.folder.searchFiltersVisible);
        const searchFiltersChanged$ = this.state.select(state => state.folder.searchFiltersChanging);
        const searchFiltersValid$ = this.state.select(state => state.folder.searchFiltersValid);

        this.initOutputStreams();
        this.initNavigationSubscriptions(routeParams$);
        this.initLanguageChangeSubscriptions(activeFolderId$);
        this.initSortSubscriptions();
        this.initFilterSubscriptions(
            searchTerm$,
            searchFiltersVisible$,
            searchFiltersChanged$,
            searchFiltersValid$,
            activeFolderId$,
        );
        this.initPaginationSubscriptions(searchTerm$, filterTerm$, activeFolderId$);
    }

    /**
     * Set up the output observables
     */
    private initOutputStreams(): void {
        const itemInfoStreams = this.itemInfoStreams = {
            file: this.state.select(state => state.folder.files),
            folder: this.state.select(state => state.folder.folders),
            form: this.state.select(state => state.folder.forms),
            image: this.state.select(state => state.folder.images),
            page: this.state.select(state => state.folder.pages),
        };
        this.itemStreams = {} as any;
        for (const key of Object.keys(itemInfoStreams) as Array<keyof typeof itemInfoStreams>) {
            this.itemStreams[key] = itemInfoStreams[key].pipe(
                map(itemInfo => itemInfo.list),
                distinctUntilChanged(isEqual),
                switchMap((ids: number[]) =>
                    this.state.select(state => state.entities).pipe(
                        map(entities => entities[key]),
                        distinctUntilChanged((a, b) => a === b || ids.every(id => a[id] === b[id])),
                        map(entityHash => ids.map(id => entityHash[id] as any)),
                    ),
                ),
            );
        }
    }

    /**
     * Set up subscriptions which react to changes in the current folder or node.
     */
    private initNavigationSubscriptions(routeParams$: Observable<ListUrlParams>): void {
        // Updates the internal state from the route
        const locationHandling$ = routeParams$.pipe(
            map((params) => ({
                nodeId: Number(params.nodeId),
                folderId: Number(params.folderId),
            })),
            // distinctUntilChanged(isEqual),
            tap(({ nodeId, folderId }) => {
                const { activeNode, activeFolder } = this.state.now.folder;

                // Only update if the node has changed
                if (activeNode !== nodeId) {
                    this.folderActions.setActiveNode(nodeId);
                }
                // Only update if the folder has changed
                if (activeFolder !== folderId) {
                    this.folderActions.setActiveFolder(folderId);
                }
            }),
        );

        // Build searchTerm from URL change
        const searchTermParam$ = routeParams$.pipe(
            map(params => params.searchTerm || ''),
            distinctUntilChanged(isEqual),
        );

        // Build searchFilters from URL change
        const searchFiltersParam$ = routeParams$.pipe(
            map(params => {
                try {
                    return this.navigationService.deserializeOptions<any>(params.searchFilters);
                } catch (e) {
                    return null;
                }
            }),
            distinctUntilChanged(isEqual),
        );

        const folderIdParam$ = combineLatest([
            locationHandling$,
            searchTermParam$,
            searchFiltersParam$,
            // A change needs to be emitted when the state changes
            this.state.select(state => state.folder.activeNode),
            this.state.select(state => state.folder.activeFolder),
        ]).pipe(
            map(([locationData, searchTerm, searchFilters, activeNode, activeFolder]) => ({
                nodeId: locationData.nodeId,
                folderId: locationData.folderId,
                searchTerm,
                searchFilters,
                activeNode,
                activeFolder,
            })),
            distinctUntilChanged((x, y) =>  isEqual(x, y)),
            map(params => {
                this.updatingByUrlParams = true;

                // Set search term if its changed in the url or it will be an empty string
                this.folderActions.setSearchTerm(params.searchTerm);
                this.updatingByUrlParams = false;

                return params.folderId;
            }),
        );

        const folderIdSub = folderIdParam$.pipe(debounceTime(50)).subscribe((folderId) => {
            if (Number.isInteger(folderId)) {
                // in case search is active, reset in order to access folder contents
                this.folderActions.resetSearchFilters();
                this.folderActions.getAllFolderContents(folderId, this.state.now.folder.searchTerm, false, true);
            }
        });

        this.subscriptions.push(folderIdSub);
    }

    /**
     * Set up a subscription which reacts to changes in the active language of the page list.
     */
    private initLanguageChangeSubscriptions(activeFolderId$: Observable<number>): void {
        // Fetch the list of pages when the activeLanguage changes
        this.subscriptions.push(combineLatest([
            this.state.select(state => state.folder.activeLanguage).pipe(
                filter(languageId => languageId != null),
                distinctUntilChanged(),
                skip(1),
            ),
            activeFolderId$.pipe(
                filter(folderId => folderId != null),
                distinctUntilChanged(),
            ),
            this.state.select(state => state.folder).pipe(
                distinctUntilChanged(isEqual),
            ),
        ]).subscribe(([, currentFolderId, folderState]) => {
            this.folderActions.getPages(currentFolderId, false, folderState.searchTerm);
        }));
    }

    /**
     * Set up subscriptions which react to changes in the sorting preferences of a list.
     */
    private initSortSubscriptions(): void {
        // When the sort order is changed for an item type, re-fetch the items of that type.
        const sortStream = (selector: (state: AppState) => ItemsInfo, handler: typeof FolderActionsService.prototype.getFolders) =>
            this.state.select(selector).pipe(
                distinctUntilChanged(sortOrderEqual),
                skip(1),
                map(itemsInfo => ({
                    handler: handler.bind(this.folderActions) as typeof FolderActionsService.prototype.getFolders,
                    itemsInfo,
                })),
            );

        /* eslint-disable @typescript-eslint/unbound-method */
        const sortingStreams$ = merge(
            sortStream(state => state.folder.folders, this.folderActions.getFolders),
            sortStream(state => state.folder.pages, this.folderActions.getPages),
            sortStream(state => state.folder.files, this.folderActions.getFiles),
            sortStream(state => state.folder.images, this.folderActions.getImages),
            sortStream(state => state.folder.forms, this.folderActions.getForms),
        );
        /* eslint-enable @typescript-eslint/unbound-method */

        this.subscriptions.push(sortingStreams$.subscribe(({ handler, itemsInfo }) => {
            const folderState = this.state.now.folder;
            handler(folderState.activeFolder, false, folderState.searchTerm);
        }));
    }

    /**
     * Sets up the subscriptions which react to changes in the search term.
     */
    private initFilterSubscriptions(
        searchTerm$: Observable<string>,
        searchFiltersVisible$: Observable<boolean>,
        searchFiltersChanged$: Observable<boolean>,
        searchFiltersValid$: Observable<boolean>,
        activeFolderId$: Observable<number>,
    ): void {

        // Emits when any filter-related factors impacting folder items list change and requests folder items accordingly.
        // This is true also for the default FolderContents view without any filter-settings applied from user-perspective.
        const searchTermSub = combineLatest([
            searchFiltersVisible$,
            searchFiltersValid$,
            searchFiltersChanged$,
        ]).pipe(
            filter(() => !this.updatingByUrlParams),
            debounceTime(50),
            withLatestFrom(
                activeFolderId$.pipe(
                    filter(activeFolderId => !!activeFolderId),
                ),
            ),
            skip(1),
        ).subscribe(([[searchFiltersVisible, searchFiltersValid], activeFolderId]) => {
            if (!searchFiltersVisible || !searchFiltersValid) {
                this.folderActions.resetSearchFilters();
            }
            this.folderActions.getAllItemsInFolder(activeFolderId);
        });

        // emits when searchfilter change and writes it to URL params in state
        const urlChangeSub = combineLatest([
            searchTerm$,
            this.state.select(state => state.folder.searchFilters),
        ]).pipe(
            filter(() => !this.updatingByUrlParams),
            withLatestFrom(this.state.select(state => state.features.elasticsearch)),
            // searchUrl for Advanced Search not yet implemented
            filter(([, elasticsearch]) => elasticsearch),
        ).subscribe(() => {
            const currentFilters = this.getCurrentFilters();
            this.setSearchUrl(
                this.state.now.folder.activeNode,
                this.state.now.folder.activeFolder,
                this.state.now.folder.searchTerm,
                currentFilters,
            );
        });

        this.subscriptions.push(searchTermSub, urlChangeSub);
    }

    private setSearchUrl(nodeId: number, folderId: number, searchTerm: string, searchFilters: any): void {
        const url = this.router
            .createUrlTree(
                this.navigationService.list(
                    nodeId,
                    folderId,
                    searchTerm,
                    searchFilters,
                ).commands(),
            ).toString();

        const elasticsearch = this.state.now.features.elasticsearch;

        // If its the same url, do not push to history!
        if (url !== this.location.path(true) && !elasticsearch) {
            this.location.pushState(null, 'Gentics CMS', url, '');
        }
    }
    private getCurrentFilters(): any {
        const currentState = this.state.now;
        let currentFilters = {} as any;
        // Object.keys(currentState.folder.searchFilters).map((key) => {
        //     if ((currentState.folder.searchFilters as any)[key] !== null || key === 'node') {
        //         currentFilters[key] = (currentState.folder.searchFilters as any)[key];
        //     }
        // });

        if (this.isDefaultFilterState(currentState.folder.searchFilters, currentState.folder.activeNode)) {
            currentFilters = null;
        }

        return currentFilters;
    }
    /**
     * Sets up subscriptions which handle changes to the pagination (currentPage and itemsPerPage) of items.rId$
     */
    private initPaginationSubscriptions(searchTerm$: Observable<string>, filterTerm$: Observable<string>, activeFolderId$: Observable<number>): void {
        const setUpPaginationSub = (
            branch: 'folders' | 'pages' | 'files' | 'forms' | 'images',
            handler: typeof FolderActionsService.prototype.getFolders,
        ): Subscription => {
            return combineLatest([
            // listen to pagination change current page
                this.state.select(state => state.folder[branch].currentPage),
                // listen to pagination change current page size
                this.state.select(state => state.folder[branch].itemsPerPage),

                this.state.select(state => state.folder[branch].fetchAll).pipe(
                // PROBLEM
                // as soon as fetchAll is TRUE, state batch operations will trigger indefinite state updates
                // thus, fetchall changes must be ignored after transgression from TRUE to FALSE

                    // getItems(fetchAll = FALSE)
                    // getItems(fetchAll = TRUE)
                    // => n times FolderStateActions.applyListBatch
                    // => n times trigger state change
                    // => n times getItems(fetchAll = FALSE) <- pointless

                    // SOLUTION
                    // ignore fetchAll changes if fetchAll became TRUE and becomes FALSE after
                    pairwise(),
                    filter(([old, current]) => {
                        if (old && !current) {
                            return false;
                        } else {
                            return true;
                        }
                    }),
                    map(([old, current]) => current),
                ),
            ]).pipe(
                withLatestFrom(searchTerm$, filterTerm$, activeFolderId$),
                skip(1),
            ).subscribe(([[currentPage, itemsPerPage, fetchAll], searchTerm, filterTerm, activeFolderId]) => {
                if (!fetchAll) {
                    const boundHandler: typeof FolderActionsService.prototype.getFolders = handler.bind(this.folderActions);
                    boundHandler(activeFolderId, fetchAll, searchTerm);
                }
            });
        };

        /* eslint-disable @typescript-eslint/unbound-method */
        this.subscriptions.push(setUpPaginationSub('folders', this.folderActions.getFolders));
        this.subscriptions.push(setUpPaginationSub('pages', this.folderActions.getPages));
        this.subscriptions.push(setUpPaginationSub('files', this.folderActions.getFiles));
        this.subscriptions.push(setUpPaginationSub('images', this.folderActions.getImages));
        this.subscriptions.push(setUpPaginationSub('forms', this.folderActions.getForms));
        /* eslint-enable @typescript-eslint/unbound-method */
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    /**
     * Predicate which returns true if the searchFilters are in the default state, i.e. the only filter
     * set is the "node", set to the value of "activeNodeId".
     */
    private isDefaultFilterState(filters: GtxChipSearchSearchFilterMap, activeNodeId: number): boolean {
        const defaultFilter: any = {};
        const nonNullMap: any = {...filters};

        for (const key in nonNullMap) {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            if (nonNullMap.hasOwnProperty(key) && nonNullMap[key] == null) {
                delete nonNullMap[key];
            }
        }

        return isEqual(nonNullMap, defaultFilter);
    }

}

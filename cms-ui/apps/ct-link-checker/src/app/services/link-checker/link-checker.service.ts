import { Injectable } from '@angular/core';
import { ExternalLinkStatistics, Language, LinkCheckerPageList, Node, NodeFeature, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { uniqWith } from'lodash-es'
import { BehaviorSubject, combineLatest, Observable, Subject } from 'rxjs';
import { concatMap, debounceTime, distinctUntilChanged, filter, first, map, switchMap, tap, toArray } from 'rxjs/operators';
import { FilterOptions } from '../../common/models/filter-options';
import { AppService } from '../app/app.service';
import { FilterService } from '../filter/filter.service';
import { UserSettingsService } from '../user-settings/user-settings.service';

export interface LinkCheckerLoaders {
    pages: Observable<boolean>;
    nodes: Observable<boolean>;
    globalStats: Observable<boolean>;
    nodeStats: Observable<boolean>;
}

export interface NodeWithStats {
    node: Node<Raw>;
    stats: ExternalLinkStatistics;
}

export interface NodeStats {
    [key: number]: ExternalLinkStatistics;
}

@Injectable({
    providedIn: 'root'
    })
export class LinkCheckerService {

    protected pages$ = new Subject<LinkCheckerPageList>();
    protected nodes$ = new Subject<NodeWithStats[]>();
    protected nodeStats$ = new BehaviorSubject<NodeStats>({});

    protected globalStatsLoading$ = new BehaviorSubject<boolean>(false);
    protected nodeStatsLoading$ = new BehaviorSubject<boolean>(false);
    protected pagesLoading$ = new BehaviorSubject<boolean>(false);
    protected nodesLoading$ = new BehaviorSubject<boolean>(false);

    constructor(
        private userSettings: UserSettingsService,
        private filterService: FilterService,
        private api: GcmsApi,
        private appService: AppService,
    ) { }

    fetchStats(nodeId?: number): Observable<ExternalLinkStatistics> {
        if (nodeId) {
            this.nodeStatsLoading$.next(true);
        } else {
            this.globalStatsLoading$.next(true);
        }

        return this.api.linkChecker.getStats(nodeId).pipe(
            first(),
            tap(stats => nodeId ? this.nodeStats$.next({
                ...this.nodeStats$.value,
                [nodeId]: stats,
            }) : null),
            tap(() => nodeId ? this.nodeStatsLoading$.next(false) : this.globalStatsLoading$.next(false)),
        );
    }

    hasNodeLinkCheckerFeatureEnabled(nodeId: number): Observable<boolean> {
        return this.api.folders.getNodeFeatures(nodeId)
            .pipe(
                map(response => !!response.features.find(feature => feature === NodeFeature.LINK_CHECKER)),
            );
    }

    fetchNodes(): void {
        this.nodesLoading$.next(true);
        this.api.folders.getNodes().pipe(
            first(),
            concatMap(response => response.nodes),
            concatMap(node => this.hasNodeLinkCheckerFeatureEnabled(node.id).pipe(
                first(),
                filter(hasFeatureEnabled => hasFeatureEnabled),
                map(() => node),
            )),
            concatMap(node => this.api.linkChecker.getStats(node.id).pipe(
                map(stats => ({ node, stats })),
            )),
            toArray(),
        ).subscribe(nodes => {
            this.nodes$.next(nodes);

            let nodeStats: NodeStats = {};

            nodes.forEach(node => {
                nodeStats = {
                    ...nodeStats,
                    [node.node.id]: node.stats,
                };
            });

            this.nodeStats$.next(nodeStats);

            const nodeId = this.filterService.options.nodeId;
            const firstNode = (nodes.slice().shift() || {}).node;

            // Select default node if null
            if (nodeId === null && !!firstNode && !!firstNode.id) {
                this.filterService.options.nodeId = firstNode.id;
            }

            this.nodesLoading$.next(false);
        });
    }

    fetchNodeLanguages(nodeId: number): Observable<Language[]> {
        return this.api.folders.getLanguagesOfNode(nodeId)
            .pipe(
                first(),
                map(response => uniqWith(response.languages, (o1, o2) => o1.id === o2.id)),
            );
    }

    fetchPages(): void {
        this.pagesLoading$.next(true);
        this.api.linkChecker.getPages({})
            .pipe(first())
            .subscribe(response => {
                this.pages$.next(response);
                this.pagesLoading$.next(false);
            });
    }

    fetchFilteredPages(): void {
        combineLatest([
            this.filterService.options.events$ as Observable<FilterOptions>,
            this.appService.update$,
        ]).pipe(
            map(([options]) => ({
                editable: options.editable,
                iscreator: options.isCreator,
                iseditor: options.isEditor,
                nodeId: options.nodeId,
                page: options.page,
                pageSize: options.pageSize,
                language: options.languages,
                q: options.searchTerm,
                sort: options.sortOptions,
                status: options.status,
                online: options.online,
            })),
            distinctUntilChanged(),
            debounceTime(100),
            switchMap((requestOptions) => {
                this.pagesLoading$.next(true);

                // Remove null / undefined properties
                Object.keys(requestOptions).forEach((key) => (requestOptions[key] == null) && delete requestOptions[key]);
                return this.api.linkChecker.getPages(requestOptions).pipe(first());
            }),
        )
            .subscribe(response => {
                this.pages$.next(response);
                this.pagesLoading$.next(false);
            });
    }

    getNodes(): Observable<Node<Raw>[]> {
        return this.nodes$.pipe(
            map(nArr => nArr.map(nodeWithStats => nodeWithStats.node)),
        );
    }

    getNodeStats(): Observable<NodeStats> {
        return this.nodeStats$.asObservable();
    }

    getPages(): Observable<LinkCheckerPageList> {
        return this.pages$.asObservable();
    }

    getLoaders(): LinkCheckerLoaders {
        return {
            pages: this.pagesLoading$.asObservable(),
            nodes: this.nodesLoading$.asObservable(),
            globalStats: this.globalStatsLoading$.asObservable(),
            nodeStats: this.nodeStatsLoading$.asObservable(),
        };
    }

    setUserSettings(): void {
        this.userSettings.setUserSettings({
            displayFields: this.appService.settings.displayFields,
            sortOptions: this.filterService.options.sortOptions,
        })
            .pipe(first())
            .subscribe();
    }
}

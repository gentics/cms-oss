import { ChangeDetectionStrategy, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Language, Node, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { I18nService } from '@gentics/cms-components';
import { combineLatest, Observable, Subscription } from 'rxjs';
import { distinctUntilChanged, flatMap, map, publishReplay, refCount, switchMap, withLatestFrom } from 'rxjs/operators';
import { FilterOptions } from '../../common/models/filter-options';
import { AppService } from '../../services/app/app.service';
import { FilterService } from '../../services/filter/filter.service';
import { LinkCheckerService, NodeStats } from '../../services/link-checker/link-checker.service';

@Component({
    selector: 'gtxct-filter-options',
    templateUrl: './filter-options.component.html',
    styleUrls: ['./filter-options.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FilterOptionsComponent implements OnInit, OnDestroy {
    @ViewChild('searchBar', { static: true }) searchBar: ElementRef;

    linkFilter: string;
    pageStatusFilter: string;

    nodes$: Observable<Node<Raw>[]>;

    stats$: Observable<NodeStats>;
    nodeStats$: Observable<any>;
    nodeLanguages$: Observable<Language[]>;
    currentNode$: Observable<Node<Raw>>;
    options$: Observable<FilterOptions>;

    statsLoading$: Observable<boolean>;

    /** page status options */
    pageStatusOptions: { value: string; label: string }[] = [
        {
            value: 'all',
            label: 'common.all',
        },
        {
            value: 'online',
            label: 'common.online',
        },
        {
            value: 'offline',
            label: 'common.offline',
        },
    ];

    /** filter options for links */
    filterOptions: { value: string; label: string }[] = [
        {
            value: 'valid',
            label: 'common.valid',
        },
        {
            value: 'invalid',
            label: 'common.invalid',
        },
        {
            value: 'unchecked',
            label: 'common.unchecked',
        },
    ];

    private subscriptions = new Subscription();
    private languageOptions = [];

    get filterLinkStatus(): 'valid' | 'invalid' | 'unchecked' {
        return this.filterService.options.status;
    }

    constructor(
        public app: AppService,
        public filterService: FilterService,
        public api: GcmsApi,
        private linkChecker: LinkCheckerService,
        private translate: I18nService,
    ) { }

    ngOnInit(): void {

        const loaders = this.linkChecker.getLoaders();
        this.statsLoading$ = loaders.nodeStats.pipe(
            publishReplay(1),
            refCount(),
        );

        this.nodes$ = this.linkChecker.getNodes();
        this.stats$ = this.linkChecker.getNodeStats();
        this.options$ = this.filterService.options.events$;

        // Current nodeId
        const nodeId$ = this.options$.pipe(
            map((options) => options.nodeId),
            distinctUntilChanged((a, b) => a === b),
            withLatestFrom(this.nodes$),
            map(([nodeId, nodes]) => {
                const firstNode = nodes.slice().shift();

                // Select default node if null
                if (nodeId === null && !!firstNode && !!firstNode.id) {
                    this.filterService.options.nodeId = firstNode.id;
                }

                return nodeId;
            }),
        );

        // Current node
        this.currentNode$ = combineLatest([this.nodes$, nodeId$]).pipe(
            flatMap(([nodes, nodeId]) => nodes.filter((node) => node.id === nodeId)),
        );

        // Fetch nodeLanguages
        this.nodeLanguages$ = this.currentNode$.pipe(switchMap((node) => this.linkChecker.fetchNodeLanguages(node.id)));

        this.subscriptions.add(this.translate.onLanguageChange().subscribe(() => {
            this.linkFilter = 'invalid';
            this.pageStatusFilter = 'all';
        }));

        // Fetch nodeStats
        this.nodeStats$ = this.currentNode$.pipe(switchMap((node) => this.linkChecker.fetchStats(node.id)));

        // Fetch All nodes
        this.app.update$.subscribe(() => {
            this.linkChecker.fetchNodes();
        });
    }

    selectEditor(event: boolean): void {
        this.filterService.options.isEditor = event;
    }

    /**
     * @param all If TRUE, display all links
     */
    setLinkFilter(status: 'invalid' | 'unchecked' | 'valid'): void {
        this.filterService.options.status = status;
    }

    /**
     * Set the page status filter
     * @param pageStatusFilter selected value of page status filter
     */
    setPageStatusFilter(pageStatusFilter: 'all' | 'online' | 'offline'): void {
        switch (pageStatusFilter) {
            case 'all':
                this.filterService.options.online = null;
                break;
            case 'online':
                this.filterService.options.online = true;
                break;
            case 'offline':
                this.filterService.options.online = false;
                break;
        }
    }

    selectNode(event: number): void {
        this.subscriptions.add(this.linkChecker.fetchNodeLanguages(event).subscribe((languages) => {
            if (languages.length < 2) {
                this.filterService.options.languages = [];
            } else {
                this.filterService.options.languages = this.languageOptions;
            }
        }));
        this.filterService.options.nodeId = event;
    }

    toggleEditable(): void {
        this.filterService.options.editable = !this.filterService.options.editable;
    }

    search(event: string): void {
        this.filterService.options.searchTerm = event;
    }

    toggleLanguage(event: any): void {
        const languages = this.filterService.options.languages;

        const langId = languages.indexOf(event.id);
        if (langId !== -1) {
            languages.splice(langId, 1);
        } else {
            languages.push(event.id);
        }

        // Save fresh copy
        this.filterService.options.languages = languages.slice();

        this.languageOptions = languages.slice();
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }
}

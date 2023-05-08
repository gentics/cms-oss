import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { LinkCheckerPageList } from '@gentics/cms-models';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { distinctUntilChanged, filter, map, publishReplay, refCount, startWith } from 'rxjs/operators';
import { AppSettings } from '../../common/models/app-settings';
import { FilterOptions } from '../../common/models/filter-options';
import { ItemsInfo } from '../../common/models/items-info';
import { AppService } from '../../services/app/app.service';
import { FilterService } from '../../services/filter/filter.service';
import { LinkCheckerService } from '../../services/link-checker/link-checker.service';

@Component({
    selector: 'gtxct-list-view',
    templateUrl: './list-view.component.html',
    styleUrls: ['./list-view.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ListViewComponent implements OnInit {
    loading$: Observable<boolean>;
    pages$: Observable<LinkCheckerPageList>;
    itemsInfo$: Observable<ItemsInfo>;
    options$: Observable<FilterOptions>;
    appSettings$: Observable<AppSettings>;

    expandedPages$ = new BehaviorSubject<number[]>([]);

    constructor(
        private appService: AppService,
        public filterService: FilterService,
        private linkChecker: LinkCheckerService
    ) { }

    ngOnInit(): void {
        const loaders = this.linkChecker.getLoaders();
        this.loading$ = combineLatest(loaders.pages, loaders.nodes).pipe(
            map(([pagesLoading, nodesLoading]) => pagesLoading || nodesLoading),
            publishReplay(1),
            refCount()
        );

        this.options$ = this.filterService.options.events$;
        this.appSettings$ = this.appService.settings.events$;

        this.pages$ = this.linkChecker.getPages();

        const pagingOptions$ = this.options$.pipe(
            distinctUntilChanged((a, b) => a.page === b.page && a.pageSize === b.pageSize),
            startWith(this.filterService.options)
        );

        const displayFields$ = this.appSettings$.pipe(
            distinctUntilChanged((a, b) => a.displayFields === b.displayFields),
            startWith(this.appService.settings)
        );

        this.itemsInfo$ = combineLatest(pagingOptions$, displayFields$, this.pages$).pipe(
            filter(([options, appSettings, pages]) => !!pages),
            map(([options, appSettings, pages]) => {
                const numItems = pages.numItems || 0;
                return ({
                    total: numItems,
                    perPage: options.pageSize,
                    currentPage: options.page,
                    displayFields: appSettings.displayFields
                });
            }),
            startWith({
                total: 0,
                perPage: this.filterService.options.pageSize,
                currentPage: this.filterService.options.page,
                displayFields: this.appService.settings.displayFields
            })
        );

        // Fetch filtered pages
        this.linkChecker.fetchFilteredPages();
    }

    expandedPagesChange(expandedArray: number[]): void {
        this.expandedPages$.next(expandedArray);
    }

    /**
     * When the page is changed by the pagination controls, update the currentPage in the state and
     * scroll the active ItemList into view.
     */
    pageChange(pageNumber: number): void {
        this.filterService.options.page = pageNumber;
    }
}

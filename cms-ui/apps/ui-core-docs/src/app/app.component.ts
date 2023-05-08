import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { SplitViewContainerComponent } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { IPageInfo, kebabToPascal, PAGES, PageType } from './common/page-list';

// Exposed globally by the Webpack DefinePlugin
// (see webpack config)
declare const VERSION: string; /* eslint-disable-line @typescript-eslint/naming-convention */
declare const LATESTBRANCH: boolean; /* eslint-disable-line @typescript-eslint/naming-convention */

interface ContentItem {
    title: string;
    keywords: string[];
    route: string;
    type: PageType,
}

@Component({
    selector: 'app',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
/* eslint-disable-next-line @angular-eslint/component-class-suffix */
export class App implements OnInit, OnDestroy {

    @ViewChild(SplitViewContainerComponent, { static: true })
    splitViewContainer: SplitViewContainerComponent;

    version: string;
    latestBranch: boolean;
    changelogBranch = 'master';
    contentItems: ContentItem[] = PAGES.map((page: IPageInfo) => {
        return {
            title: kebabToPascal(page.path),
            route: '/' + page.path,
            keywords: page.keywords || [],
            type: page.type,
        };
    });
    filteredContentItems: any[];
    hasContent = false;
    splitFocus = 'left';
    searchQuery = '';
    subscription: Subscription;

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private titleService: Title,
    ) {
        this.filteredContentItems = this.contentItems.slice(0);
        titleService.setTitle(`Gentics UI Core Docs v${VERSION}`);
        this.version = VERSION;
        this.latestBranch = LATESTBRANCH;

        if (!this.latestBranch) {
            this.changelogBranch = `v${VERSION}`;
        }
    }

    ngOnInit(): void {
        this.subscription = this.router.events
            .pipe(filter(event => event instanceof NavigationEnd))
            .subscribe(() => {
                const path = this.route.snapshot.firstChild.url[0].path;
                this.hasContent = (path !== '' && path !== 'index');
                if (this.hasContent) {
                    this.splitFocus = 'right';
                }
                this.splitViewContainer.scrollRightPanelTo(0);
            });
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    filterContentItems(query: string): void {
        if (!query || 0 > query.length) {
            this.filteredContentItems = this.contentItems.slice(0);
            return;
        }

        const match = (needle: string, haystack: string): boolean => {
            return -1 < haystack.toLowerCase().indexOf(needle.toLowerCase());
        };

        this.filteredContentItems = this.contentItems.filter((item: { title: string, keywords: string[] }) => {
            const titleMatch = match(query, item?.title);
            const keywordMatch = item.keywords.reduce((res: boolean, keyword: string) => {
                return res || match(query, keyword);
            }, false);
            return titleMatch || keywordMatch;
        });
    }

    resetFilter(): void {
        this.searchQuery = '';
        this.filterContentItems('');
    }

    goToRoute(): void {
        this.focusRightPanel();
    }

    closeContent(): void {
        this.hasContent = false;
    }

    private focusRightPanel(): void {
        this.hasContent = true;
        setTimeout(() => this.splitFocus = 'right');
    }
}

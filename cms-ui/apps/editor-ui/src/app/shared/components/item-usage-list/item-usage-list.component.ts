import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, Output } from '@angular/core';
import { BaseUsageOptions, File, Image, Item, Language, Page, UsageInPagesOptions } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { cancelEvent, ChangesOf } from '@gentics/ui-core';
import { Observable, of, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { LinkType, UsageType } from '../../../common/models';

export interface PageLoadStartEvent {
    page: number;
    wasLoaded: boolean;
}

export interface PageLoadEndEvent {
    page: number;
    totalCount: number;
}

interface ItemUsagePage {
    items: any[];
    totalCount: number;
}

let instanceCounter = 0;

@Component({
    selector: 'gtx-item-usage-list',
    templateUrl: './item-usage-list.component.html',
    styleUrls: ['./item-usage-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ItemUsageListComponent implements OnChanges, OnDestroy {

    public readonly UNIQUE_ID = `item-usage-list-${instanceCounter++}`;

    @Input()
    public type: UsageType | LinkType;

    @Input()
    public item: Page | Image | File;

    @Input()
    public nodeId: number;

    @Input()
    public activeNodeId: number;

    @Input()
    public languages: Language[] = [];

    @Input()
    public currentLanguageId: number;

    @Output()
    public pageLoadStart = new EventEmitter<PageLoadStartEvent>();

    @Output()
    public pageLoadEnd = new EventEmitter<PageLoadEndEvent>();

    @Output()
    public itemClick = new EventEmitter<Item>();

    // Load state
    public loading = false;
    public loaded = false;

    // Pagination data
    public page = 1;
    public perPage = 10;
    public totalCount = 0;

    // Display data
    public displayItems: Item[] = [];

    private subscriptions: Subscription[] = [];
    private loadSubscrition: Subscription | null = null;

    constructor(
        private changeDectector: ChangeDetectorRef,
        private client: GCMSRestClientService,
    ) {}

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.type || changes.item || changes.nodeId || changes.currentLanguageId) {
            this.loaded = false;
            this.loadPage(0, true);
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    triggerItemClick(item: Item, event?: Event): void {
        cancelEvent(event);
        this.itemClick.emit(item);
    }

    public loadPage(page: number, force: boolean = false): void {
        if ((this.loading || this.page === page) && !force) {
            return;
        }

        if (this.loadSubscrition != null) {
            this.loadSubscrition.unsubscribe();
        }

        this.loading = true;
        this.page = page;
        this.changeDectector.markForCheck();

        this.pageLoadStart.emit({ page, wasLoaded: this.loaded });

        this.loadSubscrition = this.loadUsageForItem().subscribe(res => {
            this.loading = false;
            this.loaded = true;
            this.totalCount = res.totalCount;
            this.displayItems = res.items;
            this.changeDectector.markForCheck();

            this.pageLoadEnd.emit({ page, totalCount: this.totalCount });
        });
    }

    private loadUsageForItem(): Observable<ItemUsagePage> {
        switch (this.type) {
            case 'folder':
                return this.loadUsageInFolders();
            case 'page':
                return this.loadUsageInPages();
            case 'file':
                return this.loadUsageInFiles();
            case 'image':
                return this.loadUsageInImages();
            case 'template':
                return this.loadUsageInTemplates();
            case 'tag':
                return this.loadUsageInTags();
            case 'variant':
                return this.loadUsageInVariants();
            case 'linkedPage':
                return this.loadLinkedToPages();
            case 'linkedImage':
                return this.loadLinkedToImages();
            case 'linkedFile':
                return this.loadLinkedToFiles();
            default:
                return of({ totalCount: 0, items: [] });
        }
    }

    private getLoadOptions(): BaseUsageOptions {
        return {
            id: [this.item.id],
            nodeId: this.nodeId,
            maxItems: this.perPage,
            skipCount: Math.max((this.page - 1), 0) * this.perPage,
        };
    }

    private getPageLoadOptions(): UsageInPagesOptions {
        return {
            ...this.getLoadOptions(),
            template: true,
        };
    }

    private loadUsageInFolders(): Observable<ItemUsagePage> {
        switch (this.item.type) {
            case 'file':
                return this.client.file.usageInFolders(this.getLoadOptions()).pipe(
                    map(res => ({
                        items: res.folders,
                        totalCount: res.total,
                    })),
                );

            case 'image':
                return this.client.image.usageInFolders(this.getLoadOptions()).pipe(
                    map(res => ({
                        items: res.folders,
                        totalCount: res.total,
                    })),
                );

            default:
                return of({ items: [], totalCount: 0 });
        }
    }

    private loadUsageInPages(): Observable<ItemUsagePage> {
        switch (this.item.type) {
            case 'file':
                return this.client.file.usageInPages(this.getPageLoadOptions()).pipe(
                    map(res => ({
                        items: res.pages,
                        totalCount: res.total,
                    })),
                );

            case 'image':
                return this.client.image.usageInPages(this.getPageLoadOptions()).pipe(
                    map(res => ({
                        items: res.pages,
                        totalCount: res.total,
                    })),
                );

            case 'page':
                return this.client.page.usageInPages(this.getPageLoadOptions()).pipe(
                    map(res => ({
                        items: res.pages,
                        totalCount: res.total,
                    })),
                );

            default:
                return of({ items: [], totalCount: 0 });
        }
    }

    private loadUsageInFiles(): Observable<ItemUsagePage> {
        switch (this.item.type) {
            case 'file':
                return this.client.file.usageInFiles(this.getLoadOptions()).pipe(
                    map(res => ({
                        items: res.files,
                        totalCount: res.total,
                    })),
                );

            case 'image':
                return this.client.image.usageInFiles(this.getLoadOptions()).pipe(
                    map(res => ({
                        items: res.files,
                        totalCount: res.total,
                    })),
                );

            // Doesn't exist in page
            case 'page':
            default:
                return of({ items: [], totalCount: 0 });
        }
    }

    private loadUsageInImages(): Observable<ItemUsagePage> {
        switch (this.item.type) {
            case 'file':
                return this.client.file.usageInImages(this.getLoadOptions()).pipe(
                    map(res => ({
                        items: res.files,
                        totalCount: res.total,
                    })),
                );

            case 'image':
                return this.client.image.usageInImages(this.getLoadOptions()).pipe(
                    map(res => ({
                        items: res.files,
                        totalCount: res.total,
                    })),
                );

            // Doesn't exist in page
            case 'page':
            default:
                return of({ items: [], totalCount: 0 });
        }
    }

    private loadUsageInTemplates(): Observable<ItemUsagePage> {
        switch (this.item.type) {
            case 'file':
                return this.client.file.usageInTemplates(this.getLoadOptions()).pipe(
                    map(res => ({
                        items: res.templates,
                        totalCount: res.total,
                    })),
                );

            case 'image':
                return this.client.image.usageInTemplates(this.getLoadOptions()).pipe(
                    map(res => ({
                        items: res.templates,
                        totalCount: res.total,
                    })),
                );

            case 'page':
                return this.client.page.usageInTemplates(this.getLoadOptions()).pipe(
                    map(res => ({
                        items: res.templates,
                        totalCount: res.total,
                    })),
                );

            default:
                return of({ items: [], totalCount: 0 });
        }
    }

    private loadUsageInTags(): Observable<ItemUsagePage> {
        switch (this.item.type) {
            case 'page':
                return this.client.page.usageInTags(this.getPageLoadOptions()).pipe(
                    map(res => ({
                        items: res.pages,
                        totalCount: res.total,
                    })),
                );

            default:
                return of({ items: [], totalCount: 0 });
        }
    }

    private loadUsageInVariants(): Observable<ItemUsagePage> {
        switch (this.item.type) {
            case 'page':
                return this.client.page.usageInVariants(this.getPageLoadOptions()).pipe(
                    map(res => ({
                        items: res.pages,
                        totalCount: res.total,
                    })),
                );

            default:
                return of({ items: [], totalCount: 0 });
        }
    }

    private loadLinkedToFiles(): Observable<ItemUsagePage> {
        switch (this.item.type) {
            case 'page':
                return this.client.page.usageInLinkedFiles(this.getLoadOptions()).pipe(
                    map(res => ({
                        items: res.files,
                        totalCount: res.total,
                    })),
                );

            default:
                return of({ items: [], totalCount: 0 });
        }
    }

    private loadLinkedToImages(): Observable<ItemUsagePage> {
        switch (this.item.type) {
            case 'page':
                return this.client.page.usageInLinkedImages(this.getLoadOptions()).pipe(
                    map(res => ({
                        items: res.files,
                        totalCount: res.total,
                    })),
                );

            default:
                return of({ items: [], totalCount: 0 });
        }
    }

    private loadLinkedToPages(): Observable<ItemUsagePage> {
        switch (this.item.type) {
            case 'page':
                return this.client.page.usageInLinkedPages(this.getPageLoadOptions()).pipe(
                    map(res => ({
                        items: res.pages,
                        totalCount: res.total,
                    })),
                );

            default:
                return of({ items: [], totalCount: 0 });
        }
    }
}

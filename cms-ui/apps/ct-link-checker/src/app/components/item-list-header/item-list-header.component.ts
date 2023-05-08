import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import {
    Language,
    Page,
    PageWithExternalLinks,
    Raw
} from '@gentics/cms-models';
import { Observable, Subscription } from 'rxjs';
import { ItemsInfo } from '../../common/models/items-info';
import { ToolApiService } from '../../services/tool-api/tool-api.service';

@Component({
    selector: 'gtxct-item-list-header',
    templateUrl: './item-list-header.tpl.html',
    styleUrls: ['./item-list-header.scss']
})
export class ItemListHeaderComponent implements OnInit, OnDestroy {
    @Input() item: PageWithExternalLinks<Raw>;
    @Input() itemsInfo: ItemsInfo;
    @Input() filterTerm: string;
    @Input() isCollapsed: boolean;

    @Output() isCollapsedChanged = new EventEmitter<boolean>();

    private subscriptions = new Subscription();

    brokenLinksPerPage = 0;
    activeLanguage$: Observable<Language>;

    constructor(private toolApi: ToolApiService) {}

    ngOnInit(): void {
        this.toolApi.initialize();
        this.item.links.forEach(link => {
            if (link.lastStatus === 'invalid') {
                this.brokenLinksPerPage++;
            }
        });
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    toggleCollapse(): void {
        this.isCollapsed = !this.isCollapsed;
        this.isCollapsedChanged.emit(this.isCollapsed);
    }

    openPageInEditMode(page: Page): void {
        this.subscriptions.add(
            this.toolApi.connected.subscribe(
              toolApi => {
                toolApi.ui.editPage(page.id);
              })
          );
    }

    openPageInPreviewMode(page: Page): void {
        this.subscriptions.add(
            this.toolApi.connected.subscribe(
                toolApi => {
                    toolApi.ui.previewPage(page.id);
                })
        );
    }
}

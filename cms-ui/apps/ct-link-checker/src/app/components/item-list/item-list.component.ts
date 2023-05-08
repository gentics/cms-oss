import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChange
} from '@angular/core';
import { PaginationInstance, PaginationService } from 'ngx-pagination';
import { Subscription } from 'rxjs';

import {
    ExternalLink,
    Page,
    PageWithExternalLinks,
    PagingSortOption,
    Raw
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { ColorThemes, ModalService, NotificationService } from '@gentics/ui-core';
import * as _ from 'lodash';
import { ItemsInfo } from '../../common/models/items-info';
import { AppService } from '../../services/app/app.service';
import { FilterService } from '../../services/filter/filter.service';
import { LinkCheckerService } from '../../services/link-checker/link-checker.service';
import { DisplayFieldSelectorComponent } from '../display-field-selector/display-field-selector.component';
import { SortingModalComponent } from '../sorting-modal/sorting-modal.component';
import { UpdateLinkModalComponent } from '../update-link-modal/update-link-modal.component';

function getTypeForNotification(type: string): ColorThemes | 'default' {
    switch (type) {
        case 'CRITICAL':
            return 'alert';
        case 'SUCCESS':
            return 'success';
        case 'INFO':
            return 'primary';
        default:
            return 'default';
    }
}

@Component({
    selector: 'gtxct-item-list',
    templateUrl: './item-list.tpl.html',
    styleUrls: ['./item-list.scss'],
    providers: [PaginationService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ItemListComponent implements OnInit, OnChanges, OnDestroy {
    @Input() filterTerm: string;
    @Input() itemsInfo: ItemsInfo;
    @Input() items: PageWithExternalLinks<Raw>[];
    @Input() expandedPages: number[];

    @Output() expandedPagesChange = new EventEmitter<number[]>();
    @Output() pageChange = new EventEmitter<number>();

    private subscriptions: Subscription[] = [];
    paginationConfig: PaginationInstance = {
        itemsPerPage: 10,
        currentPage: 1
    };

    constructor(
        private linkCheckerService: LinkCheckerService,
        public filterService: FilterService,
        private appService: AppService,
        public modalService: ModalService,
        private api: GcmsApi,
        private notification: NotificationService,
    ) {}

    ngOnInit(): void {
        this.paginationConfig.currentPage = this.itemsInfo.currentPage;
        this.paginationConfig.itemsPerPage = this.itemsInfo.perPage;
        this.paginationConfig.totalItems = this.itemsInfo.total;
    }

    pageChanged(newPageNumber: number): void {
        this.pageChange.emit(newPageNumber);
    }

    ngOnChanges(changes: { [K in keyof ItemListComponent]?: SimpleChange }): void {
        if (changes.itemsInfo) {
            const itemsInfo = changes.itemsInfo.currentValue;
            this.paginationConfig.currentPage = itemsInfo.currentPage;
            this.paginationConfig.itemsPerPage = itemsInfo.perPage;
            this.paginationConfig.totalItems = itemsInfo.total;
        }
    }

    isItemCollapsed(itemId: number): boolean {
        return !this.expandedPages.includes(itemId);
    }

    itemCollapsedChanged(itemId: number, state: boolean): void {
        let expanded = this.expandedPages.slice();

        if (state) {
            expanded = expanded.filter(item => item !== itemId);
        } else {
            expanded.push(itemId);
        }

        this.expandedPagesChange.emit(expanded);
    }

    collapseAll(): void {
        const expanded = this.expandedPages.slice();
        this.expandedPagesChange.emit(
            expanded.filter(id => !this.items.find(item => item.page.id === id))
        );
    }

    expandAll(): void {
        const expanded = this.expandedPages.slice();

        this.items
                .filter(item => !expanded.includes(item.page.id))
                .map(item => expanded.push(item.page.id));

        this.expandedPagesChange.emit(expanded);
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(subscription => subscription.unsubscribe());
    }

    /**
     * Open the DisplayFieldSelector component in a modal.
     */
    selectDisplayFields(): void {
        const fields = this.itemsInfo.displayFields;
        this.modalService.fromComponent(DisplayFieldSelectorComponent, {}, { fields })
            .then(modal => modal.open())
            .then((result: {selection: string[], showPath: boolean}) => {
                this.updateDisplayFields(result.selection);
            });
    }

    updateDisplayFields(fields: string[]): void {
        this.appService.setDisplayFields(fields);
        this.linkCheckerService.setUserSettings();
    }

    /**
     * Tracking function for ngFor for better performance.
     */
    identify(index: number, item: PageWithExternalLinks): number {
        return item.page.id;
    }

    /**
     * Open the modal for selecting sort option.
     */
    selectSorting(): void {
        const locals: Partial<SortingModalComponent> = {
            sortOptions: _.cloneDeep(this.filterService.options.sortOptions)
        };

        this.modalService.fromComponent(SortingModalComponent, {}, locals)
            .then(modal => modal.open())
            .then(sorting => {
                this.updateSorting(sorting);
            });
    }

    updateSorting(sorting: PagingSortOption<Page>[]): void {
        this.filterService.setSortingOptions(sorting);
        this.linkCheckerService.setUserSettings();
    }

    replaceLinkClicked(pageId: number, item: ExternalLink): void {
        this.modalService.fromComponent(UpdateLinkModalComponent, { width: '680px' }, { item, pageId })
            .then(modal => modal.open())
            .then(payload => this.api.linkChecker.updateLink(item.id, pageId, payload, { wait: 1000 }).toPromise())
            .then(response => {
                const message = response.messages.shift();

                this.notification.show({
                    message: message.message,
                    type: getTypeForNotification(message.type),
                    delay: 5000,
                });

                this.appService.updateData();
            });
    }

    min(...numbers: number[]): number {
        return Math.min(...numbers);
    }
}

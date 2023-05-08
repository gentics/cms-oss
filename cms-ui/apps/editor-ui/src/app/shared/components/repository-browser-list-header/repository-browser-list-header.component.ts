import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ModalService } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { FolderItemType, Language, SortField } from '@gentics/cms-models';
import { iconForItemType } from '../../../common/utils/icon-for-item-type';
import { UserSettingsService } from '../../../core/providers/user-settings/user-settings.service';
import { ApplicationStateService } from '../../../state';
import { RepositoryBrowserDataService } from '../../providers';
import { SortingModal } from '../sorting-modal/sorting-modal.component';

@Component({
    selector: 'repository-browser-list-header',
    templateUrl: './repository-browser-list-header.tpl.html',
    styleUrls: ['./repository-browser-list-header.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RepositoryBrowserListHeader implements OnInit {

    @Input() itemType: FolderItemType | 'contenttag' | 'templatetag' = 'folder';
    @Input() itemCount: number;
    @Input() filtering: boolean;
    @Input() collapsed: boolean;
    @Input() showImagesGridView: boolean;

    @Output() collapsedChange = new EventEmitter<boolean>();
    @Output() selectDisplayFields = new EventEmitter<void>();

    iconForItemType = iconForItemType;
    sort$: Observable<{ field: SortField, order: 'asc' | 'desc' }>;
    sortOptions: { field: SortField, order: 'asc' | 'desc' };
    activeLanguage$: Observable<Language>;
    nodeLanguages$: Observable<Language[]>;
    showAllLanguages$: Observable<boolean>;
    showStatusIcons$: Observable<boolean>;

    constructor(
        private appState: ApplicationStateService,
        private dataService: RepositoryBrowserDataService,
        private modalService: ModalService,
        private userSettings: UserSettingsService,
    ) { }

    ngOnInit(): void {
        this.sort$ = this.dataService.sortOrder$
            .map(sortOrdersByType => sortOrdersByType[this.itemType])
            .do(sortOrder => {
                this.sortOptions = sortOrder;
            })
            .publishReplay(1).refCount();

        this.nodeLanguages$ = this.dataService.currentAvailableLanguages$;
        this.activeLanguage$ = this.dataService.currentContentLanguage$;
        this.showAllLanguages$ = this.appState.select(state => state.folder.displayAllLanguages);
        this.showStatusIcons$ = this.appState.select(state => state.folder.displayStatusIcons);
    }

    selectSortOrder(): void {
        const options: Partial<SortingModal> = {
            itemType: this.itemType,
            sortBy: this.sortOptions.field,
            sortOrder: this.sortOptions.order,
        };

        this.modalService
            .fromComponent(SortingModal, {}, options)
            .then(modal => modal.open())
            .then(({ sortBy, sortOrder }: { sortBy: SortField, sortOrder: 'asc' | 'desc' }) => {
                if (this.itemType !== 'contenttag' && this.itemType !== 'templatetag') {
                    this.userSettings.setSorting(this.itemType, sortBy, sortOrder);
                }
                this.dataService.setSorting(this.itemType, sortBy, sortOrder);
            });
    }

    toggleDisplayImagesGridView(): void {
        const currentVal = this.appState.now.folder.displayImagesGridView;
        this.userSettings.setDisplayImagesGridView(!currentVal);
    }

    /**
     * The language context for the pages has been changed.
     */
    languageChanged(language: Language): void {
        this.dataService.setContentLanguage(language);
    }

    toggleDisplayAllLanguages(): void {
        const currentVal = this.appState.now.folder.displayAllLanguages;
        this.userSettings.setDisplayAllLanguages(!currentVal);
    }

    toggleDisplayStatusIcons(): void {
        const currentVal = this.appState.now.folder.displayStatusIcons;
        this.userSettings.setDisplayStatusIcons(!currentVal);
    }

}

import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import {
    FolderItemType,
    FolderItemTypePlural,
    Form,
    Item,
    ItemInNode,
    ItemType,
    Language,
    Node,
    Normalized,
    Page,
    Raw,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { isEqual as _isEqual } from 'lodash-es';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ItemsInfo } from '../../../common/models';
import { ApplicationStateService } from '../../../state';
import { RepositoryBrowserDataService } from '../../providers';
import { DisplayFieldSelectorModal } from '../display-field-selector/display-field-selector.component';
import { MasonryGridComponent } from '../masonry-grid/masonry-grid.component';

/**
 * Renders the contents of the repository browser for one item type in a specific folder.
 */
@Component({
    selector: 'repository-browser-list',
    templateUrl: './repository-browser-list.tpl.html',
    styleUrls: ['./repository-browser-list.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class RepositoryBrowserList implements OnInit, AfterViewInit, OnChanges, OnDestroy {

    @Input() itemType: ItemType | 'contenttag' | 'templatetag' = 'folder';
    @Input() canBeSelected = true;
    @Input() currentNodeId = 0;
    @Input() currentNode: Node = undefined;
    @Input() displayFields: string[] = [];
    @Input() selected: ItemInNode[];
    @Input() contents: Item[] = [];
    @Input() filtering = false;
    @Input() searching = false;
    @Input() startPageId: number;
    @Input() displayNodeName = false;
    @Input() itemsPerPage = 10;
    @Input() pageShowPath = true;

    @Output() select = new EventEmitter<Item>();
    @Output() deselect = new EventEmitter<Item>();
    @Output() itemClick = new EventEmitter<Item>();
    @Output() updateDisplayFields = new EventEmitter<string[]>();
    /** Emits if an lang icon of `item row` > `item-status-indicator` is clicked */
    @Output() pageLanguageIconClick = new EventEmitter<{ page: Page<Raw> | Page<Normalized>; language: Language; }>();
    /** Emits if a form language icon is clicked */
    @Output() formLanguageIconClick = new EventEmitter<{ form: Form<Raw> | Form<Normalized>; language: Language; }>();

    currentPage = 1;
    isCollapsed = false;
    showImagesGridView$: Observable<boolean>;

    filterTerm$: Observable<string>;

    private timeout: number;

    @ViewChild(MasonryGridComponent)
    private thumbnailGrid: MasonryGridComponent;

    languages$: Observable<Language[]>;

    constructor(
        private appState: ApplicationStateService,
        private modalService: ModalService,
        private dataService: RepositoryBrowserDataService,
    ) { }

    ngOnInit(): void {
        this.showImagesGridView$ = this.appState.select(state => state.folder.displayImagesGridView);

        this.filterTerm$ = this.appState.select(state => state.folder.filterTerm);

        this.languages$ = this.dataService.currentAvailableLanguages$;
    }

    ngOnChanges(changes: { [K in keyof RepositoryBrowserList]?: any }): void {
        if (changes.contents) {
            /**
             * For now we fetch usage data in case it is missing in one item instead of looking for array changes.
             * This will take care of edge cases where a different language is chosen and the same items are refetched (e.g. in case of images)
             * and we do not recognize (since we would not see an array change).
             *
             * It should only run once per changeset to avoid infinite loop on cases when an item does not have usage count property at all.
             */
            if (!_isEqual(changes.contents.previousValue, changes.contents.currentValue)
                && !changes.contents.currentValue.every((item: Item) => !!item.usage)) {
                this.getTotalUsage();
            }
            this.currentPage = 1;
            this.forceMasonryGridToLayoutAfterTimeout();
        }
    }

    ngAfterViewInit(): void {
        this.forceMasonryGridToLayoutAfterTimeout();
    }

    ngOnDestroy(): void {
        clearTimeout(this.timeout);
    }

    isSelected(item: Item): boolean {
        return item && this.selected && this.selected.some(sel =>
            sel.id === item.id &&
            sel.type === item.type && (this.currentNode && this.currentNode.id > 0
                ? (sel.nodeId === this.currentNode.id)
                // eslint-disable-next-line no-underscore-dangle
                : ((item as any).__favourite__ && (sel.nodeId === (item as any).__favourite__.nodeId))
            ),
        );
    }

    toggleSelect(item: Item, isSelect: boolean): void {
        if (isSelect === true) {
            this.select.emit(item);
        } else if (isSelect === false) {
            this.deselect.emit(item);
        }
    }

    getNodeName(path: string | undefined): string {
        if (typeof path !== 'string') {
            return '';
        }
        const nodeName = (/^\/([^/]+)\//.exec(path))[1];
        return nodeName;
    }

    trackById(index: number, item: Item): number {
        return item.id;
    }

    thumbnailClicked(event: Event, item: Item): void {
        if (event && event.target === event.currentTarget) {
            event.preventDefault();
            this.itemClick.emit(item);
        }
    }

    openDisplayFieldsModal(): void {
        const locals = { type: this.itemType as ItemType, fields: this.displayFields, showPath: this.pageShowPath };
        this.modalService.fromComponent(DisplayFieldSelectorModal, {}, locals)
            .then(modal => modal.open())
            .then((output: { selection: string[], showPath: boolean; }) => this.updateDisplayFields.emit(output.selection));
    }

    forceMasonryGridToLayoutAfterTimeout(): void {
        clearTimeout(this.timeout);
        // using window for tests to succeed
        // they will think we are in a node environment
        // and return "Timeout" object instead of number
        this.timeout = window.setTimeout(() => {
            if (this.thumbnailGrid) {
                this.thumbnailGrid.triggerLayout();
            }
        });
    }

    /**
     * fetches items info, however based upon state and not folder shown in repository browser
     */
    getItemsInfo(type: FolderItemType): Observable<ItemsInfo> {
        return this.appState.select(state => state.folder).pipe(
            map(folderState => folderState[`${type}s` as FolderItemTypePlural]),
        );
    }

    onPageLanguageIconClicked(data: { page: Page<Raw>; language: Language; }): void {
        this.pageLanguageIconClick.emit(data);
    }

    onFormLanguageIconClicked(data: { form: Form<Raw>; language: Language; }): void {
        this.formLanguageIconClick.emit(data);
    }

    // similar to implementation in item-list.component.ts
    private getTotalUsage(): void {
        if (this.currentNode && this.typeCanFetchTotalUsage(this.itemType)) {
            this.dataService.getTotalUsageForCurrentItemsOfType(
                this.itemType,
            );
        }
    }

    private typeCanFetchTotalUsage(type: ItemType | 'contenttag' | 'templatetag'): type is 'file' | 'form' | 'image' | 'page' {
        return ['file', 'form', 'image', 'page'].includes(type)
    }

}

import {
    AfterContentInit,
    AfterViewChecked,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    QueryList,
    SimpleChange,
    ViewChildren,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
    SuggestionItem,
    SuggestionSearchService,
} from '@editor-ui/app/shared/providers/suggestion-search/suggestion-search.service';
import { ObservableStopper } from '@gentics/cms-components';
import { EditMode, Folder, Page } from '@gentics/cms-models';
import { isEqual } from'lodash-es'
import { BehaviorSubject, combineLatest, Observable, Subscription } from 'rxjs';
import {
    debounceTime,
    delay,
    distinctUntilChanged,
    filter,
    map,
    mapTo,
    skip,
    switchMap,
    take,
    takeUntil,
    tap,
} from 'rxjs/operators';
import { EditorTab, RecentItem } from '@editor-ui/app/common/models';
import { arraysAreEqual } from '../../../common/utils/arrays-are-equal';
import { fuzzyMatch } from '../../../common/utils/fuzzy-match';
import { iconForItemType } from '../../../common/utils/icon-for-item-type';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { ApplicationStateService, FolderActionsService, ChangeTabAction } from '../../../state';
import { SearchSuggestionComponent } from '../search-suggestion/search-suggestion.component';

export enum EventKey {
    ArrowDown = 'ArrowDown',
    ArrowUp = 'ArrowUp',
    ArrowLeft = 'ArrowLeft',
    Enter = 'Enter',
    Escape = 'Escape',
    Backspace = 'Backspace',
}

@Component({
    selector: 'search-suggestion-bar',
    templateUrl: './search-suggestion-bar.component.html',
    styleUrls: ['./search-suggestion-bar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchSuggestionBarComponent implements OnInit, OnChanges, OnDestroy, AfterContentInit, AfterViewChecked {

    @Input() recentVisible = false;
    @Output() recentVisibleChange = new EventEmitter<boolean>()
    @Input() suggestionVisible = false;
    @Output() suggestionVisibleChange = new EventEmitter<boolean>()
    @Input() filterTerm = '';
    @Input() searchBarKeyup: Observable<KeyboardEvent>;

    @Output() close = new EventEmitter<void>();

    filterTerm$ = new BehaviorSubject<string>(this.filterTerm);

    matchingRecentItems$: Observable<RecentItem[]> = combineLatest([
        this.state.select(state => state.folder.recentItems).pipe(
            map(items => this.determineRecentItemsToDisplay(items)),
            delay(0), // This is necessary to avoid an Angular "expression changed after it was checked" error.
        ),
        this.filterTerm$,
    ]).pipe(
        distinctUntilChanged(arraysAreEqual),
        map(([items, filter]) => {
            return items.filter(item => this.recentItemMatchesFilter(item, filter))
        }),
    );
    matchingSuggestionItems$: Observable<SuggestionItem[]> = this.filterTerm$.pipe(
        map((searchTerm) => +searchTerm),
        debounceTime(200),
        distinctUntilChanged(isEqual),
        switchMap((searchTerm) => {
            return this.suggestionSearchService.searchInState(searchTerm);
        }),
        tap((suggestions: RecentItem[]) => {
            this.setSuggestionVisible(suggestions.length > 0);
        }),
    );
    maxRecentItems = 15;

    recentFeatureEnabled$: Observable<boolean>;

    iconForItemType = iconForItemType;

    @ViewChildren(SearchSuggestionComponent)
    suggestions: QueryList<SearchSuggestionComponent>;

    hasSuggestions = false;
    selectedSuggestion: SearchSuggestionComponent;

    private subscription = new Subscription();
    private stopper = new ObservableStopper();

    constructor(
        private changeDetector: ChangeDetectorRef,
        private navigationService: NavigationService,
        private route: ActivatedRoute,
        private router: Router,
        private state: ApplicationStateService,
        private folderActions: FolderActionsService,
        private suggestionSearchService: SuggestionSearchService,
    ) { }


    ngOnInit(): void {
        this.recentFeatureEnabled$ = this.state
            .select(state => state.features.recent_items).pipe(
                tap(() => this.changeDetector.markForCheck()),
            );

        this.searchBarKeyup
            .pipe(
                takeUntil(this.stopper.stopper$),
                tap((event) => {
                    this.inputKeyup(event);
                }),
            )
            .subscribe()
    }

    ngOnChanges(changes: { [K in keyof SearchSuggestionBarComponent]: SimpleChange }): void {
        if (changes.filterTerm) {
            this.filterTerm$.next(changes.filterTerm.currentValue);
        }

        if (changes.filterTerm && this.suggestions) {
            this.setSelection(this.suggestions.first);
        }
    }

    ngAfterContentInit(): void {
        if (this.suggestions) {
            if (this.suggestions.length > 0) {
                this.hasSuggestions = true;
                this.setSelection(this.suggestions.first);
            }

            const changeSub = this.suggestions.changes
                .subscribe(() => this.checkIfSelectionChanged());
            this.suggestions.notifyOnChanges();

            this.subscription.add(changeSub);
        }
    }

    ngAfterViewChecked(): void {
        // if only suggestions are visible when searched for, preselect it...
        if (this.suggestions.length === 1 && this.suggestionVisible && !this.recentVisible) {
            this.setSelection(this.suggestions.first)
        }
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    inputKeyup = (event: KeyboardEvent): void => {
        switch (event.key) {
            case EventKey.ArrowDown:
                this.selectDown(event);
                break;
            case EventKey.ArrowUp:
                this.selectUp(event);
                break;
            case EventKey.Enter:
                if (this.selectedSuggestion) {
                    this.selectedSuggestion.use.emit();
                }
                break;
            case EventKey.Escape:
                this.doClose();
                break;
        }

        this.filterTerm$.next((event.target as HTMLInputElement).value);
    }

    goToItem(item: SuggestionItem | RecentItem): void {
        if (item.type === 'folder' && item.mode === 'navigate') {
            this.navigateToFolder(item.id, item.nodeId);
        } else {
            this.openItemInEditor(item);
        }

        setTimeout(() => {
            this.doClose();
        });
    }

    selectDown(event: KeyboardEvent): void {
        event.preventDefault();
        this.selectByOffset(+1);
    }

    selectUp(event: KeyboardEvent): void {
        if (!this.areSuggestionsCurrentlyShown()) { return; }
        event.preventDefault();
        this.selectByOffset(-1);
    }

    areSuggestionsCurrentlyShown(): boolean {
        return this.suggestionVisible || this.recentVisible;
    }

    private checkIfSelectionChanged(): void {
        const hasSuggestions = this.suggestions.length > 0;
        const stillHasSelectedSuggestion = this.suggestions.find(s => s === this.selectedSuggestion) != null;

        if (hasSuggestions !== this.hasSuggestions || !stillHasSelectedSuggestion) {
            this.hasSuggestions = hasSuggestions;
            this.setSelection(this.suggestions.first);
            this.changeDetector.markForCheck();
        }
    }

    private selectByOffset(offset: number): void {
        const suggestions = this.suggestions.toArray();
        const selectionIndex = suggestions.indexOf(this.selectedSuggestion);
        const newIndex = selectionIndex + offset;

        if (newIndex >= 0 && newIndex < suggestions.length) {
            this.setSelection(suggestions[newIndex]);
        }

        if (newIndex < 0) {
            this.doClose();
        }
    }

    private setSelection(newSelectedSuggestion: SearchSuggestionComponent): void {
        this.selectedSuggestion = newSelectedSuggestion;
        this.suggestions.forEach(suggestion => {
            suggestion.setSelected(suggestion === newSelectedSuggestion);
        });
    }

    recentItemMatchesFilter(item: RecentItem, filter: string): boolean {
        return !filter || fuzzyMatch(filter, item.name) || String(item.id) === filter;
    }

    private determineRecentItemsToDisplay(items: RecentItem[]): RecentItem[] {
        if (!items.length) {
            return items;
        }

        // Show every item only once
        const seen: { [idTypeNodeid: string]: boolean } = {};
        let displayedItems = [items[0]];

        const firstHash = this.hashItem(items[0]);
        seen[firstHash] = true;

        for (let i = 1; i < items.length; i++) {
            const currentItem = items[i];

            if (currentItem) {
                const hash = this.hashItem(currentItem);

                if (!seen[hash]) {
                    seen[hash] = true;
                    displayedItems.push(currentItem);
                }
            }
        }

        // Remove current folder
        const { activeFolder, activeNode } = this.state.now.folder;
        displayedItems = displayedItems.filter(item => item.id !== activeFolder || item.nodeId !== activeNode || item.type !== 'folder');

        // Remove item in editor
        const { editorIsOpen, itemType, itemId, nodeId } = this.state.now.editor;
        if (editorIsOpen) {
            displayedItems = displayedItems.filter(item => item.id !== itemId || item.nodeId !== nodeId || item.type !== itemType);
        }

        // Limit to maximum amount of items
        return displayedItems.slice(0, this.maxRecentItems);
    }

    private hashItem(item: RecentItem): string {
        return `${item.type}-${item.id}-${item.nodeId}`;
    }

    private fetchParentFolder(item: RecentItem): Promise<number> {
        return this.folderActions.getItem(item.id, item.type, { nodeId: item.nodeId })
            .then(itemFromServer => (itemFromServer as Page).folderId || (itemFromServer as Folder).motherId);
    }

    private navigateToFolder(folderId: number, nodeId: number): Promise<boolean> {
        const newRoute = this.navigationService.list(nodeId, folderId).commands();

        const params = this.route.snapshot.firstChild.children.find(r => r.outlet === 'list').firstChild.firstChild.paramMap;
        if (params.get('nodeId') === nodeId.toString() && params.get('folderId') === folderId.toString()) {
            return Promise.resolve(true);
        }

        return this.router.navigate(newRoute)
            .then(success => {
                if (!success) {
                    return false;
                }

                // Wait until loaded
                return this.state.select(state => state.folder).pipe(
                    map(folderState => folderState.folders.fetching
                        || folderState.pages.fetching
                        || folderState.files.fetching
                        || folderState.images.fetching,
                    ),
                    distinctUntilChanged(isEqual),
                    skip(1),
                    filter(fetching => fetching === false),
                    take(1),
                    mapTo(true),
                ).toPromise();
            });
    }

    private openItemInEditor(item: SuggestionItem | RecentItem): void {
        this.fetchParentFolder(item)
            .then(parentFolderId => this.navigateToFolder(parentFolderId, item.nodeId))
            .then(succeeded => {
                if (succeeded) {
                    const openEditorInMode = (editMode: EditMode) => {
                        return this.navigationService
                            .detail(item.nodeId, item.type, item.id, editMode)
                            .navigate();
                    };

                    const changeTab = (tab: EditorTab) => this.state.dispatch(new ChangeTabAction(tab));

                    switch (item.mode) {
                        case 'edit':
                            openEditorInMode(EditMode.EDIT);
                            break;

                        case 'preview':
                            openEditorInMode(EditMode.PREVIEW)
                                .then(navigated => navigated && changeTab('preview'));
                            break;

                        case 'properties':
                            openEditorInMode(EditMode.EDIT_PROPERTIES)
                                .then(navigated => navigated && changeTab('properties'));
                            break;

                        default:
                            // We need the default case, because in GCMS 5.34 and before it was possible to
                            // have a mode 'objectProperties'.
                            openEditorInMode(EditMode.EDIT_PROPERTIES)
                                .then(navigated => navigated && changeTab('properties'));
                            break;
                    }
                }
            });
    }

    private doClose(): void {
        this.setSuggestionVisible(false);
        this.setRecentVisible(false);
        this.close.emit()
    }

    private setSuggestionVisible(isVisible: boolean): void {
        this.suggestionVisible = isVisible;
        this.suggestionVisibleChange.emit(isVisible)
    }

    private setRecentVisible(isVisible: boolean): void {
        this.recentVisible = isVisible;
        this.recentVisibleChange.emit(isVisible);
    }
}

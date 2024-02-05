import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    HostListener,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    QueryList,
    SimpleChanges,
    ViewChild,
    ViewChildren,
} from '@angular/core';
import {
    AbstractControl,
    UntypedFormArray,
    UntypedFormBuilder,
    UntypedFormControl,
    UntypedFormGroup,
    Validators,
} from '@angular/forms';
import {
    GtxChipOperator,
    GtxChipSearchChipData,
    GtxChipSearchChipOperatorOption,
    GtxChipSearchChipPropertyOption,
    GtxChipSearchConfig,
    GtxChipSearchData,
    GtxChipSearchPropertyKeys,
    GtxChipSearchSearchFilterMap,
    GtxChipValue,
} from '@editor-ui/app/common/models';
import { isLiveUrl } from '@editor-ui/app/common/utils/is-live-url';
import { ObservableStopper } from '@editor-ui/app/common/utils/observable-stopper/observable-stopper';
import { ListSearchService } from '@editor-ui/app/core/providers/list-search/list-search.service';
import { PresentationService } from '@editor-ui/app/shared/providers/presentation/presentation.service';
import {
    ApplicationStateService,
    FocusEditorAction, FocusListAction,
    FolderActionsService,
    PublishQueueActionsService,
} from '@editor-ui/app/state';
import { Node } from '@gentics/cms-models';
import { isEqual } from 'lodash-es';
import {
    BehaviorSubject,
    Observable,
    Subject,
    combineLatest,
} from 'rxjs';
import {
    debounceTime,
    distinctUntilChanged,
    filter,
    map,
    takeUntil,
    tap,
} from 'rxjs/operators';

/**
 * # Chip Search Bar
 * ## Description
 * Searchbar translating user-defined "filter chips" (entity-property/relation/value datasets) to Elastic Search query parameters.
 *
 * ## How to use
 * ### Typing text inside search
 * Current view (displayed folder contents or existing search results) gets filtered by items __containg__ input text.
 * ### Typing text inside search and hit enter key on keyboard or push search button in sidebar
 * Input text disappears and becomes a filter chip of type `all`, which triggers an Elastic Search query against
 * entity properties `name`, `path`, `description` and `niceUrl` __equalling__ input text.
 */
@Component({
    selector: 'chip-search-bar',
    templateUrl: './chip-search-bar.component.html',
    styleUrls: ['./chip-search-bar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChipSearchBarComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {

    /** filter properties to choose from when creating a new filter chip */
    @Input()
    chipSearchBarConfig: GtxChipSearchConfig;

    @Input()
    public loading: boolean;

    /** If TRUE visually indicates application processing user input and disabling further user input. */
    private loadingSub = new BehaviorSubject<boolean>(false);
    public loading$ = this.loadingSub.asObservable();

    /** Recent Items settings */
    showRecentButton$: Observable<boolean>;
    showRecent = false;
    showSuggestion = false;

    /** Main form group enveloping entire search bar */
    formGroupMain: UntypedFormGroup;

    /** Main form group DOM element */
    @ViewChild('formGroupMainElement')
    formGroupMainElement: ElementRef;

    /** searchbar main input DOM element */
    @ViewChild('searchBarValueElement')
    searchBarValueElement: ElementRef;

    /** searchbar dropdown for adding filter properties DOM element */
    @ViewChildren('chipElements', { read: ElementRef })
    chipElements$: QueryList<ElementRef>;

    searchBarKeyup: Subject<KeyboardEvent> = new Subject<KeyboardEvent>();

    chipElements: ElementRef[];

    /** Chip relation for data type Boolean */
    chipRelationBoolean: GtxChipSearchChipOperatorOption<any>[] = [];

    /** Chip relation for data type Number */
    chipRelationNumber: GtxChipSearchChipOperatorOption<any>[] = [];

    /** Chip relation for data type String */
    chipRelationString: GtxChipSearchChipOperatorOption<any>[] = [];

    /** Chip relation for data type String */
    chipRelationStringExtended: GtxChipSearchChipOperatorOption<any>[] = [];

    /** Chip relation for data type Date */
    chipRelationDate: GtxChipSearchChipOperatorOption<any>[] = [];

    /** User-input string not triggering search but frontend-filtering of items currently displayed. */
    filterTerm$: Observable<string>;

    /** TRUE if search has been performed and search filter have not been change since. */
    searchInputHasChanged$ = new BehaviorSubject<boolean>(false);

    /** If TRUE component won't propagate changes to state to prevent infinite loop. */
    writeFromStateToComponentInProgess = false;

    private stopper = new ObservableStopper();

    /**
     * While `true`, formGroupMain.valueChanges won't trigger any consequences.
     * This is for special search behavior like jumpToId syntax or niceUrl search.
     */
    private specialSearchActionInProgess = false;

    /** Debounced onResize event */
    private onResizeInternal$ = new Subject<void>();
    get onResizeDebounced$(): Observable<void> {
        return this.onResizeInternal$.pipe(
            debounceTime(500),
        );
    }

    /* Constructor ************************************************************************************************************ */

    constructor(
        private folderActions: FolderActionsService,
        private publishQueueActions: PublishQueueActionsService,
        private state: ApplicationStateService,
        private formBuilder: UntypedFormBuilder,
        private changeDetectorRef: ChangeDetectorRef,
        private listSearch: ListSearchService,
        private changeDetector: ChangeDetectorRef,
        private presentation: PresentationService,
    ) { }

    /* Life Cycle Hooks ******************************************************************************************************* */

    ngOnInit(): void {

        // assign data streams

        this.filterTerm$ = this.state.select(state => state.folder.filterTerm);

        this.filterTerm$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((filterTerm: string) => {
            this.setSearchbarValue(filterTerm, !this.writeFromStateToComponentInProgess);
        });

        const activeSearch$ = this.state.select(state => state.folder.searchFilters);

        const recentFeatureEnabled$ = this.state
            .select(state => state.features.recent_items).pipe(
                tap(() => this.changeDetector.markForCheck()),
            );

        const recentItems$ = this.state.select(state => state.folder.recentItems);

        // Recent Items button
        this.showRecentButton$ = combineLatest([
            this.filterTerm$,
            recentItems$,
            recentFeatureEnabled$,
            activeSearch$,
        ]).pipe(
            map(([filterTerm, matchingRecentItems, recentFeatureEnabled]) => {
                const activeSearchNull = this.chipsAllHaveNoValue();
                return activeSearchNull && !filterTerm && matchingRecentItems && matchingRecentItems.length > 0 && recentFeatureEnabled;
            }),
        );
        // listen to debounced window.resize event
        this.onResizeDebounced$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(() => this.updatePresentation());

        // request data
        this.publishQueueActions.getUsersForRevision();

        this.initFormGroup();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.loading) {
            this.loadingSub.next(this.loading);
        }
    }

    ngAfterViewInit(): void {
        this.loading$.pipe(
            debounceTime(1000),
            tap(() => this.searchInputHasChanged$.next(false)),
            takeUntil(this.stopper.stopper$),
        ).subscribe(loading => {
            if (loading) {
                this.formGroupMain?.disable({ emitEvent: false });
            } else {
                this.formGroupMain?.enable({ emitEvent: false });
            }
        });

        // We want the search to be auto-focused correctly when loading has finished.
        // However, when an editor is open, we don't want to redirect the focus back to
        // the search/list.
        combineLatest([
            this.loading$.pipe(
                debounceTime(1000),
            ),
            this.state.select(appState => appState.editor.editorIsOpen),
        ]).pipe(
            takeUntil(this.stopper.stopper$),
            distinctUntilChanged(isEqual),
        ).subscribe(([loading, editorOpen]) => {
            if (!loading && !editorOpen) {
                this.focusSearchbarValue();
            }
        });

        // sync filter state to component
        this.state.select(state => state.folder.searchFilters).pipe(
            filter(() => this.writeFromStateToComponentInProgess),
            tap(() => this.writeFromStateToComponentInProgess = true),
            takeUntil(this.stopper.stopper$),
        ).subscribe((searchFilters: GtxChipSearchSearchFilterMap) => {
            this.clearSearchbarChips();
            Object.entries(searchFilters)?.forEach(([filterKey, filterData]) => {
                filterData?.forEach(filter => {
                    this.addOrUpdateChip(filterKey, filter.operator, filter.value);
                });
            });
            this.writeFromStateToComponentInProgess = false;
        });
    }

    ngOnDestroy(): void {
        // in case the app switches to basic search, reset DOM element height
        this.updatePresentation(75);
        this.stopper.stop();
    }

    @HostListener('window:resize', ['$event'])
    onResize(): void {
        this.onResizeInternal$.next();
    }

    /* DOM-related Methods **************************************************************************************************** */

    initFormGroup(): void {
        this.formGroupMain = this.formBuilder.group({
            searchBar: [],
            chips: this.formBuilder.array([]),
        });

        this.formGroupMain.valueChanges.pipe(
            // while special search-related action in progress don't trigger any search behavior
            filter(() => !this.specialSearchActionInProgess),
            debounceTime(500),
            tap(() => this.searchInputHasChanged$.next(this.searchIsValid())),
            takeUntil(this.stopper.stopper$),
        ).subscribe((formValue: GtxChipSearchData<any>) => {
            this.onSearchBarInputChange(formValue);
        });
    }

    onSearchBarInputChange(formValue: any): void {
        this.setFilterTerm(this.getSearchbarValue());
        this.updatePresentation();
    }

    onSearchTriggered(): void {
        if (!this.showRecent && this.formGroupMain.valid) {
            this.submitSearch();
        }
    }

    onButtonSearchClicked(): void {
        this.submitSearch();
    }

    onKeyDownBackspace(): void {
        if (!this.getSearchbarValue()) {
            this.removeLastChip();
        }
    }

    onDropDownFilterPropertiesItemClicked(property: GtxChipSearchPropertyKeys): void {
        const isValidProperty = this.chipSearchBarConfig.searchableProperties.some(p => p.value === property);
        if (!isValidProperty) {
            return;
        }
        this.setDefaultFilters();
        this.addOrUpdateChip(property, null, this.getSearchbarValue());
    }

    focusSearchbarValue(): void {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.searchBarValueElement?.nativeElement?.focus();
    }

    /* Getter & Setter ******************************************************************************************************** */

    searchIsActive(): boolean {
        const chips = this.getChips();
        const atLeastOneChipIsValid = chips && chips.controls.some(c => this.getChipValue(c));
        return chips.length > 0 && atLeastOneChipIsValid;
    }

    searchIsValid(): boolean {
        return this.searchIsActive() && this.chipsAllHaveOperators() && this.chipsAllHaveValues();
    }

    /** Convenience getter for searchbar text control */
    getSearchBarValueControl(): AbstractControl | null {
        return this.formGroupMain ? this.formGroupMain.get('searchBar') : null;
    }

    /** Convenience getter filter chip controls */
    getChips(): UntypedFormArray {
        return this.formGroupMain && this.formGroupMain.get('chips') as UntypedFormArray;
    }

    /** Convenience getter filter chip values */
    getChipValues<K extends GtxChipSearchPropertyKeys>(): GtxChipSearchChipData<K>[] {
        return this.formGroupMain && this.formGroupMain.get('chips').value;
    }

    chipValuesAreAlltouched(): boolean {
        return !this.getChips().controls.some(c => c.untouched);
    }

    chipsAllHaveOperators(): boolean {
        return !this.getChips().controls.some((c: UntypedFormControl) => !this.getChipOperator(c));
    }
    chipsAllHaveValues(): boolean {
        return !this.getChips().controls.some((c: UntypedFormControl) => {
            const chipValue = this.getChipValue(c);
            // allow unchecked checkbox value
            return chipValue == null || chipValue === undefined || chipValue === '';
        });
    }

    chipsAllHaveNoValue(): boolean {
        return !this.getChips().controls.some(c => {
            const chipValue = this.getChipValue(c);
            // allow unchecked checkbox value
            return chipValue !== null || chipValue !== undefined || chipValue !== '';
        });
    }

    /** Convenience getter for current search bar input value */
    getSearchbarValue(): string {
        return this.getSearchBarValueControl().value;
    }
    setSearchbarValue(v: string, preventEmit?: boolean): void {
        if (this.formGroupMain) {
            this.getSearchBarValueControl().setValue(v, (preventEmit && { onlySelf: true, emitEvent: false }));
        }
    }

    /** Convenience getter for searchbar text control */
    getSearchablePropertiesSearchValue(): string | null {
        return this.formGroupMain ? this.formGroupMain.get('searchBar').value : null;
    }

    getChipPropertyControl(control: AbstractControl): AbstractControl | null {
        return this.getFormControl(control, 'chipProperty');
    }
    getChipPropertyIdentifier(control: AbstractControl): GtxChipSearchPropertyKeys | null {
        const c = this.getChipPropertyControl(control);
        return this.getFormControlValue(c);
    }
    setChipProperty(control: AbstractControl, newValue: GtxChipSearchPropertyKeys): void {
        const c = this.getChipPropertyControl(control);
        this.setFormControlValue(c, newValue);
    }

    getChipOperatorControl(control: AbstractControl): AbstractControl | null {
        return this.getFormControl(control, 'chipOperator');
    }
    getChipOperator<K extends GtxChipSearchPropertyKeys>(control: AbstractControl): GtxChipOperator<K> | null {
        const c = this.getChipOperatorControl(control);
        return this.getFormControlValue(c);
    }
    setChipOperator<K extends GtxChipSearchPropertyKeys>(control: AbstractControl, newValue: GtxChipOperator<K>): void {
        const c = this.getChipOperatorControl(control);
        this.setFormControlValue(c, newValue);
    }

    getChipValueControl(control: AbstractControl): AbstractControl | null {
        return this.getFormControl(control, 'chipValue');
    }
    getChipValueControlAtIndex(index: number): AbstractControl {
        const chip = this.getChips().at(index);
        if (!(chip instanceof AbstractControl)) {
            return;
        }
        const control = this.getChipValueControl(chip);
        if (!(control instanceof AbstractControl)) {
            return;
        }
        return control;
    }
    getChipValue<K extends GtxChipSearchPropertyKeys>(control: AbstractControl): GtxChipValue<K> | null {
        const c = this.getChipValueControl(control);
        return this.getFormControlValue(c);
    }
    setChipValue<K extends GtxChipSearchPropertyKeys>(control: AbstractControl, newValue: GtxChipValue<K>): void {
        const c = this.getChipValueControl(control);
        this.setFormControlValue(c, newValue);
    }

    /** Get entity property option config data object by property name. */
    getPropertyOptionsByIdentifier(chipControl: UntypedFormControl): GtxChipSearchChipPropertyOption | null {
        const chipProperty: GtxChipSearchPropertyKeys = this.getChipPropertyIdentifier(chipControl);
        const prop = this.chipSearchBarConfig.searchableProperties.find(p => p.value === chipProperty);
        return prop ? prop : null;
    }

    /* Searchbar Actions ****************************************************************************************************** */

    /** Remove all chips from searchbar */
    clearSearchbarChips(): void {
        if (this.formGroupMain) {
            this.formGroupMain.setControl('chips', this.formBuilder.array([]));
            this.syncChipsToSearchFilters();
        }
    }

    /** Remove all query data from searchbar which are chips and searchTerm. */
    clearSearchbarAll(): void {
        this.clearFilterTerm();

        if (!this.showRecent) {
            this.clearSearchbarChips();
        }

        this.hideSuggestionBar();
    }

    ignoreClick(event: Event): void {
        event.stopPropagation();
    }

    submitSearch(): void {
        if (this.specialSearchActionInProgess) {
            return;
        }

        // update searchTerm if any
        const term = this.getSearchbarValue();
        if (term) {
            this.submitTermSearch(term);
        }
        // sync current query config to app state
        this.syncChipsToSearchFilters();

    }

    removeChipAtIndex(index: number): void {
        this.getChips().removeAt(index);
    }

    removeLastChip(): void {
        this.getChips().removeAt(this.getChips().length - 1);
        this.formGroupMain.updateValueAndValidity();
    }

    /* Internal: Chips Manipulation ******************************************************************************************* */

    /** Add or update filter chips */
    private addOrUpdateChip<K extends GtxChipSearchPropertyKeys>(
        property: K,
        operator?: GtxChipOperator<K>,
        value?: GtxChipValue<K>,
    ): void {
        const chipExisting: AbstractControl = this.getChipControl(property);

        // if chip already exists, update
        if (chipExisting instanceof AbstractControl && value) {
            this.setChipValue(chipExisting, value);
        } else if (!chipExisting) {
            // create new chip
            this.addChip(property, operator, value);
        }
    }

    private addChipIfNotExisting<K extends GtxChipSearchPropertyKeys>(
        property: K,
        operator?: GtxChipOperator<K>,
        value?: GtxChipValue<K>,
    ): void {
        const chipExisting: AbstractControl = this.getChipControl(property);

        if (!(chipExisting instanceof AbstractControl)) {
            this.addChip(property, operator, value);
        }
    }

    private getChipControl<K extends GtxChipSearchPropertyKeys>(
        property: K,
    ): AbstractControl {
        return this.getChips().controls.find((chipControl: AbstractControl) => {
            return this.getChipPropertyIdentifier(chipControl) === property;
        });
    }

    private addChip<K extends GtxChipSearchPropertyKeys>(
        property: K,
        operator?: GtxChipOperator<K>,
        value?: GtxChipValue<K>,
        atIndex?: number,
    ): void {
        if (!property) {
            return;
        }
        const index = Number.isInteger(atIndex) ? atIndex : this.getChips().length;
        const newChip = this.createChip(property, operator, value);
        this.getChips().insert(index, newChip);
        this.updatePresentation();
        this.changeDetectorRef.markForCheck();
    }

    /** Create a new filter chip instance */
    private createChip<K extends GtxChipSearchPropertyKeys>(
        property: K,
        operator?: GtxChipOperator<K>,
        value?: GtxChipValue<K>,
    ): UntypedFormGroup {
        // if chip is of value type boolean, its value input is hideen and will always be true
        const propertyConfig = this.chipSearchBarConfig.searchableProperties.find(p => p.value === property);
        const isBoolean = propertyConfig && propertyConfig.type === 'boolean';
        return this.formBuilder.group({
            chipProperty: [property, Validators.required],
            chipOperator: [operator || '', Validators.required],
            chipValue: [isBoolean || value || null, Validators.required],
        });
    }

    /* Internal: Filter Management ******************************************************************************************** */

    /** Check searchbar for defined filter settings and set corresponding state filters */
    private syncChipsToSearchFilters<K extends GtxChipSearchPropertyKeys>(): void {

        const searchIsActive = this.searchIsActive();
        const searchIsValid = this.searchIsValid();

        // if either there is no search configured at all or a valid search to be propagated
        if (!searchIsActive || searchIsValid) {
            // notify state that filters start changing
            this.folderActions.setSearchFiltersChanging(true);
            // if chips exist and at least one chip is valid
            const chipPropertyIdentifiers = this.chipSearchBarConfig.searchableProperties.map(item => item.value);
            const chips: AbstractControl[] = this.getChips().controls || [];
            // iterate through all searchable fields and check if chips are set for them
            chipPropertyIdentifiers.forEach(chipPropertyIdentifier => {
                // try to get chip of field
                const chipExisting = chips.find(chip => this.getChipPropertyIdentifier(chip) === chipPropertyIdentifier);

                // if chip is set for field, set field to filter values of chip
                if (chipExisting) {
                    // get value from it
                    const chipValue: GtxChipValue<K> = this.getChipValue(chipExisting);
                    const chipOperator: GtxChipOperator<K> = this.getChipOperator(chipExisting);

                    this.setFilter(chipPropertyIdentifier, chipOperator, chipValue);
                } else {
                    // remove filter
                    this.folderActions.setSearchFilter(chipPropertyIdentifier, null);
                }
            });
            // notify state that filters finnished changing
            this.folderActions.setSearchFiltersChanging(false);
        }
        // notify state
        this.folderActions.setSearchFiltersVisible(searchIsActive);
        this.folderActions.setSearchFiltersValid(searchIsValid);
    }

    private setFilter<K extends GtxChipSearchPropertyKeys>(
        chipProperty: K,
        chipOperator: GtxChipOperator<K>,
        chipValue: GtxChipValue<K> | null,
    ): void {

        if (!chipProperty || !chipOperator) {
            return;
        }

        let searchFiltersNew = null;
        if (chipValue !== null && chipValue !== undefined) {
            searchFiltersNew = [{ value: chipValue, operator: chipOperator }];
        }

        this.folderActions.setSearchFilter(
            chipProperty,
            searchFiltersNew,
        );
    }

    /* Internal: Getter & Setter ********************************************************************************************** */

    private getFormControl(control: AbstractControl, identifier: string): AbstractControl | null {
        if (control instanceof AbstractControl) {
            return control.get(identifier);
        } else {
            return null;
        }
    }

    private getFormControlValue<V>(control: AbstractControl | null): V | null {
        return control ? control.value : null;
    }

    private setFormControlValue<K extends GtxChipSearchPropertyKeys>(
        control: AbstractControl | null,
        newValue: GtxChipValue<K>,
    ): void {
        if (!(control instanceof AbstractControl)) {
            return;
        }
        control.setValue(newValue, { emitEvent: !this.writeFromStateToComponentInProgess });
    }

    /* Internal: Filter setter ************************************************************************************************ */

    private setFilterTerm(filterTerm: string): void {
        this.folderActions.setFilterTerm(filterTerm);
    }

    private clearFilterTerm(): void {
        this.setFilterTerm('');
        this.setSearchbarValue('', !this.writeFromStateToComponentInProgess);
    }


    // Suggestion bar - recent and suggestion items

    showSuggestionBar(): void {
        this.showRecent = Array.isArray(this.state.now.folder.recentItems) &&
            this.state.now.folder.recentItems.length > 0;
        this.showSuggestion = true;
    }

    hideSuggestionBar(): void {
        this.showRecent = false;
        this.showSuggestion = false;
    }

    private setEditorIsFocused(isFocused: boolean): void {
        if (isFocused) {
            this.state.dispatch(new FocusEditorAction());
        } else {
            this.state.dispatch(new FocusListAction());
        }
    }

    /** Update presentation information */
    private updatePresentation(height?: number): void {
        // wait until dom resize is triggered
        setTimeout(() => {
            let headerHeightNew = height;
            if (!height) {
                const rect = (this.formGroupMainElement.nativeElement as HTMLElement).getBoundingClientRect();
                headerHeightNew = rect.height + 39;
            }

            this.presentation.headerHeight = `${headerHeightNew}px`;
        }, 1);
    }

    private submitTermSearch(term: string): void {
        const activeNode = this.state.now.entities.node[this.state.now.folder.activeNode];
        const hosts = Object.values(this.state.now.entities.node).map((node: Node) => node.host);

        if (activeNode) {
            // check if the search term is no live url
            // check if it is a live url without removed www
            // check if it is a live url with added www
            let searchTerm = term;
            if (!isLiveUrl(term, hosts)) {
                if (term.indexOf('www.') !== -1) {
                    const noWWWTerm = term.replace('www.', '');
                    if (isLiveUrl(noWWWTerm, hosts)) {
                        searchTerm = noWWWTerm;
                    }
                } else {
                    if (isLiveUrl(`www.${term}`, hosts)) {
                        searchTerm = `www.${term}`;
                    }
                }
            }

            if (isLiveUrl(searchTerm, hosts)) {
                this.specialSearchActionInProgess = true;
                this.listSearch.searchLiveUrl(term, hosts)
                    .pipe(
                        takeUntil(this.stopper.stopper$),
                    )
                    .subscribe(() => {
                        this.setEditorIsFocused(true);
                        this.specialSearchActionInProgess = false;
                    });
                return;
            }
        }

        const patternShortCutSyntaxId = new RegExp(/^(jump):\d+$/);
        if (patternShortCutSyntaxId.test(term)) {
            this.specialSearchActionInProgess = true;
            // extract number from shortcut syntax
            const entityId = parseInt((/\d+/.exec(term))[0], 10);
            this.listSearch.searchPageId(entityId, activeNode.id || this.state.now.folder.activeNode)
                .then(() => this.specialSearchActionInProgess = false);
            this.folderActions.setFilterTerm('');
        } else {
            // if search value as oure text exists, convert it into a all/any search chip
            this.setDefaultFilters();
            this.addOrUpdateChip('all', null, term);
            this.syncChipsToSearchFilters();
            this.clearFilterTerm();
        }
    }

    /** Configuration allows for additional filters to be set with the `all`-filter. */
    private setDefaultFilters(): void {
        const filters = this.chipSearchBarConfig.defaultFilters;
        if (!Array.isArray(filters) || filters.length === 0) {
            return
        }
        filters.forEach(filterData => {
            this.addChipIfNotExisting(filterData.chipProperty, filterData.chipOperator, filterData.chipValue);
        });
    }

}

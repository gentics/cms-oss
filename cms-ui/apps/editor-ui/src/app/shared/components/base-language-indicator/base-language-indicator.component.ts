import { Directive, EventEmitter, Input, Output, SimpleChange, OnInit, OnChanges, OnDestroy } from '@angular/core';
import { ItemLanguageClickEvent, UIMode } from '@editor-ui/app/common/models';
import { Form, ItemListRowMode, Language, Normalized, Page, StagedItemsMap } from '@gentics/cms-models';
import { BehaviorSubject, Observable, Subject, combineLatest } from 'rxjs';
import { first, map, mergeMap, takeUntil } from 'rxjs/operators';
import { ApplicationStateService, FolderActionsService } from '../../../state';

@Directive()
export abstract class BaseLanguageIndicatorComponent<T extends Page<Normalized> | Form<Normalized>>
    implements OnInit, OnChanges, OnDestroy {

    readonly UIMode = UIMode;

    /** All available languages of the current node */
    @Input()
    public nodeLanguages: Language[];

    /** Current object */
    @Input()
    public item: T;

    /** If TRUE the initial state of the language icons is expanded */
    @Input()
    public expandByDefault = true;

    @Input()
    public activeNodeId: number;

    @Input()
    public mode: ItemListRowMode = ItemListRowMode.DEFAULT;

    @Input()
    public uiMode: UIMode = UIMode.EDIT;

    @Input()
    public stagingMap: StagedItemsMap;

    /** Emits if an action from a langauge icon has been clicked */
    @Output()
    public languageClick = new EventEmitter<ItemLanguageClickEvent<T>>();

    /** Emits if an lang icon is clicked */
    @Output()
    public languageIconClick = new EventEmitter<{ item: T; language: Language; }>();

    /** Current object data stream */
    item$ = new BehaviorSubject<T>(null as T);

    activeFolderLanguage$: Observable<Language>;

    /** Indicating whether current node is in Single- or Multilanguage Mode, which will control styles */
    isMultiLanguage$ = new BehaviorSubject<boolean>(false);

    /** If TRUE additional status icons around the language icons arre visible */
    displayStatusInfos$: Observable<boolean>;
    /** All available item translation languages */
    itemLanguages$: Observable<Language[]>;
    /** Current language in which existing item translations are displayed */
    currentLanguage$: Observable<Language>;

    /** IDs of all existing translations of the current item */
    languageVariantsIds$ = new BehaviorSubject<number[]>([]);
    /** All existing translations of the current item */
    languageVariants$: Observable<{ [key: number]: T }>;
    /** All existing items including translations */
    allItems$: Observable<{ [key: number]: T }>;

    /** Controls visibility of untranslated languages. */
    expanded$: Observable<boolean>;
    /** Controls visibility of untranslated languages of component instance. */
    expandedLocal$ = new BehaviorSubject<boolean>(false);
    /** Memorizing last value of visibility of untranslated languages of component instance. */
    expandedLocalMemory$ = new BehaviorSubject<boolean>(false);

    /** Determine whether to display the "show more" ellipses icon. */
    displayMoreIcon$: Observable<boolean>;
    /** Determine whether to display the "show less" arrow icon. */
    displayLessIcon$: Observable<boolean>;
    /** Returns true if the item has not yet been translated into all possible languages for the node. */
    hasUntranslatedLanguages$: Observable<boolean>;
    /** On this.expanded$ changing, limit the displayed languages to those for which translations already exist. */
    displayLanguages$: Observable<Language[]>;

    displayDeleted$: Observable<boolean>;

    protected destroy$ = new Subject<void>();

    /** CONSTRUCTOR */
    constructor(
        protected itemIdentifier: string,
        protected appState: ApplicationStateService,
        protected folderActions: FolderActionsService,
    ) { }

    /** On component initialization */
    ngOnInit(): void {
        this.activeFolderLanguage$ = this.appState.select(state => state.folder.activeLanguage).pipe(
            mergeMap((activeLanguageId: number) => this.appState.select(state => state.entities.language[activeLanguageId])),
        );

        // initial settings
        this.expandedLocal$.next(this.expandByDefault);

        // get all existing items from store
        this.allItems$ = this.appState.select(state => state.entities[this.itemIdentifier]);

        // get display options from app state
        this.displayStatusInfos$ = combineLatest([
            this.appState.select(state => state.folder.displayStatusIcons),
            this.appState.select(state => state.ui.mode),
        ]).pipe(
            map(([enabled, uiMode]) => enabled || uiMode === UIMode.STAGING),
        );

        this.expanded$ = this.appState.select(state => state.folder.displayAllLanguages);

        // override local value with store value
        this.expanded$.pipe(
            takeUntil(this.destroy$),
        ).subscribe(expanded => this.expandedLocal$.next(expanded));

        // listen to current language variants IDs and all items from app state
        this.languageVariants$ = combineLatest([
            this.languageVariantsIds$,
            this.allItems$,
        ]).pipe(
            first(),
            map(([variantsIds, allItems]) => {
                return Object.keys(allItems)
                    .reduce((items: { [key: number]: T }, key: string) => {
                        return -1 < variantsIds.indexOf(parseInt(key, 10)) ? { ...items, ...{ [key]: allItems[key] } } : items;
                    }, {});
            }),
        );

        this.displayDeleted$ = this.appState.select(state => state.folder.displayDeleted);
    }

    ngOnChanges(changes: { [K in keyof this]?: SimpleChange }): void {
        if (changes.item) {
            this.item$.next(this.item);
        }

        // check for multiple languages available for current node
        if (changes.nodeLanguages && changes.nodeLanguages.currentValue) {
            this.isMultiLanguage$.next(1 < this.nodeLanguages.length);
        }
    }

    /**
     * On component destruction
     */
    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    afterLanguageInit(): void {
        this.hasUntranslatedLanguages$ = this.itemLanguages$.pipe(
            map(itemLanguages => itemLanguages.length < this.nodeLanguages.length),
        );

        this.displayMoreIcon$ = combineLatest([this.hasUntranslatedLanguages$, this.expandedLocal$]).pipe(
            map(([hasUntranslatedLanguages, expandedLocal]) => hasUntranslatedLanguages && !expandedLocal),
        );

        this.displayLessIcon$ = combineLatest([this.hasUntranslatedLanguages$, this.expandedLocal$]).pipe(
            map(([hasUntranslatedLanguages, expandedLocal]) => hasUntranslatedLanguages && expandedLocal),
        );

        this.displayLanguages$ = combineLatest([this.itemLanguages$, this.expandedLocal$]).pipe(
            map(([itemLanguages, expandedLocal]) => expandedLocal ? this.nodeLanguages : itemLanguages),
        );
    }

    identify(index: number, element: Language): string {
        return element.code;
    }

    /** Emits action information to parent component */
    languageClicked(language: Language, compare: boolean = false, source: boolean = true, restore: boolean = false): void {
        this.languageClick.emit({
            item: this.item,
            language,
            compare,
            source,
            restore,
        });
    }

    /**
     * The "show more" ellipses or "show less" arrow was clicked.
     */
    toggleExpand(value: boolean): void {
        this.expandedLocal$.next(value);
    }

    isModeSelect(): boolean {
        return this.mode === ItemListRowMode.SELECT;
    }

    onIconClicked(language: Language): void {
        this.languageIconClick.emit({ item: this.item, language });
    }
}

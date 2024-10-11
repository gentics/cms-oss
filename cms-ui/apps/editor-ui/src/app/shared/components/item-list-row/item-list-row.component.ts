import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { ItemLanguageClickEvent, ItemListRowMode, ItemsInfo, UIMode } from '@editor-ui/app/common/models';
import {
    ApplicationStateService,
    ChangeListSelectionAction,
    FocusEditorAction,
    FolderActionsService,
    WastebinActionsService,
} from '@editor-ui/app/state';
import { EditMode } from '@gentics/cms-integration-api-models';
import {
    File,
    Folder,
    FolderItemType,
    Form,
    Image,
    Item,
    Language,
    Node as NodeModel,
    Normalized,
    Page,
    Raw,
    StagedItemsMap,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { DecisionModalsService } from '../../../core/providers/decision-modals/decision-modals.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { EntityStateUtil, PublishableStateUtil } from '../../../shared/util/entity-states';
import { TranslatePageModal, TranslatePageModalActions, TranslateResult } from '../translate-page-modal/translate-page-modal.component';
import { UsageModalComponent } from '../usage-modal/usage-modal.component';

type AllowedItemType =
    | Folder<Raw | Normalized>
    | Form<Raw | Normalized>
    | Page<Raw | Normalized>
    | File<Raw | Normalized>
    | Image<Raw | Normalized>
    ;

@Component({
    selector: 'item-list-row',
    templateUrl: './item-list-row.component.html',
    styleUrls: ['./item-list-row.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ItemListRowComponent implements OnInit {

    readonly UIMode = UIMode;

    @Input()
    public item: AllowedItemType;

    @Input()
    public nodeId: number;

    @Input()
    public itemInEditor: any;

    @Input()
    public icon: string;

    @Input()
    public selected: boolean;

    @Input()
    public itemType: FolderItemType;

    @Input()
    public startPageId: number;

    @Input()
    public linkPaths: string;

    @Input()
    public expandByDefault: string;

    @Input()
    public nodeLanguages: Language[];

    @Input()
    public itemsInfo: ItemsInfo;

    @Input()
    public activeNode: NodeModel;

    @Input()
    public filterTerm: string;

    @Input()
    public showDeleted: boolean;

    @Input()
    public canBeSelected = true;

    /**
     * Determine whether this component is in DEFAULT mode (behavior in `item-list`) or
     * in SELECT mode (behavior in `repository-browser-list`).
     */
    @Input()
    public mode: ItemListRowMode = ItemListRowMode.DEFAULT;

    /**
     * Determine which functionality is currently beiung used by the UI.
     */
    @Input()
    public uiMode: UIMode = UIMode.EDIT;

    @Input()
    public stagingMap: StagedItemsMap;

    /** On selection change by click on checkbox */
    @Output()
    public selectedChange = new EventEmitter<boolean>();

    /** On click on `<a>item.name</a>` */
    @Output()
    public itemClick = new EventEmitter<Item>();

    /** Emits if a page language icon is clicked */
    @Output()
    public pageLanguageIconClick = new EventEmitter<{ page: Page<Raw>; language: Language; }>();

    /** Emits if a form language icon is clicked */
    @Output()
    public formLanguageIconClick = new EventEmitter<{ form: Form<Raw>; language: Language; }>();

    /**
     * @returns TRUE if selected node provides more than one language. If there is only one node language
     * the indicator-current is not a language code but a cloud icon instead including status icons, will be hidden by default
     * and display on ```state.folder.displayStatusIcons = true```.
     */
    get singleLanguageIndicatorCurrentIsVisible$(): Observable<boolean> {
        return this.appState.select(state => {
            return this.nodeLanguages && this.nodeLanguages.length > 1 || state.folder.displayStatusIcons === true;
        });
    }

    searchTerm$: Observable<string>;
    elasticSearchQueryActive$: Observable<boolean>;

    @ViewChild('itemPrimary', { read: ElementRef, static: true })
    itemPrimary: ElementRef;

    constructor(
        private appState: ApplicationStateService,
        private modalService: ModalService,
        private errorHandler: ErrorHandler,
        private entityResolver: EntityResolver,
        private navigationService: NavigationService,
        private decisionModals: DecisionModalsService,
        private folderActions: FolderActionsService,
        private wastebinActions: WastebinActionsService,
        private changeDetectorRef: ChangeDetectorRef,
    ) { }

    ngOnInit(): void {
        this.searchTerm$ = this.appState.select(state => state.folder.searchTerm);
        this.elasticSearchQueryActive$ = this.appState.select(state => state.folder.searchFiltersVisible);
    }

    toggleSelect(): void {
        // if in SELECT mode, prevent propagation of select event to state which
        // shall be done in DEFAULT mode only
        if (this.mode !== ItemListRowMode.SELECT) {
            this.appState.dispatch(new ChangeListSelectionAction(this.itemType, this.selected ? 'remove' : 'append', [this.item.id]));
        }

        this.selected = !this.selected;
        this.selectedChange.emit(this.selected);
    }

    /**
     * Focus the editor when an item is clicked.
     */
    itemClicked(e: MouseEvent, item: Item): void {
        e.preventDefault();
        e.stopPropagation();
        e.stopImmediatePropagation();

        this.itemClick.emit(item);
        // do nothing if in SELECT mode or if in STAGING mode
        if (this.isModeSelect() || this.isModeStaging()) {
            return;
        }

        if (item.type === 'page' || item.type === 'form' || item.type === 'file' || item.type === 'image') {
            this.appState.dispatch(new FocusEditorAction());
        }
    }

    /**
     * Opens up a modal displaying the usage for the selected item.
     */
    showUsage(item: Item): void {
        const nodeId = this.activeNode.id;
        const currentLanguageId = this.appState.now.folder.activeLanguage;
        this.modalService.fromComponent(UsageModalComponent, {}, { item, nodeId, currentLanguageId })
            .then(modal => modal.open())
            .catch(this.errorHandler.catch);
    }

    /**
     * A page language variant was clicked. If the variant exists, we preview it. If it does not
     * exist, we display the "create translation" dialog.
     */
    pageLanguageClicked(event: ItemLanguageClickEvent<Page<Normalized>>): void {
        const { item, language, compare, source, restore } = event;
        const pageLanguageIds = item.languageVariants ? Object.keys(item.languageVariants).map(id => +id) : [];
        const languageVariantId = item.languageVariants && item.languageVariants[language.id];
        const pageTranslation = languageVariantId && this.entityResolver.getPage(languageVariantId);

        if (restore) {
            const entityToBeRestoredId = languageVariantId;
            this.wastebinActions.restoreItemsFromWastebin('page', [entityToBeRestoredId])
                .then(() => this.changeDetectorRef.markForCheck());
            return;
        }

        const isDeleted = this.isDeleted(pageTranslation);

        if (-1 < pageLanguageIds.indexOf(language.id) && pageTranslation && !isDeleted) {
            if (compare) {
                // compare two language versions
                this.editPageCompareWithLanguage(item, item.languageVariants[language.id]);
            } else {
                const languageVariantId = item.languageVariants[language.id];
                const languageVariant = this.entityResolver.getPage(languageVariantId);
                this.navigationService.detailOrModal(this.activeNode.id, 'page', languageVariant.id, EditMode.PREVIEW).navigate();
            }
            return;
        } else if (source && !pageTranslation && !isDeleted) {
            this.folderActions.updatePageLanguage(item.id, language).then(() => {
                this.folderActions.refreshList('page');
            });
            return;
        }

        // Page does not exist in the selected language, so prompt to create a translation.
        this.decisionModals.showTranslatePageDialog(item, this.activeNode.id)
            .then(nodeId => this.modalService.fromComponent(TranslatePageModal, null, {
                defaultProps: {
                    name: item.name,
                    description: item.description || '',
                    language: language.code,
                    priority: item.priority,
                    templateId: item.templateId,
                },
                languageName: language.name,
                pageId: item.id,
                nodeId,
                folderId: item.folderId,
            }))
            .then(modal => modal.open())
            // If user created translation and wants to edit, opensplitscreen; if not, do nothing.
            .then((data: TranslateResult) => {
                if (!data.newPage) {
                    return;
                }
                switch (data.action) {
                    case TranslatePageModalActions.EDIT_PAGE:
                        this.editPage(data.newPage);
                        break;
                    case TranslatePageModalActions.EDIT_PAGE_COMPARE_WITH_LANGUAGE:
                        this.editPageCompareWithLanguage(data.newPage, item.id);
                        break;
                }
            });
    }

    /**
     * A page language variant was clicked. If the variant exists, we preview it. If it does not
     * exist, we display the "create translation" dialog.
     */
    async formLanguageClicked(event: ItemLanguageClickEvent<Form>): Promise<void> {
        const { item, language, source, restore } = event;

        if (restore) {
            const entityToBeRestoredId = item.id;
            await this.wastebinActions.restoreItemsFromWastebin('form', [entityToBeRestoredId]);
            this.changeDetectorRef.markForCheck();
            return;
        }

        const isDeleted = PublishableStateUtil.stateDeleted(item);
        if (isDeleted) {
            return;
        }

        if (source) {
            this.folderActions.setActiveFormLanguage(language.id);
            this.navigationService.detailOrModal(this.activeNode.id, 'form', item.id, EditMode.PREVIEW).navigate();
            return;
        }

        await this.folderActions.updateFormLanguage(item, language);
        await this.folderActions.setActiveFormLanguage(language.id);
        await this.folderActions.refreshList('form');
        this.navigationService.detailOrModal(this.activeNode.id, 'form', item.id, EditMode.EDIT).navigate();
    }

    isModeSelect(): boolean {
        return this.mode === ItemListRowMode.SELECT;
    }

    isModeStaging(): boolean {
        return this.uiMode === UIMode.STAGING;
    }

    getItemDetailsDisplayFields(): string[] {
        return this.itemsInfo && this.itemsInfo.displayFields;
    }

    getItemDetailsShowPaths(): boolean {
        return this.itemsInfo && this.itemsInfo.showPath;
    }

    onPageLanguageIconClicked(data: { page: Page<Raw> | Page<Normalized>; language: Language; }): void {
        const pageRaw = this.entityResolver.denormalizeEntity('page', data.page);
        this.pageLanguageIconClick.emit({ page: pageRaw, language: data.language });
    }

    onFormLanguageIconClicked(data: { form: Form<Raw> | Form<Normalized>; language: Language; }): void {
        const formRaw = this.entityResolver.denormalizeEntity('form', data.form);
        this.formLanguageIconClick.emit({ form: formRaw, language: data.language });
    }

    /**
     * @returns TRUE if item has been deleted and is in wastebin
     */
    isDeleted(item: Item): boolean {
        if (item) {
            return EntityStateUtil.stateDeleted(item);
        }
    }

    /**
     * Edit a page.
     */
    private editPage(page: Page): void {
        const nodeId = page.inherited ? page.inheritedFromId : this.activeNode.id;
        this.navigationService.detailOrModal(nodeId, 'page', page.id, EditMode.EDIT).navigate();
    }

    /**
     * Edit a page in split screen mode, comparing it with the given language variant.
     */
    private editPageCompareWithLanguage(page: Page, compareWithId: number): void {
        const nodeId = page.inherited ? page.inheritedFromId : this.activeNode.id;
        const options = { compareWithId };
        this.navigationService.detailOrModal(nodeId, 'page', page.id, EditMode.EDIT, options).navigate();
    }

}

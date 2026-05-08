import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
} from '@angular/core';
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
import { BaseComponent, ChangesOf, ModalService } from '@gentics/ui-core';
import { FolderPermissionData, ItemLanguageClickEvent, ItemListRowMode, ItemsInfo, LanguageState, UIMode } from '../../../common/models';
import { DecisionModalsService } from '../../../core/providers/decision-modals/decision-modals.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { EntityStateUtil, PublishableStateUtil } from '../../../shared/util/entity-states';
import {
    ApplicationStateService,
    ChangeListSelectionAction,
    FocusEditorAction,
    FolderActionsService,
    WastebinActionsService,
} from '../../../state';
import { TranslatePageModal, TranslatePageModalActions, TranslateResult } from '../translate-page-modal/translate-page-modal.component';
import { UsageModalComponent } from '../usage-modal/usage-modal.component';
import { FormListLoaderService } from '../../providers';

type AllowedItemType
    = | Folder<Raw | Normalized>
      | Form
      | Page<Raw | Normalized>
      | File<Raw | Normalized>
      | Image<Raw | Normalized>
    ;

@Component({
    selector: 'item-list-row',
    templateUrl: './item-list-row.component.html',
    styleUrls: ['./item-list-row.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class ItemListRowComponent extends BaseComponent implements OnChanges {

    public readonly UIMode = UIMode;
    public readonly ItemListRowMode = ItemListRowMode;

    @Input()
    public item: AllowedItemType;

    @Input()
    public nodeId: number;

    @Input()
    public activeItemId: number;

    @Input()
    public icon: string;

    @Input()
    public selected: boolean;

    @Input()
    public itemType: FolderItemType;

    @Input()
    public external = false;

    @Input()
    public permissions: FolderPermissionData;

    @Input()
    public startPageId: number;

    @Input()
    public linkPaths: string;

    @Input()
    public expandByDefault: boolean;

    @Input()
    public nodeLanguages: Language[];

    @Input()
    public activeLanguage: Language | null = null;

    @Input()
    public itemsInfo: ItemsInfo;

    @Input()
    public activeNode: NodeModel;

    @Input()
    public filterTerm: string;

    @Input()
    public showDeleted: boolean;

    @Input()
    public showStatusIcons: boolean;

    @Input()
    public canBeSelected = true;

    @Input()
    public searching: boolean;

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
    public pageLanguageIconClick = new EventEmitter<{ page: Page<Raw>; language: Language }>();

    /** Emits if a form language icon is clicked */
    @Output()
    public formLanguageIconClick = new EventEmitter<{ form: Form; language: Language }>();

    public languageState: LanguageState;
    public itemIdDeleted = false;

    constructor(
        changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
        private modalService: ModalService,
        private errorHandler: ErrorHandler,
        private entityResolver: EntityResolver,
        private navigationService: NavigationService,
        private decisionModals: DecisionModalsService,
        private folderActions: FolderActionsService,
        private wastebinActions: WastebinActionsService,
        private formListLoader: FormListLoaderService,
    ) {
        super(changeDetector);
    }

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.item || changes.nodeLanguages || changes.activeLanguage) {
            this.updateLanguageState();
        }
    }

    updateLanguageState(): void {
        let itemLang: Language;
        let available: boolean;

        if (this.item.type === 'page') {
            itemLang = this.nodeLanguages.find((lang) => lang.code === (this.item as Page).language);
            available = !!itemLang;
        } else if (this.item.type === 'form') {
            itemLang = this.nodeLanguages.find((lang) => (lang.id === this.appState.now.folder.activeFormLanguage && ((this.item as Form).languages.includes(lang.code))));
            if (!itemLang) {
                itemLang = this.nodeLanguages.find((lang) => (this.item as Form).languages.includes(lang.code));
            }
            available = !!itemLang;
        } else {
            this.languageState = null;
            return;
        }

        this.itemIdDeleted = this.item != null
          && PublishableStateUtil.stateDeleted(this.item);

        this.languageState = {
            ...itemLang,
            available,
            deleted: this.itemIdDeleted,
            inherited:
                this.item != null
                && PublishableStateUtil.stateInherited(this.item),
            localized:
                this.item != null
                && PublishableStateUtil.stateLocalized(this.item),
            modified:
                this.item != null
                && PublishableStateUtil.stateModified(this.item),
            planned:
                this.item != null
                && PublishableStateUtil.statePlanned(this.item),
            published:
                this.item != null
                && PublishableStateUtil.statePublished(this.item),
            queued:
                this.item != null
                && PublishableStateUtil.stateInQueue(this.item),
            staged: this.item != null
              && this.stagingMap?.[this.item.globalId]?.included,
        };
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
        if (this.mode === ItemListRowMode.SELECT || this.uiMode === UIMode.STAGING) {
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
        const currentLanguageId = this.activeLanguage?.id;
        this.modalService.fromComponent(UsageModalComponent, {}, { item, nodeId, currentLanguageId })
            .then((modal) => modal.open())
            .catch(this.errorHandler.catch);
    }

    /**
     * A page language variant was clicked. If the variant exists, we preview it. If it does not
     * exist, we display the "create translation" dialog.
     */
    pageLanguageClicked(event: ItemLanguageClickEvent<Page<Normalized>>): void {
        const { item, language, compare, source, restore } = event;
        const pageLanguageIds = item.languageVariants ? Object.keys(item.languageVariants).map((id) => +id) : [];
        const languageVariantId = item.languageVariants && item.languageVariants[language.id];
        const pageTranslation = languageVariantId && this.entityResolver.getPage(languageVariantId);
        const pageLanguageIsSet = item.language ?? false;

        if (restore) {
            const entityToBeRestoredId = languageVariantId;
            this.wastebinActions.restoreItemsFromWastebin('page', [entityToBeRestoredId])
                .then(() => this.changeDetector.markForCheck());
            return;
        }

        const isDeleted = this.isDeleted(pageTranslation);

        if (pageLanguageIds.includes(language.id) && pageTranslation && !isDeleted) {
            if (compare) {
                // compare two language versions
                this.editPageCompareWithLanguage(item, item.languageVariants[language.id]);
            } else {
                const languageVariantId = item.languageVariants[language.id];
                const languageVariant = this.entityResolver.getPage(languageVariantId);
                this.navigationService.detailOrModal(this.activeNode.id, 'page', languageVariant.id, EditMode.PREVIEW).navigate();
            }
            return;
        } else if (source && !pageTranslation && !pageLanguageIsSet && !isDeleted) {
            this.folderActions.updatePageLanguage(item.id, language).then(() => {
                this.folderActions.refreshList('page');
            });
            return;
        }

        // Page does not exist in the selected language, so prompt to create a translation.
        this.decisionModals.showTranslatePageDialog(item, this.activeNode.id)
            .then((nodeId) => this.modalService.fromComponent(TranslatePageModal, null, {
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
            .then((modal) => modal.open())
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
            this.changeDetector.markForCheck();
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
        this.formListLoader.reload();
        this.navigationService.detailOrModal(this.activeNode.id, 'form', item.id, EditMode.EDIT).navigate();
    }

    getItemDetailsDisplayFields(): string[] {
        return this.itemsInfo && this.itemsInfo.displayFields;
    }

    getItemDetailsShowPaths(): boolean {
        return this.itemsInfo && this.itemsInfo.showPath;
    }

    onPageLanguageIconClicked(data: { item: Page<Raw> | Page<Normalized>; language: Language }): void {
        const pageRaw = this.entityResolver.denormalizeEntity('page', data.item);
        this.pageLanguageIconClick.emit({ page: pageRaw, language: data.language });
    }

    onFormLanguageIconClicked(data: { item: Form; language: Language }): void {
        const formRaw = this.entityResolver.denormalizeEntity('form', data.item);
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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, Type } from '@angular/core';
import { EditorPermissions, ItemsInfo, StageableItem, StagingMode, UIMode, getNoPermissions, plural } from '@editor-ui/app/common/models';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { EditMode } from '@gentics/cms-integration-api-models';
import {
    EditableFormProps,
    EditablePageProps,
    Feature,
    FolderItemType,
    FolderItemTypePlural,
    Form,
    InheritableItem,
    Item,
    ItemType,
    Language,
    Node as NodeModel,
    Page,
    SortField,
    StagedItemsMap,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { PaginationInstance } from 'ngx-pagination';
import { Observable, Subscription, combineLatest } from 'rxjs';
import { debounceTime, filter, map, switchMap, take } from 'rxjs/operators';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { UploadConflictService } from '../../../core/providers/upload-conflict/upload-conflict.service';
import { UserSettingsService } from '../../../core/providers/user-settings/user-settings.service';
import { DisplayFieldSelectorModal, SortingModal } from '../../../shared/components';
import { EntityStateUtil } from '../../../shared/util/entity-states';
import { ApplicationStateService, ChangeListSelectionAction, FolderActionsService } from '../../../state';
import { CreateFolderModalComponent } from '../create-folder-modal/create-folder-modal.component';
import { CreateFormModalComponent } from '../create-form-modal/create-form-modal.component';
import { CreatePageModalComponent } from '../create-page-modal/create-page-modal.component';

@Component({
    selector: 'item-list-header',
    templateUrl: './item-list-header.component.html',
    styleUrls: ['./item-list-header.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ItemListHeaderComponent implements OnInit, OnChanges, OnDestroy {

    readonly UIMode = UIMode;
    readonly StagingMode = StagingMode;
    readonly plural = plural;

    @Input()
    public itemsInfo: ItemsInfo;

    @Input()
    public filterTerm = '';

    @Input()
    public selectedItems: Item[] = [];

    @Input()
    public items: Item[];

    @Input()
    public icon = '';

    @Input()
    public nodeLanguages: Language[] = [];

    // TODO: Check if this is somehow used? Doesn't seem like it
    @Input()
    public activeLanguage: Language;

    @Input()
    public itemType: FolderItemType;

    @Input()
    public acceptUploads = '';

    @Input()
    public folderPermissions: EditorPermissions = getNoPermissions();

    @Input()
    public canCreateItem = true;

    @Input()
    public activeNode: NodeModel;

    @Input()
    public currentFolderId: number;

    @Input()
    public showAllLanguages: boolean;

    @Input()
    public showStatusIcons: boolean;

    @Input()
    public showDeleted: boolean;

    @Input()
    public showImagesGridView: boolean;

    @Input()
    public paginationConfig: PaginationInstance;

    @Input()
    public uiMode: UIMode = UIMode.EDIT;

    @Input()
    public stagingMap: StagedItemsMap;

    isCollapsed = false;
    wastebinEnabled = false;
    multiChannelingEnabled = false;
    folderLanguage: Language = null;
    elasticsearchQueryActive = false;
    searchQueryActive = false;

    private itemsInfo$: Observable<ItemsInfo>;
    protected subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
        private modalService: ModalService,
        private errorHandler: ErrorHandler,
        private entityResolver: EntityResolver,
        private navigationService: NavigationService,
        private folderActions: FolderActionsService,
        private userSettings: UserSettingsService,
        private uploadConflictService: UploadConflictService,
        private contextMenuOperations: ContextMenuOperationsService,
        private notifications: I18nNotification,
        private i18n: I18nService,
    ) {}

    ngOnInit(): void {
        this.itemsInfo$ = this.appState.select(state => state.folder).pipe(
            map(folderState => folderState[`${this.itemType}s` as FolderItemTypePlural]),
        );

        const basicSearchQueryActive$ = this.appState.select(state => state.folder.searchTerm).pipe(
            map(term => this.isValidString(term)),
        );

        const esQueryActive$ = combineLatest([
            this.appState.select(state => state.features[Feature.ELASTICSEARCH]),
            this.appState.select(state => state.folder.searchFiltersVisible),
        ]).pipe(
            map(([elasticsearchFeatureEnabled, seearchQueryActive]) => elasticsearchFeatureEnabled && seearchQueryActive),
        );

        this.subscriptions.push(this.appState.select(state => state.folder.activeLanguage).pipe(
            map(langId => this.entityResolver.getLanguage(langId)),
        ).subscribe(lang => {
            this.folderLanguage = lang;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(esQueryActive$.subscribe(active => {
            this.elasticsearchQueryActive = active;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(combineLatest([
            basicSearchQueryActive$,
            esQueryActive$,
        ]).pipe(
            map(([basicSearchQueryActive, elasticsearchQueryActive]) => basicSearchQueryActive || elasticsearchQueryActive),
        ).subscribe(active => {
            this.searchQueryActive = active;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.features[Feature.WASTEBIN]).subscribe(enabled => {
            this.wastebinEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.features[Feature.MULTICHANNELLING]).subscribe(enabled => {
            this.multiChannelingEnabled = enabled;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['selectedItems'] && !changes['selectedItems'].currentValue) {
            this.selectedItems = [];
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    toggleSelectAll(selectAllItems: boolean): void {
        if (selectAllItems) {
            this.selectAll();
        } else {
            this.appState.dispatch(new ChangeListSelectionAction(this.itemType, 'clear'));
        }
    }

    /**
     * Add all visible (i.e. not hidden by a filter) items to the selectedItems array.
     */
    selectAll(): void {
        if (!this.itemsInfo.hasMore) {
            this.appState.dispatch(new ChangeListSelectionAction(this.itemType, 'replace', this.items.map(item => item.id)));
            return;
        }

        // If only the first page of items have been loaded, we need to load them
        // all before selecting them all, otherwise we only know the ids of the
        // first page of items.
        this.folderActions.getItemsOfTypeInFolder(this.itemType, this.currentFolderId, this.appState.now.folder.searchTerm, true);
        this.itemsInfo$.pipe(
            map(itemsInfo => itemsInfo.hasMore),
            filter(hasMore => hasMore === false),
            take(1),
            switchMap(() => this.itemsInfo$.pipe(
                map(itemsInfo => itemsInfo.list)),
            ),
            // This debounce is required due to the batched update feature
            // (see folder-state-actions.ts, applyListBatch())
            debounceTime(Math.ceil(this.itemsInfo.total / 20) * 15),
            take(1),
        ).subscribe(ids => {
            this.appState.dispatch(new ChangeListSelectionAction(this.itemType, 'replace', ids));
        });
    }

    /**
     * Display a create folder/page dialog.
     */
    createClicked(): void {
        let isPage: boolean;
        let isForm: boolean;
        let dialog: Type<CreateFolderModalComponent> | Type<CreatePageModalComponent> | Type<CreateFormModalComponent>;
        const activeNodeId = this.activeNode.id;
        const activeLanguage = this.entityResolver.getLanguage(this.appState.now.folder.activeLanguage);
        const defaultProps: EditablePageProps & EditableFormProps = {
            language: activeLanguage && activeLanguage.code,
        };

        switch (this.itemType) {
            case 'page':
                isPage = true;
                defaultProps.templateId = this.appState.now.folder.templates.list[0] || undefined;
                dialog = CreatePageModalComponent;
                break;
            case 'folder':
                dialog = CreateFolderModalComponent;
                break;
            case 'form':
                isForm = true;
                dialog = CreateFormModalComponent;
                break;
            default:
                throw new Error(`Invalid item type: "${this.itemType}".`);
        }

        this.modalService.fromComponent<CreateFolderModalComponent | CreatePageModalComponent | CreateFormModalComponent>(
            dialog,
            { width: '600px' },
            { defaultProps },
        )
            .then(modal => modal.open())
            .then((newItem: Page | Form) => {
                this.folderActions.refreshList(this.itemType);
                if (isPage) {
                    this.navigationService.detailOrModal(activeNodeId, 'page', newItem.id, EditMode.EDIT).navigate();
                }
                if (isForm) {
                    this.navigationService.detailOrModal(activeNodeId, 'form', newItem.id, EditMode.EDIT).navigate();
                }
            })
            .catch(this.errorHandler.catch);
    }

    /**
     * The language context for the pages has been changed.
     */
    selectLanguage(language: Language): void {
        this.userSettings.setActiveLanguage(language?.id);
    }

    toggleDisplayAllLanguages(): void {
        const currentVal = this.appState.now.folder.displayAllLanguages;
        this.userSettings.setDisplayAllLanguages(!currentVal);
    }

    toggleDisplayStatusIcons(): void {
        const currentVal = this.appState.now.folder.displayStatusIcons;
        this.userSettings.setDisplayStatusIcons(!currentVal);
    }

    toggleDisplayDeleted(): void {
        const currentVal = this.appState.now.folder.displayDeleted;
        this.userSettings.setDisplayDeleted(!currentVal);
        // refresh list as refetch is required
        this.folderActions.refreshList('folder');
        this.folderActions.refreshList('form');
        this.folderActions.refreshList('page');
        this.folderActions.refreshList('file');
        this.folderActions.refreshList('image');
    }

    toggleDisplayImagesGridView(): void {
        const currentVal = this.appState.now.folder.displayImagesGridView;
        this.userSettings.setDisplayImagesGridView(!currentVal);
    }

    uploadFiles(files: File[]): void {
        this.uploadConflictService.uploadFilesWithConflictsCheck(files, this.activeNode.id, this.currentFolderId);
    }

    /**
     * Helper method to filter out deleted items and which shows a notification if
     * no regular/not deleted item has been selected yet.
     */
    private getNotDeletedItems(): Item[] {
        const validItems = this.selectedItems.filter(item => !EntityStateUtil.stateDeleted(item));
        if (validItems.length !== 0) {
            return validItems;
        }

        this.notifications.show({
            message: 'editor.select_not_deleted_items',
            translationParams: {
                itemTypePlural: this.i18n.translate(`common.type_${this.itemType}s`),
            },
            type: 'warning',
        });
        return [];

    }

    /**
     * Copy the selected items to a different folder and clear the selection.
     */
    copySelected(): void {
        const itemsToCopy = this.getNotDeletedItems();
        if (itemsToCopy.length === 0) {
            return;
        }

        this.contextMenuOperations.copyItems(this.itemType, itemsToCopy, this.activeNode.id);
    }

    /**
     * Move the selected items to a different folder and clear the selection.
     */
    moveSelected(): void {
        const itemsToMove = this.getNotDeletedItems();
        if (itemsToMove.length === 0) {
            return;
        }

        this.contextMenuOperations.moveItems(this.itemType, itemsToMove, this.activeNode.id, this.currentFolderId);
    }

    /**
     * Publish the selected pages and clear the selection.
     */
    publishSelected(type: FolderItemType): void {
        const itemsToPublish = this.getNotDeletedItems();
        if (itemsToPublish.length === 0) {
            return;
        }

        switch (type) {
            case 'page':
                this.contextMenuOperations.publishPages(itemsToPublish as Page[]);
                break;
            case 'form':
                this.contextMenuOperations.publishForms(itemsToPublish as Form[]);
                break;
            default:
                throw new Error(`Behavior not defined for type: "${type}".`);
        }
    }

    /**
     * Publish the selected pages and clear the selection.
     */
    publishLanguageVariantsSelected(): void {
        const itemsToPublish = this.getNotDeletedItems();
        if (itemsToPublish.length === 0) {
            return;
        }

        this.contextMenuOperations.publishPages(itemsToPublish as Page[], true);
    }

    /**
     * Take the selected pages offline and clear the selection.
     */
    takeSelectedOffline(type: ItemType): void {
        const itemsToTakeOffline = this.getNotDeletedItems();
        if (itemsToTakeOffline.length === 0) {
            return;
        }

        switch (type) {
            case 'page':
                this.contextMenuOperations.takePagesOffline(itemsToTakeOffline as Page[]);
                break;
            case 'form':
                this.contextMenuOperations.takeFormsOffline(itemsToTakeOffline as Form[]);
                break;
            default:
                throw new Error(`Behavior not defined for type: "${type}".`);
        }
    }

    /**
     * Take the selected items offline and clear the selection.
     */
    localizeSelected(): void {
        const itemsToBeLocalized = this.getNotDeletedItems();
        if (itemsToBeLocalized.length === 0) {
            return;
        }

        this.contextMenuOperations.localizeItems(this.itemType, itemsToBeLocalized as InheritableItem[], this.activeNode.id);
    }

    /**
     * Delete the selected items and clear the selection.
     */
    deleteSelected(type: FolderItemType): void {
        const itemsToBeDeleted = this.getNotDeletedItems();
        if (itemsToBeDeleted.length === 0) {
            return;
        }

        this.contextMenuOperations.deleteItems(type, itemsToBeDeleted, this.activeNode.id);
    }

    /**
     * Restore the selected items and clear the selection.
     */
    restoreSelected(): void {
        const itemsToBeRestored = this.selectedItems.filter(item => EntityStateUtil.stateDeleted(item));
        if (itemsToBeRestored.length === 0) {
            this.notifications.show({
                message: 'editor.select_deleted_items',
                translationParams: {
                    itemTypePlural: this.i18n.translate(`common.type_${this.itemType}s`),
                },
                type: 'warning',
            });

            return;
        }

        this.contextMenuOperations.restoreItems(itemsToBeRestored);
    }

    /**
     * Open the DisplayFieldSelector component in a modal.
     */
    selectDisplayFields(): void {
        const type = this.itemType;
        const fields = this.itemsInfo.displayFields;
        const showPath = this.itemsInfo.showPath;
        this.modalService.fromComponent(DisplayFieldSelectorModal, {}, { type, showPath, fields })
            .then(modal => modal.open())
            .then((result: {selection: string[], showPath: boolean}) => {
                this.updateDisplayFields(this.itemType, result.selection);
                this.updateShowPath(this.itemType, result.showPath);
            })
            .catch(this.errorHandler.catch);
    }

    updateDisplayFields(type: ItemType, fields: string[]): void {
        this.userSettings.setDisplayFields(type, fields);
    }

    updateShowPath(type: ItemType, showPath: boolean): void {
        this.userSettings.setShowPath(type, showPath);
    }

    /**
     * Open the modal for selecting sort option.
     */
    selectSorting(): void {
        const locals: Partial<SortingModal> = {
            itemType: this.itemType,
            sortBy: this.itemsInfo.sortBy,
            sortOrder: this.itemsInfo.sortOrder,
        };

        this.modalService.fromComponent(SortingModal, {}, locals)
            .then(modal => modal.open())
            .then(sorting => {
                this.updateSorting(this.itemType, sorting);
            })
            .catch(this.errorHandler.catch);
    }

    updateSorting(type: ItemType, sorting: { sortBy: SortField; sortOrder: 'asc' | 'desc'; }): void {
        this.userSettings.setSorting(type, sorting.sortBy, sorting.sortOrder);
    }

    createVariationsClicked(): void {
        const pagesToCreateVariationsFrom = this.getNotDeletedItems();
        if (pagesToCreateVariationsFrom.length === 0) {
            return;
        }

        this.contextMenuOperations.createVariationsClicked(pagesToCreateVariationsFrom as Page[], this.activeNode.id);
    }

    async stageItems(mode: StagingMode): Promise<void> {
        const itemsToStage = this.getNotDeletedItems();
        if (itemsToStage.length === 0) {
            return;
        }

        let counter = 0;

        for (const item of itemsToStage) {
            let res: boolean;
            if (this.itemType === 'page') {
                res = await this.contextMenuOperations.stagePageToCurrentPackage(item as Page, mode === StagingMode.ALL_LANGUAGES, false);
            } else {
                res = await this.contextMenuOperations.stageItemToCurrentPackage(item as StageableItem, mode === StagingMode.RECURSIVE, false);
            }
            if (res) {
                counter++;
            }
        }

        if (counter === itemsToStage.length) {
            if (counter === 1) {
                this.notifications.show({
                    type: 'success',
                    message: 'editor.stage_item_success_message',
                    translationParams: {
                        itemType: this.i18n.translate(`common.type_${this.itemType}_article`),
                        itemName: itemsToStage[0].name,
                    },
                });
            } else {
                this.notifications.show({
                    type: 'success',
                    message: 'editor.stage_multiple_items_success_message',
                    translationParams: {
                        itemType: this.i18n.translate(`common.type_${plural[this.itemType]}`),
                        amount: itemsToStage.length,
                    },
                });
            }
        } else if (counter > 0) {
            this.notifications.show({
                type: 'warning',
                message: 'editor.stage_multiple_items_partial_success_message',
                translationParams: {
                    itemType: this.i18n.translate(`common.type_${plural[this.itemType]}`),
                    amount: itemsToStage.length,
                    count: counter,
                },
            });
        }
        // All other errors are reported anyways
    }

    async unstageItems(mode: StagingMode): Promise<void> {
        const itemsToUnstage = this.getNotDeletedItems();
        if (itemsToUnstage.length === 0) {
            return;
        }

        let counter = 0;

        for (const item of itemsToUnstage) {
            let res: boolean;
            if (this.itemType === 'page') {
                res = await this.contextMenuOperations.unstagePageFromCurrentPackage(item as Page, mode === StagingMode.ALL_LANGUAGES, false);
            } else {
                res = await this.contextMenuOperations.unstageItemFromCurrentPackage(item as StageableItem, false);
            }
            if (res) {
                counter++;
            }
        }

        if (counter === itemsToUnstage.length) {
            if (counter === 1) {
                this.notifications.show({
                    type: 'success',
                    message: 'editor.unstage_item_success_message',
                    translationParams: {
                        itemType: this.i18n.translate(`common.type_${this.itemType}_article`),
                        itemName: itemsToUnstage[0].name,
                    },
                });
            } else {
                this.notifications.show({
                    type: 'success',
                    message: 'editor.unstage_multiple_items_success_message',
                    translationParams: {
                        itemType: this.i18n.translate(`common.type_${plural[this.itemType]}`),
                        amount: itemsToUnstage.length,
                    },
                });
            }
        } else if (counter > 0) {
            this.notifications.show({
                type: 'warning',
                message: 'editor.unstage_multiple_items_partial_success_message',
                translationParams: {
                    itemType: this.i18n.translate(`common.type_${plural[this.itemType]}`),
                    amount: itemsToUnstage.length,
                    count: counter,
                },
            });
        }
        // All other errors are reported anyways
    }

    private isValidString(data: any): boolean {
        return typeof data === 'string' && data.length > 0;
    }

}

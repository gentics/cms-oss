import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { EditorPermissions, StageableItem } from '@editor-ui/app/common/models';
import { parentFolderOfItem } from '@editor-ui/app/common/utils/parent-folder-of-item';
import {
    ChannelDependenciesModal,
    InheritanceDialog,
    LinkTemplateModal,
    PublishProtocolModalComponent,
    MultiDeleteResult,
    PageVersionsModal,
    PublishTimeManagedPagesModal,
    SynchronizeChannelModal,
    TimeManagementModal,
} from '@editor-ui/app/shared/components';
import { RepositoryBrowserClient } from '@editor-ui/app/shared/providers';
import { EntityStateUtil, PublishableStateUtil } from '@editor-ui/app/shared/util/entity-states';
import {
    ApplicationStateService,
    ChangeListSelectionAction,
    ContentStagingActionsService,
    FolderActionsService,
    TemplateActionsService,
    UsageActionsService,
    WastebinActionsService,
} from '@editor-ui/app/state';
import { InitializableServiceBase } from '@gentics/cms-components';
import {
    EditMode,
    ModalCloseError, ModalClosingReason,
    RepositoryBrowserOptions,
    wasClosedByUser
} from '@gentics/cms-integration-api-models';
import {
    ChannelSyncRequest,
    CmsFormData,
    CmsFormElement,
    DependencyItemTypePlural,
    Favourite,
    Feature,
    File as FileModel,
    Folder,
    FolderItemType,
    Form,
    Image,
    InheritableItem,
    InheritanceRequest,
    Item,
    ItemInNode,
    ItemsGroupedByChannelId,
    Node,
    Normalized,
    Page,
    Raw,
    Template,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { Observable, combineLatest, forkJoin, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, take, takeUntil } from 'rxjs/operators';
import { ApiError } from '../api';
import { DecisionModalsService } from '../decision-modals/decision-modals.service';
import { EntityResolver } from '../entity-resolver/entity-resolver';
import { ErrorHandler } from '../error-handler/error-handler.service';
import { FavouritesService } from '../favourites/favourites.service';
import { I18nNotification } from '../i18n-notification/i18n-notification.service';
import { I18nService } from '../i18n/i18n.service';
import { LocalizationMap } from '../localizations/localizations.service';
import { NavigationInstruction, NavigationService } from '../navigation/navigation.service';
import { PermissionService } from '../permissions/permission.service';

/**
 * Encapsulates the logic for the various context-menu functions which are shared amongst a number of context menus.
 */
@Injectable()
export class ContextMenuOperationsService extends InitializableServiceBase {

    private currentStagingPackage: string;

    constructor(
        private decisionModals: DecisionModalsService,
        private folderActions: FolderActionsService,
        private router: Router,
        private state: ApplicationStateService,
        private entityResolver: EntityResolver,
        private notification: I18nNotification,
        private permissions: PermissionService,
        private wastebinActions: WastebinActionsService,
        private errorHandler: ErrorHandler,
        private modalService: ModalService,
        private usageActions: UsageActionsService,
        private navigationService: NavigationService,
        private favourites: FavouritesService,
        private i18n: I18nService,
        private repositoryBrowserClient: RepositoryBrowserClient,
        private templateActions: TemplateActionsService,
        private contentStagingActions: ContentStagingActionsService,
    ) {
        super();
        // Actual way to initialize the service is only done in the admin-ui
        this.init();
    }

    protected onServiceInit(): void {
        this.state.select(state => state.contentStaging.activePackage).pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(pkg => {
            this.currentStagingPackage = pkg;
        });
    }

    editNodeProperties(nodeId: number): void {
        this.navigationService.detailOrModal(nodeId, 'node', nodeId, EditMode.EDIT_PROPERTIES).navigate();
    }

    editItem(item: InheritableItem, activeNodeId: number): void {
        this.decisionModals.showInheritedDialog(item, activeNodeId)
            .then(({ item, nodeId }) => this.navigationService.detailOrModal(nodeId, item.type, item.id, EditMode.EDIT).navigate());
    }

    editProperties(item: InheritableItem, activeNodeId: number): void {
        this.decisionModals.showInheritedDialog(item, activeNodeId)
            .then(({ item, nodeId }) => {
                // This is the case when editing the nodes root-folder properties, and then the
                // type is incorrect which would fail.
                let type: FolderItemType | 'node' | 'channel' = item.type;
                if (type === 'node' || type === 'channel') {
                    type = 'folder';
                }

                this.navigationService.detailOrModal(nodeId, type, item.id, EditMode.EDIT_PROPERTIES).navigate();
            });
    }

    editInParentNode(item: InheritableItem): void {
        const nodeId = item.inherited ? item.inheritedFromId : item.masterNodeId;
        const editMode = (item.type === 'page' || item.type === 'form' || item.type === 'image') ? EditMode.EDIT : EditMode.EDIT_PROPERTIES;

        this.folderActions.getItem(item.id, item.type, { nodeId })
            .then(itemInMasterNode => {
                const folderToNavigateTo = parentFolderOfItem(itemInMasterNode);
                const instruction: NavigationInstruction = {
                    list: {
                        nodeId,
                        folderId: folderToNavigateTo,
                    },
                    detail: {
                        nodeId,
                        itemType: itemInMasterNode.type as FolderItemType,
                        itemId: itemInMasterNode.id,
                        editMode,
                    },
                };
                this.navigationService.instruction(instruction).navigate();
            });
    }

    /**
     * Opens a modal where the user can link all templates available in current node to the folder defined
     */
    async linkTemplatesToFolder(
        nodeId: number,
        folderId: number,
    ): Promise<Template[] | null> {
        const featureLinkTemplatesNew = this.state.now.features[Feature.FOLDER_BASED_TEMPLATE_SELECTION];

        if (!featureLinkTemplatesNew) {
            const modal = await this.modalService.fromComponent(LinkTemplateModal, {
                padding: true,
                width: '1000px',
            }, {
                nodeId,
                folderId,
            });
            try {
                await modal.open();
            } catch (err) {
                if (!wasClosedByUser(err)) {
                    throw err;
                }
            }
            return null;
        }

        let selectResult: Template[];
        try {
            selectResult = await this.repositoryBrowserClient.openRepositoryBrowser({
                allowedSelection: ['template'],
                selectMultiple: true,
                startNode: nodeId,
                startFolder: folderId,
            });
        } catch (err) {
            if (!wasClosedByUser(err)) {
                throw err;
            }
        }

        if (!selectResult || !selectResult.length) {
            return null;
        }

        const dialog = await this.modalService.dialog({
            title: this.i18n.translate('modal.link_templates_to_folder_modal_checkbox_apply_recursively_label'),
            buttons: [
                {
                    label: this.i18n.translate('common.no_label'),
                    type: 'secondary',
                    flat: true,
                    returnValue: false,
                    shouldReject: false,
                },
                {
                    label: this.i18n.translate('common.yes_label'),
                    type: 'alert',
                    returnValue: true,
                },
            ],
        });

        try {
            const recursive: boolean = await dialog.open();

            await this.templateActions.linkTemplatesToFolders(nodeId, selectResult.map(t => t.id), [folderId], recursive).toPromise();
            return this.folderActions.getTemplates(folderId, true);
        } catch (err) {
            if (!wasClosedByUser(err)) {
                throw err;
            }
        }
    }

    /**
     * Deletes (or unlocalizes) the given items. Returns a promise which resolves to an array of successfully deleted item ids.
     *
     * Deleting can be applied to two kinds of entities: translatable (Pages, Forms) and non-translatable (Images, Files).
     * Then there are two kinds of translatable entities:
     *
     * Pages have their translations in `page.languageVariants` which are fully-qualified page instances in
     * other languages linked to each other.
     *
     * Forms work different: they indicate active translations in `form.languages[string]` and the corresponding values to
     * be availabe in properties with `_i18n` suffixes.
     *
     * Consequentially, Page translations are deleted by deleting their pageVariants, while Form translations are deleted by
     * removing the language to be deleted from the form.languages array and all object withing `i18n`-properties with key [language.code].
     */
    async deleteItems(type: FolderItemType, items: Item[], activeNodeId: number): Promise<number[]> {
        // If multiple items are to be deleted, some of them may be inherited or localized.
        // In that case, open a modal to give feedback to the user.
        let selectResult: MultiDeleteResult;
        try {
            selectResult = await this.decisionModals.selectItemsToDelete(items as InheritableItem[]);
        } catch (err) {
            // Ignore user close errors
            if (!(err instanceof ModalCloseError) || err.reason === ModalClosingReason.ERROR) {
                throw err;
            }
            return;
        }

        let deleteResult: { succeeded: number, failed: number, error: ApiError } | null = null;
        let updateResult: { succeeded: number, failed: number, error: ApiError } | null = null;

        let deleteIds: number[] = [];
        let unlocalizeIds: number[] = [];
        // deleting Form translations without deleting the Form means updating the Form
        const updateItems: {
            itemId: number;
            payload: Partial<Form>;
        }[] = [];

        if (type === 'form') {
            Object.keys(selectResult.deleteForms).forEach(id => {
                const formId = parseInt(id, 10);
                const languageCodesToDelete = selectResult.deleteForms[formId];
                const form = items.find(i => i.id === formId) as Form<Normalized>;

                if (
                    !Array.isArray(languageCodesToDelete)
                    || languageCodesToDelete.length >= form.languages.length
                ) {
                    deleteIds.push(formId);
                    return;
                }

                if (languageCodesToDelete.length === 0) {
                    return;
                }

                updateItems.push({
                    itemId: formId,
                    payload: {
                        languages: form.languages.filter(l => !languageCodesToDelete.includes(l)),
                        data: this.removeLanguagesFromFormData(form.data, languageCodesToDelete),
                    },
                });
            });
        } else {
            deleteIds = selectResult.delete.map(item => item.id) || [];
            unlocalizeIds = selectResult.unlocalize.map(item => item.id) || [];
        }

        const localizationIdsDeleted: LocalizationMap = {};
        let localizationIds: number[];

        if (deleteIds.length) {
            deleteResult = await this.wastebinActions.moveItemsToWastebin(type, deleteIds, activeNodeId, deleteIds.length > 1);

            // filter only localizations that has been deleted and put them to array of IDs
            // forms cannot be localized
            if (type !== 'form') {
                deleteIds.forEach(id => { localizationIdsDeleted[id] = selectResult.localizations[id]; });
                localizationIds = localizationIdsDeleted && this.flattenMap(localizationIdsDeleted);
            }
        }
        if (unlocalizeIds.length) {
            await this.folderActions.unlocalizeItems(type, unlocalizeIds, activeNodeId);
        }
        if (updateItems.length) {
            updateResult = { succeeded: 0, failed: 0, error: null };
            for (const item of updateItems) {
                try {
                    await this.folderActions.updateItem(type, item.itemId, item.payload);
                    updateResult.succeeded++;
                } catch (error) {
                    updateResult.failed++;
                    updateResult.error = error;
                }
            }
        }

        const removedItemIds = [...deleteIds, ...unlocalizeIds, ...updateItems.map(i => i.itemId)];
        const results = {
            succeeded: deleteResult?.succeeded ?? 0 + updateResult?.succeeded ?? 0,
            failed: deleteResult?.failed ?? 0 + updateResult?.failed ?? 0,
            error: deleteResult?.error ?? updateResult?.error ?? null,
        };
        await this.showMultiDeleteResultNotification(results, unlocalizeIds, type, removedItemIds, localizationIds, type !== 'form')
            .toPromise();
        await this.deleteItemsFavourites(activeNodeId, type, deleteResult);

        if (deleteResult && deleteResult.failed) {
            this.showMultiDeleteErrorNotification(type, deleteResult.failed, deleteResult.error);
        }
        await this.folderActions.refreshList(type);
        await this.state.dispatch(new ChangeListSelectionAction(type, 'remove', removedItemIds)).toPromise();

        return removedItemIds;
    }

    /**
     * Restores the given items. Returns a promise which resolves to an array of successfully restored item ids.
     */
    async restoreItems(items: Item[]): Promise<void> {
        const itemMap: { [key in FolderItemType]: Item[] } = {
            file: [],
            folder: [],
            form: [],
            image: [],
            page: [],
        };

        Object.keys(itemMap).forEach(key => {
            itemMap[key] = items.filter(singleItem => singleItem.type === key && this.isDeleted(singleItem));
        });

        // if no language variants exist, dont show modal as there would be nothing to select
        if (itemMap.page.length === 0 || !itemMap.page.some(p => Object.keys((p as Page).languageVariants).length > 0)) {
            await Promise.all(Object.keys(itemMap).map((type: FolderItemType) => {
                if (itemMap[type].length <= 0) {
                    return Promise.resolve();
                }

                const parentFolderId = this.state.now.folder.activeFolder;
                const idsBeRestored: number[] = itemMap[type].map((i: Item) => i.id);
                return this.wastebinActions.restoreItemsFromWastebin(type, idsBeRestored)
                    .then(() => this.folderActions.getItems(parentFolderId, type));
            }));

            return;
        }

        const itemIdsToBeRestored = await this.decisionModals.selectItemsToRestore(
            itemMap.file as FileModel[],
            itemMap.folder as Folder[],
            itemMap.form as Form[],
            itemMap.image as Image[],
            itemMap.page as Page[],
        );

        if (!itemIdsToBeRestored) {
            return;
        }

        const requests: Promise<void>[] = Object.keys(itemIdsToBeRestored).map((type: FolderItemType) => {
            if (itemIdsToBeRestored[type].length > 0) {
                return this.wastebinActions.restoreItemsFromWastebin(type, itemIdsToBeRestored[type]);
            }
        }).filter(action => action != null);

        await Promise.all(requests);
    }

    /**
     * Delete items from favourites list
     */
    private async deleteItemsFavourites(nodeId: number, type: FolderItemType, deleteResult: any): Promise<void> {
        if (deleteResult && deleteResult.ids && deleteResult.ids.succeeded && deleteResult.ids.succeeded.length > 0) {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            const favouritesToRemove: Favourite[] = deleteResult.ids.succeeded.map((id: number) => {
                const entity = this.entityResolver.getEntity(type, id);
                return { id: entity.id, type: entity.type, name: entity.name, globalId: entity.globalId, nodeId: entity.masterNodeId };
            });

            await this.favourites.remove(favouritesToRemove);
        }
    }

    private showMultiDeleteResultNotification(
        deleteResult: { succeeded: number; failed: number; },
        unlocalizeIds: number[],
        type: FolderItemType,
        removedItemIds: number[],
        localizationIds: number[],
        isUndoable: boolean = true,
    ): Observable<void> {
        let message: string;
        if (deleteResult && deleteResult.succeeded) {
            if (unlocalizeIds.length) {
                message = 'message.items_deleted_or_unlocalized';
            } else {
                message = 'message.items_deleted';
            }
        } else if (unlocalizeIds.length) {
            message = 'message.items_unlocalized';
        }

        const undoAction = isUndoable && {
            label: 'common.undo_button',
            onClick: (): void => {
                this.wastebinActions.restoreItemsFromWastebin(type, removedItemIds, localizationIds);
            },
        };

        return combineLatest([
            this.permissions.wastebin$,
            this.state.select(state => state.features.wastebin),
        ])
            .pipe(
                debounceTime(50),
                distinctUntilChanged(isEqual),
                map(([wastebinFeature, wastebinPermission]) => wastebinFeature && wastebinPermission),
                take(1),
                map((isUndoablePerFeature) => {
                    this.notification.show({
                        message,
                        translationParams: {
                            count: deleteResult && deleteResult.succeeded || 0,
                            unlocalizedCount: unlocalizeIds.length,
                            _type: type,
                        },
                        type: 'default',
                        delay: 5000,
                        action: isUndoablePerFeature && undoAction || null,
                    });
                }),
            );
    }

    private showMultiDeleteErrorNotification(type: string, failed: number, error: ApiError): void {
        this.notification.show({
            message: 'message.items_deleted_error',
            translationParams: {
                count: failed,
                _type: type,
                errorMessage: error.message,
            },
            type: 'alert',
            delay: 5000,
        });
    }

    private flattenMap<T>(hashMap: { [id: number]: T[] }): number[] {
        return Object.keys(hashMap).reduce((all, id) => all.concat(hashMap[+id]), []);
    }

    /**
     * Show Modal to configure page / form TimeManagement (scheduling publish/unpublish events).
     */
    showTimeManagement(item: Page | Form, nodeId: number): void {
        this.modalService.fromComponent(TimeManagementModal, {}, { item, currentNodeId: nodeId })
            .then(modal => modal.open())
            .then(() => {
                // refresh folder content list to display new TimeManagement settings
                this.folderActions.refreshList('page');
                this.folderActions.refreshList('form');
            })
            .catch(err => {
                if (!wasClosedByUser(err)) {
                    throw err;
                }
            });
    }

    /**
     * Publish pages (and possibly their language variants) and
     * then clear the current pages selection.
     */
    async publishPages(pages: Page[], publishLanguageVariants: boolean = false): Promise<void> {
        const pagesNotDeleted = pages.filter(page => !this.isDeleted(page));
        try {
            const pagesToPublish = await this.decisionModals.selectPagesToPublish(pagesNotDeleted, publishLanguageVariants)
            const ids = await this.publishPagesWithTimeManagementCheck(pagesToPublish);
            const pageLanguages = ids.map(id => this.entityResolver.getPage(id).language);
            await this.state.dispatch(new ChangeListSelectionAction('page', 'clear')).toPromise();
            await this.folderActions.refreshList('page', pageLanguages);
        } catch (err) {
            this.errorHandler.catch(err);
        }
    }

    /**
     * Publishes the specified pages - for those that have a publishAt date set,
     * the user will be asked what to do.
     */
    private publishPagesWithTimeManagementCheck(pages: Page[]): Promise<number[]> {
        const timeManagedPages: Page[] = [];
        const nonTimeManagedPages: Page[] = [];
        pages.forEach((page) => {
            if (PublishableStateUtil.statePlannedOnline(page) || PublishableStateUtil.stateInQueue(page)) {
                timeManagedPages.push(page);
            } else {
                nonTimeManagedPages.push(page);
            }
        });

        const publishedPages: Promise<number[]>[] = [];

        if (nonTimeManagedPages.length > 0) {
            const promise = this.folderActions.publishPages(nonTimeManagedPages)
                .then(({ queued, published }) => [...queued, ...published].map(item => item.id));
            publishedPages.push(promise);
        }

        if (timeManagedPages.length > 0) {
            const promise = this.modalService.fromComponent(
                PublishTimeManagedPagesModal, {}, { pages: timeManagedPages, allPages: pages.length },
            )
                .then(modal => modal.open())
                .then((pages: Page[]) => {
                    return pages.map(page => page.id);
                })
                .catch(() => ([]));
            publishedPages.push(promise);
        }

        return Promise.all(publishedPages)
            .then(results => {
                const ret: number[] = [];
                results.forEach(ids => {
                    ret.push(...ids);
                });
                return ret;
            });
    }

    /**
     * Take pages (and possibly their language variants) offline.
     */
    async takePagesOffline(pages: Page[]): Promise<void> {
        try {
            const pagesToTakeOffline = await this.decisionModals.selectPagesToTakeOffline(pages);
            const response = await this.folderActions.takePagesOffline(pagesToTakeOffline);
            // Wait half a second to let the server unlock the pages
            await new Promise(resolve => setTimeout(resolve, 500));
            await this.state.dispatch(new ChangeListSelectionAction('page', 'clear')).toPromise();
            const languages = new Set([...response.queued, ...response.takenOffline].map(page => page.language));
            await this.folderActions.refreshList('page', Array.from(languages));
        } catch (error) {
            this.errorHandler.catch(error);
        }
    }

    /**
     * Take forms (and possibly their language variants) offline.
     */
    async takeFormsOffline(forms: Form[]): Promise<any> {
        const formIds = forms.map(form => form.id);
        try {
            await this.folderActions.takeFormsOffline(formIds);
            await this.state.dispatch(new ChangeListSelectionAction('form', 'clear')).toPromise();
            await this.folderActions.refreshList('form');
        } catch (error) {
            this.errorHandler.catch(error);
        }
    }

    listPageVersions(page: Page, activeNodeId: number): void {
        const options = { page, nodeId: activeNodeId };
        this.modalService.fromComponent(PageVersionsModal, null, options)
            .then(modal => modal.open())
            .catch(err => {});
    }

    openPublishProtocol(item: Page | Form): void {
        const options = {
            item,
        };

        this.modalService.fromComponent(PublishProtocolModalComponent, null, options)
            .then(modal => modal.open())
            .catch(err => {});
    }

    localize(item: InheritableItem, activeNodeId: number): void {
        const localizingEditedItem: boolean = item.id === this.state.now.editor.itemId;
        this.folderActions.localizeItem(item.type, item.id, activeNodeId)
            .then((item: InheritableItem | null) => {
                // It's null when the localization is being done in the background
                if (item == null) {
                    return;
                }

                this.folderActions.refreshList(item.type);
                if (this.state.now.editor.editorIsOpen && localizingEditedItem) {
                    this.navigationService.detailOrModal(activeNodeId, item.type, item.id, EditMode.PREVIEW).navigate();
                }
            });
    }

    /**
     * Localize pages.
     */
    localizeItems(type: FolderItemType, items: InheritableItem[], activeNodeId: number): Promise<void> {
        return this.folderActions.localizeItems(type, items, activeNodeId).then(() => {
            this.state.dispatch(new ChangeListSelectionAction(type, 'clear'));
            this.folderActions.refreshList(type);
        });
    }

    /**
     * Display a repo browser and then copy the items to the target folder.
     * Returns a promise that resolves to the folder which the items have been copied to.
     */
    async copyItems(itemType: FolderItemType, items: Item[], activeNodeId: number): Promise<Folder | undefined> {

        const options: RepositoryBrowserOptions = {
            allowedSelection: 'folder',
            title: 'modal.copy_item_title',
            submitLabel: 'modal.copy_item_submit',
            requiredPermissions: (selection: any, parent: Folder<Raw> | Node<Raw>, node: Node): Observable<boolean> => {
                return this.permissions.forFolder(parent.id, node.id).pipe(
                    map((perms: any) => perms[itemType] ? perms[itemType].create : false),
                );
            },
            selectMultiple: false,
        };

        const targetFolder = await this.repositoryBrowserClient.openRepositoryBrowser(options) as ItemInNode<Folder<Raw>>;
        if (!targetFolder) {
            return;
        }


        if (itemType === 'page') {
            const itemIds = items.map(item => item.id);
            await this.folderActions.copyPagesToFolder(itemIds, activeNodeId, targetFolder.id, targetFolder.nodeId);
        } else if (itemType === 'form') {
            const itemIds = items.map(item => item.id);
            await this.folderActions.copyFormsToFolder(itemIds, activeNodeId, targetFolder.id);
        } else if (itemType === 'file' || itemType === 'image') {
            const files = items as FileModel[];
            await this.folderActions.copyFilesToFolder(files, activeNodeId, targetFolder.id, targetFolder.nodeId);
        } else {
            return
        }

        await this.goToOrRefreshFolder(targetFolder, itemType);
        return targetFolder;
    }

    private goToOrRefreshFolder(folder: Folder, itemType: FolderItemType): Promise<any> {
        const { activeFolder, activeNode } = this.state.now.folder;

        if (folder.id === activeFolder && folder.nodeId === activeNode) {
            this.folderActions.refreshList(itemType);
            return Promise.resolve();
        } else {
            return this.navigationService.list(folder.nodeId, folder.id).navigate();
        }
    }

    /**
     * Display a repo browser for selecting the location(s) in which to create the page variation(s).
     */
    createVariationsClicked(pages: Page[], activeNodeId: number): void {
        const options: RepositoryBrowserOptions = {
            allowedSelection: 'folder',
            selectMultiple: true,
            submitLabel: 'modal.create_page_variations_submit',
            title: 'modal.create_page_variations_title',
        };

        this.repositoryBrowserClient.openRepositoryBrowser(options)
            .then((targetFolders: Folder<Raw>[]) => this.folderActions.createPageVariations(pages, activeNodeId, targetFolders))
            .then(() => this.usageActions.getTotalUsage('page', pages.map((page) => page.id), activeNodeId))
            .catch(this.errorHandler.catch);
    }

    /**
     * Display a repo browser for selecting the location in which to create the page variation.
     */
    createVariationClicked(page: Page, activeNodeId: number): void {
        const options: RepositoryBrowserOptions = {
            allowedSelection: 'folder',
            selectMultiple: false,
            submitLabel: 'modal.create_page_variation_submit',
            title: 'modal.create_page_variation_title',
        };

        this.repositoryBrowserClient.openRepositoryBrowser(options)
            .then((targetFolder: Folder<Raw>) => this.folderActions.createSinglePageVariation(page, activeNodeId, targetFolder))
            .catch(this.errorHandler.catch);
    }

    moveItems(itemType: FolderItemType, items: Item[], activeNodeId: number, currentFolderId: number): Promise<any> {
        const options: RepositoryBrowserOptions = {
            allowedSelection: 'folder',
            selectMultiple: false,
            title: 'modal.move_item_title',
            submitLabel: 'modal.move_item_submit',
            requiredPermissions: (selection: any, parent: Folder<Raw> | Node<Raw>, node: Node): Observable<boolean> => {
                return this.permissions.forFolder(parent.id, node.id).pipe(
                    map((perms: EditorPermissions) => perms[itemType] ? perms[itemType].create : false),
                );
            },
        };

        return this.repositoryBrowserClient.openRepositoryBrowser(options)
            .then((folder: Folder<Raw>) => {
                if (folder && (folder.id !== currentFolderId || folder.nodeId !== activeNodeId)) {
                    const node = this.entityResolver.getNode(folder.nodeId);
                    return this.decisionModals.moveMultipleItems(items as InheritableItem[], folder, node)
                        .then(items => this.folderActions.moveItemsToFolder(itemType, items.map(item => item.id), folder.id, folder.nodeId))
                        .then(success => success && this.navigationService.list(folder.nodeId, folder.id).navigate());
                }
            })
            .catch(err => {
                if (!wasClosedByUser(err)) {
                    this.errorHandler.catch(err);
                }
            });
    }

    requestTranslation(pageId: number, nodeId: number): void {
        const tool = this.state.now.tools.available.find(tool => tool.key.startsWith('task-management'));
        const urlInTool = `new-process/translate?sourcePage=${pageId}&nodeid=${nodeId}`;
        this.router.navigateByUrl(`/tools/${tool.key}/${urlInTool}`);
    }

    setInheritance(item: InheritableItem, activeNodeId: number): void {
        this.folderActions.fetchItemInheritance(item.type, item.id, activeNodeId)
            .then(item => {
                const nodes = this.state.now.entities.node;
                return this.modalService.fromComponent(InheritanceDialog, {}, { item, nodes });
            })
            .then(modal => modal.open())
            .then((request: InheritanceRequest) => this.folderActions.updateItemInheritance(item.type, item.id, request))
            .catch(err => {
                if (!wasClosedByUser(err)) {
                    this.errorHandler.catch(err);
                }
            });
    }

    async synchronizeChannel(item: Folder | Page | FileModel | Image): Promise<void> {
        const channel: Node = this.entityResolver.getNode(this.state.now.folder.activeNode);
        let folderResponse: ChannelSyncRequest;

        const syncModal = await this.modalService.fromComponent(SynchronizeChannelModal, {}, { item, channel });
        let syncResponse: ChannelSyncRequest;
        try {
            syncResponse = await syncModal.open();
        } catch (err) {
            if (!wasClosedByUser(err)) {
                throw err;
            }
            return;
        }

        if (item.type === 'folder') {
            folderResponse = syncResponse;
            folderResponse.ids = [item.id];
        }
        const masterNode = this.entityResolver.getEntity('node', syncResponse.masterId);

        const depModal = await this.modalService.fromComponent(ChannelDependenciesModal, {
            padding: true,
            width: '800px',
        }, {
            item,
            response: syncResponse,
        });
        let depResponse: ItemsGroupedByChannelId;

        try {
            depResponse = await depModal.open();
        } catch (err) {
            if (!wasClosedByUser(err)) {
                throw err;
            }
            return;
        }

        try {
            await this.pushFolderToMaster(item.type, folderResponse).toPromise();
            await this.pushNonFolderItems(depResponse).toPromise();
            this.showPushToMasterSuccessNotification(masterNode);
        } catch (error) {
            this.showPushToMasterSuccessNotification(masterNode);
            this.errorHandler.catch(error);
        }

        this.folderActions.refreshList('page');
        this.folderActions.refreshList('file');
        this.folderActions.refreshList('image');
    }

    /**
     * Publish forms (there is only one `online` property accounting for all translations) and
     * then clear the current forms selection.
     */
    publishForms(forms: Form[]): Promise<number[]> {
        const formsToPublish = forms.filter(form => !this.isDeleted(form));
        return this.folderActions.publishForms(formsToPublish)
            .then(({ queued, published }) => {
                this.state.dispatch(new ChangeListSelectionAction('form', 'clear'));
                this.folderActions.refreshList('form');
                return published.map(form => form.id);
            });
    }

    async stagePageToCurrentPackage(page: Page, allVariations: boolean = false, notify: boolean = true): Promise<boolean> {
        let error: Error;

        try {
            if (allVariations) {
                await this.contentStagingActions.stageAllPageVariants(page, this.currentStagingPackage);
            } else {
                await this.contentStagingActions.stageItem(page, this.currentStagingPackage);
            }

        } catch (err) {
            error = err;
        }

        if (notify || error) {
            this.showStagingNotification(page, 'stage_item', error);
        }

        return !error;
    }

    async unstagePageFromCurrentPackage(page: Page, allVariations: boolean = false, notify: boolean = true): Promise<boolean> {
        let error: Error;

        try {
            if (allVariations) {
                await this.contentStagingActions.unstageAllPageVariants(page, this.currentStagingPackage);
            } else {
                await this.contentStagingActions.unstageItem(page, this.currentStagingPackage);
            }
        } catch (err) {
            error = err;
        }

        if (notify || error) {
            this.showStagingNotification(page, 'unstage_item', error);
        }

        return !error;
    }

    async stageItemToCurrentPackage(item: StageableItem, recursive: boolean = false, notify: boolean = true): Promise<boolean> {
        let error: Error;

        try {
            await this.contentStagingActions.stageItem(item, this.currentStagingPackage, recursive);
        } catch (err) {
            error = err;
        }

        if (notify || error) {
            this.showStagingNotification(item, 'stage_item', error);
        }

        return !error;
    }

    async unstageItemFromCurrentPackage(item: StageableItem, notify: boolean = true): Promise<boolean> {
        let error: Error;

        try {
            await this.contentStagingActions.unstageItem(item, this.currentStagingPackage);
        } catch (err) {
            error = err;
        }

        if (notify || error) {
            this.showStagingNotification(item, 'unstage_item', error);
        }

        return !error;
    }

    private showStagingNotification(item: StageableItem, base: string, error?: Error): void {
        const itemType = this.i18n.translate(`common.type_${item.type}_article`);
        const itemName = item.name;

        if (!error) {
            this.notification.show({
                type: 'success',
                message: `editor.${base}_success_message`,
                translationParams: {
                    itemType,
                    itemName,
                },
            });
        } else {
            console.error('Error while (un-)staging element', error);

            this.notification.show({
                type: 'alert',
                message: `editor.${base}_error_message`,
                translationParams: {
                    itemType,
                    itemName,
                    errorMessage: error.message,
                },
            });
        }
    }

    private pushNonFolderItems(response: ItemsGroupedByChannelId): Observable<any> {
        const requests: Array<Observable<any>> = [];
        Object.keys(response).forEach((type: DependencyItemTypePlural) => {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            response[type].forEach((item: ChannelSyncRequest) => {
                requests.push(this.folderActions.pushItemsToMaster((type.slice(0, -1)) as FolderItemType | any, item));
            });
        });
        return forkJoin(requests);
    }

    private pushFolderToMaster(itemType: FolderItemType, folderResponse: ChannelSyncRequest): Observable<any> {
        if (folderResponse) {
            return this.folderActions.pushItemsToMaster('folder' as FolderItemType | any, folderResponse as ChannelSyncRequest | any);
        } else {
            return of();
        }
    }

    private showPushToMasterSuccessNotification(masterNode: Node): void {
        this.notification.show({
            type: 'success',
            message: 'message.synchronized_items',
            translationParams: {
                masterNode: masterNode.name,
            },
        });
    }

    private showPushToMasterErrorNotification(masterNode: Node): void {
        this.notification.show({
            message: 'message.synchronized_items_error',
            type: 'alert',
            translationParams: {
                masterNode: masterNode.name,
            },
            delay: 5000,
        });
    }

    setAsStartpage(page: Page, currentFolderId: number): void {
        this.folderActions.setFolderStartpage(currentFolderId, page)
            .then(() => this.folderActions.getFolder(currentFolderId));
    }

    /**
     * @returns TRUE if item has been deleted and is in wastebin
     */
    private isDeleted(item: Item): boolean {
        if (item) {
            return EntityStateUtil.stateDeleted(item);
        }
    }

    private removeLanguagesFromFormData(formData: CmsFormData, languages: string[]): CmsFormData {
        formData.elements.forEach((element: CmsFormElement) => {
            this.removeLanguagesFromFormElement(element, languages);
        });
        if (formData.mailsubject_i18n) {
            for (const language of languages) {
                delete formData.mailsubject_i18n[language];
            }
        }
        if (formData.mailtemp_i18n) {
            for (const language of languages) {
                delete formData.mailtemp_i18n[language];
            }
        }
        return formData;
    }

    private removeLanguagesFromFormElement(element: CmsFormElement, languages: string[]): void {
        this.removeLanguagesFromObject(element, languages);
    }

    private removeLanguagesFromObject(object: object, languages: string[]): void {
        Object.keys(object).forEach((propertyName: string) => {
            const property = object[propertyName];

            if (propertyName.endsWith('_i18n')) {
                if (!property || typeof property !== 'object') {
                    return;
                }

                for (const language of languages) {
                    delete property[language];
                }

                return;
            }

            if (!!property && typeof property === 'object') {
                this.removeLanguagesFromObject(property, languages);
            } else if (!!property && Array.isArray(property)) {
                this.removeLanguagesFromArray(property, languages);
            }
        });
    }

    private removeLanguagesFromArray(array: any[], languages: string[]): void {
        for (const entry of array) {
            if (!!entry && typeof entry === 'object') {
                this.removeLanguagesFromObject(entry, languages);
            } else if (!!entry && Array.isArray(entry)) {
                this.removeLanguagesFromArray(entry, languages);
            }
        }
    }
}

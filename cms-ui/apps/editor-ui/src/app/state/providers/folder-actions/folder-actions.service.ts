import { Injectable } from '@angular/core';
import { fileSchema, folderSchema, imageSchema, pageSchema } from '@editor-ui/app/common/models';
import { getDefaultNode } from '@editor-ui/app/common/utils/get-default-node';
import {
    Api,
    ApiError,
    FileUploader,
    UploadResponse,
} from '@editor-ui/app/core/providers/api';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { NavigationInstruction, NavigationService } from '@editor-ui/app/core/providers/navigation/navigation.service';
import { PermissionService } from '@editor-ui/app/core/providers/permissions/permission.service';
import { SortedFiles } from '@editor-ui/app/core/providers/upload-conflict/upload-conflict.service';
import {
    QueryAssemblerElasticSearchService,
    QueryAssemblerGCMSSearchService,
} from '@editor-ui/app/shared/providers/query-assembler';
import {
    AnyModelType,
    BaseListResponse,
    ChannelSyncRequest,
    CmsFormElement,
    CmsFormElementI18nValue,
    CmsFormType,
    CommonSortFields,
    CropResizeParameters,
    DependencyItemType,
    EditableFileProps,
    EditableFolderProps,
    EditableFormProps,
    EditableImageProps,
    EditableNodeProps,
    EditablePageProps,
    ElasticSearchQuery,
    File,
    FileCopyRequest,
    FileCreateRequest,
    FileListResponse,
    FileResponse,
    FileUploadResponse,
    Folder,
    FolderItemOrNodeSaveOptionsMap,
    FolderItemOrTemplateType,
    FolderItemSaveOptionsMap,
    FolderItemType,
    FolderItemTypeMap,
    FolderItemTypePlural,
    FolderListOptions,
    FolderListResponse,
    FolderRequestOptions,
    Form,
    FormListOptions,
    FormListResponse,
    FormPermissions,
    FormRequestOptions,
    GtxCmsQueryOptions,
    Image,
    ImageRequestOptions,
    ImageResponse,
    InheritableItem,
    InheritanceRequest,
    Item,
    ItemListResponse,
    ItemType,
    ItemTypeMap,
    Language,
    Node,
    NodeFeature,
    NodeFeatures,
    Normalized,
    Page,
    PageListOptions,
    PageListResponse,
    PagePermissions,
    PageRequestOptions,
    PageVersion,
    PagedTemplateListResponse,
    PagingSortOrder,
    QueuedActionRequestClear,
    Raw,
    Response,
    ResponseMessage,
    RotateParameters,
    SearchPagesOptions,
    SortField,
    Tags,
    Template,
    TemplateFolderListRequest,
    TemplateListRequest,
    TemplateRequestOptions,
    TimeManagement,
    TypedItemListResponse,
    folderItemTypePlurals,
} from '@gentics/cms-models';
import { normalize, schema } from 'normalizr';
import {
    Observable,
    forkJoin,
    of,
    throwError,
} from 'rxjs';
import {
    catchError,
    finalize,
    first,
    map,
    mergeMap,
    publishLast,
    refCount,
    switchMap,
    take,
} from 'rxjs/operators';
import {
    DisplayFields,
    GtxChipSearchProperties,
    GtxChipSearchSearchFilterMap,
    ItemsInfo,
    UIMode,
    plural,
} from '../../../common/models';
import {
    AddContentStagingMapAction,
    AddEntitiesAction,
    ChannelSyncReportFetchingErrorAction,
    ChannelSyncReportFetchingSuccessAction,
    CreateItemSuccessAction,
    EditImageSuccessAction,
    InheritanceFetchingSuccessAction,
    ItemFetchingSuccessAction,
    LanguageFetchingSuccessAction,
    ListCreatingErrorAction,
    ListCreatingSuccessAction,
    ListFetchingErrorAction,
    ListFetchingSuccessAction,
    ListSavingErrorAction,
    ListSavingSuccessAction,
    NodeFetchingSuccessAction,
    SetActiveFolderAction,
    SetActiveNodeAction,
    SetDisplayAllLanguagesAction,
    SetDisplayDeletedAction,
    SetDisplayImagesGridViewAction,
    SetDisplayStatusIconsAction,
    SetFilterTermAction,
    SetFolderLanguageAction,
    SetFormLanguageAction,
    SetListDisplayFieldsAction,
    SetListPageAction,
    SetListPerPageAction,
    SetListShowPathAction,
    SetListSortingAction,
    SetRepositoryBrowserDisplayFieldsAction,
    SetSearchFilterValueAction,
    SetSearchFiltersChangingAction,
    SetSearchFiltersValidAction,
    SetSearchFiltersVisibleAction,
    SetSearchTermAction,
    SetUploadStatusAction,
    StartChannelSyncReportFetchingAction,
    StartListCreatingAction,
    StartListFetchingAction,
    StartListSavingAction,
    UpdateEntitiesAction,
} from '../../modules';
import { getNormalizrSchema } from '../../state-utils';
import { ApplicationStateService } from '../application-state/application-state.service';

/** Parameters for the `updateItem()` and `updateItems()` methods. */
export interface PostUpdateBehavior {
    /**
     * Determines if a notification should be shown when the operation completes successfully.
     * Default: true
     */
    showNotification?: boolean;

    /**
     * If true, the saved item will be fetched with `update=true` after saving (the item will be locked if it is a page).
     * If false, the saved item will still be fetched after saving, but not with the `update=true` parameter.
     * Default: false
     */
    fetchForUpdate?: boolean;

    /**
     * If true, the saved item will be fetched with `construct=true` after saving.
     * If false, the saved item will still be fetched after saving, but not with the `construct=true` parameter.
     * Default: false
     */
    fetchForConstruct?: boolean;
}

interface UpdateableItem <T extends ItemType> {
    itemId: number;
    payload: Partial<ItemTypeMap<Raw>[T]>;
    requestOptions?: Partial<FolderItemOrNodeSaveOptionsMap[T]>;
}

interface UpdateableItemObjectProperty <T extends FolderItemType, R extends FolderItemSaveOptionsMap[T]> {
    itemId: number;
    updatedObjProps: Partial<Tags>;
    requestOptions?: Partial<R>;
}

@Injectable()
export class FolderActionsService {

    private uploaders: { [type: string]: FileUploader } = {};

    constructor(
        private appState: ApplicationStateService,
        private notification: I18nNotification,
        private entityResolver: EntityResolver,
        private errorHandler: ErrorHandler,
        private permissions: PermissionService,
        private i18n: I18nService,
        private api: Api,
        private navigationService: NavigationService,
        private queryAssemblerElasticSearch: QueryAssemblerElasticSearchService,
        private queryAssemblerGCMSSearchService: QueryAssemblerGCMSSearchService,
    ) { }

    /**
     * Fetches all available nodes.
     */
    getNodes(): void {
        forkJoin([
            this.appState.dispatch(new StartListFetchingAction('nodes', undefined, true)),
            this.api.folders.getNodes(),
        ]).pipe(
            switchMap(([, res]) => {
                return this.appState.dispatch(new NodeFetchingSuccessAction(res.folders, res.nodes)).pipe(
                    map(() => res),
                );
            }),
        ).subscribe(res => {
            if (res.nodes.length > 0) {
                this.getActiveNodeLanguages()
                    .then(languages => this.setActiveLanguageFromAvailable(languages));
            }
        }, error => {
            this.appState.dispatch(new ListFetchingErrorAction('nodes', error.message));
            this.errorHandler.catch(error);
        });
    }

    /**
     * Get a single node by id.
     */
    async getNode(nodeId: number): Promise<Node<Raw> | void> {
        await this.appState.dispatch(new StartListFetchingAction('nodes', undefined, true)).toPromise();

        try {
            const res = await this.api.folders.getNode(nodeId).toPromise();
            await this.appState.dispatch(new ItemFetchingSuccessAction('nodes', res.node)).toPromise();
            return res.node;
        } catch (error) {
            await this.appState.dispatch(new ListFetchingErrorAction('nodes', error.message)).toPromise();
            this.errorHandler.catch(error);
        }
    }

    /**
     * Set the active node.
     */
    setActiveNode(nodeId: number): void {
        this.appState.dispatch(new SetActiveNodeAction(nodeId));
        this.getActiveNodeLanguages()
            .then(languages => this.setActiveLanguageFromAvailable(languages));
    }

    /**
     * Set the active folder.
     */
    setActiveFolder(folderId: number): void {
        this.appState.dispatch(new SetActiveFolderAction(folderId));
    }

    /**
     * Set the active language and persist that selection to localStorage.
     */
    setActiveLanguage(languageId: number): Promise<void> {
        return this.appState.dispatch(new SetFolderLanguageAction(languageId)).toPromise();
    }

    /**
     * Set the active form language and persist that selection to localStorage.
     */
    setActiveFormLanguage(languageId: number): Promise<void> {
        return this.appState.dispatch(new SetFormLanguageAction(languageId)).toPromise();
    }

    /**
     * Navigates to the first node (or channel, if the user has no permissions)
     * in the list of nodes by alphabetic order.
     */
    navigateToDefaultNode(): void {
        const defaultNode = this.resolveDefaultNode();

        if (defaultNode) {
            this.navigationService.list(defaultNode.id, defaultNode.folderId).navigate();
            return;
        }

        this.navigationService.navigateToNoNodes();
    }

    resolveDefaultNode(): Node {
        const nodes = this.appState.now.folder.nodes.list
            .map(nodeId => this.entityResolver.getNode(nodeId))
            .filter(node => node != null);
        const defaultNode = getDefaultNode(nodes);

        return defaultNode;
    }

    /**
     * Navigates to a URL fit to represent the current folder and editor state.
     */
    navigateListAndEditorFromState(): void {
        const { folder, editor } = this.appState.now;
        const instruction: NavigationInstruction = {
            list: {
                folderId: folder.activeFolder,
                nodeId: folder.activeNode,
            },
        };

        if (editor.editorIsOpen) {
            instruction.detail = {
                editMode: editor.editMode,
                itemId: editor.itemId,
                itemType: editor.itemType,
                nodeId: editor.nodeId,
                options: {
                    compareWithId: editor.compareWithId,
                    oldVersion: editor.oldVersion,
                    openTab: editor.openTab,
                    version: editor.version,
                },
            };
        }

        this.navigationService.instruction(instruction).navigate();
    }

    /**
     * Toggle whether all available languages should be displayed by default
     * in the page item list.
     */
    setDisplayAllPageLanguages(displayAll: boolean): void {
        this.appState.dispatch(new SetDisplayAllLanguagesAction(displayAll));
    }

    /**
     * Toggle additional mini status icons indicating page translation states.
     */
    setDisplayStatusIcons(displayStatusIcons: boolean): void {
        this.appState.dispatch(new SetDisplayStatusIconsAction(displayStatusIcons));
    }

    /**
     * Toggle deleted objects.
     */
    setDisplayDeleted(displayDeleted: boolean): void {
        this.appState.dispatch(new SetDisplayDeletedAction(displayDeleted));
    }

    setDisplayImagesGridView(displayGrid: boolean): void {
        this.appState.dispatch(new SetDisplayImagesGridViewAction(displayGrid));
    }

    setCurrentPage(type: FolderItemType, currentPage: number): void {
        this.appState.dispatch(new SetListPageAction(type, currentPage));
    }

    setItemsPerPage(type: FolderItemType, itemsPerPage: number): void {
        this.appState.dispatch(new SetListPerPageAction(type, itemsPerPage));
    }

    /**
     * Set the active language to be the one from the User Settings if this
     * is one of the available languages. If not, default to the first language
     * in the list.
     */
    private async setActiveLanguageFromAvailable(languages: Language[]): Promise<void> {
        const activeLanguage = this.appState.now.folder.activeLanguage;
        let activeLanguageAvail: number;

        if (activeLanguage && languages.map(l => l.id).includes(activeLanguage)) {
            activeLanguageAvail = activeLanguage;
        } else {
            if (Array.isArray(languages) && languages.length > 0) {
                activeLanguageAvail = languages[0].id;
            } else {
                // hardcoded fallback
                activeLanguageAvail = 1;
            }
        }

        await this.setActiveLanguage(activeLanguageAvail);
        await this.setActiveFormLanguage(activeLanguageAvail);
    }

    /**
     * Set the filter term.
     * If the current folder has more items (but only the first 10 were loaded), load the other items.
     */
    async setFilterTerm(term: string): Promise<void> {
        await this.appState.dispatch(new SetFilterTermAction(term)).toPromise();

        if (!term) {
            return;
        }

        const folderState = this.appState.now.folder;
        const loaders: Promise<any>[] = [];
        for (const itemType of folderItemTypePlurals) {
            const items: ItemsInfo = folderState[itemType];
            if (!items || !items.hasMore || items.fetchAll) {
                continue;
            }

            switch (itemType) {
                case 'folders':
                    loaders.push(this.getFolders(folderState.activeFolder, true, folderState.searchTerm));
                    break;

                case 'pages':
                    loaders.push(this.getPages(folderState.activeFolder, true, folderState.searchTerm));
                    break;

                case 'files':
                    loaders.push(this.getFiles(folderState.activeFolder, true, folderState.searchTerm));
                    break;

                case 'images':
                    loaders.push(this.getImages(folderState.activeFolder, true, folderState.searchTerm));
                    break;

                case 'forms':
                    loaders.push(this.getForms(folderState.activeFolder, true, folderState.searchTerm));
                    break;
            }
        }

        await Promise.all(loaders);
    }

    /**
     * Set the filter term
     */
    setSearchTerm(term: string): void {
        this.appState.dispatch(new SetSearchTermAction(term));
    }

    /**
     * Set if search filters (advanced search options) are in the process of being altered
     */
    setSearchFiltersChanging(changing: boolean): void {
        this.appState.dispatch(new SetSearchFiltersChangingAction(changing));
    }

    /**
     * Set the validity of the search filters (advanced search options)
     */
    setSearchFiltersValid(valid: boolean): void {
        this.appState.dispatch(new SetSearchFiltersValidAction(valid));
    }

    /**
     * Set the visibility of the search filters (advanced search options)
     */
    setSearchFiltersVisible(visible: boolean): void {
        this.appState.dispatch(new SetSearchFiltersVisibleAction(visible));
    }

    /**
     * Reset advanced search options to default
     */
    resetSearchFilters(): void {
        const folderState = this.appState.now.folder;
        Object.keys(folderState.searchFilters).forEach((key: keyof GtxChipSearchSearchFilterMap) => {
            this.setSearchFilter(key, null);
        });
    }

    /**
     * Set a search filter key to a given value. A value of null removes the filter.
     */
    setSearchFilter<K extends keyof GtxChipSearchProperties>(key: K, value: GtxChipSearchProperties[K] | null): void {
        this.appState.dispatch(new SetSearchFilterValueAction<K>(key, value));
    }

    /**
     * Search by a page's liveUrl.
     */
    searchLiveUrl(liveUrl: string): Observable<Page<Raw> | undefined> {
        this.appState.dispatch(new StartListFetchingAction('page', undefined, true));

        const stripped = liveUrl.replace(/^https?:\/\//, '');
        const options: SearchPagesOptions = {
            liveUrl: stripped,
            update: false,
            template: true,
            langvars: true,
        };
        return this.api.folders.searchPages(undefined, options)
            .pipe(
                map(res => {
                    this.appState.dispatch(new ItemFetchingSuccessAction('page', res.page));
                    return res.page;
                }),
                catchError((error: any) => {
                    this.appState.dispatch(new ListFetchingErrorAction('page', error.message));
                    this.errorHandler.catch(error, { notification: false });
                    return of(undefined).pipe(take(1));
                }),
            );
    }

    breadcrumbFetchSuccess(
        breadcrumbs: Array<Folder<Raw> | Node<Raw>>,
        folderId: number,
        nodeId: number,
    ): Observable<void> {
        return this.appState.dispatch(new ListFetchingSuccessAction('breadcrumbs', {
            hasMore: false,
            folderId,
            nodeId,
            items: breadcrumbs,
            schema: folderSchema,
            total: breadcrumbs.length,
        }));
    }

    /**
     * Get all items of a certain type (folders, pages, files, images, or templates) in a given folder.
     */
    async getItemsOfTypeInFolder(itemType: ItemType, parentId: number, search: string = '', fetchAll = true, pageNumber = 1): Promise<void> {
        switch (itemType) {
            case 'folder':
                await this.getFolders(parentId, fetchAll, search, pageNumber);
                break;

            case 'page':
                await this.getPages(parentId, fetchAll, search, pageNumber);
                break;

            case 'file':
                await this.getFiles(parentId, fetchAll, search, pageNumber);
                break;

            case 'image':
                await this.getImages(parentId, fetchAll, search, pageNumber);
                break;

            case 'template':
                await this.getTemplates(parentId, fetchAll, search, pageNumber);
                break;

            case 'form':
                await this.getForms(parentId, fetchAll, search, pageNumber);
                break;

            default:
                // TODO: error logging solution
                console.log(`itemType ${itemType} not valid.`);
        }
    }

    /**
     * Get all items (folders, pages, files, images) in a given folder.
     */
    async getAllItemsInFolder(parentId: number, search: string = '', fetchAll: boolean = false): Promise<void> {
        await Promise.all([
            this.getFolders(parentId, fetchAll, search),
            this.getPages(parentId, fetchAll, search),
            this.getFiles(parentId, fetchAll, search),
            this.getImages(parentId, fetchAll, search),
            this.getForms(parentId, fetchAll, search),
        ]);
    }

    /**
     * Get all items as well as templates and breadcrumbs for a given folder.
     */
    async getAllFolderContents(parentId: number, search: string = '', fetchAll: boolean = false, searchInItemsOnly: boolean = false): Promise<void> {
        await Promise.all([
            this.getAllItemsInFolder(parentId, search, fetchAll),
            this.getTemplates(parentId, true, (searchInItemsOnly ? '' : search)),
            this.getBreadcrumbs(parentId),
        ]);

        // permissions are not available for the current folder
        // when the page is loaded from within a folder
        const parentFolder = this.entityResolver.getFolder(parentId);
        if (!parentFolder || !(parentFolder.privilegeMap || parentFolder.privilegeBits)) {
            await this.getFolder(parentId);
        }
    }

    /**
     * Refresh the list of items, maintaining all the current settings.
     */
    async refreshList(type: FolderItemType, itemLanguages?: string[]): Promise<void> {
        const folderState = this.appState.now.folder;
        const languages = this.appState.now.entities.language;
        const itemInfo: ItemsInfo = folderState[`${type}s` as FolderItemTypePlural];
        const fetchAll = itemInfo.fetchAll;
        const maxItems = fetchAll ? -1 : 10;
        const search = folderState.searchTerm;
        const recursive = search !== '';
        const parentId = folderState.activeFolder;
        const options: PageListOptions = {
            maxItems,
            search,
            recursive,
            langvars: true,
            template: true,
            sortby: itemInfo.sortBy,
            sortorder: itemInfo.sortOrder,
            skipCount: this.getSkipCount(itemInfo.currentPage, maxItems),
        };

        if (type === 'page' && itemLanguages?.length > 0) {
            for (const language of itemLanguages) {
                options.language = language;
                await this.getItems(parentId, type, fetchAll, options);
            }
        } else {
            if (folderState.activeLanguage && languages[folderState.activeLanguage]) {
                options.language = languages[folderState.activeLanguage].code;
            }
            await this.getItems(parentId, type, fetchAll, options);
        }
    }

    /**
     * Fetches the root folders of each available node.
     */
    getFolders(parentId: number, fetchAll: boolean = false, search: string = '', pageNumber = 1): Promise<void> {
        const itemInfo: ItemsInfo = this.appState.now.folder['folders' as FolderItemTypePlural];
        const maxItems = fetchAll ? -1 : itemInfo.itemsPerPage;
        const recursive = search !== '';

        return this.getItems(parentId, 'folder', fetchAll, { maxItems, search, recursive, skipCount: this.getSkipCount(pageNumber, maxItems) });
    }

    getFolder(folderId: number, options: any = { }): Promise<Folder> {
        return this.getItem(folderId, 'folder', <FolderRequestOptions> { privilegeMap: true, ...options });
    }

    /**
     * Get pages in this folder
     */
    getPages(parentId: number, fetchAll: boolean = false, search: string = '', pageNumber = 1): Promise<void> {
        const itemInfo: ItemsInfo = this.appState.now.folder['pages' as FolderItemTypePlural];
        const maxItems = fetchAll ? -1 : itemInfo.itemsPerPage;
        const recursive = search !== '';
        const language = this.entityResolver.getLanguage(this.appState.now.folder.activeLanguage);
        const options: PageListOptions = {
            maxItems,
            search,
            recursive,
            langvars: true,
            template: true,
            sortby: 'filename',
            skipCount: this.getSkipCount(pageNumber, maxItems),
        };
        if (language) {
            options.language = language.code;
        }
        if (search) {
            options.folder = true;
        }

        return this.getItems(parentId, 'page', fetchAll, options);
    }

    /**
     * Get an individual page.
     */
    getPage(pageId: number, options?: PageRequestOptions): Promise<Page<Raw>> {
        return this.getItem(pageId, 'page', options);
    }

    /**
     * Get files in this folder
     */
    getFiles(parentId: number, fetchAll: boolean = false, search: string = '', pageNumber = 1): Promise<void> {
        const itemInfo: ItemsInfo = this.appState.now.folder['files' as FolderItemTypePlural];
        const maxItems = fetchAll ? -1 : itemInfo.itemsPerPage;
        const recursive = search !== '';

        const options: FolderListOptions = {
            maxItems,
            search,
            recursive,
            skipCount: this.getSkipCount(pageNumber, maxItems),
        };
        if (search) {
            options.folder = true;
        }

        return this.getItems(parentId, 'file', fetchAll, options);
    }

    /**
     * Get an individual file.
     */
    getFile(fileId: number): Promise<File<Raw>> {
        return this.getItem(fileId, 'file');
    }

    /**
     * Get images in this folder
     */
    getImages(parentId: number, fetchAll: boolean = false, search: string = '', pageNumber = 1): Promise<void> {
        const itemInfo: ItemsInfo = this.appState.now.folder['images' as FolderItemTypePlural];
        const maxItems = fetchAll ? -1 : itemInfo.itemsPerPage;
        const recursive = search !== '';

        const options: FolderListOptions = {
            maxItems,
            search,
            recursive,
            skipCount: this.getSkipCount(pageNumber, maxItems),
        };
        if (search) {
            options.folder = true;
        }

        return this.getItems(parentId, 'image', fetchAll, options);
    }

    /**
     * Get an individual image.
     */
    getImage(imageId: number): Promise<Image<Raw>> {
        return this.getItem(imageId, 'image');
    }

    /**
     * Get forms in this folder
     */
    getForms(parentId: number, fetchAll: boolean = false, search: string = '', pageNumber: number = 1): Promise<void> {
        const itemInfo: ItemsInfo = this.appState.now.folder['forms' as FolderItemTypePlural];
        const maxItems = fetchAll ? -1 : itemInfo.itemsPerPage;
        const recursive = search !== '';
        const options: FormListOptions = {
            folderId: parentId,
            pageSize: maxItems,
            q: search,
            recursive,
            sort: {
                sortOrder: PagingSortOrder.Asc,
                attribute: 'name',
            },
            page: pageNumber,
        };

        return this.getItems(parentId, 'form', fetchAll, options);
    }

    /**
     * Get an individual form.
     */
    getForm(formId: number, options?: FormRequestOptions): Promise<Form<Raw>> {
        return this.getItem(formId, 'form', options);
    }

    /**
     * Get the items of a given type which are children of the specified folder
     */
    getItems(parentId: number, type: 'page', fetchAll?: boolean, options?: PageListOptions): Promise<void>;
    getItems(parentId: number, type: FolderItemType, fetchAll?: boolean, options?: FolderListOptions): Promise<void>;
    async getItems(parentId: number, type: FolderItemType, fetchAll?: boolean, options: any = {}): Promise<void> {
        await this.appState.dispatch(new StartListFetchingAction(plural[type], fetchAll)).toPromise();

        // assign query params from state
        const nodeId = options && options.nodeId || this.getCurrentNodeId();
        const itemInfo: ItemsInfo = this.appState.now.folder[`${type}s` as FolderItemTypePlural];
        const searchFiltersVisible = this.appState.now.folder.searchFiltersVisible;
        const searchFiltersValid = this.appState.now.folder.searchFiltersValid;
        const isSearchActive = !!searchFiltersVisible && !!searchFiltersValid;
        const isStagingActive = this.appState.now.ui.mode === UIMode.STAGING;
        const stagingPackage = this.appState.now.contentStaging.activePackage;

        options = Object.assign({}, options, {
            nodeId,
            sortby: itemInfo.sortBy,
            sortorder: itemInfo.sortOrder,
            wastebin: this.appState.now.folder.displayDeleted ? 'include' : 'exclude',
            recursive: isSearchActive,
        });

        // `sort` query param formatting
        if (options.sort) {
            let sortorder: string;
            switch (itemInfo.sortOrder) {
                case 'asc':
                    sortorder = PagingSortOrder.Asc;
                    break;
                case 'desc':
                    sortorder = PagingSortOrder.Desc;
                    break;
                default:
                    sortorder = PagingSortOrder.None;
                    break;
            }
            options.sort.sortOrder = sortorder;
            options.sort.attribute = itemInfo.sortBy;
        }

        // assign entity-specific query params
        if (type === 'folder') {
            options.privilegeMap = true;
        } else if (type === 'page') {
            options.langvars = true;
        }

        if (isStagingActive && stagingPackage) {
            options.package = stagingPackage;
        }

        const elasticSearchMode = this.shouldUseElasticSearch();
        const searchFilters = this.appState.now.folder.searchFilters;

        // get nodeId and folderId query params
        let correctedParentId = parentId;
        const folderState = this.appState.now.folder;
        const hasNodeId = searchFilters.nodeId || null;
        // If `searchFilters` would contain `nodeId`-filter-definitions, then
        // URL query param `id` will get overridden in favour of `nodeId`-filter-definition's `folderId`-property.
        if (Array.isArray(hasNodeId) && hasNodeId.length > 0) {
            if (!searchFilters.nodeId.some(f => f.value === folderState.activeNode )) {
                const nodeIdValue = searchFilters.nodeId[0].value;
                const nodeIdValueParsed = parseInt(nodeIdValue.toString(), 10);
                const node = Number.isInteger(nodeIdValueParsed) && this.entityResolver.getNode(nodeIdValueParsed);
                correctedParentId = node && Number.isInteger(node.folderId) ? node.folderId : parentId;
            }
        }

        // Pseudo-filter of `objecttype` must not be treated as query param but as indicator for
        // UI logic not to request nor display types not queried by user!
        let returnEmptyResponse = false;
        if (searchFilters.objecttype) {
            returnEmptyResponse = searchFilters.objecttype.some(otfilter => {
                const isType: boolean = otfilter.operator === 'IS' && otfilter.value !== type;
                const isNoneButType: boolean = otfilter.operator === 'IS_NOT' && otfilter.value === type;
                return isType || isNoneButType;
            });
        }

        // The REST API provides separate end points for folders and for other items.
        let apiMethod: Observable<FolderListResponse | FormListResponse | PageListResponse | FileListResponse | TypedItemListResponse | ItemListResponse>;
        if (elasticSearchMode) {
            apiMethod = this.elasticSearchItems(type, correctedParentId, searchFilters, options, returnEmptyResponse);
        } else {
            apiMethod = this.basicSearchItems(type, nodeId, correctedParentId, searchFilters, options, returnEmptyResponse);
        }

        try {
            const res = await apiMethod.pipe(
                first(),
                this.postProcessItemListResponse(type, nodeId),
            ).toPromise();

            const collectionKey = type === 'image' ? 'files' : `${type}s`;
            const collection: any[] = (res as any)[collectionKey] || (res as any).items;

            await this.appState.dispatch(new ListFetchingSuccessAction(type, {
                fetchAll,
                folderId: parentId,
                hasMore: res.hasMoreItems,
                items: collection,
                nodeId,
                total: res.numItems,
                schema: getNormalizrSchema(type),
            })).toPromise();
            await this.appState.dispatch(new AddContentStagingMapAction(res.stagingStatus)).toPromise();
        } catch (error) {
            await this.appState.dispatch(new ListFetchingErrorAction(type, error.message)).toPromise();
            this.errorHandler.catch(error);
        }
    }

    /** Custom RxJs operator to modify list response. */
    private postProcessItemListResponse(
        type: ItemType,
        nodeId: number,
    ): <T extends BaseListResponse>(source: Observable<T>) => Observable<T> {
        const normalizeFolderPrivilegeMap = (folders: Folder[]): void => {
            folders.forEach(singleFolder => {
                singleFolder.privilegeMap = this.permissions.normalizeAPIResponse(singleFolder.privilegeMap as any);
            });
        };
        return <T extends BaseListResponse>(source: Observable<T>) => source.pipe(
            map((res: T) => {
                const folders: Folder<Raw>[] = res && type === 'folder' && (res as unknown as FolderListResponse).folders;
                if (Array.isArray(folders)) {
                    // Because latest convention permissions are not included in `folder` entity response, it needs to be fetched per entity.
                    normalizeFolderPrivilegeMap((res as unknown as FolderListResponse).folders);
                }
                return res;
            }),
        );
    }

    /**
     * Returns true is elasticsearch should be used for this getItems() API call.
     */
    private shouldUseElasticSearch(): boolean {
        const state = this.appState.now;
        // Preconditions for ES endpoint to be used is not only `elasticsearch`-feature to be active.
        // Also, only requests explicitally meant to be search queries (in contrast to listing folder contents)
        // shall use ES endpoint, since ES response lacks sorting params, is ordered by relevance.
        return state.features.elasticsearch && state.folder.searchFiltersValid;
    }

    /**
     * Wrapped API method for searching entities via GCMS Search
     */
    private basicSearchItems(
        type: FolderItemType,
        nodeId: number,
        parentId: number,
        filters: GtxChipSearchSearchFilterMap = {},
        options: GtxCmsQueryOptions = {} as any,
        returnEmptyResponse: boolean = false,
    ): Observable<FolderListResponse | FormListResponse | PageListResponse | FileListResponse | TypedItemListResponse | ItemListResponse> {
        const emptyResponse: ItemListResponse = {
            hasMoreItems: false,
            messages: [{
                type: null,
                timestamp: null,
                message: `
                    This is a fake response.
                    Provided query-params are not valid for entity-type "${type}".
                `,
            }],
            responseInfo: null,
            numItems: 0,
            items: [],
        }
        if (returnEmptyResponse) {
            return of(emptyResponse);
        }
        return this.queryAssemblerGCMSSearchService.getOptions(type, parentId, filters, options).pipe(
            mergeMap((requestOptions: FolderListOptions & PageListOptions & FolderListOptions & FormListOptions) => {
                // If return value of query-assembler is `null` it means that filters defined
                // are not valid for given entity-type and thus an empty response shall be returned.
                // Because GCMS REST API would just ignore undefined query-params and still return items in repsonse,
                // frontend must return empty respose here to have the rendered list empty.
                if (!requestOptions) {
                    return of(emptyResponse);
                }

                let apiMethod: Observable<
                FolderListResponse |
                FormListResponse |
                PageListResponse |
                FileListResponse |
                TypedItemListResponse |
                ItemListResponse
                >;

                switch (type) {
                    case 'folder':
                        apiMethod = this.api.folders.getFolders(parentId, requestOptions);
                        break;
                    case 'page':
                        apiMethod = this.api.folders.getPages(parentId, requestOptions);
                        break;
                    case 'file':
                        apiMethod = this.api.folders.getFiles(parentId, requestOptions);
                        break;
                    case 'image':
                        apiMethod = this.api.folders.getImages(parentId, requestOptions);
                        break;
                    // entity `form` is optional -> CMS feature `forms`.
                    case 'form':
                        // if filters define another nodeId than original options, prioritize filters over folder contents settings
                        apiMethod = this.conditionalItemListRequest(options.nodeId || nodeId, this.api.folders.getForms(parentId, requestOptions));
                        break;
                    default:
                        // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                        throw new Error(`Entity type of "${type}" is not defined.`);
                }
                return apiMethod;
            }),
        );
    }

    /** Check defined conditions before triggering request. */
    private conditionalItemListRequest(
        nodeId: number,
        getRequest: Observable<FormListResponse>,
    ): Observable<ItemListResponse> {
        return this.nodeFeatureIsActive(nodeId, NodeFeature.FORMS).pipe(
            switchMap((isActive) => {
                if (isActive) {
                    return getRequest;
                }

                const emptyResponse: ItemListResponse = {
                    hasMoreItems: null,
                    messages: [{
                        type: null,
                        timestamp: null,
                        message: `
                            This is a fake response.
                            Forms are not active on this node.
                            Response will always be empty.
                        `,
                    }],
                    responseInfo: null,
                    numItems: 0,
                    items: [],
                }

                return of(emptyResponse);
            }),
        );
    }

    /**
     * Wrapped API method for searching entities via Elastic Search
     */
    private elasticSearchItems(
        type: FolderItemType,
        parentId: number,
        filters: GtxChipSearchSearchFilterMap = {},
        options: GtxCmsQueryOptions = {} as any,
        returnEmptyResponse: boolean = false,
    ): Observable<ItemListResponse> {
        const emptyResponse: ItemListResponse = {
            items: [],
            numItems: 0,
            hasMoreItems: false,
            messages: [],
            responseInfo: {
                responseCode: 'OK',
            },
        };

        if (returnEmptyResponse) {
            return of(emptyResponse);
        }

        return this.queryAssemblerElasticSearch.getQuery(type, parentId, filters, options).pipe(
            switchMap((queryData: [ElasticSearchQuery, GtxCmsQueryOptions] | null) => {
                // if query won't exist due to type property sanitization, return empty response instead of request
                if (queryData === null) {
                    return of(emptyResponse);
                }

                const [query, assembledOptions] = queryData;
                return this.api.folders.searchItems(type, parentId, query, assembledOptions);
            }),
        );
    }

    /**
     * Fetch a single item. Returns a promise which resolves to the fetched item.
     *
     * @param throwError If true, the any error is not passed to the error handler, but is rethrown
     * This was part of an emergency fix for SUP-8010. Remove this again after we have a proper solution.
     */
    getItem(itemId: number, type: 'folder', options?: FolderRequestOptions, throwError?: boolean): Promise<Folder<Raw>>;
    getItem(itemId: number, type: 'page', options?: PageRequestOptions, throwError?: boolean): Promise<Page<Raw>>;
    getItem(itemId: number, type: 'image', options?: ImageRequestOptions, throwError?: boolean): Promise<Image<Raw>>;
    getItem(itemId: number, type: 'file', options?: FolderRequestOptions, throwError?: boolean): Promise<File<Raw>>;
    getItem(itemId: number, type: 'form', options?: FormRequestOptions, throwError?: boolean): Promise<Form<Raw>>;
    getItem(itemId: number | string, type: 'template', options?: TemplateRequestOptions, throwError?: boolean): Promise<Template<Raw>>;
    getItem(itemId: number | string, type: FolderItemOrTemplateType, options?: any, throwError?: boolean): Promise<InheritableItem<Raw> | Template<Raw>>;
    async getItem(itemId: number | string, type: FolderItemOrTemplateType, options?: any, throwError?: boolean): Promise<InheritableItem<Raw> | Template<Raw>> {
        this.appState.dispatch(new StartListFetchingAction(type, undefined, true));
        const nodeId = options && options.nodeId || this.getCurrentNodeId();

        options = Object.assign({}, options, { nodeId, construct: true });

        let fetchPromise: Promise<any>;

        if (type === 'page') {
            options.langvars = true;
        }

        if (type === 'template') {
            fetchPromise = this.api.folders.getItem(itemId, type, options).toPromise();
        } else {
            fetchPromise = this.api.folders.getItem(Number(itemId), type, options).toPromise();
        }

        if (type === 'folder') {
            // For folders, fetch the permissions as well and normalize the returned data
            const permPromise = this.api.permissions.getFolderPermissions(Number(itemId), nodeId).toPromise();
            fetchPromise = Promise.all([fetchPromise, permPromise])
                .then((responses: any) => {
                    const [res, perm] = responses;
                    (res.folder as Folder<Raw>).privilegeMap = this.permissions.normalizeAPIResponse(perm.privilegeMap);
                    if (!perm.permissionsMap) {
                        // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                        throw new Error(`Folder with ID ${res.folder.id} has no permissionsMap.`);
                    }
                    (res.folder as Folder<Raw>).permissionsMap = perm.permissionsMap;
                    return res;
                });
        }

        try {
            const res = await fetchPromise;
            const entity = (res.folder || res.page || res.file || res.image || res.node || res.item || res.template) as InheritableItem<Raw>;
            if (entity) {
                // When loading the root folder of a node the CMS sets the type to 'node', because in the CMS
                // the node and its root folder are the same object. To work around this strange fact, we always force the correct type here
                // (we always load the node and the folder "views" independently).
                (entity as any).type = type;
            }

            await this.appState.dispatch(new ItemFetchingSuccessAction(type, entity as any)).toPromise();
            return entity;
        } catch (error) {
            await this.appState.dispatch(new ListFetchingErrorAction(type, error.message, true)).toPromise();
            if (throwError) {
                throw error;
            }
            this.errorHandler.catch(error);
            return null;
        }
    }

    /**
     * Get existing items of a specified type from the provided ids in a node.
     */
    getExistingItems(ids: number[], nodeId: number, type: ItemType, options?: any): Observable<Item<Raw>[]> {
        return this.api.folders.getExistingItems(ids, nodeId, type, options);
    }

    /**
     * Get templates of this folder
     */
    async getTemplates(parentId: number, fetchAll: boolean = false, search: string = '', pageNumber = 1): Promise<void> {
        const nodeId = this.getCurrentNodeId();

        const maxItems = fetchAll ? -1 : 10;
        const options: TemplateFolderListRequest = {
            maxItems,
            search,
            nodeId,
            recursive: search !== '',
            skipCount: this.getSkipCount(pageNumber, maxItems),
        };
        await this.appState.dispatch(new StartListFetchingAction('templates', fetchAll)).toPromise();

        try {
            const res = await this.api.folders.getTemplates(parentId, options).toPromise();
            await this.appState.dispatch(new ListFetchingSuccessAction('templates', {
                fetchAll,
                folderId: parentId,
                hasMore: res.hasMoreItems,
                items: res.templates,
                nodeId,
                total: res.numItems,
                schema: getNormalizrSchema('templates'),
            })).toPromise();
        } catch (error) {
            await this.appState.dispatch(new ListFetchingErrorAction('templates', error.message)).toPromise();
            throw error;
        }
    }

    /**
     * Get templates of this folder and return API response data
     */
    getTemplatesRaw(nodeId: number, folderId: number, fetchAll: boolean = false, search: string = ''): Observable<Template<Raw>[] | void> {
        const options: TemplateFolderListRequest = {
            maxItems: fetchAll ? -1 : 10,
            search,
            nodeId,
            recursive: search !== '',
        };

        return forkJoin([
            this.appState.dispatch(new StartListFetchingAction('template', fetchAll)),
            this.api.folders.getTemplates(folderId, options),
        ]).pipe(
            map(([, res]) => res),
            switchMap(res => {
                return this.appState.dispatch(new ListFetchingSuccessAction('templates', {
                    fetchAll,
                    folderId,
                    hasMore: res.hasMoreItems,
                    items: res.templates,
                    nodeId,
                    total: res.numItems,
                    schema: getNormalizrSchema('templates'),
                })).pipe(
                    map(() => res.templates),
                );
            }),
            catchError(error => of(this.errorHandler.catch(error))),
        );
    }

    /**
     * Get templates of this node
     */
    getAllTemplatesOfNode(nodeId: number, search: string = '', sort: CommonSortFields = 'name', fetchAll: boolean = true): Observable<Template<Raw>[] | void> {
        const options: TemplateListRequest = { pageSize: fetchAll ? -1 : 10, sort, ...( search && { q: search }) };
        return this.api.folders.getAllTemplatesOfNode(nodeId, options).pipe(
            map((response: PagedTemplateListResponse) => response.items),
            catchError(error => of(this.errorHandler.catch(error))),
        );
    }

    /**
     * Fetches the breadcrumbs for a given folder
     */
    getBreadcrumbs(parentId: number): void {
        const nodeId = this.getCurrentNodeId();

        forkJoin([
            this.appState.dispatch(new StartListFetchingAction('breadcrumbs', false)),
            this.api.folders.getBreadcrumbs(parentId, { nodeId }),
        ]).pipe(
            switchMap(([, res]) => this.breadcrumbFetchSuccess(res.folders, parentId, nodeId)),
            catchError(error => {
                const tmp = this.appState.dispatch(new ListFetchingErrorAction('breadcrumbs', error.message, true));
                this.errorHandler.catch(error);
                return tmp;
            }),
        ).subscribe();
    }

    /**
     * Create a new folder
     */
    async createNewFolder(
        folder: {
            name: string,
            directory: string,
            description: string,
            parentFolderId: number,
            nodeId: number,
            failOnDuplicate?: boolean,
        },
    ): Promise<Folder<Raw> | void> {
        await this.appState.dispatch(new StartListCreatingAction('folder')).toPromise();

        const newFolder = {
            name: folder.name,
            publishDir: folder.directory,
            description: folder.description,
            nodeId: folder.nodeId,
            motherId: folder.parentFolderId,
            failOnDuplicate: folder.failOnDuplicate,
        };

        try {
            const res = await this.api.folders.createFolder(newFolder).toPromise()
            await this.appState.dispatch(new CreateItemSuccessAction('folder', [res.folder], false)).toPromise();
            return res.folder;
        } catch (error) {
            await this.appState.dispatch(new ListCreatingErrorAction('folder', error.message)).toPromise();
            this.errorHandler.catch(error, { notification: true });
        }
    }

    /**
     * Apply cropping and / or resizing to an image.
     */
    async cropAndResizeImage(sourceImage: Image, resizeParams: CropResizeParameters): Promise<Image<Raw> | void> {
        await this.appState.dispatch(new StartListSavingAction('image')).toPromise();
        const copying = resizeParams.copyFile;

        try {
            const res = await this.api.folders.cropAndResizeImage(resizeParams).toPromise();
            const newImage = this.fileToImage(res.file, resizeParams.width, resizeParams.height, sourceImage, resizeParams);
            this.notification.show({
                message: copying ? 'message.image_copied' : 'message.image_updated',
                type: 'success',
            });
            await this.appState.dispatch(new EditImageSuccessAction(newImage)).toPromise();
            return newImage;
        } catch (error) {
            this.notification.show({
                message: copying ?
                    'message.image_copy_error' :
                    'message.image_update_error',
                translationParams: {
                    error: error.message,
                },
                type: 'alert',
                delay: 10000,
            });
            await this.appState.dispatch(new ListSavingErrorAction('image', error.message)).toPromise();
            this.errorHandler.catch(error, { notification: false });
        }
    }

    /**
     * Rotate an image.
     */
    async rotateImage(rotateParams: RotateParameters): Promise<void> {
        await this.appState.dispatch(new StartListSavingAction('image')).toPromise();
        try {
            const res = await this.api.folders.rotateImage(rotateParams).toPromise();
            await this.appState.dispatch(new EditImageSuccessAction(res.image)).toPromise();
            this.notification.show({
                message: 'message.image_updated',
                type: 'success',
            });
        } catch (error) {
            await this.appState.dispatch(new ListSavingErrorAction('image', error.message)).toPromise();
            this.errorHandler.catch(error, { notification: false });
            this.notification.show({
                message: 'message.image_update_error',
                translationParams: {
                    error: error.message,
                },
                type: 'alert',
                delay: 10000,
            });
        }
    }

    /**
     * Create a new form in the currently active folder
     */
    async createNewForm(form: {
        nodeId: number;
        folderId: number;
        name: string;
        description?: string;
        successPageId?: number;
        successNodeId?: number;
        data: {
            email?: string,
            successurl_i18n?: CmsFormElementI18nValue<string>,
            mailsubject_i18n?: CmsFormElementI18nValue<string>,
            mailtemp_i18n?: CmsFormElementI18nValue<string>,
            mailsource_pageid?: number;
            mailsource_nodeid?: number;
            templateContext: string,
            type: CmsFormType,
            elements: CmsFormElement[],
        }
        languages: string[];
    }): Promise<Form<Raw> | void> {
        await this.appState.dispatch(new StartListCreatingAction('form')).toPromise();

        try {
            const res = await this.api.folders.createForm(form).toPromise();
            await this.appState.dispatch(new CreateItemSuccessAction('form', [res.item], true)).toPromise();
            return res.item;
        } catch (error) {
            await this.appState.dispatch(new ListCreatingErrorAction('form', error.message)).toPromise();
            this.errorHandler.catch(error, { notification: true });
        }
    }

    /**
     * Create a new page in the currently active folder
     */
    async createNewPage(page: {
        pageName: string;
        fileName: string;
        description: string;
        language: string;
        priority: number;
        templateId: number;
        folderId: number;
        nodeId: number;
        niceUrl: string;
        alternateUrls?: string[];
    }): Promise<Page<Raw> | void> {
        await this.appState.dispatch(new StartListCreatingAction('page')).toPromise();

        try {
            const res = await this.api.folders.createPage(page).toPromise();
            await this.appState.dispatch(new CreateItemSuccessAction('page', [res.page], true)).toPromise();
            return res.page;
        } catch (error) {
            await this.appState.dispatch(new ListCreatingErrorAction('page', error.message)).toPromise();
            this.errorHandler.catch(error, { notification: true });
        }
    }

    /**
     * Create multiple page variations.
     */
    createPageVariations(sourcePages: Page[], sourceNodeId: number, targetFolders: Folder[]): Promise<void> {
        const allPromises: Promise<void | Page>[] = [];
        sourcePages.forEach((sourcePage) => {
            const promises = targetFolders.map(folder => {
                const config = {
                    targetFolderId: folder.id,
                    targetNodeId: folder.inheritedFromId,
                    sourcePageId: sourcePage.id,
                    sourceNodeId: sourceNodeId,
                    // if a variation is being created in the source page's folder, we want to
                    // update the UI by adding the new page to the list.
                    addToList: sourcePage.folderId === folder.id,
                };
                return this.createPageVariation(config);
            });

            allPromises.push(...promises);
        });

        return Promise.all(allPromises)
            .then((pages: any[]) => {
                this.notification.show({
                    type: 'success',
                    message: 'message.page_variations_created',
                    translationParams: {
                        count: pages.length,
                        _type: 'page',
                    },
                });
            });
    }


    /**
     * Create page variation.
     */
    createSinglePageVariation(sourcePage: Page, sourceNodeId: number, targetFolder: Folder): Promise<void> {
        const config = {
            targetFolderId: targetFolder.id,
            targetNodeId: targetFolder.inheritedFromId,
            sourcePageId: sourcePage.id,
            sourceNodeId: sourceNodeId,
            // if a variation is being created in the source page's folder, we want to
            // update the UI by adding the new page to the list.
            addToList: sourcePage.folderId === targetFolder.id,
        };

        return Promise.resolve(this.createPageVariation(config))
            .then(() => {
                this.notification.show({
                    type: 'success',
                    message: 'message.page_variations_created',
                    translationParams: {
                        count: 1,
                        _type: 'page',
                    },
                });
            });
    }

    /**
     * Create a new page variation based on a source page.
     */
    private async createPageVariation(config: {
        sourcePageId: number;
        sourceNodeId: number;
        targetNodeId: number;
        targetFolderId: number;
        addToList: boolean;
    }): Promise<Page<Raw> | void> {
        await this.appState.dispatch(new StartListCreatingAction('page')).toPromise();

        const newPage = {
            folderId: config.targetFolderId,
            nodeId: config.targetNodeId,
            variantId: config.sourcePageId,
            variantChannelId: config.sourceNodeId,
        };

        try {
            const res = await this.api.folders.createPageVariation(newPage).toPromise();
            await this.appState.dispatch(new CreateItemSuccessAction('page', [res.page], config.addToList)).toPromise();
            return res.page;
        } catch (error) {
            await this.appState.dispatch(new ListCreatingErrorAction('page', error.message)).toPromise();
            this.errorHandler.catch(error, { notification: true });
        }
    }

    /**
     * Create a new language variant of the given page.
     */
    async createPageTranslation(nodeId: number, pageId: number, languageCode: string): Promise<Page<Raw> | void> {
        await this.appState.dispatch(new StartListCreatingAction('page')).toPromise();

        try {
            const res = await this.api.folders.translatePage(nodeId, pageId, languageCode).toPromise();
            await this.appState.dispatch(new ListCreatingSuccessAction('page')).toPromise();

            const newPage = res.page;
            const oldPageId = pageId;
            const entityState = this.appState.now.entities;
            const normalized = normalize(newPage, pageSchema);
            // Add the ID of the new page to the languageVariants of the original page and add
            // the language variants to the new page (the API does not return the language variants).
            const languageVariants = {
                ...entityState.page[oldPageId].languageVariants,
                [newPage.contentGroupId]: newPage.id,
            };
            await this.appState.dispatch(new AddEntitiesAction(normalized)).toPromise();
            await this.appState.dispatch(new UpdateEntitiesAction({
                page: {
                    [oldPageId]: { languageVariants },
                    [newPage.id]: { languageVariants },
                },
            })).toPromise();

            return res.page;
        } catch (error) {
            await this.appState.dispatch(new ListCreatingErrorAction('page', error.message)).toPromise();
            this.errorHandler.catch(error, { notification: true });
        }
    }

    /**
     * Update the editable properties of a folder.
     */
    updateFolderProperties(folderId: number, properties: EditableFolderProps, postUpdateBehavior?: PostUpdateBehavior): Promise<Folder<Raw> | void> {
        const folderProps = {
            name: properties.name,
            description: properties.description,
            publishDir: properties.directory,
            nameI18n: properties.nameI18n,
            descriptionI18n: properties.descriptionI18n,
            publishDirI18n: properties.publishDirI18n,
        };
        return this.updateItem('folder', folderId, folderProps, {}, postUpdateBehavior);
    }

    /**
     * Update the editable properties of a form.
     */
    updateFormProperties(formId: number, properties: EditableFormProps, postUpdateBehavior?: PostUpdateBehavior): Promise<Form<Raw> | void> {
        const formProps = {
            name: properties.name,
            description: properties.description,
            successPageId: properties.successPageId,
            successNodeId: properties.successNodeId,
            data: {
                email: properties.email,
                successurl_i18n: properties.successurl_i18n, /* old successurl value is removed upon saving form !properties! */
                mailsubject_i18n: properties.mailsubject_i18n,
                mailtemp_i18n: properties.mailtemp_i18n,
                mailsource_pageid: properties.mailsource_pageid,
                mailsource_nodeid: properties.mailsource_nodeid,
                templateContext: properties.templateContext,
                type: properties.type,
                elements: properties.elements,
            },
        };
        return this.updateItem(
            'form',
            formId,
            formProps,
            {},
            postUpdateBehavior,
        );
    }

    /**
     * Update the editable properties of a page.
     */
    updatePageProperties(pageId: number, properties: EditablePageProps, postUpdateBehavior?: PostUpdateBehavior): Promise<Page<Raw> | void> {
        const pageProps: Partial<Page<Raw>> = {
            name: properties.pageName,
            fileName: properties.fileName,
            description: properties.description,
            niceUrl: properties.niceUrl,
            alternateUrls: properties.alternateUrls,
            templateId: properties.templateId,
            customCdate: properties.customCdate,
            customEdate: properties.customEdate,
            priority: properties.priority,
        };
        if (properties.language) {
            pageProps.language = properties.language;
        }
        return this.updateItem('page', pageId, pageProps, {}, postUpdateBehavior);
    }

    /**
     * Update the time management property of a page.
     */
    savePageTimeManagement(pageId: number, timeManagement: TimeManagement): Promise<Page<Raw> | void> {
        const page = this.entityResolver.getPage(pageId);
        const props = { timeManagement, unlock: page && !page.locked };
        return this.updateItem('page', pageId, props, {},  { showNotification: true, fetchForUpdate: false });
    }

    /**
     * Update language for page.
     */
    updatePageLanguage(pageId: number, language: Language): Promise<Page | void> {
        const pageProps: any = {
            id: pageId,
            language: language.code,
        };
        const requestOptions: any = {
            createVersion: true,
            unlock: true,
            deriveFileName: true,
        };
        return this.updateItem('page', pageId, pageProps, requestOptions);
    }

    /**
     * Update language for form.
     */
    updateFormLanguage(form: Form<Normalized>, language: Language): Promise<Form | void> {
        const formProps: Partial<Form> = {
            id: form.id,
            languages: [ ...form.languages, language.code ],
        };
        const requestOptions: any = {
            createVersion: true,
            unlock: true,
        };
        return this.updateItem('form', form.id, formProps, requestOptions);
    }

    /**
     * Update the editable properties of a file.
     */
    updateFileProperties(fileId: number, properties: EditableFileProps, postUpdateBehavior?: PostUpdateBehavior): Promise<File<Raw> | void> {
        const fileProps = {
            name: properties.name,
            description: properties.description,
            forceOnline: properties.forceOnline,
            niceUrl: properties.niceUrl,
            alternateUrls: properties.alternateUrls,
        };
        return this.updateItem('file', fileId, fileProps, {}, postUpdateBehavior);
    }

    /**
     * Update the editable properties of an image.
     */
    updateImageProperties(imageId: number, properties: EditableImageProps, postUpdateBehavior?: PostUpdateBehavior): Promise<Image<Raw> | void> {
        const imageProps = {
            name: properties.name,
            description: properties.description,
            forceOnline: properties.forceOnline,
            fpX: properties.fpX,
            fpY: properties.fpY,
            niceUrl: properties.niceUrl,
            alternateUrls: properties.alternateUrls,
        };
        return this.updateItem('image', imageId, imageProps, {}, postUpdateBehavior);
    }

    updateNodeProperties(nodeId: number, properties: EditableNodeProps): Promise<Node<Raw> | void> {
        const nodeProps = {
            binaryPublishDir: properties.fileSystemBinaryDir,
            defaultFileFolderId: properties.defaultFileFolderId,
            defaultImageFolderId: properties.defaultImageFolderId,
            disablePublish: properties.disablePublish,
            host: properties.host,
            https: properties.https,
            name: properties.nodeName,
            publishContentMap: properties.contentRepository,
            publishContentMapFiles: properties.contentRepositoryFiles,
            publishContentMapFolders: properties.contentRepositoryFolders,
            publishContentMapPages: properties.contentRepositoryPages,
            publishDir: properties.fileSystemPageDir,
            publishFs: properties.fileSystem,
            publishFsFiles: properties.fileSystemFiles,
            publishFsPages: properties.fileSystemPages,
            urlRenderWayFiles: properties.urlRenderingFiles,
            urlRenderWayPages: properties.urlRenderingPages,
            utf8: properties.utf8,
        };
        return this.updateItem('node', nodeId, nodeProps)
            .then(node => {
                if (!node || !node.folderId) {
                    throw new Error(`No update response data of Node with ID ${nodeId} returned by REST API.`);
                }
                // Updating the node name or description actually changes the base folder properties,
                // so the base folder should be newly-fetched to prevent the UI state going out-of-sync
                return this.getFolder(node.folderId)
                    .then(() => node);
            });
    }

    private updatedItemChanges<T extends ItemType>(
        type: T,
        updatedItem: Promise<ItemTypeMap<AnyModelType>[T]>,
        changes: Partial<ItemTypeMap<AnyModelType>[T]>,
    ): Promise<ItemTypeMap<AnyModelType>[T]> {
        return updatedItem.then(async updatedItem => {
            let propertiesDeleted = false;
            for (const property in changes) {
                // eslint-disable-next-line no-prototype-builtins
                if (updatedItem && changes.hasOwnProperty(property) && !updatedItem.hasOwnProperty(property)) {
                    updatedItem[property] = null;
                    propertiesDeleted = true;
                }
            }
            if (propertiesDeleted) {
                await this.appState.dispatch(new ItemFetchingSuccessAction(type as any, updatedItem)).toPromise();
            }
            return updatedItem;
        });
    }

    /**
     * Updates the object properties of the specified item.
     *
     * @param type The type of the item.
     * @param itemId The ID of the item.
     * @param updatedObjProps The object properties that should be updated.
     * @param postUpdateBehavior Determines what actions should be taken after the update completes successfully (fetchForUpdate must be set).
     * @param requestOptions Additional request options.
     */
    updateItemObjectProperties<T extends FolderItemType, U extends FolderItemTypeMap<Raw>[T], R extends FolderItemSaveOptionsMap[T]>(
        type: T,
        itemId: number,
        updatedObjProps: Partial<Tags>,
        postUpdateBehavior: PostUpdateBehavior & Required<Pick<PostUpdateBehavior, 'fetchForUpdate'>>,
        requestOptions?: Partial<R>,
    ): Promise<U> {
        return this.updateItemsObjectProperties(type, [{ itemId, updatedObjProps, requestOptions }], postUpdateBehavior)
            .then<any>(values => values ? values[0] : undefined);
    }

    /**
     * Updates the object properties of the specified items.
     *
     * @param type The type of the items.
     * @param items The information that should be updated.
     * @param postUpdateBehavior Determines what actions should be taken after the update completes successfully (fetchForUpdate must be set).
     */
    updateItemsObjectProperties<T extends FolderItemType, U extends ItemTypeMap<Raw>[T], R extends FolderItemSaveOptionsMap[T]>(
        type: T,
        items: UpdateableItemObjectProperty<T, R>[],
        postUpdateBehavior: PostUpdateBehavior & Required<Pick<PostUpdateBehavior, 'fetchForUpdate'>>,
    ): Promise<U[]> {
        const updateItems: UpdateableItem<T>[] = [];
        items.forEach(item => {
            const update = { tags: item.updatedObjProps } as unknown as Partial<U>;
            updateItems.push({
                itemId: item.itemId,
                payload: update,
                requestOptions: item.requestOptions,
            });
        });
        return this.updateItems(type, updateItems, postUpdateBehavior) as unknown as Promise<U[]>;
    }

    /**
     * Basic item update operation without loading state and error handling.
     *
     * @see updateItem
     * @see updateItems
     */
    private internalUpdateItem<T extends ItemType>(
        type: T,
        itemId: number,
        payload: Partial<ItemTypeMap<Raw>[T]>,
        requestOptions?: Partial<FolderItemOrNodeSaveOptionsMap[T]>,
        postUpdateBehavior?: PostUpdateBehavior,
    ): Promise<ItemTypeMap<Raw>[T]> {
        // sanitize data
        if (postUpdateBehavior && typeof postUpdateBehavior.showNotification !== 'boolean') {
            postUpdateBehavior.showNotification = true;
        }

        // perform basic update operation
        return this.api.folders.updateItem(type, itemId, payload, requestOptions).toPromise().then<any>(() => {
            if (type === 'node') {
                return this.getNode(itemId);
            }

            const options = { };
            if (postUpdateBehavior) {
                if (postUpdateBehavior.fetchForUpdate) {
                    options['update'] = true;
                }
                if (postUpdateBehavior.fetchForConstruct) {
                    options['construct'] = true;
                }
            }
            const updatedItem: Promise<any> = this.getItem(itemId, type as FolderItemType, options);
            return this.updatedItemChanges(type, updatedItem, payload);
        });
    }

    /**
     * Wraps `_updateItem` and manages loading state and notifications and error handling.
     *
     * @see internalUpdateItem
     */
    async updateItem<T extends ItemType>(
        type: T,
        itemId: number,
        payload: Partial<ItemTypeMap<Raw>[T]>,
        requestOptions?: Partial<FolderItemOrNodeSaveOptionsMap[T]>,
        postUpdateBehavior?: PostUpdateBehavior,
    ): Promise<ItemTypeMap<Raw>[T] | void> {
        await this.appState.dispatch(new StartListSavingAction(type as any)).toPromise();

        return this.internalUpdateItem(type, itemId, payload, requestOptions, postUpdateBehavior)
            .then(async values => {
                await this.appState.dispatch(new ListSavingSuccessAction(type as any)).toPromise();

                if (postUpdateBehavior && postUpdateBehavior.showNotification) {
                    this.notification.show({
                        type: 'success',
                        message: 'message.updated_item',
                        translationParams: { _type: type },
                    });
                }

                return values;
            })
            .catch(error => {
                this.appState.dispatch(new ListSavingErrorAction(type as any, error.message));
                this.errorHandler.catch(error, { notification: true });
            });
    }

    /**
     * Wraps `_updateItem` for multiple items and manages loading state and notifications and error handling.
     *
     * @see internalUpdateItem
     */
    async updateItems<T extends ItemType>(
        type: T,
        items: UpdateableItem<T>[],
        postUpdateBehavior: PostUpdateBehavior = { showNotification: true, fetchForUpdate: false, fetchForConstruct: false },
    ): Promise<ItemTypeMap<Raw>[T][]> {
        if (typeof postUpdateBehavior.showNotification !== 'boolean') {
            postUpdateBehavior.showNotification = true;
        }

        await this.appState.dispatch(new StartListSavingAction(type as any)).toPromise();

        const itemPromises: Promise<ItemTypeMap<Raw>[T]>[] = items.map((item) => {
            return this.internalUpdateItem(
                type,
                item.itemId,
                item.payload,
                item.requestOptions,
                postUpdateBehavior,
            );
        })

        // aggregate all request responses
        try {
            const values = await Promise.all(itemPromises);
            if (postUpdateBehavior && postUpdateBehavior.showNotification) {
                this.notification.show({
                    type: 'success',
                    message: 'message.updated_item',
                    // eslint-disable-next-line @typescript-eslint/naming-convention
                    translationParams: { _type: type },
                });
            }

            await this.appState.dispatch(new ListSavingSuccessAction(type as any)).toPromise();

            return values;
        } catch (error) {
            await this.appState.dispatch(new ListSavingErrorAction(type as any, error.message)).toPromise();
            this.errorHandler.catch(error, { notification: true });
            return null;
        }
    }

    localizeFolder(folderId: number, channelId: number): Promise<Folder<Raw>> {
        return this.localizeItem('folder', folderId, channelId);
    }

    localizePage(pageId: number, channelId: number): Promise<Page<Raw>> {
        return this.localizeItem('page', pageId, channelId);
    }

    async localizeItems(type: FolderItemType, items: InheritableItem[], channelId: number): Promise<void> {
        const inheritedItemsToLocalize = items.filter(item => item.inherited);
        const otherItems = items.filter(item => !item.inherited);

        const notifyUserThatInheritedItemsWillNotBePublished = () => {
            if (otherItems.length > 0) {
                this.notification.show({
                    message: 'message.items_not_inherited_or_localized',
                    translationParams: {
                        _type: type,
                        count: otherItems.length,
                    },
                    type: 'default',
                });
            }
        };

        if (!inheritedItemsToLocalize.length) {
            notifyUserThatInheritedItemsWillNotBePublished();
            return Promise.resolve();
        }

        await this.appState.dispatch(new StartListSavingAction(type)).toPromise();

        const localizeObservables = inheritedItemsToLocalize.map(item =>
            this.api.folders.localizeItem(type, item.id, channelId));

        try {
            const results = await forkJoin(localizeObservables).toPromise();
            notifyUserThatInheritedItemsWillNotBePublished();
            this.notification.show({
                type: 'success',
                message: results.length > 1 ?  'message.items_localized_plural' : 'message.items_localized_singular',
                translationParams: { count: results.length, _type: type },
            });
            await this.appState.dispatch(new ListSavingSuccessAction(type)).toPromise();
        } catch (error) {
            notifyUserThatInheritedItemsWillNotBePublished();
            await this.appState.dispatch(new ListSavingErrorAction(type, error.message)).toPromise();
            this.errorHandler.catch(error);
        }
    }

    localizeFile(fileId: number, channelId: number): Promise<File<Raw>> {
        return this.localizeItem('file', fileId, channelId);
    }

    localizeImage(imageId: number, channelId: number): Promise<Image<Raw>> {
        return this.localizeItem('image', imageId, channelId);
    }

    /**
     * Creates a localized version of an inherited item in a channel, and returns the newly-created
     * item (which is useful for getting the new id).
     */
    localizeItem(type: 'folder', itemId: number, channelId: number): Promise<Folder<Raw>>;
    localizeItem(type: 'page', itemId: number, channelId: number): Promise<Page<Raw>>;
    localizeItem(type: 'file', itemId: number, channelId: number): Promise<File<Raw>>;
    localizeItem(type: 'image', itemId: number, channelId: number): Promise<Image<Raw>>;
    localizeItem(type: FolderItemType, itemId: number, channelId: number): Promise<InheritableItem<Raw>>;
    async localizeItem(type: FolderItemType, itemId: number, channelId: number): Promise<InheritableItem<Raw> | void> {
        await this.appState.dispatch(new StartListSavingAction(type)).toPromise();

        try {
            await this.api.folders.localizeItem(type, itemId, channelId).toPromise()
            this.notification.show({
                type: 'success',
                message: 'message.localized_item',
                translationParams: {
                    _type: type,
                },
            });
            await this.appState.dispatch(new ListSavingSuccessAction(type)).toPromise();
            return this.getItem(itemId, type) as Promise<InheritableItem<Raw>>;
        } catch (error) {
            await this.appState.dispatch(new ListSavingErrorAction(type, error.message)).toPromise();
            this.errorHandler.catch(error);
        }
    }

    /**
     * Deletes a localized version of an inherited item in a channel and re-inherits it from the parent channel.
     */
    unlocalizeItem(type: FolderItemType, itemId: number, channelId: number): Promise<any> {
        return this.unlocalizeItems(type, [itemId], channelId);
    }

    /**
     * Deletes multiple localized versions of inherited items in a channel and re-inherits them from the parent channel.
     */
    async unlocalizeItems(type: FolderItemType, itemIds: number[], channelId: number): Promise<any> {
        await this.appState.dispatch(new StartListSavingAction(type)).toPromise();

        const apiCall = itemIds.length === 1
            ? this.api.folders.unlocalizeItem(type, itemIds[0], channelId)
            : this.api.folders.unlocalizeItems(type, itemIds, channelId);

        try {
            const res = await apiCall.pipe(
                map(res => Object.assign(res, { itemIds })),
            ).toPromise();
            await this.appState.dispatch(new ListSavingSuccessAction(type)).toPromise();

            // if the item that is currently being edited is deleted, close the editor
            const { itemType, itemId } = this.appState.now.editor;
            if (itemType === type && res.itemIds.indexOf(itemId) >= 0) {
                this.navigationService.instruction({ detail: null }).navigate();
            }
        } catch (error) {
            await this.appState.dispatch(new ListSavingErrorAction(type, error.message)).toPromise();
            this.errorHandler.catch(error, { notification: true });
        }
    }

    /**
     * Get the inheritance information for the given item.
     */
    async fetchItemInheritance(type: FolderItemType, itemId: number, nodeId: number): Promise<InheritableItem<Normalized>> {
        await this.appState.dispatch(new StartListFetchingAction(type)).toPromise();

        try {
            const res = await this.api.folders.getInheritance(type, itemId, nodeId).toPromise();
            await this.appState.dispatch(new InheritanceFetchingSuccessAction(type, itemId, {
                disinherit: res.disinherit,
                disinheritDefault: res.disinheritDefault,
                excluded: res.exclude,
                inheritable: res.inheritable,
            })).toPromise();
            // Return the merged version of the entity from the store.
            return this.entityResolver.getEntity(type, itemId);
        } catch (error) {
            await this.appState.dispatch(new ListFetchingErrorAction(type, error.message, true)).toPromise();
            throw error;
        }
    }

    /**
     * Update the inheritance settings of an item.
     */
    async updateItemInheritance(type: FolderItemType, itemId: number, settings: InheritanceRequest): Promise<void> {
        await this.appState.dispatch(new StartListSavingAction(type)).toPromise();

        try {
            await this.api.folders.setInheritance(type, itemId, settings).toPromise();

            this.notification.show({
                type: 'success',
                message: 'message.updated_item_inheritance',
                translationParams: { _type: type },
            });

            await this.appState.dispatch(new ListSavingSuccessAction(type)).toPromise();
            await this.appState.dispatch(new UpdateEntitiesAction({
                [type]: {
                    [itemId]: {
                        disinherit: settings.disinherit,
                        disinheritDefault: settings.disinheritDefault,
                        excluded: settings.exclude,
                    },
                },
            })).toPromise();
        } catch (error) {
            await this.appState.dispatch(new ListSavingErrorAction(type, error.message)).toPromise();
            this.errorHandler.catch(error, { notification: true });
        }
    }

    /**
     * Gets a report of all the objects (folders, pages, files, images, templates) which will be
     * affected by a "push to master" operation on a folder from a channel.
     */
    async getChannelSyncReport(folderId: number, channelId: number, recursive: boolean): Promise<void> {
        await this.appState.dispatch(new StartChannelSyncReportFetchingAction()).toPromise();

        const options: FolderListOptions = {
            recursive,
            nodeId: channelId,
            inherited: false,
        };

        const addEntities: <T>(res: T, type: string, items: any[]) => Observable<T> = (res, type, items) => {
            const normalized = normalize(items, new schema.Array(getNormalizrSchema(type)));
            return this.appState.dispatch(new AddEntitiesAction(normalized)).pipe(
                map(() => res),
            );
        }

        const templateRequest = this.api.folders.getTemplates(folderId, options).pipe(
            switchMap(res => addEntities(res, 'template', res.templates)),
        );
        const folderRequest = this.api.folders.getFolders(folderId, options).pipe(
            switchMap(res => addEntities(res, 'folder', res.folders)),
        );
        const itemRequests: Observable<ItemListResponse>[] = (['page', 'file', 'image'] as ItemType[])
            .map(type => this.api.folders.getItems(folderId, type, options).pipe(
                switchMap(res => addEntities(res, type, res.items)),
            ));

        try {
            const responses = await forkJoin([itemRequests[0], itemRequests[1], itemRequests[2], folderRequest, templateRequest]).toPromise();
            await this.appState.dispatch(new ChannelSyncReportFetchingSuccessAction({
                pages: responses[0].items.map(page => page.id),
                files: responses[1].items.map(file => file.id),
                images: responses[2].items.map(image => image.id),
                folders: responses[3].folders.map(folder => folder.id),
                templates: responses[4].templates.map(template => template.id),
                // TODO: Fetch infos for forms?
                // forms: [],
            })).toPromise();
        } catch (error) {
            this.notification.show({
                message: error?.response?.responseInfo?.responseMessage,
                type: 'alert',
                delay: 10000,
            });
            await this.appState.dispatch(new ChannelSyncReportFetchingErrorAction(error.message)).toPromise();
        }
    }

    /**
     * Synchronize a localized channel item with the master node.
     */
    pushToMaster(type: FolderItemType, itemId: number, settings: ChannelSyncRequest): Promise<any> {
        this.appState.dispatch(new StartListSavingAction(type));

        return this.api.folders.pushToMaster(type, itemId, settings).toPromise()
            .then(res => {
                this.notification.show({
                    type: 'success',
                    message: 'message.synchronized_item',
                    translationParams: { _type: type },
                });
                this.appState.dispatch(new ListSavingSuccessAction(type));
            })
            .catch(error => {
                this.appState.dispatch(new ListSavingErrorAction(type, error.message));
                this.errorHandler.catch(error, { notification: true });
            });
    }

    /**
     * Synchronize a localized channel item with the master node.
     */
    pushItemsToMaster(type: FolderItemType, settings: ChannelSyncRequest): Observable<any> {
        return this.api.folders.pushItemsToMaster(type, settings);
    }

    /**
     * Set a page as the startpage for a folder.
     */
    setFolderStartpage(folder: number | Folder, page: number | Page): Promise<any> {
        const folderId = typeof folder === 'number' ? folder : (folder ).id;
        const pageId = typeof page === 'number' ? page : (page ).id;
        const pageName = this.entityResolver.getPage(pageId).name;

        return this.api.folders.setFolderStartpage(folderId, pageId).toPromise()
            .then(() => {
                this.notification.show({
                    type: 'success',
                    message: 'message.set_as_start_page',
                    translationParams: { name: pageName },
                });
            })
            .catch(error => this.errorHandler.catch(error, { notification: true }));
    }

    /**
     * Restore a page at a specific version.
     */
    async restorePageVersion(pageId: number, version: PageVersion, showNotification: boolean = true): Promise<boolean> {
        try {
            const res = await this.api.folders.restorePageVersion(pageId, version.timestamp).toPromise();
            const normalized = normalize(res.page, pageSchema);
            const normalizedPage = normalized.entities.page[normalized.result] as Page<Normalized>;

            await this.appState.dispatch(new UpdateEntitiesAction({
                page: {
                    [res.page.id]: {
                        currentVersion: normalizedPage.currentVersion,
                        locked: normalizedPage.locked,
                        lockedSince: normalizedPage.lockedSince,
                        lockedBy: normalizedPage.lockedBy,
                    },
                },
            })).toPromise();

            if (showNotification) {
                this.notification.show({
                    type: 'success',
                    message: `Version ${version.number} was restored.`,
                });
            }

            return true;
        } catch (error) {
            this.errorHandler.catch(error, { notification: true });
            return false;
        }
    }

    /**
     * Copy pages to a folder in the same or a different node.
     */
    async copyPagesToFolder(ids: number[], sourceNodeId: number, targetFolderId: number, targetNodeId: number): Promise<boolean> {
        if (!ids.length) {
            return false;
        }

        await this.appState.dispatch(new StartListSavingAction('page')).toPromise();

        try {
            await this.api.folders.copyPages(ids, sourceNodeId, targetFolderId, targetNodeId).toPromise();
            await this.appState.dispatch(new ListSavingSuccessAction('page')).toPromise();
            const translationParams: any = {
                count: ids.length,
            };
            let message = 'message.pages_copied';
            if (ids.length === 1) {
                const copiedPage: Page = this.appState.now.entities.page[ids[0]];
                translationParams.name = copiedPage.name;
                message = 'message.page_copied';
            }
            this.notification.show({
                type: 'success',
                message,
                translationParams,
            });

            return true;
        } catch (error) {
            await this.appState.dispatch(new ListSavingErrorAction('page', error.message)).toPromise();
            this.errorHandler.catch(error, { notification: true });
            return false;
        }
    }

    /**
     * Copy forms to a folder in the same or a different node.
     */
    copyFormsToFolder(ids: number[], sourceNodeId: number, targetFolderId: number, targetNodeId: number): Promise<boolean> {
        if (!ids.length) { return; }

        this.appState.dispatch(new StartListSavingAction('form'));

        return this.api.folders.copyForms(ids, targetFolderId, targetNodeId).toPromise()
            .then(res => {
                this.appState.dispatch(new ListSavingSuccessAction('form'));
                const translationParams: any = {
                    count: ids.length,
                };
                let message = 'message.forms_copied';
                if (ids.length === 1) {
                    const copiedForm: Form = this.appState.now.entities.form[ids[0]];
                    translationParams.name = copiedForm.name;
                    message = 'message.form_copied';
                }
                this.notification.show({
                    type: 'success',
                    message,
                    translationParams,
                });

                return true;
            })
            .catch(error => {
                this.appState.dispatch(new ListSavingErrorAction('form', error.message));
                this.errorHandler.catch(error, { notification: true });
                return false;
            });
    }

    /**
     * Copy a file or image to the target folder. Internally this creates a copy in the source folder and then
     * moves the new object to the target folder.
     */
    copyFilesToFolder(sourceFiles: File<Raw>[], sourceNodeId: number, targetFolderId: number, targetNodeId: number): Promise<boolean> {
        if (!sourceFiles.length) {
            return Promise.resolve(false);
        }

        this.appState.dispatch(new StartListSavingAction(sourceFiles[0].type));
        const copyText = this.i18n.translate('common.copy');

        const filePromises: Promise<Response>[] = sourceFiles
            .map(sourceFile => {
                const newFilename = sourceFile.name.replace(/(\.[\w\d_-]+)$/i, `_${copyText}$1`);
                const payload: FileCopyRequest = {
                    newFilename,
                    file: {
                        id: sourceFile.id,
                    },
                    nodeId: sourceNodeId,
                    targetFolder: {
                        id: targetFolderId,
                        channelId: targetNodeId,
                    },
                };
                return this.api.folders.copyFile(payload).toPromise()
                    .then(res => {
                        this.appState.dispatch(new ListSavingSuccessAction(sourceFile.type));

                        if (sourceFile.folderId !== targetFolderId || sourceNodeId !== targetNodeId ||
                                (sourceFile.inherited && sourceFile.inheritedFromId !== targetNodeId)) {
                            // a different target folder was selected than that of the source file, so we need to perform
                            // a move operation on the new file.
                            const newFile = res.file;
                            return this.moveFileToFolder(newFile, targetFolderId, targetNodeId);
                        } else {
                            return <any> res;
                        }
                    });
            });

        return Promise.all(filePromises)
            .then(responses => {
                this.notification.show({
                    type: 'success',
                    message: 'message.copied_files',
                    translationParams: {
                        count: sourceFiles.length,
                        _type: sourceFiles[0].type,
                    },
                });
                return true;
            })
            .catch(error => {
                this.appState.dispatch(new ListSavingErrorAction(sourceFiles[0].type, error.message));
                this.errorHandler.catch(error, { notification: true });
                return false;
            });
    }

    private moveFileToFolder(file: File, targetFolderId: number, targetNodeId: number): Promise<Response> {
        const type = file.type;
        this.appState.dispatch(new StartListSavingAction(type));

        return this.api.folders.moveItems(type, [file.id], targetFolderId, targetNodeId).toPromise()
            .then(res => {
                this.appState.dispatch(new ListSavingSuccessAction(type));
                return res;
            })
            .catch(error => {
                this.appState.dispatch(new ListSavingErrorAction(type, error.message));
                this.errorHandler.catch(error, { notification: true });
            });
    }

    /**
     * Move items from one folder to another. Can change inheritance chains.
     */
    moveItemsToFolder(type: FolderItemType, ids: number[], targetFolderId: number, targetNodeId: number): Promise<boolean> {
        if (!ids.length) { return Promise.resolve(false); }

        this.appState.dispatch(new StartListSavingAction(type));

        return this.api.folders.moveItems(type, ids, targetFolderId, targetNodeId).toPromise()
            .then(() => {
                this.appState.dispatch(new ListSavingSuccessAction(type));

                if (ids.length === 1) {
                    const movedItem: Item = this.entityResolver.getEntity(type, ids[0]);
                    this.notification.show({
                        type: 'success',
                        message: 'message.item_moved',
                        translationParams: { name: movedItem.name },
                    });
                } else {
                    this.notification.show({
                        type: 'success',
                        message: 'message.items_moved',
                        translationParams: {
                            _type: type,
                            count: ids.length,
                        },
                    });
                }

                return true;
            })
            .catch(error => {
                this.appState.dispatch(new ListSavingErrorAction(type, error.message));
                this.errorHandler.catch(error, { notification: true });

                return false;
            });
    }

    /**
     * Set the displayFields for a given type of item.
     */
    setDisplayFields(type: FolderItemType, displayFields: string[]): void {
        this.appState.dispatch(new SetListDisplayFieldsAction(type, displayFields || []));
    }

    /**
     * Set the displayFields for a given type of item for Repository Browser.
     */
    setRepositoryBrowserDisplayFields(type: FolderItemType, displayFields: DisplayFields): void {
        this.appState.dispatch(new SetRepositoryBrowserDisplayFieldsAction(type, displayFields));
    }

    /**
     * Set the showPath for a given type of item.
     */
    setShowPath(type: DependencyItemType, showPath: boolean): void {
        this.appState.dispatch(new SetListShowPathAction(type, showPath));
    }

    /**
     * Set the sortBy and sortOrder for a given type of item.
     */
    setSorting(type: FolderItemType, sortBy: SortField, sortOrder: 'asc' | 'desc'): void {
        this.appState.dispatch(new SetListSortingAction(type, sortBy, sortOrder));
    }

    /**
     * Uploads a file to the backend. If uploads are already in progress,
     * the new files are added to the current upload state.
     */
    uploadFiles(type: 'file' | 'image', files: any[], folderId: number): Observable<UploadResponse[]> {
        if (type !== 'image' && type !== 'file') {
            // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
            console.error(`Can not upload ${type}.`);
            return;
        }

        let uploader = this.uploaders[type];
        const nodeId = this.getCurrentNodeId();

        this.appState.dispatch(new StartListCreatingAction(type));
        this.appState.dispatch(new SetUploadStatusAction(true));

        if (uploader) {
            files.forEach(file => uploader.upload(file, { nodeId, folderId }));
            return uploader.done$;
        }

        uploader = this.api.folders.upload( type, files, { nodeId, folderId });
        this.uploaders[type] = uploader;
        uploader.done$.subscribe({
            next: (uploads): void => {
                const cancelled: UploadResponse[] = [];
                const completed: UploadResponse[] = [];
                const failed: UploadResponse[] = [];

                for (const upload of uploads) {
                    if (upload.cancelled) {
                        cancelled.push(upload);
                    } else if (upload.error || !upload.response || !upload.response.success) {
                        failed.push(upload);
                    } else {
                        completed.push(upload);
                    }
                }

                if (failed.length > 0) {
                    // If the server provides an error message, show it to the user.
                    const fileErrors = failed.map(file => {
                        const fileError = (file.response && file.response.messages
                            && file.response.messages[0] && file.response.messages[0].message
                            || (file.error && file.error.message) || '')
                            .replace(/\.$/, '');
                        return file.name + (fileError ? ' - ' + fileError : '');
                    });

                    this.notification.show({
                        message: 'message.file_uploads_error',
                        translationParams: {
                            files: fileErrors.map(msg => '\n    ' + msg).join(''),
                            count: failed.length,
                            _type: type,
                        },
                        type: 'alert',
                        delay: 0,
                        dismissOnClick: true,
                    });

                    const errorMessage = 'Uploads failed:\n' + fileErrors.join('\n');
                    this.appState.dispatch(new ListCreatingErrorAction(type, errorMessage));
                }

                if (cancelled.length > 0) {
                    this.notification.show({
                        message: cancelled.length === 1 ?
                            'message.file_upload_cancelled' :
                            'message.file_uploads_cancelled',
                        translationParams: { count: cancelled.length },
                        type: 'default',
                        delay: 4000,
                        dismissOnClick: true,
                    });
                }

                if (completed.length > 0) {
                    const schema = type === 'image' ? imageSchema : fileSchema;
                    const key = schema.key;

                    // Because the dimensions of an Image can only be accessed async, we need to use promises for
                    // all entities. If the dimensions are assumed to be available, a race condition can result.
                    const entityPromises: Promise<File<Raw> | Image<Raw>>[] = completed.map(file => {
                        let entityPromise: Promise<File<Raw> | Image<Raw>>;
                        if (type === 'image') {
                            // uploaded images are returned with type "file", so we need to manually set the
                            // type before putting it in the entity store.
                            entityPromise = uploader.getFileByUid(file.uid).dimensions
                                .then(dimensions => {
                                    return this.fileToImage(file.response.file, dimensions.width, dimensions.height);
                                });
                        } else {
                            entityPromise = Promise.resolve(file.response.file);
                        }
                        return entityPromise;
                    });

                    Promise.all(entityPromises).then(entities => {
                        return this.appState.dispatch(new CreateItemSuccessAction(type, entities, true)).toPromise();
                    });
                }
            },
            complete: (): void => {
                this.uploaders[type] = undefined;
                this.appState.dispatch(new SetUploadStatusAction(false));
            },
        });

        return uploader.done$;
    }

    /**
     * Uploads a file to replace an existing file.
     */
    replaceFile(type: 'image' | 'file', fileId: number, file: any, folderId: number, nodeId?: number, fileName?: string): FileUploader {
        if (type !== 'image' && type !== 'file') {
            // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
            console.error(`Can not upload ${type}.`);
            return;
        }

        nodeId = (nodeId != null) ? nodeId : this.getCurrentNodeId();
        this.appState.dispatch(new StartListSavingAction(type));

        const uploader = this.api.folders.replaceFile(type, fileId, file, { folderId, nodeId }, fileName);
        uploader.done$.subscribe(responses => {
            const response = responses[0];

            // If the backend provides error information (e.g. "file is too big"), show that to the user.
            let errorMessage = '';
            if (response && (response.error || !response.response || !response.response.success)) {
                const messages: ResponseMessage[] = response.response && response.response.messages;
                const errorDetail = messages && messages.filter(msg => msg.type !== 'INFO')[0];
                if (errorDetail) {
                    errorMessage = errorDetail.message;
                } else {
                    errorMessage = response.error && response.error.message || '';
                }
            }

            if (response.cancelled) {
                this.notification.show({
                    message: 'message.file_upload_cancelled',
                    type: 'default',
                    delay: 4000,
                    dismissOnClick: true,
                });
            } else if (response.error || !response.response || !response.response.success) {
                this.notification.show({
                    message: errorMessage || 'message.file_upload_error',
                    type: 'alert',
                    delay: 10000,
                    dismissOnClick: true,
                });
            } else {
                // Because the dimensions of an Image can only be accessed async, we need to use promises for
                // all entities. If the dimensions are assumed to be available, a race condition can result.
                let entityPromise: Promise<File<Raw> | Image<Raw>>;

                if (type === 'image') {
                    // Uploaded images are returned with type "file", so we need to manually set the
                    // type before putting it in the entity store.
                    entityPromise = uploader.getFileByUid(response.uid).dimensions
                        .then(dimensions => {
                            return this.fileToImage(response.response.file, dimensions.width, dimensions.height);
                        });
                } else {
                    entityPromise = Promise.resolve(response.response.file);
                }

                entityPromise.then(entity => {
                    const normalized = normalize({ ...entity, id: fileId }, getNormalizrSchema(type));
                    this.appState.dispatch(new AddEntitiesAction(normalized));
                    this.appState.dispatch(new ListSavingSuccessAction(type));
                });

                return;
            }

            this.appState.dispatch(new ListSavingErrorAction(type, errorMessage));
        });

        return uploader;
    }

    /**
     * Takes a SortedFiles object which is created by the UploadConflictService, and translates this into the correct
     * upload & replace calls.
     */
    uploadAndReplace(sortedFiles: SortedFiles, folderId: number, nodeId: number): Observable<UploadResponse[][]> {
        const completedObservables: Observable<UploadResponse[]>[] = [];

        if (sortedFiles.create.images.length > 0) {
            completedObservables.push(this.uploadFiles('image', sortedFiles.create.images, folderId));
        }
        if (sortedFiles.create.files.length > 0) {
            completedObservables.push(this.uploadFiles('file', sortedFiles.create.files, folderId));
        }
        if (sortedFiles.replace.images.length > 0) {
            const replaceImages = sortedFiles.replace.images.map(data =>
                this.replaceFile('image', data.id, data.file, folderId, nodeId).done$);
            completedObservables.push(...replaceImages);
        }
        if (sortedFiles.replace.files.length > 0) {
            const replaceFiles = sortedFiles.replace.files.map(data =>
                this.replaceFile('file', data.id, data.file, folderId, nodeId).done$);
            completedObservables.push(...replaceFiles);
        }

        const observable = forkJoin(completedObservables).pipe(
            take(1),
            publishLast(),
            refCount(),
        );

        observable
            .subscribe((result: UploadResponse[][]) => {
                const successfulUploads = ([].concat(...result) as UploadResponse[])
                    .filter((item: UploadResponse) =>
                        !item.error && !item.cancelled && item.response && item.response.success);

                if (successfulUploads.length) {
                    const onlyImages = successfulUploads.every(upload =>
                        /^image\//.test(upload.response.file.fileType));

                    this.notification.show({
                        message: 'message.file_uploads_success',
                        translationParams: {
                            count: successfulUploads.length,
                            _type: onlyImages ? 'image' : 'file',
                        },
                        type: 'success',
                    });
                }
            });

        return observable;
    }

    /**
     * Trigger the CMS backend to download a file from give `sourceURL`.
     */
    uploadFromSourceUrl(type: 'file' | 'image', payload: FileCreateRequest): Observable<FileUploadResponse> {
        this.appState.dispatch(new StartListCreatingAction(type));
        this.appState.dispatch(new SetUploadStatusAction(true));

        return this.api.folders.uploadFromSourceUrl(payload).pipe(
            switchMap((response: FileUploadResponse) => {
                // Refetch uploaded to since response from `POST file/create` differs from default entity data.
                // Since this method semantically indicates that return value emits when upload is accomplished,
                // `switchMap` is used here to wait for the uploadeded entity to be added to the app state.
                return this.api.folders.getItem(response.file.id, type).toPromise()
                    .then((data: FileResponse | ImageResponse) => {
                        const item: File | Image = type === 'image' ? (data as ImageResponse).image : (data as FileResponse).file;
                        this.appState.dispatch(new CreateItemSuccessAction(type, [item], true));
                        this.appState.dispatch(new SetUploadStatusAction(false));

                        this.notification.show({
                            message: 'message.file_uploads_success',
                            translationParams: {
                                count: 1,
                                _type: type,
                            },
                            type: 'success',
                        });

                        return response;
                    });
            }),
            catchError(error => {
                this.appState.dispatch(new ListCreatingErrorAction(type, error.message));

                this.notification.show({
                    message: 'message.file_uploads_error',
                    translationParams: {
                        files: error.message,
                        count: 1,
                        _type: type,
                    },
                    type: 'alert',
                    delay: 0,
                    dismissOnClick: true,
                });

                return throwError(error);
            }),
            finalize(() => this.appState.dispatch(new SetUploadStatusAction(false))),
        );
    }

    /**
     * Publish a page or pages.
     */
    publishPages(pages: Page[]): Promise<{ queued: Page<Normalized>[], published: Page<Normalized>[] }> {
        const pagesToPublish = pages.filter(page => !page.inherited);
        const inheritedPages = pages.filter(page => page.inherited);

        const showInheritedNotPublishedMessage = () => {
            // Notify the user that inherited pages will not be published
            if (inheritedPages.length > 0) {
                this.notification.show({
                    message: 'message.pages_not_published_inherited',
                    translationParams: {
                        type: 'page',
                        count: inheritedPages.length,
                    },
                    type: 'default',
                    delay: 6000,
                });
            }
        };

        if (pagesToPublish.length === 0) {
            showInheritedNotPublishedMessage();
            return Promise.resolve({ queued: [], published: [] });
        }

        this.appState.dispatch(new StartListSavingAction('page'));

        const pageIds = pagesToPublish.map(page => page.id);
        const nodeId = this.getCurrentNodeId();
        const permissionRequests = pageIds.map(id =>
            this.permissions.forItem(id, 'page', nodeId).pipe(
                first(),
                map(permissions => ({ id, permissions })),
            ),
        );

        // Combine the publishing and permission fetching
        return forkJoin([this.api.folders.publishPages(pageIds, nodeId), ...permissionRequests]).pipe(
            // After publish reqeuest(s) display notifications depending on permissions:
            // those pages a user is not permitted to publish will have been queued as publish requests.
            map(allResults => {

                // remove response message from forkjoined array
                const publishedOrQueuedPages = allResults.slice(1) as Array<{ id: number, permissions: PagePermissions }>;

                // notify state
                this.appState.dispatch(new ListSavingSuccessAction('page'));

                const published: Page<Normalized>[] = [];
                const queued: Page<Normalized>[] = [];
                const type = 'page';
                let message: string;

                // assign to arrays depending on page permissions
                for (const page of publishedOrQueuedPages) {
                    const pageEntity = this.entityResolver.getPage(page.id);
                    if (page.permissions.publish) {
                        published.push(pageEntity);
                    } else {
                        queued.push(pageEntity);
                    }
                }

                // if permitted, display 'published' notifications
                if (published.length) {
                    message = published.length > 1 ? 'message.pages_published_plural' : 'message.pages_published_singular';
                    this.notification.show({
                        type: 'success',
                        message,
                        translationParams: { count: published.length, _type: type },
                    });
                }
                // if NOT permitted, display 'queued' notifications
                if (queued.length) {
                    message = queued.length > 1 ? 'message.pages_published_queued_plural' : 'message.pages_published_queued_singular';
                    this.notification.show({
                        type: 'success',
                        message,
                        translationParams: { count: queued.length, _type: type },
                    });
                }

                // check if page is inherited
                showInheritedNotPublishedMessage();

                return { queued, published };
            }),
            catchError(error => {
                this.appState.dispatch(new ListSavingErrorAction('page', error.message));
                this.errorHandler.catch(error);
                return of({ queued: [], published: [] });
            }),
        ).toPromise();
    }

    /**
     * Take a page offline (unpublish).
     */
    takePagesOffline(pageIds: number[]): Promise<any> {
        this.appState.dispatch(new StartListSavingAction('page'));

        const requests = pageIds.map(id =>
            this.api.folders.takePageOffline(id).pipe(
                map(response => ({ id, response, failed: response.responseInfo.responseCode !== 'OK' })),
                catchError((error: ApiError) => {
                    const errorMsg = error && error.message || `Error on taking page offline with id ${id}.`;
                    this.appState.dispatch(new ListSavingErrorAction('page', errorMsg));
                    this.errorHandler.catch(error, { notification: true });
                    return of(error.response);
                }),
            ),
        );
        const permissionRequests = pageIds.map(id =>
            this.permissions.forItem(id, 'page', id).pipe(
                first(),
                map(permissions => ({ id, permissions })),
            ),
        );

        return forkJoin([...requests, ...permissionRequests]).pipe(
            map(allResponses => {
                // split responses by type
                const rawResults = allResponses.slice(0, pageIds.length) as Array<{ id: number; response: Response; failed: boolean; }>;
                const permissions = allResponses.slice(pageIds.length) as Array<{ id: number; permissions: PagePermissions; }>;
                // merge results
                const results: Array<{
                    id: number;
                    response: Response;
                    failed: boolean;
                    permissions: PagePermissions;
                }> = rawResults.map(rawResult => {
                    return {
                        ...rawResult,
                        permissions: permissions.find(permission => permission.id === rawResult.id).permissions,
                    };
                });

                const succeeded = results.filter(r => !r.failed).map(r => r.id);
                const badResponses = results.filter(r => r.failed);
                const failed = badResponses.map(r => r.id);
                const errorResponse = badResponses.length && badResponses[0].response.responseInfo;

                if (failed.length) {
                    this.appState.dispatch(new ListSavingErrorAction('page', errorResponse.responseMessage));
                    this.notification.show({
                        message: 'message.take_pages_offline_error',
                        translationParams: {
                            count: failed.length,
                            errorMessage: errorResponse.responseMessage,
                        },
                        type: 'alert',
                        delay: 5000,
                    });
                }

                if (succeeded.length) {
                    this.appState.dispatch(new ListSavingSuccessAction('page'));
                    const pageUpdates: { [id: number]: Partial<Page<Normalized>> } = {};
                    for (const id of pageIds) {
                        pageUpdates[id] = {
                            online: false,
                        };
                    }
                    this.appState.dispatch(new UpdateEntitiesAction(pageUpdates));
                    const takenOffline: Page[] = [];
                    const queued: Page[] = [];
                    let message: string;

                    // assign to arrays depending on page permissions
                    for (const page of results) {
                        const pageEntity = this.entityResolver.getPage(page.id);
                        if (page.permissions.publish) {
                            takenOffline.push(pageEntity);
                        } else {
                            queued.push(pageEntity);
                        }
                    }

                    // if permitted, display 'takenOffline' notifications
                    if (takenOffline.length) {
                        message = 'message.take_pages_offline';
                    }
                    // if NOT permitted, display 'queued' notifications
                    if (queued.length) {
                        message = 'message.take_pages_offline_queued';
                    }
                    this.notification.show({
                        message,
                        translationParams: {
                            count: succeeded.length,
                            _type: 'page',
                        },
                        type: 'success',
                    });

                    return { queued, takenOffline };
                }
            }),
            catchError(error => {
                this.appState.dispatch(new ListSavingErrorAction('page', error.message));
                this.errorHandler.catch(error, { notification: true });
                return of({ queued: [], takenOffline: [] });
            }),
        ).toPromise();
    }

    /**
     * Change page properties
     *
     * @param pageId unique page node identifier
     * @param payload update request body
     */
    updatePage(pageId: number, payload: Partial<Page<Raw>>): Promise<Page<Raw> | void> {
        return this.updateItem('page', pageId, payload);
    }

    /**
     * Publish a form at a certain date&time
     *
     * @param formId Id of the form
     * @param timestamp When the form should be published at
     * @param keepVersion If the form has been edited after timemanagement has been set, editing timemanagement
     * will affect prior instead of current form version if TRUE.
     */
    publishFormAt(formId: number, timestamp: number, keepVersion?: boolean): Promise<void> {
        this.appState.dispatch(new StartListSavingAction('form'));
        const nodeId = this.getCurrentNodeId();

        return this.api.folders.publishFormAt(formId, timestamp, keepVersion).pipe(
            switchMap(() => forkJoin([
                this.getFormVersionPlanned(formId),
                this.permissions.forItem(formId, 'form', nodeId)
                    .pipe(
                        first(),
                        map(permissions => permissions.publish),
                    ),
            ],
            )),
            map(([formVersionPlanned, isPermitted]) => {
                this.appState.dispatch(new ListSavingSuccessAction('form'));
                let message: string;
                if (isPermitted) {
                    message = 'message.form_published_at';
                } else {
                    message = 'message.form_published_at_queued';
                }
                this.notification.show({
                    type: 'success',
                    message,
                    translationParams: { version: formVersionPlanned },
                });
            }),
            first(),
            catchError(error => {
                this.appState.dispatch(new ListSavingErrorAction('form', error.message));
                this.errorHandler.catch(error, { notification: true });
                return of(error);
            }),
        ).toPromise();
    }

    /**
     * Publish a page at a certain date&time
     *
     * @param pageId Id of the page
     * @param timestamp When the form should be published at
     * @param keepVersion If the page has been edited after timemanagement has been set, editing timemanagement
     * will affect prior instead of current page version if TRUE.
     */
    publishPageAt(pageId: number, timestamp: number, keepVersion?: boolean): Promise<void> {
        this.appState.dispatch(new StartListSavingAction('page'));
        const nodeId = this.getCurrentNodeId();

        return this.api.folders.publishPageAt(pageId, nodeId, timestamp, keepVersion).pipe(
            switchMap(() => forkJoin([
                this.getPageVersionPlanned(pageId),
                this.permissions.forItem(pageId, 'page', nodeId)
                    .pipe(
                        first(),
                        map(permissions => permissions.publish),
                    ),
            ],
            )),
            map(([pageVersionPlanned, isPermitted]) => {
                this.appState.dispatch(new ListSavingSuccessAction('page'));

                let message: string;
                if (isPermitted) {
                    message = 'message.page_published_at';
                } else {
                    message = 'message.page_published_at_queued';
                }

                this.notification.show({
                    type: 'success',
                    message,
                    translationParams: { version: pageVersionPlanned },
                });
            }),
            first(),
            catchError(error => {
                const errorMsg = error && error.message || `Error on publishing page for date with id ${pageId}.`;
                this.appState.dispatch(new ListSavingErrorAction('page', errorMsg));
                this.errorHandler.catch(error, { notification: true });
                return of(error);
            }),
        ).toPromise();
    }

    /**
     * Publish pages at a certain date&time
     *
     * @param pages The pages to publish
     * @param timestamp Timestamp of publication. If set to 0, publishing will take place immediately.
     * @param keepPublishAt If set to true, pages will keep their existing publication date. In case one does not have one,
     * the provided timestamp will be used.
     * @param keepVersion If the pages have been edited after timemanagement has been set, editing timemanagement
     * will affect prior instead of current respective page version if TRUE.
     */
    publishPagesAt(pages: Page[], timestamp: number, keepPublishAt?: boolean, keepVersion?: boolean): Promise<void> {

        /**
         * This method was implemented in a support ticket. To keep changes at a minimum, it maps this action to the existing
         * publishPageAt method. However, it is meant to mimic the request
         * https://gentics.com/Content.Node/guides/restapi/resource_PageResource.html#resource_PageResource_publish_POST.
         * In future, this action can be changed to make use of this request which will reduce the number of requests needed to 1.
         */

        let promise: Promise<any>;

        if (keepPublishAt) {
            promise = Promise.all(pages.map((page: Page) => this.publishPageAt(
                page.id,
                page.timeManagement.at === 0 ? timestamp : page.timeManagement.at,
                keepVersion,
            )));
        } else {
            promise = Promise.all(pages.map((page: Page) => this.publishPageAt(page.id, timestamp, keepVersion)));
        }

        // we add this .then() to change the type from void[] to void (to keep the API consistent in case we switch to a solution based on a single request)
        return promise.then(() => {
            return;
        });
    }

    /**
     * Take a form offline at a certain date&time
     */
    takeFormOfflineAt(formId: number, timestamp: number): Promise<void> {
        this.appState.dispatch(new StartListSavingAction('form'));
        const nodeId = this.getCurrentNodeId();

        return this.api.folders.takeFormOfflineAt(formId, timestamp).pipe(
            mergeMap(() => this.permissions.forItem(formId, 'form', nodeId)),
            map(permissions => permissions.publish),
            map(isPermitted => {
                this.appState.dispatch(new ListSavingSuccessAction('form'));

                let message: string;
                if (isPermitted) {
                    message = 'message.form_take_offline_at';
                } else {
                    message = 'message.form_take_offline_at_queued';
                }

                this.notification.show({
                    type: 'success',
                    message,
                });
            }),
            first(),
            catchError(error => {
                this.appState.dispatch(new ListSavingErrorAction('form', error.message));
                this.errorHandler.catch(error, { notification: true });
                return of(error);
            }),
        ).toPromise();
    }

    /**
     * Take a page offline at a certain date&time
     */
    takePageOfflineAt(pageId: number, timestamp: number): Promise<void> {
        this.appState.dispatch(new StartListSavingAction('page'));
        const nodeId = this.getCurrentNodeId();

        return this.api.folders.takePageOfflineAt(pageId, nodeId, timestamp).pipe(
            mergeMap(() => this.permissions.forItem(pageId, 'page', nodeId)),
            map(permissions => permissions.publish),
            map(isPermitted => {
                this.appState.dispatch(new ListSavingSuccessAction('page'));

                let message: string;
                if (isPermitted) {
                    message = 'message.page_take_offline_at';
                } else {
                    message = 'message.page_take_offline_at_queued';
                }

                this.notification.show({
                    type: 'success',
                    message,
                });
            }),
            first(),
            catchError(error => {
                this.appState.dispatch(new ListSavingErrorAction('page', error.message));
                this.errorHandler.catch(error, { notification: true });
                return of(error);
            }),
        ).toPromise();
    }

    /**
     * Clearing timemanagement property settings for a form.
     */
    async formTimeManagementClear(formId: number, payload: QueuedActionRequestClear): Promise<void> {
        const formVersionPlanned = await this.getFormVersionPlanned(formId);
        return this.api.folders.formTimeManagementClear(formId, payload)
            .toPromise()
            .then(() => {
                if (payload.clearPublishAt) {
                    this.notification.show({
                        type: 'success',
                        message: 'message.form_published_at_cleared',
                        translationParams: { version: formVersionPlanned },
                    });
                }

                if (payload.clearOfflineAt) {
                    this.notification.show({
                        type: 'success',
                        message: 'message.form_take_offline_at_cleared',
                    });
                }

                this.appState.dispatch(new ListSavingSuccessAction('form'));
            })
            .catch(error => {
                this.appState.dispatch(new ListSavingErrorAction('form', error.message));
                this.errorHandler.catch(error, { notification: true });
            });
    }

    /**
     * Clearing timemanagement property settings for a page.
     */
    async pageTimeManagementClear(pageId: number, payload: QueuedActionRequestClear): Promise<void> {
        const pageVersionPlanned = await this.getPageVersionPlanned(pageId);
        return this.api.folders.pageTimeManagementClear(pageId, payload)
            .toPromise()
            .then(() => {
                if (payload.clearPublishAt) {
                    this.notification.show({
                        type: 'success',
                        message: 'message.page_published_at_cleared',
                        translationParams: { version: pageVersionPlanned },
                    });
                }

                if (payload.clearOfflineAt) {
                    this.notification.show({
                        type: 'success',
                        message: 'message.page_take_offline_at_cleared',
                    });
                }

                this.appState.dispatch(new ListSavingSuccessAction('page'));
            })
            .catch(error => {
                this.appState.dispatch(new ListSavingErrorAction('page', error.message));
                this.errorHandler.catch(error, { notification: true });
            });
    }

    /**
     * Approve page actions queued which had been requested by users with insufficient permissions before.
     */
    async pageQueuedApprove(pages: Page[]): Promise<void> {
        const requests: Promise<any>[] = [];
        const pageLanguages = pages.map(page => page.language);
        pages.forEach((page) => {
            const queuedRequestForPublish = page.timeManagement.queuedPublish;
            const queuedRequestForTakeOffline = page.timeManagement.queuedOffline;
            if (queuedRequestForPublish || queuedRequestForTakeOffline) {
                requests.push(this.api.publishQueue.approvePageStatus([page.id]).toPromise());
            }
        });
        return Promise.all(requests).then(() => {
            if (requests) {
                if (requests.length === 1) {
                    this.notification.show({
                        type: 'success',
                        message: 'message.request_approved',
                    });
                } else {
                    this.notification.show({
                        type: 'success',
                        message: 'message.requests_approved',
                    });
                }
            }
            return this.refreshList('page', pageLanguages);
        })
            .catch(error => {
                this.errorHandler.catch(error, { notification: true });
            });
    }

    /**
     * Publish a form or forms.
     */
    publishForms(forms: Form[]): Promise<{ queued: Form<Normalized>[], published: Form<Normalized>[] }> {
        this.appState.dispatch(new StartListSavingAction('form'));
        const formIds = forms.map(form => form.id);
        const nodeId = this.getCurrentNodeId();
        const permissionRequests = formIds.map(id =>
            this.permissions.forItem(id, 'form', nodeId).pipe(
                first(),
                map(permissions => ({ id, permissions })),
            ),
        );

        // Combine the publishing and permission fetching
        return forkJoin([
            this.api.folders.publishForms(formIds),
            ...permissionRequests,
        ]).pipe(
            // After publish reqeuest(s) display notifications depending on permissions:
            // those forms a user is not permitted to publish will have been queued as publish requests.
            map(allResults => {

                // remove response message from forkjoined array
                const publishedOrQueuedForms = allResults.slice(1) as Array<{ id: number, permissions: FormPermissions }>;

                // notify state
                this.appState.dispatch(new ListSavingSuccessAction('form'));

                const published: Form<Normalized>[] = [];
                const queued: Form<Normalized>[] = [];
                const type = 'form';
                let message: string;

                // assign to arrays depending on form permissions
                for (const form of publishedOrQueuedForms) {
                    const formEntity = this.entityResolver.getForm(form.id);
                    if (form.permissions.publish) {
                        published.push(formEntity);
                    } else {
                        queued.push(formEntity);
                    }
                }

                // if permitted, display 'published' notifications
                if (published.length) {
                    message = published.length > 1 ? 'message.forms_published_plural' : 'message.forms_published_singular';
                    this.notification.show({
                        type: 'success',
                        message,
                        translationParams: { count: published.length, _type: type },
                    });
                }
                // if NOT permitted, display 'queued' notifications
                if (queued.length) {
                    message = queued.length > 1 ? 'message.forms_published_queued_plural' : 'message.forms_published_queued_singular';
                    this.notification.show({
                        type: 'success',
                        message,
                        translationParams: { count: queued.length, _type: type },
                    });
                }

                return { queued, published };
            }),
            catchError(error => {
                this.appState.dispatch(new ListSavingErrorAction('form', error.message));
                this.errorHandler.catch(error, { notification: true });
                return of({ queued: [], published: [] });
            }),
        ).toPromise();
    }

    /**
     * Take a form offline (unpublish).
     */
    takeFormsOffline(formIds: number[]): Promise<any> {
        this.appState.dispatch(new StartListSavingAction('form'));

        const requests = formIds.map(id =>
            this.api.folders.takeFormOffline(id)
                .catch((error: ApiError) => {
                    const errorMsg = error && error.message || `Error on taking form offline with id ${id}.`;
                    this.appState.dispatch(new ListSavingErrorAction('form', errorMsg));
                    this.errorHandler.catch(error);
                    return of(error.response);
                })
                .map(response =>
                    ({ id, response, failed: response.responseInfo.responseCode !== 'OK' }),
                ),
        );
        const permissionRequests = formIds.map(id =>
            this.permissions.forItem(id, 'form', id).pipe(
                first(),
                map(permissions => ({ id, permissions })),
            ),
        );

        return forkJoin([
            ...requests,
            ...permissionRequests,
        ]).pipe(
            map(allResponses => {
                // split responses by type
                const rawResults = allResponses.slice(0, formIds.length) as Array<{ id: number; response: Response; failed: boolean; }>;
                const permissions = allResponses.slice(formIds.length) as Array<{ id: number; permissions: FormPermissions; }>;
                // merge results
                const results: Array<{
                    id: number;
                    response: Response;
                    failed: boolean;
                    permissions: FormPermissions;
                }> = rawResults.map(rawResult => {
                    return {
                        ...rawResult,
                        permissions: permissions.find(permission => permission.id === rawResult.id).permissions,
                    };
                });

                const succeeded = results.filter(r => !r.failed).map(r => r.id);
                const badResponses = results.filter(r => r.failed);
                const failed = badResponses.map(r => r.id);
                const errorResponse = badResponses.length && badResponses[0].response.responseInfo;

                if (failed.length) {
                    this.appState.dispatch(new ListSavingErrorAction('form', errorResponse.responseMessage));
                }

                if (succeeded.length) {
                    this.appState.dispatch(new ListSavingSuccessAction('form'));
                    const formUpdates: { [id: number]: Partial<Form<Normalized>> } = {};
                    for (const id of succeeded) {
                        formUpdates[id] = {
                            online: false,
                        };
                    }
                    this.appState.dispatch(new UpdateEntitiesAction(formUpdates));
                    const takenOffline: Form[] = [];
                    const queued: Form[] = [];
                    let message: string;

                    // assign to arrays depending on form permissions
                    for (const form of results) {
                        const formEntity = this.entityResolver.getForm(form.id);
                        if (form.permissions.publish) {
                            takenOffline.push(formEntity);
                        } else {
                            queued.push(formEntity);
                        }
                    }

                    // if permitted, display 'takenOffline' notifications
                    if (takenOffline.length) {
                        message = 'message.take_forms_offline';
                    }
                    // if NOT permitted, display 'queued' notifications
                    if (queued.length) {
                        message = 'message.take_forms_offline_queued';
                    }
                    this.notification.show({
                        message,
                        translationParams: {
                            count: succeeded.length,
                            _type: 'form',
                        },
                        type: 'success',
                    });

                    return { queued, takenOffline };
                }
            }),
            catchError(error => {
                this.appState.dispatch(new ListSavingErrorAction('form', error.message));
                this.errorHandler.catch(error, { notification: true });
                return of({ queued: [], takenOffline: [] });
            }),
        ).toPromise();
    }

    /**
     * Get the languages available for the specified node folder
     */
    private getActiveNodeLanguages(): Promise<Language[]> {
        this.appState.dispatch(new StartListFetchingAction('activeNodeLanguages', undefined, true));
        const nodeId = this.getCurrentNodeId();
        let nodePromise: Promise<number>;

        if (nodeId) {
            nodePromise = Promise.resolve(nodeId);
        } else {
            nodePromise = this.appState
                .select(state => state.folder.activeNode && state.entities.node[state.folder.activeNode])
                .filter(Boolean)
                .take(1)
                .map((node: Node) => node.id)
                .toPromise();
        }

        return nodePromise
            .then(nodeId => this.api.folders.getLanguagesOfNode(nodeId).toPromise())
            .then(res => {
                this.appState.dispatch(new LanguageFetchingSuccessAction(res.languages, res.numItems, res.hasMoreItems));
                return res.languages;
            })
            .catch(error => {
                this.appState.dispatch(new ListFetchingErrorAction('activeNodeLanguages', error.message));
                this.errorHandler.catch(error);
                return null;
            });
    }

    /**
     * Convert a File object into an Image by adding the missing properties. This is needed because the
     * `image/resize` and `file/create` endpoints returns a file object rather than an image.
     */
    private fileToImage(file: File, width: number, height: number, sourceImage?: Image, resizeParams?: CropResizeParameters): Image<Raw> {
        const imageProperties: any = {
            type: 'image',
            sizeX: width,
            sizeY: height,
        };

        if (resizeParams && resizeParams.fpX && resizeParams.fpY) {
            imageProperties.fpX = resizeParams.fpX;
            imageProperties.fpY = resizeParams.fpY;
        }

        let sourceProperties: any = {};
        if (sourceImage) {
            sourceProperties = {
                dpiX: sourceImage.dpiX,
                dpiY: sourceImage.dpiY,
                gisResizable: sourceImage.gisResizable,
            };
        }

        return Object.assign({}, file, imageProperties, sourceProperties) as Image<Raw>;
    }

    /**
     * Returns the nodeId of the currently-active node.
     */
    private getCurrentNodeId(): number {
        const state = this.appState.now;
        return state.folder.activeNode;
    }

    /**
     * @param formId of form
     * @returns latest version of form
     */
    private async getFormVersionPlanned(formId: number): Promise<string> {
        const options: FormRequestOptions = {
            nodeId: this.getCurrentNodeId(),
        };
        const response = await this.api.folders.getItem(formId, 'form', options).toPromise();
        let versionPlanned: string;
        if (response.item.timeManagement.version) {
            versionPlanned = response.item.timeManagement.version.number;
        } else if (response.item.version) {
            versionPlanned = response.item.version.number;
        } else {
            versionPlanned = undefined;
        }
        return versionPlanned;
    }

    /**
     * @param pageId of page
     * @returns latest version of page
     */
    private async getPageVersionPlanned(pageId: number): Promise<string> {
        const options: PageRequestOptions = {
            nodeId: this.getCurrentNodeId(),
            versioninfo: true,
            langvars: true,
        };
        const response = await this.api.folders.getItem(pageId, 'page', options).toPromise();
        let versionPlanned: string;
        if (response.page.timeManagement.version) {
            versionPlanned = response.page.timeManagement.version.number;
        } else {
            const versions = response.page.versions;
            versionPlanned = versions[versions.length - 1].number;
        }
        return versionPlanned;
    }

    private nodeFeatureIsActive(nodeId: number, nodeFeature: keyof NodeFeatures): Observable<boolean> {
        return this.appState.select(state => state.features.nodeFeatures).pipe(
            map(nodeFeatures => {
                const activeNodeFeatures: (keyof NodeFeatures)[] = nodeFeatures[nodeId];
                return Array.isArray(activeNodeFeatures) && activeNodeFeatures.includes(nodeFeature);
            }),
        );
    }

    private getSkipCount(pageNumber: number, itemsPerPage: number): number {
        if (itemsPerPage === -1 || pageNumber <= 1) {
            return 0;
        }

        return itemsPerPage * (pageNumber - 1);
    }
}

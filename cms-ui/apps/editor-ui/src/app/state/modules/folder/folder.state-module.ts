import { Injectable } from '@angular/core';
import {
    folderSchema,
    imageSchema,
    languageSchema,
    nodeSchema,
} from '@editor-ui/app/common/models';
import { defaultUserSettings } from '@editor-ui/app/core/models';
import {
    File,
    Folder,
    FolderItemOrTemplateType,
    Form,
    Image,
    ItemType,
    Node,
    Normalized,
    Page,
    Raw,
    folderItemTypes,
} from '@gentics/cms-models';
import { StateContext } from '@ngxs/store';
import { append, compose, iif, patch } from '@ngxs/store/operators';
import { normalize, schema as schemaNamespace } from 'normalizr';
import { MAX_RECENT_ITEMS } from '../../../common/config/config';
import {
    ChannelSyncReport,
    FolderState,
    FolderStateItemListKey,
    GtxChipSearchProperties,
    GtxChipSearchSearchFilterMap,
    ItemsInfo,
    RecentItem,
    RecentItemMode,
    emptyItemInfo,
    plural,
} from '../../../common/models';
import { deepEqual } from '../../../common/utils/deep-equal';
import { ApplicationStateService } from '../../providers';
import { ActionDefinition, AppStateBranch, concatUnique, getNormalizrSchema } from '../../state-utils';
import { FocusListAction } from '../editor/editor.actions';
import { AddEntitiesAction, UpdateEntitiesAction } from '../entity/entity.actions';
import {
    AddEditedEntityToRecentItemsAction,
    AddToRecentItemsAction,
    ChangeListSelectionAction,
    ChannelSyncReportFetchingErrorAction,
    ChannelSyncReportFetchingSuccessAction,
    CreateItemSuccessAction,
    EditImageSuccessAction,
    FOLDER_STATE_KEY,
    InheritanceFetchingSuccessAction,
    ItemFetchingSuccessAction,
    LanguageFetchingSuccessAction,
    ListCreatingErrorAction,
    ListCreatingSuccessAction,
    ListDeletingErrorAction,
    ListDeletingSuccessAction,
    ListFetchingErrorAction,
    ListFetchingSuccessAction,
    ListSavingErrorAction,
    ListSavingSuccessAction,
    NodeFetchingSuccessAction,
    RecentItemsFetchingSuccessAction,
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
    SetListPackageAction,
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
    StartChannelSyncReportFetchingAction,
    StartListCreatingAction,
    StartListDeletingAction,
    StartListFetchingAction,
    StartListSavingAction,
    UpdateSearchFilterAction,
} from './folder.actions';

type CacheTypePlural = keyof CachedFolderContents;
const cacheTypesPlural = ['breadcrumbs', 'files', 'folders', 'images', 'pages', 'templates'] as CacheTypePlural[];

interface FolderCache {
    [folderId: number]: CachedFolderContents;
}

type CachedFolderContents = Record<FolderStateItemListKey, CachedItemsInfo>;

type CachedItemsInfo = Pick<ItemsInfo, 'fetchAll' | 'hasMore' | 'list' | 'total'>;

/** When large lists are loaded (e.g. 1000 pages), process them in batches to prevent the UI from freezing. */
const LIST_BATCH_SIZE = 20;

const INITIAL_FOLDER_STATE: FolderState = {
    activeFolder: null,
    activeLanguage: null,
    activeFormLanguage: null,
    activeNode: null,
    activeNodeLanguages: { ...emptyItemInfo },
    breadcrumbs: { ...emptyItemInfo },
    channelSyncReport: {
        folders: [],
        forms: [],
        pages: [],
        images: [],
        files: [],
        templates: [],
        fetching: false,
    },
    displayAllLanguages: false,
    displayStatusIcons: false,
    displayDeleted: false,
    displayImagesGridView: true,
    files: { ...emptyItemInfo },
    filterTerm: '',
    folders: { ...emptyItemInfo },
    forms: { ...emptyItemInfo },
    images: { ...emptyItemInfo },
    lastError: '',
    nodes: { ...emptyItemInfo },
    pages: { ...emptyItemInfo },
    recentItems: [],
    searchTerm: '',
    searchFilters: {},
    searchFiltersChanging: false,
    searchFiltersValid: false,
    searchFiltersVisible: false,
    templates: { ...emptyItemInfo },
};

@AppStateBranch<FolderState>({
    name: FOLDER_STATE_KEY,
    defaults: INITIAL_FOLDER_STATE,
})
@Injectable()
export class FolderStateModule {

    protected folderCache: FolderCache = {};

    constructor(
        private appState: ApplicationStateService,
    ) {}

    @ActionDefinition(AddEditedEntityToRecentItemsAction)
    public handleAddEditedEntityToRecentItemsAction(ctx: StateContext<FolderState>, action: AddEditedEntityToRecentItemsAction): void {
        const state = this.appState.now;
        const { editMode, itemId, itemType, nodeId } = state.editor;
        if (itemType === 'node') {
            return;
        }

        let mode: RecentItemMode = 'preview';
        if (editMode === 'edit') {
            mode = 'edit';
        } else if (editMode === 'editProperties') {
            mode = 'properties';
        }

        const newEntry: RecentItem = {
            id: itemId,
            mode,
            name: itemType,
            nodeId,
            time: new Date().toISOString(),
            type: itemType,
        };

        this.handleAddToRecentItemsAction(ctx, new AddToRecentItemsAction(newEntry));
    }

    @ActionDefinition(AddToRecentItemsAction)
    handleAddToRecentItemsAction(ctx: StateContext<FolderState>, action: AddToRecentItemsAction): void {
        const entityState = this.appState.now.entities;
        const { id, nodeId, type, mode } = action.item;
        const { recentItems } = ctx.getState();

        const lastAdded = recentItems[0];

        if (lastAdded && lastAdded.id === id && lastAdded.nodeId === nodeId && lastAdded.type === type && lastAdded.mode === mode) {
            return;
        }

        const newEntry: RecentItem = {
            id,
            mode,
            type,
            nodeId,
            name: action.item.name || type,
            time: action.item.time || new Date().toISOString(),
        };

        const newRecentItems = [newEntry, ...recentItems]
            .filter(item => {
                if (!item) {
                    console.error('Invalid recent item: ', item);
                    return false;
                }

                return true;
            })
            .map(item => {
                // Update the name of all recent items, e.g. when a page was renamed
                let entity: File<Normalized> | Folder<Normalized> | Form<Normalized> | Image<Normalized> | Page<Normalized>;

                if (entityState && entityState[item.type]) {
                    entity = entityState[item.type][item.id];
                }

                if (entity && entity.name !== item.name) {
                    return { ...item, name: entity.name };
                } else {
                    return item;
                }
            })
            .slice(0, MAX_RECENT_ITEMS);

        ctx.patchState({
            recentItems: newRecentItems,
        });
    }

    /**
     * Generic method when an item list starts to be fetched from the server.
     * If folder contents for the current folder are cached, they are applied until loaded from the server.
     */
    @ActionDefinition(StartListFetchingAction)
    handleStartListFetchingAction(ctx: StateContext<FolderState>, action: StartListFetchingAction): void {
        // Making sure it's always the plural version, if available
        let key: FolderStateItemListKey = plural[action.type as any] || action.type;
        const state = ctx.getState();

        // Pre-set from cache
        let cached: CachedItemsInfo | null = null;
        if (!action.skipCache && state.activeFolder != null && this.folderCache[state.activeFolder]) {
            cached = this.folderCache[state.activeFolder][key];
        }

        ctx.setState(patch<FolderState>({
            [key]: iif(cached != null,
                patch(cached),
                patch({
                    fetching: true,
                    fetchAll: iif(action.fetchAll != null, action.fetchAll),
                }),
            ),
        }));
    }

    /** Generic method when an item list was fetched from the server. */
    @ActionDefinition(ListFetchingSuccessAction)
    async handleListFetchingSuccessAction(ctx: StateContext<FolderState>, action: ListFetchingSuccessAction): Promise<void> {
        const type: FolderStateItemListKey = plural[action.type] || action.type;

        // If there's no result/items to process, then simply set the fetching status
        if (!action.result || !Array.isArray(action.result.items)) {
            ctx.setState(patch<FolderState>({
                [type]: patch<ItemsInfo>({
                    fetching: false,
                }),
            }));
            return;
        }

        const { fetchAll, folderId, hasMore, items, nodeId, schema, total } = action.result;
        let batch = 0;

        const state = ctx.getState();
        let hasMoreBatches = true;
        let hasFinished = false;

        while (hasMoreBatches) {
            const itemsWithThisBatch = action.skipBatching ? total : Math.min(total, (batch + 1) * LIST_BATCH_SIZE);
            const slice = items.slice(batch * LIST_BATCH_SIZE, itemsWithThisBatch);

            // Update looped variables
            batch++;
            hasMoreBatches = itemsWithThisBatch < items.length;

            // Entities can always be applied to the entity state.
            const normalized = normalize(slice, new schemaNamespace.Array(schema));
            await ctx.dispatch(new AddEntitiesAction(normalized)).toPromise();

            let idsToSaveToFolderState: number[];
            if (hasMoreBatches) {
                // A list of IDs that is pre-filled with the ID of item 1
                // and part by part filled with the real ids.
                idsToSaveToFolderState = [
                    ...items.slice(0, itemsWithThisBatch).map(item => item.id),
                    ...new Array(Math.max(0, total - itemsWithThisBatch)).fill(items[0].id),
                ];
            } else {
                // All pages are finished
                idsToSaveToFolderState = items.map(item => item.id);
            }

            // Apply the batch to the list state if the user did not change folder / node.
            if (state.activeFolder === folderId && state.activeNode === nodeId) {
                // calculate correct page to display after items reload
                // to prevent current page to be higher than max pages returned and thus display an empty page
                const totalpages = Math.ceil(total / state[type].itemsPerPage);
                const currentPage: number = state[type].currentPage;
                const newCurrentPage = currentPage + 1 > totalpages ? totalpages : currentPage;

                // If there's no more batches to process, then we finalize the fetching as well
                const finishNow = !hasMoreBatches;
                if (finishNow) {
                    hasFinished = true;
                }

                ctx.setState(patch<FolderState>({
                    [type]: patch<ItemsInfo>({
                        fetchAll: fetchAll,
                        hasMore: hasMore,
                        list: idsToSaveToFolderState,
                        total: total,
                        currentPage: newCurrentPage,
                        fetching: iif(finishNow, false),
                    }),
                }));
            }

            // Always apply to the folder cache when loading folders/pages/files/images.
            if (cacheTypesPlural.indexOf(type) >= 0) {
                this.folderCache = updateCache(this.folderCache, folderId, type, {
                    fetchAll,
                    hasMore,
                    list: idsToSaveToFolderState,
                    total,
                });
            }
        }

        if (!hasFinished) {
            ctx.setState(patch<FolderState>({
                [type]: patch<ItemsInfo>({
                    fetching: false,
                }),
            }));
        }
    }

    /** Generic method when an error was received when fetching an item list */
    @ActionDefinition(ListFetchingErrorAction)
    handleListFetchingErrorAction(ctx: StateContext<FolderState>, action: ListFetchingErrorAction): void {
        const type = plural[action.type] || action.type;

        ctx.setState(patch({
            [type]: iif<ItemsInfo>(action.keepList,
                patch({ fetching: false }),
                patch({ ...emptyItemInfo }),
            ),
            lastError: action.errorMessage,
        }));
    }

    @ActionDefinition(SetListPageAction)
    handleSetListPageAction(ctx: StateContext<FolderState>, action: SetListPageAction): void {
        const type = plural[action.type] || action.type;

        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                currentPage: action.currentPage,
            }),
        }));
    }

    @ActionDefinition(SetListPerPageAction)
    handleSetListPerPageAction(ctx: StateContext<FolderState>, action: SetListPerPageAction): void {
        const type = plural[action.type] || action.type;

        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                itemsPerPage: action.itemsPerPage,
            }),
        }));
    }

    @ActionDefinition(StartListSavingAction)
    handleStartListSavingAction(ctx: StateContext<FolderState>, action: StartListSavingAction): void {
        const type = plural[action.type] || action.type;

        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                saving: true,
            }),
        }));
    }

    @ActionDefinition(ListSavingSuccessAction)
    handleListSavingSuccessAction(ctx: StateContext<FolderState>, action: ListSavingSuccessAction): void {
        const type = plural[action.type] || action.type;

        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                saving: false,
            }),
        }));
    }

    @ActionDefinition(ListSavingErrorAction)
    handleListSavingErrorAction(ctx: StateContext<FolderState>, action: ListSavingErrorAction): void {
        const type = plural[action.type] || action.type;

        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                saving: false,
            }),
            lastError: action.errorMessage,
        }));
    }

    @ActionDefinition(StartListCreatingAction)
    handleStartListCreatingAction(ctx: StateContext<FolderState>, action: StartListCreatingAction): void {
        const type = plural[action.type] || action.type;

        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                creating: true,
            }),
        }));
    }

    @ActionDefinition(ListCreatingSuccessAction)
    handle(ctx: StateContext<FolderState>, action: ListCreatingSuccessAction): void {
        const type = plural[action.type] || action.type;

        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                creating: false,
            }),
        }));
    }

    @ActionDefinition(ListCreatingErrorAction)
    handleListCreatingErrorAction(ctx: StateContext<FolderState>, action: ListCreatingErrorAction): void {
        const type = plural[action.type] || action.type;

        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                creating: false,
            }),
            lastError: action.errorMessage,
        }));
    }

    @ActionDefinition(SetListPackageAction)
    handleSetListPackageAction(ctx: StateContext<FolderState>, action: SetListPackageAction): void {
        const type = plural[action.type] || action.type;

        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                activePackage: action.activePackage,
            }),
        }));
    }

    @ActionDefinition(CreateItemSuccessAction)
    async handleCreateItemSuccessAction<T extends FolderItemOrTemplateType>(
        ctx: StateContext<FolderState>,
        action: CreateItemSuccessAction<T>,
    ): Promise<void> {
        const state = ctx.getState();
        const typeKey = plural[action.type];

        const normalized = normalize(action.items, new schemaNamespace.Array(getNormalizrSchema(action.type)));

        await ctx.dispatch(new AddEntitiesAction(normalized)).toPromise();

        ctx.setState(patch<FolderState>({
            [typeKey]: compose(
                patch<ItemsInfo>({
                    creating: false,
                }),
                iif(action.addToList, patch<ItemsInfo>({
                    list: append(normalized.result),
                    total: (state[typeKey] as ItemsInfo).total + action.items.length,
                })),
            ),
        }));
    }

    @ActionDefinition(StartListDeletingAction)
    handleStartListDeletingAction(ctx: StateContext<FolderState>, action: StartListDeletingAction): void {
        const type = plural[action.type] || action.type;
        const state: ItemsInfo = ctx.getState()[type];

        ctx.setState(patch({
            [type]: patch<ItemsInfo>({
                deleting: concatUnique(state.deleting, action.itemIds),
                list: state.list.filter(id => !action.itemIds.includes(id)),
            }),
        }));
    }

    @ActionDefinition(ListDeletingSuccessAction)
    handleListDeletingSuccessAction(ctx: StateContext<FolderState>, action: ListDeletingSuccessAction): void {
        const type = plural[action.type] || action.type;
        const state: ItemsInfo = ctx.getState()[type];

        ctx.setState(patch({
            [type]: patch<ItemsInfo>({
                deleting: state.deleting.filter((id) => !action.deletedItemIds.includes(id)),
            }),
        }));
    }

    @ActionDefinition(ListDeletingErrorAction)
    handleListDeletingErrorAction(ctx: StateContext<FolderState>, action: ListDeletingErrorAction): void {
        const type = plural[action.type] || action.type;
        const state: ItemsInfo = ctx.getState()[type];

        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                deleting: state.deleting.filter((id) => !action.deletedItemIds.includes(id)),
                list: concatUnique(state.list, action.deletedItemIds),
            }),
            lastError: action.errorMessage,
        }));
    }

    @ActionDefinition(EditImageSuccessAction)
    async handleEditImageSuccessAction(ctx: StateContext<FolderState>, action: EditImageSuccessAction): Promise<void> {
        const state = ctx.getState().images;
        const normalized = normalize(action.image, imageSchema);

        await ctx.dispatch(new AddEntitiesAction(normalized)).toPromise();

        ctx.setState(patch<FolderState>({
            images: patch<ItemsInfo>({
                saving: false,
                list: concatUnique(state.list, [action.image.id]),
                total: state.total + (state.list.includes(action.image.id) ? 0 : 1),
            }),
        }));
    }

    @ActionDefinition(InheritanceFetchingSuccessAction)
    async handleInheritanceFetchingSuccessAction(ctx: StateContext<FolderState>, action: InheritanceFetchingSuccessAction): Promise<void> {
        const type = plural[action.type] || action.type;

        // Update the item with the data returned by the `<type>/disinherit` endpoint
        const updatedProps: Partial<File<Raw> | Folder<Raw> | Image<Raw> | Page<Raw>> = {
            ...action.result,
        };

        await ctx.dispatch(new UpdateEntitiesAction({
            [action.type]: {
                [action.itemId]: updatedProps,
            },
        })).toPromise();

        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                fetching: false,
            }),
        }));
    }

    @ActionDefinition(ItemFetchingSuccessAction)
    async handleItemFetchingSuccessAction<T extends ItemType>(ctx: StateContext<FolderState>, action: ItemFetchingSuccessAction<T>): Promise<void> {
        const normalized = normalize(action.item, getNormalizrSchema(action.type));
        await ctx.dispatch(new AddEntitiesAction(normalized)).toPromise();

        const type = plural[action.type] || action.type;
        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                fetching: false,
            }),
        }));
    }

    @ActionDefinition(LanguageFetchingSuccessAction)
    async handleLanguageFetchingSuccessAction(ctx: StateContext<FolderState>, action: LanguageFetchingSuccessAction): Promise<void> {
        const normalized = normalize(action.languages, new schemaNamespace.Array(languageSchema));
        await ctx.dispatch(new AddEntitiesAction(normalized)).toPromise();

        ctx.setState(patch({
            activeNodeLanguages: patch({
                fetchAll: false,
                fetching: false,
                list: [].concat(normalized.result),
                hasMore: action.hasMore,
                total: action.total,
            }),
        }));
    }

    @ActionDefinition(NodeFetchingSuccessAction)
    async handleNodeFetchingSuccessAction(ctx: StateContext<FolderState>, action: NodeFetchingSuccessAction): Promise<void> {
        const normalizedFolders = normalize(action.folders, new schemaNamespace.Array(folderSchema));
        const normalizedNodes = normalize(action.nodes, new schemaNamespace.Array(nodeSchema));
        await Promise.all([
            ctx.dispatch(new AddEntitiesAction(normalizedFolders)).toPromise(),
            ctx.dispatch(new AddEntitiesAction(normalizedNodes)).toPromise(),
        ]);

        const state = ctx.getState();
        const entityState = this.appState.now.entities;
        const activeNode: Node<Normalized> | null = entityState.node[state.activeNode];
        const diff: Partial<FolderState> = {};

        if (state.activeNode != null && state.activeFolder == null && activeNode != null ) {
            diff.activeFolder = activeNode.folderId;
        }

        ctx.setState(patch<FolderState>({
            nodes: patch<ItemsInfo>({
                fetching: false,
                hasMore: false,
                list: action.nodes.map(node => node.id),
                total: action.nodes.length,
            }),
            ...diff,
        }));
    }

    @ActionDefinition(StartChannelSyncReportFetchingAction)
    handleStartChannelSyncReportFetchingAction(ctx: StateContext<FolderState>, action: StartChannelSyncReportFetchingAction): void {
        ctx.setState(patch<FolderState>({
            channelSyncReport: patch<ChannelSyncReport>({
                fetching: true,
                folders: [],
                pages: [],
                files: [],
                images: [],
                templates: [],
            }),
        }));
    }

    @ActionDefinition(ChannelSyncReportFetchingSuccessAction)
    async handleChannelSyncReportFetchingSuccessAction(
        ctx: StateContext<FolderState>,
        action: ChannelSyncReportFetchingSuccessAction,
    ): Promise<void> {
        let adders: Promise<any>[] = [];
        Object.entries(action.report).forEach(([itemType, items]) => {
            const normalized = normalize(items, new schemaNamespace.Array(getNormalizrSchema(itemType)));
            adders.push(ctx.dispatch(new AddEntitiesAction(normalized)).toPromise());
        });

        await Promise.all(adders);

        ctx.setState(patch<FolderState>({
            channelSyncReport: patch<ChannelSyncReport>({
                ...action.report,
                fetching: false,
            }),
        }));
    }

    @ActionDefinition(ChannelSyncReportFetchingErrorAction)
    handleChannelSyncReportFetchingErrorAction(ctx: StateContext<FolderState>, action: ChannelSyncReportFetchingErrorAction): void {
        ctx.setState(patch<FolderState>({
            channelSyncReport: patch<ChannelSyncReport>({
                fetching: false,
            }),
            lastError: action.errorMessage,
        }));
    }

    @ActionDefinition(RecentItemsFetchingSuccessAction)
    handleRecentItemsFetchingSuccessAction(ctx: StateContext<FolderState>, action: RecentItemsFetchingSuccessAction): void {
        if (!Array.isArray(action.items)) {
            action.items = [];
        }

        ctx.patchState({
            recentItems: action.items.filter(item => {
                if (item == null || typeof item !== 'object') {
                    return false;
                }
                const { id, type } = item;
                if (!folderItemTypes.includes(type) || typeof id !== 'number' || isNaN(id) || !isFinite(id)) {
                    return false;
                }

                return true;
            }),
        });
    }

    @ActionDefinition(SetActiveFolderAction)
    async handleSetActiveFolderAction(ctx: StateContext<FolderState>, action: SetActiveFolderAction): Promise<void> {
        const state = ctx.getState();

        await ctx.dispatch(new FocusListAction()).toPromise();

        if (Number.isInteger(action.folderId) && action.folderId !== state.activeFolder) {
            if (state.activeFolder && state.activeNode) {
                await ctx.dispatch(new AddToRecentItemsAction({
                    id: action.folderId,
                    mode: 'navigate',
                    nodeId: state.activeNode,
                    type: 'folder',
                })).toPromise();
            }

            ctx.setState(compose(
                patch({
                    activeFolder: action.folderId,
                }),
                resetPagingForAllTypes(),
            ));
        }
    }

    @ActionDefinition(SetFolderLanguageAction)
    handleSetFolderLanguageAction(ctx: StateContext<FolderState>, action: SetFolderLanguageAction): void {
        const state = ctx.getState();

        if (Number.isInteger(action.languageId) && action.languageId !== state.activeLanguage) {
            ctx.patchState({
                activeLanguage: action.languageId,
            });
        }
    }

    @ActionDefinition(SetFormLanguageAction)
    handleSetFormLanguageAction(ctx: StateContext<FolderState>, action: SetFormLanguageAction): void {
        const state = ctx.getState();

        if (Number.isInteger(action.languageId) && action.languageId !== state.activeLanguage) {
            ctx.patchState({
                activeFormLanguage: action.languageId,
            });
        }
    }

    @ActionDefinition(SetActiveNodeAction)
    async handleSetActiveNodeAction(ctx: StateContext<FolderState>, action: SetActiveNodeAction): Promise<void> {
        const state = ctx.getState();

        if (Number.isInteger(action.nodeId) && action.nodeId !== state.activeNode) {
            this.folderCache = { };
            ctx.patchState({
                activeNode: action.nodeId,
            });
        }
        await ctx.dispatch(new FocusListAction()).toPromise();
    }

    @ActionDefinition(SetDisplayAllLanguagesAction)
    handleSetDisplayAllLanguagesAction(ctx: StateContext<FolderState>, action: SetDisplayAllLanguagesAction): void {
        ctx.patchState({
            displayAllLanguages: action.displayAll,
        });
    }

    @ActionDefinition(SetDisplayStatusIconsAction)
    handleSetDisplayStatusIconsAction(ctx: StateContext<FolderState>, action: SetDisplayStatusIconsAction): void {
        ctx.patchState({
            displayStatusIcons: action.displayIcons,
        });
    }

    @ActionDefinition(SetDisplayDeletedAction)
    handleSetDisplayDeletedAction(ctx: StateContext<FolderState>, action: SetDisplayDeletedAction): void {
        ctx.patchState({
            displayDeleted: action.displayDeleted,
        });
    }

    @ActionDefinition(SetDisplayImagesGridViewAction)
    handleSetDisplayImagesGridViewAction(ctx: StateContext<FolderState>, action: SetDisplayImagesGridViewAction): void {
        ctx.patchState({
            displayImagesGridView: action.displayGrid,
        });
    }

    @ActionDefinition(SetListDisplayFieldsAction)
    handleSetListDisplayFieldsAction(ctx: StateContext<FolderState>, action: SetListDisplayFieldsAction): void {
        const type = plural[action.type] || action.type;
        let fields: string[] = [];
        if (Array.isArray(action.displayFields)) {
            fields = action.displayFields;
        } else if (typeof action.displayFields === 'string') {
            fields = [action.displayFields];
        }

        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                displayFields: Array.from(new Set<string>(fields))
                    .filter(val => typeof val === 'string' && !!val),
            }),
        }));
    }

    @ActionDefinition(SetRepositoryBrowserDisplayFieldsAction)
    handleSetRepositoryBrowserDisplayFieldsAction(ctx: StateContext<FolderState>, action: SetRepositoryBrowserDisplayFieldsAction): void {
        const type = plural[action.type] || action.type;

        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                displayFieldsRepositoryBrowser: action.displayFields,
            }),
        }));
    }

    @ActionDefinition(SetListShowPathAction)
    handleSetListShowPathAction(ctx: StateContext<FolderState>, action: SetListShowPathAction): void {
        const type = plural[action.type] || action.type;

        ctx.setState(patch<FolderState>({
            [type]: patch<ItemsInfo>({
                showPath: action.showPath,
            }),
        }));
    }

    @ActionDefinition(SetFilterTermAction)
    handleSetFilterTermAction(ctx: StateContext<FolderState>, action: SetFilterTermAction): void {
        ctx.setState(compose(
            patch({
                filterTerm: action.filterTerm,
            }),
            resetPagingForAllTypes(),
        ));
    }

    @ActionDefinition(SetSearchTermAction)
    handleSetSearchTermAction(ctx: StateContext<FolderState>, action: SetSearchTermAction): void {
        ctx.setState(compose(
            patch({
                searchTerm: action.searchTerm,
            }),
            resetPagingForAllTypes(),
        ));
    }

    @ActionDefinition(SetSearchFiltersChangingAction)
    handleSetSearchFiltersChangingAction(ctx: StateContext<FolderState>, action: SetSearchFiltersChangingAction): void {
        ctx.patchState({
            searchFiltersChanging: action.isChanging,
        });
    }

    @ActionDefinition(SetSearchFiltersValidAction)
    handleSetSearchFiltersValidAction(ctx: StateContext<FolderState>, action: SetSearchFiltersValidAction): void {
        ctx.patchState({
            searchFiltersValid: action.isValid,
        });
    }

    @ActionDefinition(SetSearchFiltersVisibleAction)
    handleSetSearchFiltersVisibleAction(ctx: StateContext<FolderState>, action: SetSearchFiltersVisibleAction): void {
        ctx.patchState({
            searchFiltersVisible: action.isVisible,
        });
    }

    @ActionDefinition(UpdateSearchFilterAction)
    handleUpdateSearchFilterAction(ctx: StateContext<FolderState>, action: UpdateSearchFilterAction): void {
        ctx.setState(patch({
            searchFiltersChanging: iif(action.settings.changing != null, action.settings.changing),
            searchFiltersValid: iif(action.settings.valid != null, action.settings.valid),
            searchFiltersVisible: iif(action.settings.visible != null, action.settings.visible),
        }));
    }

    @ActionDefinition(SetSearchFilterValueAction)
    handleSetSearchFilterValueAction<K extends keyof GtxChipSearchProperties>(ctx: StateContext<FolderState>, action: SetSearchFilterValueAction<K>): void {
        ctx.setState(patch<FolderState>({
            searchFilters: patch<GtxChipSearchSearchFilterMap>({
                [action.key]: action.value,
            }),
        }));
    }

    @ActionDefinition(SetListSortingAction)
    handleSetListSortingAction(ctx: StateContext<FolderState>, action: SetListSortingAction): void {
        const state = ctx.getState();
        const listKey = plural[action.type];

        if (!state[listKey]) {
            return;
        }

        let { sortBy, sortOrder } = action;

        if (typeof sortBy !== 'string' || sortBy.trim().length === 0) {
            sortBy = defaultUserSettings[`${action.type}Sorting`].sortBy;
        }

        if (sortOrder !== 'asc' && sortOrder !== 'desc') {
            sortOrder = defaultUserSettings[`${action.type}Sorting`].sortOrder;
        }

        ctx.setState(patch<FolderState>({
            [listKey]: patch<ItemsInfo>({
                sortBy: sortBy,
                sortOrder: sortOrder,
            }),
        }));
    }

    @ActionDefinition(ChangeListSelectionAction)
    handleChangeListSelectionAction(ctx: StateContext<FolderState>, action: ChangeListSelectionAction): void {
        const state = ctx.getState();
        const type = plural[action.type] || action.type;
        let op;

        switch (action.mode) {
            case 'replace':
                op = action.ids;
                break;

            case 'append':
                op = concatUnique((state[type].selected || []), action.ids);
                break;

            case 'remove':
                op = ((state[type].selected || []) as number[]).filter(id => !action.ids.includes(id));
                break;

            case 'clear':
                op = [];
                break;
        }

        if (!op) {
            return;
        }

        if (action.type == null) {
            ctx.setState(compose(...['files', 'folders', 'images', 'pages', 'forms'].map(actualType => {
                return patch<FolderState>({
                    [actualType]: patch<ItemsInfo>({
                        selected: op,
                    }),
                });
            })));
        } else {
            ctx.setState(patch<FolderState>({
                [type]: patch<ItemsInfo>({
                    selected: op,
                }),
            }));
        }
    }
}

function resetPagingForAllTypes() {
    const itemPatch = patch<ItemsInfo>({
        currentPage: 1,
    });

    return patch<FolderState>({
        folders: itemPatch,
        pages: itemPatch,
        images: itemPatch,
        files: itemPatch,
        forms: itemPatch,
    });
}

/** Updates the cache for a folder's contents with the items received from the server. */
function updateCache(
    oldCache: FolderCache,
    parentFolderId: number,
    typePlural: keyof CachedFolderContents,
    newEntry: CachedItemsInfo,
): FolderCache {

    const cached = oldCache[parentFolderId] && oldCache[parentFolderId][typePlural];
    if (cached && deepEqual(cached, newEntry)) {
        return oldCache;
    }

    return Object.assign({}, oldCache, {
        [parentFolderId]: Object.assign({}, oldCache[parentFolderId], {
            [typePlural]: newEntry,
        }),
    });
}

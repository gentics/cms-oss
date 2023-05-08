import {
    Folder,
    FolderItemOrTemplateType,
    FolderItemOrTemplateTypeMap,
    Image,
    ItemType,
    ItemTypeMap,
    Language,
    Node,
    Raw,
    SortField,
} from '@gentics/cms-models';
import { Schema } from 'normalizr';
import { AppState, ChannelSyncReport, DisplayFields, FolderStateItemListKey, GtxChipSearchProperties, RecentItem } from '../../../common/models';
import { ActionDeclaration } from '../../state-utils';

export interface ListFetchResult {
    folderId: number,
    nodeId: number,
    items: any[],
    schema: Schema,
    fetchAll?: boolean,
    hasMore: boolean,
    total: number,
}

export const FOLDER_STATE_KEY: keyof AppState = 'folder';

@ActionDeclaration(FOLDER_STATE_KEY)
export class UpdateSearchFilterAction {
    constructor(public settings: {
        changing?: boolean;
        valid?: boolean;
        visible?: boolean;
    }) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class AddEditedEntityToRecentItemsAction {}

@ActionDeclaration(FOLDER_STATE_KEY)
export class AddToRecentItemsAction {
    constructor(public item: Omit<RecentItem, 'time' | 'name'> & {
        time?: string,
        name?: string;
    }) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetActiveFolderAction {
    constructor(
        public folderId: number,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class StartListFetchingAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public fetchAll?: boolean,
        public skipCache: boolean = false,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class ListFetchingSuccessAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public result?: ListFetchResult,
        public skipBatching: boolean = false,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class ListFetchingErrorAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public errorMessage: string,
        public keepList: boolean = false,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetListPageAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public currentPage: number,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetListPerPageAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public itemsPerPage: number,
    ) {}
}


@ActionDeclaration(FOLDER_STATE_KEY)
export class SetListPackageAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public activePackage: string,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class StartListSavingAction {
    constructor(
        public type: 'node' | 'nodes' | FolderItemOrTemplateType | FolderStateItemListKey,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class ListSavingSuccessAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class ListSavingErrorAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public errorMessage: string,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class StartListCreatingAction {
    constructor(
        public type: 'node' | 'nodes' | FolderItemOrTemplateType | FolderStateItemListKey,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class ListCreatingSuccessAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class ListCreatingErrorAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public errorMessage: string,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class CreateItemSuccessAction<T extends FolderItemOrTemplateType> {
    constructor(
        public type: T,
        public items: FolderItemOrTemplateTypeMap<Raw>[T][],
        public addToList: boolean = false,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class StartListDeletingAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public itemIds: number[],
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class ListDeletingSuccessAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public deletedItemIds: number[],
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class ListDeletingErrorAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public deletedItemIds: number[],
        public errorMessage: string,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class EditImageSuccessAction {
    constructor(
        public image: Image<Raw>,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class InheritanceFetchingSuccessAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public itemId: number,
        public result: {
            disinherit: number[],
            disinheritDefault: boolean,
            excluded: boolean,
            inheritable: number[],
        },
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class ItemFetchingSuccessAction<T extends ItemType> {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public item: ItemTypeMap<Raw>[T],
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class LanguageFetchingSuccessAction {
    constructor(
        public languages: Language[],
        public total: number = languages.length,
        public hasMore: boolean = false,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class NodeFetchingSuccessAction {
    constructor(
        public folders: Folder<Raw>[],
        public nodes: Node<Raw>[],
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class StartChannelSyncReportFetchingAction {}

@ActionDeclaration(FOLDER_STATE_KEY)
export class ChannelSyncReportFetchingSuccessAction {
    constructor(
        public report: Partial<Omit<ChannelSyncReport, 'fetching'>>,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class ChannelSyncReportFetchingErrorAction {
    constructor(
        public errorMessage: string,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class RecentItemsFetchingSuccessAction {
    constructor(
        public items: RecentItem[],
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetFolderLanguageAction {
    constructor(
        public languageId: number,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetFormLanguageAction {
    constructor(
        public languageId: number,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetActiveNodeAction {
    constructor(
        public nodeId: number,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetDisplayAllLanguagesAction {
    constructor(
        public displayAll: boolean,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetDisplayStatusIconsAction {
    constructor(
        public displayIcons: boolean,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetDisplayDeletedAction {
    constructor(
        public displayDeleted: boolean,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetDisplayImagesGridViewAction {
    constructor(
        public displayGrid: boolean,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetListDisplayFieldsAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public displayFields: string[],
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetRepositoryBrowserDisplayFieldsAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public displayFields: DisplayFields,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetListShowPathAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey,
        public showPath: boolean,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetListSortingAction {
    constructor(
        public type: FolderItemOrTemplateType,
        public sortBy: SortField,
        public sortOrder: 'asc' | 'desc',
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetFilterTermAction {
    constructor(
        public filterTerm: string,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetSearchTermAction {
    constructor(
        public searchTerm: string,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetSearchFiltersChangingAction {
    constructor(
        public isChanging: boolean,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetSearchFiltersValidAction {
    constructor(
        public isValid: boolean,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetSearchFiltersVisibleAction {
    constructor(
        public isVisible: boolean,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class SetSearchFilterValueAction<K extends keyof GtxChipSearchProperties> {
    constructor(
        public key: K,
        public value: GtxChipSearchProperties[K] | null,
    ) {}
}

@ActionDeclaration(FOLDER_STATE_KEY)
export class ChangeListSelectionAction {
    constructor(
        public type: FolderItemOrTemplateType | FolderStateItemListKey | null,
        public mode: 'replace' | 'append' | 'remove' | 'clear',
        public ids?: number[],
    ) {}
}

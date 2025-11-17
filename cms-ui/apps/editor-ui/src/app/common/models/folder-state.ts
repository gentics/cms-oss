import { FolderItemType, SortField } from '@gentics/cms-models';
import { GtxChipSearchSearchFilterMap } from '../../common/models';

export interface DisplayFields {
    selection: string[];
    showPath: boolean;
}

export interface ItemsInfo {
    list: number[];
    showPath: boolean;
    selected: number[];
    total: number;
    hasMore: boolean;
    fetchAll: boolean;
    displayFields?: string[];
    displayFieldsRepositoryBrowser?: DisplayFields;
    sortBy?: SortField;
    sortOrder?: 'asc' | 'desc';
    creating: boolean;
    fetching: boolean;
    saving: boolean;
    deleting: number[];
    currentPage: number;
    itemsPerPage: number;
    activePackage?: string;
}

export interface FolderState {
    folders: ItemsInfo;
    forms: ItemsInfo;
    pages: ItemsInfo;
    images: ItemsInfo;
    files: ItemsInfo;
    templates: ItemsInfo;
    nodes: ItemsInfo;
    nodesLoaded: boolean;
    activeFolder: number;
    activeNode: number;
    activeNodeLanguages: ItemsInfo;
    activeLanguage: number;
    activeFormLanguage: number;
    displayAllLanguages: boolean;
    displayStatusIcons: boolean;
    displayDeleted: boolean;
    displayImagesGridView: boolean;
    filterTerm: string;
    searchTerm: string;
    searchFilters: GtxChipSearchSearchFilterMap;
    searchFiltersChanging: boolean;
    searchFiltersVisible: boolean;
    searchFiltersValid: boolean;
    recentItems: RecentItem[];
    breadcrumbs: ItemsInfo;
    lastError?: string;
    channelSyncReport: ChannelSyncReport;
    userSettingsLoaded: boolean;
    folderStateLoaded: boolean;
}

export interface RecentItem {
    id: number;
    type: FolderItemType;
    mode: RecentItemMode;
    name: string;
    nodeId: number;
    time: string;
}

export type RecentItemMode = 'navigate' | 'preview' | 'edit' | 'properties';

/** All FolderState properties which are an `ItemsInfo` */
export type FolderStateItemListKey =
    keyof Pick<FolderState, 'activeNodeLanguages' | 'breadcrumbs' | 'files' | 'folders' | 'forms' | 'images' | 'nodes' | 'pages' | 'templates'>;

export interface ChannelSyncReport {
    folders: number[];
    forms: number[];
    pages: number[];
    images: number[];
    files: number[];
    templates: number[];
    fetching: boolean;
}

/** "file" => "files", etc. */
export const plural = {
    file: 'files' as const,
    folder: 'folders' as const,
    form: 'forms' as const,
    image: 'images' as const,
    page: 'pages' as const,
    node: 'nodes' as const,
    template: 'templates' as const,
};

export const emptyItemInfo: ItemsInfo = {
    creating: false,
    deleting: [],
    fetchAll: false,
    fetching: false,
    hasMore: false,
    list: [],
    selected: [],
    saving: false,
    showPath: true,
    sortBy: 'name',
    sortOrder: 'asc',
    total: 0,
    currentPage: 1,
    itemsPerPage: 10,
};

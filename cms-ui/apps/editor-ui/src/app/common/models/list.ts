import { PagingSortOrder, StagedItemsMap } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { GtxChipSearchChipData, GtxChipSearchSearchFilterMap } from './chip-search';

/**
 * GCMSUI component `item-list-row` has different states
 * causing different component behavior.
 */
export enum ItemListRowMode {
    DEFAULT = 'DEFAULT',
    SELECT = 'SELECT',
}

export type UsageType
    = | 'page'
      | 'variant'
      | 'folder'
      | 'file'
      | 'image'
      | 'template'
      | 'tag';

export const USAGE_TYPES: UsageType[] = [
    'page',
    'variant',
    'folder',
    'file',
    'image',
    'template',
    'tag',
];

export type LinkType
    = | 'linkedPage'
      | 'linkedFile'
      | 'linkedImage';

export const LINK_TYPES: LinkType[] = [
    'linkedPage',
    'linkedFile',
    'linkedImage',
];

export interface ItemLoadData<T> {
    items: T[];
    totalCount: number;
    stagingData?: StagedItemsMap;
}

export interface ListLoadOptions {
    // Pagination settings
    page: number;
    pageSize: number;

    // References to where to load from
    folderId: number;
    nodeId: number;
    recursive?: boolean;

    // Sorting & Filtering
    sortBy?: string;
    sortOrder?: PagingSortOrder;
    searchFilters?: GtxChipSearchSearchFilterMap;
    searchString?: string;

    // List settings
    showDeleted?: boolean;
    package?: string;
    withUsage?: boolean;
}

export interface ItemListLoaderService<T, O extends ListLoadOptions = ListLoadOptions> {
    readonly reload$: Observable<void>;

    reload(): void;
    loadItems(options: O): Observable<ItemLoadData<T>>;
}
